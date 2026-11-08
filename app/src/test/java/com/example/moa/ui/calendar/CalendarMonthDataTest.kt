package com.example.moa.ui.calendar

import com.example.moa.data.local.entity.TransactionEntity
import com.example.moa.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarMonthDataTest {

    @Test
    fun calendarMonthData_comparesOnlyPreviousMonthExpenses() {
        val transactions = listOf(
            transaction(TransactionType.EXPENSE, 300_000L, LocalDate.of(2026, 10, 5)),
            transaction(TransactionType.EXPENSE, 200_000L, LocalDate.of(2026, 10, 20)),
            transaction(TransactionType.INCOME, 1_000_000L, LocalDate.of(2026, 10, 25)),
            transaction(TransactionType.EXPENSE, 420_000L, LocalDate.of(2026, 11, 5))
        )

        val result = calendarMonthData(
            month = YearMonth.of(2026, 11),
            transactions = transactions
        )

        assertEquals(500_000L, result.previousMonthExpense)
        assertEquals(1, result.transactions.size)
        assertEquals(420_000L, result.transactions.single().amount)
    }

    private fun transaction(
        type: TransactionType,
        amount: Long,
        date: LocalDate
    ) = TransactionEntity(
        type = type,
        amount = amount,
        date = date,
        categoryId = 1L
    )
}
