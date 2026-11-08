package com.example.moa.data.repository

import com.example.moa.data.local.dao.FinancialProfileDao
import com.example.moa.data.local.entity.FinancialProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinancialProfileRepository(
    private val dao: FinancialProfileDao
) {
    fun observeInitialBalance(): Flow<Long> {
        return dao.observe().map { it?.initialBalance ?: 0L }
    }

    suspend fun saveInitialBalance(amount: Long) {
        require(amount >= 0L) {
            "시작 보유금액은 0원 이상이어야 합니다."
        }

        dao.save(
            FinancialProfileEntity(
                initialBalance = amount
            )
        )
    }
}
