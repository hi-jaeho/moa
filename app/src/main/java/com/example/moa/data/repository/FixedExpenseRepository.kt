package com.example.moa.data.repository

import androidx.room.withTransaction
import com.example.moa.data.local.AppDatabase
import com.example.moa.data.local.entity.CategoryEntity
import com.example.moa.data.local.entity.FixedExpenseEntity
import com.example.moa.data.local.entity.TransactionEntity
import com.example.moa.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

data class FixedExpenseApplyResult(
    val totalCount: Int,
    val insertedCount: Int
)

internal fun fixedExpenseDate(
    month: YearMonth,
    dayOfMonth: Int
): LocalDate {
    require(dayOfMonth in 1..31)
    return month.atDay(dayOfMonth.coerceAtMost(month.lengthOfMonth()))
}

class FixedExpenseRepository(
    private val database: AppDatabase
) {
    private val fixedExpenseDao = database.fixedExpenseDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()

    fun observeAll(): Flow<List<FixedExpenseEntity>> {
        return fixedExpenseDao.observeAll()
    }

    suspend fun add(
        name: String,
        dayOfMonth: Int,
        amount: Long
    ) {
        require(name.isNotBlank()) {
            "고정비 항목을 입력해 주세요."
        }
        require(dayOfMonth in 1..31) {
            "날짜는 1일부터 31일 사이여야 합니다."
        }
        require(amount > 0L) {
            "금액은 0원보다 커야 합니다."
        }

        fixedExpenseDao.insert(
            FixedExpenseEntity(
                name = name.trim(),
                dayOfMonth = dayOfMonth,
                amount = amount
            )
        )
    }

    suspend fun delete(fixedExpense: FixedExpenseEntity) {
        fixedExpenseDao.delete(fixedExpense)
    }

    suspend fun applyToMonth(month: YearMonth): FixedExpenseApplyResult {
        return database.withTransaction {
            val categoryId = getOrCreateFixedExpenseCategoryId()
            val rules = fixedExpenseDao.getAll()

            val insertedCount = rules.count { rule ->
                val insertedId = transactionDao.insertFixedExpense(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = rule.amount,
                        date = fixedExpenseDate(month, rule.dayOfMonth),
                        categoryId = categoryId,
                        memo = rule.name,
                        fixedExpenseId = rule.id,
                        fixedExpenseYearMonth = month.toString()
                    )
                )

                insertedId != -1L
            }

            FixedExpenseApplyResult(
                totalCount = rules.size,
                insertedCount = insertedCount
            )
        }
    }

    private suspend fun getOrCreateFixedExpenseCategoryId(): Long {
        val existing = categoryDao.getByNameAndType(
            name = FIXED_EXPENSE_CATEGORY_NAME,
            type = TransactionType.EXPENSE
        )
        if (existing != null) return existing.id

        return categoryDao.insert(
            CategoryEntity(
                name = FIXED_EXPENSE_CATEGORY_NAME,
                type = TransactionType.EXPENSE,
                sortOrder = 8
            )
        )
    }

    companion object {
        const val FIXED_EXPENSE_CATEGORY_NAME = "고정비"
    }
}
