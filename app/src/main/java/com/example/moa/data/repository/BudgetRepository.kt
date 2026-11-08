
package com.example.moa.data.repository

import com.example.moa.data.local.dao.BudgetDao
import com.example.moa.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import com.example.moa.data.local.dao.TransactionDao
import com.example.moa.data.local.dao.CategoryExpenseTotal
import java.time.YearMonth

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) {
    fun observeBudgets(
        yearMonth: String
    ): Flow<List<BudgetEntity>> {
        return budgetDao.observeBudgets(yearMonth)
    }

    suspend fun getBudget(
        yearMonth: String,
        categoryId: Long
    ): BudgetEntity? {
        return budgetDao.getBudget(yearMonth, categoryId)
    }

    suspend fun saveBudget(
        yearMonth: String,
        categoryId: Long,
        amount: Long
    ) {
        require(amount >= 0L) {
            "예산은 0원 이상이어야 합니다."
        }

        val existing = budgetDao.getBudget(
            yearMonth = yearMonth,
            categoryId = categoryId
        )

        budgetDao.insert(
            BudgetEntity(
                id = existing?.id ?: 0L,
                yearMonth = yearMonth,
                categoryId = categoryId,
                amount = amount,
                createdAt = existing?.createdAt
                    ?: System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteBudget(
        yearMonth: String,
        categoryId: Long
    ) {
        budgetDao.delete(yearMonth, categoryId)
    }


    fun observeMonthlyExpenses(
        yearMonth: YearMonth
    ): Flow<List<CategoryExpenseTotal>> {
        return transactionDao.getMonthlyExpenseByCategory(
            startDate = yearMonth.atDay(1),
            endDate = yearMonth.atEndOfMonth()
        )
    }
}
