package com.example.moa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.moa.data.local.entity.FinancialProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialProfileDao {

    @Query("SELECT * FROM financial_profile WHERE id = 1 LIMIT 1")
    fun observe(): Flow<FinancialProfileEntity?>

    @Query("SELECT * FROM financial_profile WHERE id = 1 LIMIT 1")
    suspend fun getForBackup(): FinancialProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(profile: FinancialProfileEntity)

    @Query("DELETE FROM financial_profile")
    suspend fun deleteAll()
}
