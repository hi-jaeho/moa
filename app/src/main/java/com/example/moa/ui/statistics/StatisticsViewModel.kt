
package com.example.moa.ui.statistics

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moa.MoaApplication
import com.example.moa.data.local.entity.TransactionEntity
import com.example.moa.data.local.entity.TransactionType
import kotlinx.coroutines.flow.*
import java.time.YearMonth

data class CategoryExpense(
    val categoryId: Long,
    val name: String,
    val amount: Long,
    val ratio: Float
)

data class MonthlyTrend(
    val month: YearMonth,
    val income: Long,
    val expense: Long
)

data class StatisticsUiState(
    val month: YearMonth = YearMonth.now(),
    val income: Long = 0,
    val expense: Long = 0,
    val categories: List<CategoryExpense> = emptyList(),
    val trends: List<MonthlyTrend> = emptyList(),
    val isLoading: Boolean = true
) {
    val balance: Long get() = income - expense
}

class StatisticsViewModel(
    application: MoaApplication
) : AndroidViewModel(application) {

    private val transactions = application.transactionRepository
    private val categories = application.categoryRepository

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val transactionData = selectedMonth.flatMapLatest { month ->
        val firstMonth = month.minusMonths(5)

        transactions.getByPeriod(
            firstMonth.atDay(1),
            month.atEndOfMonth()
        )
    }

    val uiState: StateFlow<StatisticsUiState> =
        combine(
            selectedMonth,
            transactionData,
            categories.getAll()
        ) { month, allTransactions, allCategories ->

            val current = allTransactions.filter {
                YearMonth.from(it.date) == month
            }

            val income = current
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }

            val expense = current
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            val names = allCategories.associate {
                it.id to it.name
            }

            val categoryExpenses = current
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.categoryId }
                .map { (id, items) ->
                    val total = items.sumOf { it.amount }

                    CategoryExpense(
                        categoryId = id,
                        name = names[id] ?: "기타",
                        amount = total,
                        ratio = if (expense > 0L) {
                            total.toFloat() / expense.toFloat()
                        } else 0f
                    )
                }
                .sortedByDescending { it.amount }

            val trends = (5 downTo 0).map { offset ->
                val target = month.minusMonths(offset.toLong())

                val items = allTransactions.filter {
                    YearMonth.from(it.date) == target
                }

                MonthlyTrend(
                    month = target,
                    income = items
                        .filter { it.type == TransactionType.INCOME }
                        .sumOf { it.amount },
                    expense = items
                        .filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.amount }
                )
            }

            StatisticsUiState(
                month = month,
                income = income,
                expense = expense,
                categories = categoryExpenses,
                trends = trends,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatisticsUiState()
        )

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    fun goToCurrentMonth() {
        selectedMonth.value = YearMonth.now()
    }
}
