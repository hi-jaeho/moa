
package com.example.moa.ui.calendar

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moa.MoaApplication
import com.example.moa.data.local.entity.TransactionEntity
import com.example.moa.data.local.entity.TransactionType
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth

data class DaySummary(
    val income: Long = 0L,
    val expense: Long = 0L
)

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val transactions: List<TransactionEntity> = emptyList(),
    val income: Long = 0L,
    val expense: Long = 0L,
    val dailySummaries: Map<LocalDate, DaySummary> = emptyMap(),
    val isLoading: Boolean = true
) {
    val balance: Long
        get() = income - expense

    val selectedTransactions: List<TransactionEntity>
        get() = transactions
            .filter { it.date == selectedDate }
            .sortedByDescending { it.id }
}

class CalendarViewModel(
    application: MoaApplication
) : AndroidViewModel(application) {

    private val repository =
        application.transactionRepository

    private val currentMonth =
        MutableStateFlow(YearMonth.now())

    private val selectedDate =
        MutableStateFlow(LocalDate.now())

    private val monthlyTransactions =
        currentMonth.flatMapLatest { month ->
            repository.getByPeriod(
                month.atDay(1),
                month.atEndOfMonth()
            )
        }

    val uiState: StateFlow<CalendarUiState> =
        combine(
            currentMonth,
            selectedDate,
            monthlyTransactions
        ) { month, date, transactions ->

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
                dailySummaries = summaries,
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

    private fun changeMonth(month: YearMonth) {
        currentMonth.value = month
        selectedDate.value = month.atDay(1)
    }
}
