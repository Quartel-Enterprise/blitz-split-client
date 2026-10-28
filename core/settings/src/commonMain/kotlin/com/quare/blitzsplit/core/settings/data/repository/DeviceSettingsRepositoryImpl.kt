package com.quare.blitzsplit.core.settings.data.repository

import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.provider.room.entity.DeviceSettingsEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import com.quare.blitzsplit.core.settings.data.datasource.DeviceSettingsLocalDataSource
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DeviceSettingsRepositoryImpl(
    private val local: DeviceSettingsLocalDataSource,
) : DeviceSettingsRepository {
    // Why: no row yet, or a code from a newer version, follows the system.
    override fun observeTheme(): Flow<ThemePreference> = local
        .observe()
        .map { entity -> entity?.theme?.let(ThemePreference::fromCodeOrNull) ?: ThemePreference.SYSTEM }
        .distinctUntilChanged()

    override suspend fun updateTheme(theme: ThemePreference) {
        local.upsert(
            DeviceSettingsEntity(
                id = SINGLE_ROW_ID,
                theme = theme.code,
            ),
        )
    }
}
