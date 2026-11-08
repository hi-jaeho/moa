
package com.example.moa.data.repository

import com.example.moa.data.local.dao.TransactionDao
import com.example.moa.data.local.entity.TransactionEntity
import com.example.moa.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TransactionRepository(
    private val transactionDao: TransactionDao
) {

    suspend fun insert(
        transaction: TransactionEntity
    ): Long {
        require(transaction.amount > 0) {
            "금액은 0보다 커야 합니다."
        }
        return transactionDao.insert(transaction)
    }

    suspend fun update(
        transaction: TransactionEntity
    ) {
        require(transaction.amount > 0) {
            "금액은 0보다 커야 합니다."
        }
        transactionDao.update(transaction)
    }

    suspend fun delete(
        transaction: TransactionEntity
    ) {
        transactionDao.delete(transaction)
    }

    suspend fun getById(
        id: Long
    ): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }

    fun getByDate(
        date: LocalDate
    ): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByDate(date)
    }

    fun getByPeriod(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByPeriod(
            startDate,
            endDate
        )
    }

    fun getTotalByType(
        type: TransactionType,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Long> {
        return transactionDao.getTotalByType(
            type,
            startDate,
            endDate
        )
    }

    fun getTotalByTypeUntil(
        type: TransactionType,
        endDate: LocalDate
    ): Flow<Long> {
        return transactionDao.getTotalByTypeUntil(type, endDate)
    }
}
