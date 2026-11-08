
package com.example.moa.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.moa.data.local.entity.TransactionEntity
import com.example.moa.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class CategoryExpenseTotal(
    val categoryId: Long,
    val totalAmount: Long
)

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFixedExpense(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("""
        SELECT * FROM transactions
        WHERE date = :date
        ORDER BY createdAt DESC
    """)
    fun getTransactionsByDate(
        date: LocalDate
    ): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date DESC, createdAt DESC
    """)
    fun getTransactionsByPeriod(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE id = :id LIMIT 1
    """)
    suspend fun getTransactionById(
        id: Long
    ): TransactionEntity?

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE type = :type
        AND date BETWEEN :startDate AND :endDate
    """)
    fun getTotalByType(
        type: TransactionType,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE type = :type
        AND date <= :endDate
    """)
    fun getTotalByTypeUntil(
        type: TransactionType,
        endDate: LocalDate
    ): Flow<Long>

    @Query("SELECT * FROM transactions ORDER BY id ASC")
    suspend fun getAllForBackup(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()


    @Query("""
    SELECT categoryId, COALESCE(SUM(amount), 0) AS totalAmount
    FROM transactions
    WHERE type = 'EXPENSE'
      AND date BETWEEN :startDate AND :endDate
    GROUP BY categoryId
""")
    fun getMonthlyExpenseByCategory(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<CategoryExpenseTotal>>

}
