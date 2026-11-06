package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.quare.blitzsplit.core.provider.room.entity.PushSettingsEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import kotlinx.coroutines.flow.Flow

@Dao
interface PushSettingsDao {
    @Query("SELECT * FROM push_settings WHERE id = '$SINGLE_ROW_ID'")
    fun observe(): Flow<PushSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PushSettingsEntity)
}
