package com.quare.blitzsplit.core.settings.domain.repository

import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.model.StatsChartPreferences
import com.quare.blitzsplit.core.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface DeviceSettingsRepository {
    fun observeTheme(): Flow<ThemePreference>

    suspend fun updateTheme(theme: ThemePreference)

    fun observeGroupsLayout(): Flow<GroupsLayout>

    suspend fun updateGroupsLayout(layout: GroupsLayout)

    fun observeStatsChart(): Flow<StatsChartPreferences>

    suspend fun updateStatsChart(preferences: StatsChartPreferences)

    fun observeDynamicColors(): Flow<Boolean>

    suspend fun updateDynamicColors(isOn: Boolean)

    fun observeBitcoinInSats(): Flow<Boolean>

    suspend fun updateBitcoinInSats(isOn: Boolean)
}
