package com.quare.blitzsplit.core.settings.domain

import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.model.StatsChartPreferences
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeDeviceSettingsRepository(
    stored: ThemePreference = ThemePreference.SYSTEM,
) : DeviceSettingsRepository {
    val theme = MutableStateFlow(stored)
    val updatedThemes = mutableListOf<ThemePreference>()
    val layout = MutableStateFlow(GroupsLayout.initial)
    val updatedLayouts = mutableListOf<GroupsLayout>()
    val statsChart = MutableStateFlow(StatsChartPreferences.initial)
    val updatedStatsCharts = mutableListOf<StatsChartPreferences>()

    override fun observeTheme(): Flow<ThemePreference> = theme

    override suspend fun updateTheme(theme: ThemePreference) {
        updatedThemes += theme
        this.theme.value = theme
    }

    override fun observeGroupsLayout(): Flow<GroupsLayout> = layout

    override suspend fun updateGroupsLayout(layout: GroupsLayout) {
        updatedLayouts += layout
        this.layout.value = layout
    }

    override fun observeStatsChart(): Flow<StatsChartPreferences> = statsChart

    override suspend fun updateStatsChart(preferences: StatsChartPreferences) {
        updatedStatsCharts += preferences
        statsChart.value = preferences
    }
}
