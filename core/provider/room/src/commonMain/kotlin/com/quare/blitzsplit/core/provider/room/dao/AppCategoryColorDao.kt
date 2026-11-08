package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.quare.blitzsplit.core.provider.room.entity.AppCategoryColorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCategoryColorDao {
    @Query("SELECT * FROM app_category_color")
    fun observeColors(): Flow<List<AppCategoryColorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppCategoryColorEntity)

    // Why: "Restaurar cor": the category reads as the app's default again.
    @Query("DELETE FROM app_category_color WHERE categoryKey = :categoryKey")
    suspend fun delete(categoryKey: String)
}
