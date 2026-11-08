
package com.example.moa.ui.calendar

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moa.MoaApplication
import com.example.moa.data.local.entity.TransactionEntity
import com.example.moa.data.local.entity.TransactionType
import com.example.moa.data.local.entity.FixedExpenseEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class DaySummary(
    val income: Long = 0L,
    val expense: Long = 0L
)

internal data class CalendarMonthData(
    val transactions: List<TransactionEntity>,
    val previousMonthExpense: Long
)

internal fun calendarMonthData(
    month: YearMonth,
    transactions: List<TransactionEntity>
): CalendarMonthData {
    val currentTransactions = transactions.filter {
        YearMonth.from(it.date) == month
    }
    val previousMonth = month.minusMonths(1)
    val previousMonthExpense = transactions
        .filter {
            YearMonth.from(it.date) == previousMonth &&
                it.type == TransactionType.EXPENSE
        }
        .sumOf { it.amount }

    return CalendarMonthData(
        transactions = currentTransactions,
        previousMonthExpense = previousMonthExpense
    )
}

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val transactions: List<TransactionEntity> = emptyList(),
    val income: Long = 0L,
    val expense: Long = 0L,
    val previousMonthExpense: Long = 0L,
    val dailySummaries: Map<LocalDate, DaySummary> = emptyMap(),
    val fixedExpenses: List<FixedExpenseEntity> = emptyList(),
    val isApplyingFixedExpenses: Boolean = false,
    val fixedExpenseMessage: String? = null,
    val isLoading: Boolean = true
) {
    val balance: Long
        get() = income - expense

    val selectedTransactions: List<TransactionEntity>
        get() = transactions
            .filter { it.date == selectedDate }
            .sortedByDescending { it.id }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    application: MoaApplication
) : AndroidViewModel(application) {

    private val repository =
        application.transactionRepository

    private val fixedExpenseRepository =
        application.fixedExpenseRepository

    private data class FixedExpenseOperationState(
        val isApplying: Boolean = false,
        val message: String? = null
    )

    private val fixedExpenseOperationState =
        MutableStateFlow(FixedExpenseOperationState())

    private val currentMonth =
        MutableStateFlow(YearMonth.now())

    private val selectedDate =
        MutableStateFlow(LocalDate.now())

    private val monthlyData =
        currentMonth.flatMapLatest { month ->
            repository.getByPeriod(
                month.minusMonths(1).atDay(1),
                month.atEndOfMonth()
            ).map { transactions ->
                calendarMonthData(month, transactions)
            }
        }

    val uiState: StateFlow<CalendarUiState> =
        combine(
            currentMonth,
            selectedDate,
            monthlyData,
            fixedExpenseRepository.observeAll(),
            fixedExpenseOperationState
        ) { month, date, monthData, fixedExpenses, operationState ->

            val transactions = monthData.transactions

            val income = transactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }

            val expense = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            val summaries = transactions
                .groupBy { it.date }
                .mapValues { (_, items) ->
                    DaySummary(
                        income = items
                            .filter { it.type == TransactionType.INCOME }
                            .sumOf { it.amount },
                        expense = items
                            .filter { it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount }
                    )
                }

            CalendarUiState(
                currentMonth = month,
                selectedDate = date,
                transactions = transactions,
                income = income,
                expense = expense,
                previousMonthExpense = monthData.previousMonthExpense,
                dailySummaries = summaries,
                fixedExpenses = fixedExpenses,
                isApplyingFixedExpenses = operationState.isApplying,
                fixedExpenseMessage = operationState.message,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CalendarUiState()
        )

    fun previousMonth() {
        changeMonth(currentMonth.value.minusMonths(1))
    }

    fun nextMonth() {
        changeMonth(currentMonth.value.plusMonths(1))
    }

    fun goToToday() {
        val today = LocalDate.now()
        currentMonth.value = YearMonth.from(today)
        selectedDate.value = today
    }

    fun selectDate(date: LocalDate) {
        currentMonth.value = YearMonth.from(date)
        selectedDate.value = date
    }

    fun addFixedExpense(
        name: String,
        dayOfMonth: Int,
        amount: Long
    ) {
        viewModelScope.launch {
            try {
                fixedExpenseRepository.add(name, dayOfMonth, amount)
                fixedExpenseOperationState.value = FixedExpenseOperationState(
                    message = "고정비 항목이 추가되었습니다."
                )
            } catch (e: Exception) {
                fixedExpenseOperationState.value = FixedExpenseOperationState(
                    message = e.message ?: "고정비를 추가하지 못했습니다."
                )
            }
        }
    }

    fun deleteFixedExpense(fixedExpense: FixedExpenseEntity) {
        viewModelScope.launch {
            try {
                fixedExpenseRepository.delete(fixedExpense)
                fixedExpenseOperationState.value = FixedExpenseOperationState(
                    message = "고정비 항목이 삭제되었습니다."
                )
            } catch (e: Exception) {
                fixedExpenseOperationState.value = FixedExpenseOperationState(
                    message = e.message ?: "고정비를 삭제하지 못했습니다."
                )
            }
        }
    }

    fun applyFixedExpenses() {
        if (fixedExpenseOperationState.value.isApplying) return

        fixedExpenseOperationState.value = FixedExpenseOperationState(
            isApplying = true
        )

        viewModelScope.launch {
            try {
                val result = fixedExpenseRepository.applyToMonth(
                    currentMonth.value
                )
                val message = when {
                    result.totalCount == 0 -> "적용할 고정비가 없습니다."
                    result.insertedCount == 0 ->
                        "이 달의 고정비는 이미 모두 적용되어 있습니다."
                    else -> "고정비 ${result.insertedCount}건을 적용했습니다."
                }
                fixedExpenseOperationState.value = FixedExpenseOperationState(
                    message = message
                )
            } catch (e: Exception) {
                fixedExpenseOperationState.value = FixedExpenseOperationState(
                    message = e.message ?: "고정비를 적용하지 못했습니다."
                )
            }
        }
    }

    fun clearFixedExpenseMessage() {
        fixedExpenseOperationState.update { it.copy(message = null) }
    }

    private fun changeMonth(month: YearMonth) {
        currentMonth.value = month
        selectedDate.value = month.atDay(1)
    }
}
