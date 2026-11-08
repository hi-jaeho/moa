
package com.example.moa.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index(
            value = ["yearMonth", "categoryId"],
            unique = true
        )
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val yearMonth: String,
    val categoryId: Long,
    val amount: Long,
    val createdAt: Long = System.currentTimeMillis()
)
