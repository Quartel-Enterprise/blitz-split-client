package com.quare.blitzsplit.core.settings.domain.repository

import com.quare.blitzsplit.core.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface DeviceSettingsRepository {
    fun observeTheme(): Flow<ThemePreference>

    suspend fun updateTheme(theme: ThemePreference)
}
