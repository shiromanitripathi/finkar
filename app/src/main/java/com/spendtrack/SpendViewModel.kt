package com.spendtrack

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spendtrack.data.AppDatabase
import com.spendtrack.data.Budget
import com.spendtrack.data.Categories
import com.spendtrack.data.Friend
import com.spendtrack.data.FriendBalance
import com.spendtrack.data.Fmt
import com.spendtrack.data.Goal
import com.spendtrack.data.Ledger
import com.spendtrack.data.Payback
import com.spendtrack.data.SplitShare
import com.spendtrack.data.Transaction
import com.spendtrack.data.UiTx
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpendViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).dao()
    private val prefs = app.getSharedPreferences("spendtrack", Context.MODE_PRIVATE)

    private fun <T> kotlinx.coroutines.flow.Flow<T>.hold(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    val transactions: StateFlow<List<Transaction>> = dao.transactions().hold(emptyList())
    val friends: StateFlow<List<Friend>> = dao.friends().hold(emptyList())
    val shares: StateFlow<List<SplitShare>> = dao.shares().hold(emptyList())
    val paybacks: StateFlow<List<Payback>> = dao.paybacks().hold(emptyList())
    val budgets: StateFlow<List<Budget>> = dao.budgets().hold(emptyList())
    val goals: StateFlow<List<Goal>> = dao.goals().hold(emptyList())

    val uiTxs: StateFlow<List<UiTx>> = combine(transactions, shares) { txs, sh ->
        val owed = sh.groupBy { it.transactionId }.mapValues { e -> e.value.sumOf { it.amount } }
        txs.map { UiTx(it, owed[it.id] ?: 0.0) }
    }.hold(emptyList())

    val balances: StateFlow<List<FriendBalance>> =
        combine(friends, shares, paybacks) { f, s, p -> Ledger.balances(f, s, p) }
            .hold(emptyList())

    /** Transaction currently being split (opened from the list or from the payment pop-up). */
    val splitTargetId = MutableStateFlow<Long?>(null)

    val userName = MutableStateFlow(prefs.getString("name", "") ?: "")
    val upiId = MutableStateFlow(prefs.getString("upi", "") ?: "")

    fun saveProfile(name: String, upi: String) {
        prefs.edit().putString("name", name.trim()).putString("upi", upi.trim()).apply()
        userName.value = name.trim()
        upiId.value = upi.trim()
    }

    // ---- Transactions ----

    fun addTransaction(
        amount: Double, merchant: String, category: String, isIncome: Boolean, note: String
    ) = viewModelScope.launch {
        dao.insert(
            Transaction(
                amount = amount, merchant = merchant, category = category,
                isIncome = isIncome, note = note
            )
        )
    }

    fun updateTransaction(
        old: Transaction, amount: Double, merchant: String,
        category: String, isIncome: Boolean, note: String
    ) = viewModelScope.launch {
        dao.update(
            old.copy(
                amount = amount, merchant = merchant, category = category,
                isIncome = isIncome, note = note
            )
        )
        // Keep friends' shares in proportion if the total changed.
        if (old.amount > 0 && amount != old.amount) dao.scaleShares(old.id, amount / old.amount)
    }

    fun deleteTransaction(id: Long) = viewModelScope.launch {
        dao.deleteSharesForTransaction(id)
        dao.deleteTransaction(id)
    }

    /** Called when the user taps "Split" on the payment pop-up. */
    fun fromPrompt(amount: Double, merchant: String, source: String, raw: String) =
        viewModelScope.launch {
            val id = dao.insert(
                Transaction(
                    amount = amount, merchant = merchant,
                    category = Categories.guess(merchant), source = source, rawText = raw
                )
            )
            splitTargetId.value = id
        }

    // ---- Friends and splits ----

    fun addFriend(name: String, phone: String) = viewModelScope.launch {
        dao.insertFriend(Friend(name = name.trim(), phone = phone.trim()))
    }

    fun deleteFriend(id: Long) = viewModelScope.launch {
        dao.deleteSharesForFriend(id)
        dao.deletePaybacksForFriend(id)
        dao.deleteFriend(id)
    }

    fun openSplit(txId: Long?) {
        splitTargetId.value = txId
    }

    /** [amounts] maps friendId to the amount that friend owes for this transaction. */
    fun saveSplit(txId: Long, amounts: Map<Long, Double>) = viewModelScope.launch {
        dao.deleteSharesForTransaction(txId)
        val rows = amounts.filter { it.value > 0.0 }.map { (friendId, amt) ->
            SplitShare(transactionId = txId, friendId = friendId, amount = Fmt.round2(amt))
        }
        if (rows.isNotEmpty()) dao.insertShares(rows)
    }

    fun recordPayback(friendId: Long, amount: Double, note: String) = viewModelScope.launch {
        dao.insertPayback(Payback(friendId = friendId, amount = amount, note = note.trim()))
    }

    // ---- Budgets and goals ----

    fun setBudget(category: String, limit: Double) = viewModelScope.launch {
        dao.upsertBudget(Budget(category, limit))
    }

    fun removeBudget(category: String) = viewModelScope.launch { dao.deleteBudget(category) }

    fun addGoal(name: String, target: Double) = viewModelScope.launch {
        dao.insertGoal(Goal(name = name.trim(), target = target))
    }

    fun addToGoal(goal: Goal, amount: Double) = viewModelScope.launch {
        dao.updateGoal(goal.copy(saved = goal.saved + amount))
    }

    fun deleteGoal(id: Long) = viewModelScope.launch { dao.deleteGoal(id) }

    // ---- Export ----

    fun exportCsv(): String {
        fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder("Date,Type,Paid to / From,Category,Amount,Friends owe,Note\n")
        uiTxs.value.sortedBy { it.tx.timestamp }.forEach {
            sb.append(fmt.format(Date(it.tx.timestamp))).append(',')
                .append(if (it.tx.isIncome) "Income" else "Expense").append(',')
                .append(esc(it.tx.merchant)).append(',')
                .append(esc(it.tx.category)).append(',')
                .append(it.tx.amount).append(',')
                .append(it.owed).append(',')
                .append(esc(it.tx.note)).append('\n')
        }
        return sb.toString()
    }
}
