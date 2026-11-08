package com.example.moa.ui.statistics

import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsUiStateTest {

    @Test
    fun estimatedBalance_includesInitialBalanceAndCumulativeTransactions() {
        val state = StatisticsUiState(
            initialBalance = 1_000_000L,
            cumulativeIncome = 300_000L,
            cumulativeExpense = 200_000L
        )

        assertEquals(1_100_000L, state.estimatedBalance)
    }

    @Test
    fun estimatedBalance_canShowDeficit() {
        val state = StatisticsUiState(
            initialBalance = 100_000L,
            cumulativeIncome = 50_000L,
            cumulativeExpense = 200_000L
        )

        assertEquals(-50_000L, state.estimatedBalance)
    }
}
