
package com.example.moa.data.repository

import com.example.moa.data.local.dao.CategoryDao
import com.example.moa.data.local.entity.CategoryEntity
import com.example.moa.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val categoryDao: CategoryDao
) {

    suspend fun insert(
        category: CategoryEntity
    ): Long {
        require(category.name.isNotBlank()) {
            "카테고리 이름을 입력해야 합니다."
        }
        return categoryDao.insert(category)
    }

    suspend fun update(
        category: CategoryEntity
    ) {
        categoryDao.update(category)
    }

    suspend fun delete(
        category: CategoryEntity
    ) {
        categoryDao.delete(category)
    }

    fun getAll(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    fun getByType(
        type: TransactionType
    ): Flow<List<CategoryEntity>> {
        return categoryDao.getCategoriesByType(type)
    }

    suspend fun getById(
        id: Long
    ): CategoryEntity? {
        return categoryDao.getCategoryById(id)
    }
}
