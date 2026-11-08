
package com.example.moa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.moa.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("""
        SELECT * FROM budgets
        WHERE yearMonth = :yearMonth
        ORDER BY categoryId ASC
    """)
    fun observeBudgets(
        yearMonth: String
    ): Flow<List<BudgetEntity>>

    @Query("""
        SELECT * FROM budgets
        WHERE yearMonth = :yearMonth
        AND categoryId = :categoryId
        LIMIT 1
    """)
    suspend fun getBudget(
        yearMonth: String,
        categoryId: Long
    ): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Query("""
        DELETE FROM budgets
        WHERE yearMonth = :yearMonth
        AND categoryId = :categoryId
    """)
    suspend fun delete(
        yearMonth: String,
        categoryId: Long
    )
}
