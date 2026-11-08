
package com.example.moa.ui.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moa.MoaApplication
import com.example.moa.data.local.entity.BudgetEntity
import com.example.moa.data.repository.BudgetRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import com.example.moa.data.local.entity.TransactionType
data class CategoryBudgetUiState(
    val categoryId: Long,
    val categoryName: String,
    val budgetAmount: Long?,
    val spentAmount: Long
) {
    val remainingAmount: Long
        get() = (budgetAmount ?: 0L) - spentAmount

    val usageRate: Float
        get() = if ((budgetAmount ?: 0L) > 0L) {
            spentAmount.toFloat() / budgetAmount!!
        } else {
            0f
        }
}

data class BudgetUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val categories: List<CategoryBudgetUiState> = emptyList()
) {
    val totalBudget: Long
        get() = categories.sumOf { it.budgetAmount ?: 0L }

    val totalSpent: Long
        get() = categories.sumOf { it.spentAmount }

    val budgetedSpent: Long
        get() = categories
            .filter { it.budgetAmount != null }
            .sumOf { it.spentAmount }

    val totalRemaining: Long
        get() = totalBudget - budgetedSpent

    val usageRate: Float
        get() = if (totalBudget > 0L) {
            budgetedSpent.toFloat() / totalBudget
        } else {
            0f
        }
}


class BudgetViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: BudgetRepository =
        (application as MoaApplication).budgetRepository

    private val categoryRepository =
        (application as MoaApplication).categoryRepository

    private val selectedMonth =
        MutableStateFlow(YearMonth.now())


    val uiState: StateFlow<BudgetUiState> =
        selectedMonth.flatMapLatest { month ->
            combine(
                categoryRepository.getByType(TransactionType.EXPENSE),
                repository.observeBudgets(month.toString()),
                repository.observeMonthlyExpenses(month)
            ) { categories, budgets, expenses ->

                val budgetMap = budgets.associateBy {
                    it.categoryId
                }

                val expenseMap = expenses.associate {
                    it.categoryId to it.totalAmount
                }

                val categoryStates = categories.map { category ->
                    CategoryBudgetUiState(
                        categoryId = category.id,
                        categoryName = category.name,
                        budgetAmount = budgetMap[category.id]?.amount,
                        spentAmount = expenseMap[category.id] ?: 0L
                    )
                }

                BudgetUiState(
                    yearMonth = month,
                    categories = categoryStates
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BudgetUiState()
        )

    fun changeMonth(month: YearMonth) {
        selectedMonth.value = month
    }

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    fun saveBudget(
        categoryId: Long,
        amount: Long
    ) {
        viewModelScope.launch {
            repository.saveBudget(
                yearMonth = selectedMonth.value.toString(),
                categoryId = categoryId,
                amount = amount
            )
        }
    }

    fun deleteBudget(categoryId: Long) {
        viewModelScope.launch {
            repository.deleteBudget(
                yearMonth = selectedMonth.value.toString(),
                categoryId = categoryId
            )
        }
    }
}
