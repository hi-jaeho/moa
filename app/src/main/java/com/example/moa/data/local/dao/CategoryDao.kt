
package com.example.moa.data.local.dao

import androidx.room.*
import com.example.moa.data.local.entity.CategoryEntity
import com.example.moa.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("""
        SELECT * FROM categories
        ORDER BY sortOrder ASC, id ASC
    """)
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("""
        SELECT * FROM categories
        WHERE type = :type
        ORDER BY sortOrder ASC, id ASC
    """)
    fun getCategoriesByType(
        type: TransactionType
    ): Flow<List<CategoryEntity>>

    @Query("""
        SELECT * FROM categories
        WHERE id = :id LIMIT 1
    """)
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("""
        SELECT COUNT(*) FROM categories
        WHERE name = :name AND type = :type
    """)
    suspend fun countByNameAndType(
        name: String,
        type: TransactionType
    ): Int

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getAllForBackup(): List<CategoryEntity>

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

}
