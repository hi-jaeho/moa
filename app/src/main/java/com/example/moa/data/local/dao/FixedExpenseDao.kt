package com.example.moa.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.moa.data.local.entity.FixedExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedExpenseDao {

    @Query("SELECT * FROM fixed_expenses ORDER BY dayOfMonth ASC, id ASC")
    fun observeAll(): Flow<List<FixedExpenseEntity>>

    @Query("SELECT * FROM fixed_expenses ORDER BY id ASC")
    suspend fun getAll(): List<FixedExpenseEntity>

    @Insert
    suspend fun insert(fixedExpense: FixedExpenseEntity): Long

    @Delete
    suspend fun delete(fixedExpense: FixedExpenseEntity)

    @Query("DELETE FROM fixed_expenses")
    suspend fun deleteAll()
}
