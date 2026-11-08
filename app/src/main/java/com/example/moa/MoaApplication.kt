
package com.example.moa

import android.app.Application
import androidx.room.Room
import androidx.room.withTransaction
import com.example.moa.data.local.AppDatabase
import com.example.moa.data.local.entity.CategoryEntity
import com.example.moa.data.local.entity.TransactionType
import com.example.moa.data.repository.CategoryRepository
import com.example.moa.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.example.moa.data.local.migration.MIGRATION_1_2
import com.example.moa.data.repository.BudgetRepository

class MoaApplication : Application() {

    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private val preferences by lazy {
        getSharedPreferences("moa_settings", MODE_PRIVATE)
    }

    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "moa_database"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    val transactionRepository by lazy {
        TransactionRepository(database.transactionDao())
    }

    val categoryRepository by lazy {
        CategoryRepository(database.categoryDao())
    }

    val budgetRepository by lazy {
        BudgetRepository(
            budgetDao = database.budgetDao(),
            transactionDao = database.transactionDao()
        )
    }

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            initializeCategories()
        }
    }

    private suspend fun initializeCategories() {
        if (preferences.getBoolean("categories_initialized", false)) {
            return
        }

        val defaults = listOf(
            CategoryEntity(name = "식비", type = TransactionType.EXPENSE, sortOrder = 1),
            CategoryEntity(name = "교통비", type = TransactionType.EXPENSE, sortOrder = 2),
            CategoryEntity(name = "주거비", type = TransactionType.EXPENSE, sortOrder = 3),
            CategoryEntity(name = "생활용품", type = TransactionType.EXPENSE, sortOrder = 4),
            CategoryEntity(name = "쇼핑", type = TransactionType.EXPENSE, sortOrder = 5),
            CategoryEntity(name = "의료비", type = TransactionType.EXPENSE, sortOrder = 6),
            CategoryEntity(name = "기타 지출", type = TransactionType.EXPENSE, sortOrder = 7),
            CategoryEntity(name = "급여", type = TransactionType.INCOME, sortOrder = 1),
            CategoryEntity(name = "부수입", type = TransactionType.INCOME, sortOrder = 2),
            CategoryEntity(name = "투자수익", type = TransactionType.INCOME, sortOrder = 3),
            CategoryEntity(name = "기타 수입", type = TransactionType.INCOME, sortOrder = 4)
        )

        val dao = database.categoryDao()

        database.withTransaction {
            defaults.forEach { category ->
                if (dao.countByNameAndType(category.name, category.type) == 0) {
                    dao.insert(category)
                }
            }
        }

        preferences.edit()
            .putBoolean("categories_initialized", true)
            .commit()
    }
}
