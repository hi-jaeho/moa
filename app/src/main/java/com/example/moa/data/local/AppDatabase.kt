
package com.example.moa.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.moa.data.local.converter.DateConverter
import com.example.moa.data.local.dao.BudgetDao
import com.example.moa.data.local.dao.CategoryDao
import com.example.moa.data.local.dao.FinancialProfileDao
import com.example.moa.data.local.dao.FixedExpenseDao
import com.example.moa.data.local.dao.TransactionDao
import com.example.moa.data.local.entity.BudgetEntity
import com.example.moa.data.local.entity.CategoryEntity
import com.example.moa.data.local.entity.FinancialProfileEntity
import com.example.moa.data.local.entity.FixedExpenseEntity
import com.example.moa.data.local.entity.TransactionEntity

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        FinancialProfileEntity::class,
        FixedExpenseEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao

    abstract fun budgetDao(): BudgetDao

    abstract fun financialProfileDao(): FinancialProfileDao

    abstract fun fixedExpenseDao(): FixedExpenseDao
}
