package com.example.moa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val type: TransactionType,

    val iconName: String = "category",

    val sortOrder: Int = 0
)
