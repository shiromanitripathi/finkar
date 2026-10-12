package com.spendtrack.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Transactions
    @Insert
    suspend fun insert(t: Transaction): Long

    @Update
    suspend fun update(t: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun transactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun transaction(id: Long): Transaction?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    // Friends
    @Insert
    suspend fun insertFriend(f: Friend): Long

    @Query("SELECT * FROM friends ORDER BY name COLLATE NOCASE")
    fun friends(): Flow<List<Friend>>

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteFriend(id: Long)

    // Split shares
    @Insert
    suspend fun insertShares(s: List<SplitShare>)

    @Query("SELECT * FROM split_shares")
    fun shares(): Flow<List<SplitShare>>

    @Query("DELETE FROM split_shares WHERE transactionId = :id")
    suspend fun deleteSharesForTransaction(id: Long)

    @Query("DELETE FROM split_shares WHERE friendId = :id")
    suspend fun deleteSharesForFriend(id: Long)

    @Query("UPDATE split_shares SET amount = amount * :ratio WHERE transactionId = :id")
    suspend fun scaleShares(id: Long, ratio: Double)

    // Paybacks
    @Insert
    suspend fun insertPayback(p: Payback)

    @Query("SELECT * FROM paybacks ORDER BY timestamp DESC")
    fun paybacks(): Flow<List<Payback>>

    @Query("DELETE FROM paybacks WHERE friendId = :id")
    suspend fun deletePaybacksForFriend(id: Long)

    // Budgets
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(b: Budget)

    @Query("SELECT * FROM budgets")
    fun budgets(): Flow<List<Budget>>

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudget(category: String)

    // Goals
    @Insert
    suspend fun insertGoal(g: Goal): Long

    @Update
    suspend fun updateGoal(g: Goal)

    @Query("SELECT * FROM goals ORDER BY id DESC")
    fun goals(): Flow<List<Goal>>

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)
}

@Database(
    entities = [
        Transaction::class, Friend::class, SplitShare::class,
        Payback::class, Budget::class, Goal::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spendtrack.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
