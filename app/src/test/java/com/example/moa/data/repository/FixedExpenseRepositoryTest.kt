package com.example.moa.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FixedExpenseRepositoryTest {

    @Test
    fun fixedExpenseDate_usesConfiguredDayWhenItExists() {
        val date = fixedExpenseDate(
            month = YearMonth.of(2026, 11),
            dayOfMonth = 25
        )

        assertEquals(LocalDate.of(2026, 11, 25), date)
    }

    @Test
    fun fixedExpenseDate_usesLastDayForShortMonth() {
        val date = fixedExpenseDate(
            month = YearMonth.of(2028, 2),
            dayOfMonth = 31
        )

        assertEquals(LocalDate.of(2028, 2, 29), date)
    }
}
