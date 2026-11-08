package com.example.moa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fixed_expenses")
data class FixedExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dayOfMonth: Int,
    val amount: Long,
    val createdAt: Long = System.currentTimeMillis()
)
