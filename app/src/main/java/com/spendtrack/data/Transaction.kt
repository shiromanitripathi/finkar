package com.spendtrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String = Categories.OTHERS,
    val isIncome: Boolean = false,
    val note: String = "",
    val source: String = "Manual",
    val rawText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = ""
)

/** Part of a transaction that a friend owes the user. */
@Entity(tableName = "split_shares")
data class SplitShare(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val friendId: Long,
    val amount: Double
)

/** Money a friend has given back. */
@Entity(tableName = "paybacks")
data class Payback(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val friendId: Long,
    val amount: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: String,
    val monthlyLimit: Double
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Double,
    val saved: Double = 0.0
)
