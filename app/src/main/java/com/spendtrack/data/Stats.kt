package com.spendtrack.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** A transaction plus how much of it friends owe. Only "my share" counts as my spending. */
data class UiTx(val tx: Transaction, val owed: Double) {
    val myShare: Double get() = tx.amount - owed
}

data class MonthStats(
    val spent: Double,
    val income: Double,
    val budget: Double?,
    val daysLeft: Int,
    val byCategory: List<Pair<String, Double>>,
    val last7: List<Pair<String, Double>>,
    val topMerchants: List<Pair<String, Double>>
) {
    val left: Double? get() = budget?.let { it - spent }
    val safePerDay: Double? get() = left?.let { if (it <= 0.0) 0.0 else it / daysLeft }
}

object Stats {

    fun month(all: List<UiTx>, budget: Double?, now: Long = System.currentTimeMillis()): MonthStats {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val start = DateUtil.monthStart(now)
        val thisMonth = all.filter { it.tx.timestamp >= start }
        val expenses = thisMonth.filter { !it.tx.isIncome }

        val spent = expenses.sumOf { it.myShare }
        val income = thisMonth.filter { it.tx.isIncome }.sumOf { it.tx.amount }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysLeft = daysInMonth - cal.get(Calendar.DAY_OF_MONTH) + 1

        val byCategory = expenses
            .groupBy { it.tx.category }
            .mapValues { e -> e.value.sumOf { it.myShare } }
            .toList()
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }

        val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val last7 = (6 downTo 0).map { back ->
            val s = DateUtil.dayStart(now, -back)
            val e = DateUtil.dayStart(now, -back + 1)
            val amount = all
                .filter { !it.tx.isIncome && it.tx.timestamp >= s && it.tx.timestamp < e }
                .sumOf { it.myShare }
            dayFmt.format(Date(s)) to amount
        }

        val topMerchants = expenses
            .groupBy { it.tx.merchant.trim().lowercase() }
            .map { (_, list) -> list.first().tx.merchant.trim() to list.sumOf { it.myShare } }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .take(5)

        return MonthStats(spent, income, budget, daysLeft, byCategory, last7, topMerchants)
    }
}
