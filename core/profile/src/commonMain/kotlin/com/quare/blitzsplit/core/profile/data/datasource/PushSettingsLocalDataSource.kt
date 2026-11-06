package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.PushSettingsEntity
import kotlinx.coroutines.flow.Flow

interface PushSettingsLocalDataSource {
    fun observe(): Flow<PushSettingsEntity?>

    suspend fun upsert(entity: PushSettingsEntity)
}
