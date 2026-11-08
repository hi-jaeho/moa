package com.example.moa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_profile")
data class FinancialProfileEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val initialBalance: Long,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
