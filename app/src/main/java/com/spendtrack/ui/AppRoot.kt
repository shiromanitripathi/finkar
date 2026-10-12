package com.spendtrack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.spendtrack.SpendViewModel
import com.spendtrack.data.Fmt
import com.spendtrack.data.Transaction

enum class Tab(val label: String, val emoji: String) {
    HOME("Home", "🏠"),
    PAYMENTS("Payments", "🧾"),
    FRIENDS("Friends", "👥"),
    PLAN("Plan", "🎯"),
    INSIGHTS("Insights", "📊")
}

@Composable
fun AppRoot(vm: SpendViewModel) {
    val tabs = Tab.values()
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var showAddFriend by remember { mutableStateOf(false) }

    val txs by vm.transactions.collectAsState()
    val friends by vm.friends.collectAsState()
    val shares by vm.shares.collectAsState()
    val splitId by vm.splitTargetId.collectAsState()
    val tab = tabs[tabIndex]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = i == tabIndex,
                        onClick = { tabIndex = i },
                        icon = { Text(t.emoji, fontSize = 20.sp) },
                        label = { Text(t.label, fontSize = 11.sp) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == Tab.HOME || tab == Tab.PAYMENTS) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Text("＋ Add", fontWeight = FontWeight.Bold) }
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner)) {
            when (tab) {
                Tab.HOME -> HomeScreen(
                    vm,
                    onSeeAll = { tabIndex = Tab.PAYMENTS.ordinal },
                    onOpenFriends = { tabIndex = Tab.FRIENDS.ordinal },
                    onOpenPlan = { tabIndex = Tab.PLAN.ordinal },
                    onOpenTx = { editing = it }
                )
                Tab.PAYMENTS -> TransactionsScreen(vm, onOpenTx = { editing = it })
                Tab.FRIENDS -> FriendsScreen(vm, onAddFriend = { showAddFriend = true })
                Tab.PLAN -> PlanScreen(vm)
                Tab.INSIGHTS -> InsightsScreen(vm)
            }
        }
    }

    if (showAdd) {
        TxEditorDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { amount, merchant, category, isIncome, note ->
                vm.addTransaction(amount, merchant, category, isIncome, note)
                showAdd = false
            }
        )
    }

    editing?.let { tx ->
        val owed = shares.filter { it.transactionId == tx.id }
        TxEditorDialog(
            initial = tx,
            onDismiss = { editing = null },
            onSave = { amount, merchant, category, isIncome, note ->
                vm.updateTransaction(tx, amount, merchant, category, isIncome, note)
                editing = null
            },
            onDelete = { vm.deleteTransaction(tx.id); editing = null },
            onSplit = { editing = null; vm.openSplit(tx.id) },
            splitInfo = if (owed.isEmpty()) null
            else "Split with ${owed.size} friend" + (if (owed.size > 1) "s" else "") +
                " · they owe " + Fmt.money(owed.sumOf { it.amount })
        )
    }

    val splitTx = splitId?.let { id -> txs.firstOrNull { it.id == id } }
    if (splitTx != null) {
        SplitDialog(
            tx = splitTx,
            friends = friends,
            existing = shares.filter { it.transactionId == splitTx.id }.associate { it.friendId to it.amount },
            onAddFriend = { showAddFriend = true },
            onDismiss = { vm.openSplit(null) },
            onSave = { amounts ->
                vm.saveSplit(splitTx.id, amounts)
                vm.openSplit(null)
            }
        )
    }

    if (showAddFriend) {
        AddFriendDialog(
            onDismiss = { showAddFriend = false },
            onSave = { name, phone ->
                vm.addFriend(name, phone)
                showAddFriend = false
            }
        )
    }
}
