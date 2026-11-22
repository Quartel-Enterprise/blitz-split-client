package com.quare.blitzsplit.core.settings.domain

import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.model.StatsChartPreferences
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.model.TimeFormatPreference
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeDeviceSettingsRepository(
    stored: ThemePreference = ThemePreference.SYSTEM,
) : DeviceSettingsRepository {
    val theme = MutableStateFlow(stored)
    val betaReleaseNotesChoice = MutableStateFlow<Boolean?>(null)
    val updatedThemes = mutableListOf<ThemePreference>()
    val layout = MutableStateFlow(GroupsLayout.initial)
    val updatedLayouts = mutableListOf<GroupsLayout>()
    val statsChart = MutableStateFlow(StatsChartPreferences.initial)
    val updatedStatsCharts = mutableListOf<StatsChartPreferences>()
    val dynamicColors = MutableStateFlow(false)
    val updatedDynamicColors = mutableListOf<Boolean>()
    val bitcoinInSats = MutableStateFlow(false)
    val updatedBitcoinInSats = mutableListOf<Boolean>()
    val timeFormat = MutableStateFlow(TimeFormatPreference.SYSTEM)
    val updatedTimeFormats = mutableListOf<TimeFormatPreference>()
    val lastUpdatePromptAt = MutableStateFlow<Long?>(null)
    val usageDataSharing = MutableStateFlow(true)
    val updatedUsageDataSharing = mutableListOf<Boolean>()
    val lastNotificationPromptAt = MutableStateFlow<Long?>(null)

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

    override fun observeDynamicColors(): Flow<Boolean> = dynamicColors

    override suspend fun updateDynamicColors(isOn: Boolean) {
        updatedDynamicColors += isOn
        dynamicColors.value = isOn
    }

    override fun observeBitcoinInSats(): Flow<Boolean> = bitcoinInSats

    override suspend fun updateBitcoinInSats(isOn: Boolean) {
        updatedBitcoinInSats += isOn
        bitcoinInSats.value = isOn
    }

    override fun observeTimeFormat(): Flow<TimeFormatPreference> = timeFormat

    override suspend fun updateTimeFormat(timeFormat: TimeFormatPreference) {
        updatedTimeFormats += timeFormat
        this.timeFormat.value = timeFormat
    }

    override fun observeLastUpdatePromptAt(): Flow<Long?> = lastUpdatePromptAt

    override suspend fun updateLastUpdatePromptAt(epochMillis: Long) {
        lastUpdatePromptAt.value = epochMillis
    }

    override fun observeLastNotificationPromptAt(): Flow<Long?> = lastNotificationPromptAt

    override suspend fun updateLastNotificationPromptAt(epochMillis: Long) {
        lastNotificationPromptAt.value = epochMillis
    }

    val lastSyncedAt = MutableStateFlow<Long?>(null)

    override fun observeLastSyncedAt(): Flow<Long?> = lastSyncedAt

    override suspend fun updateLastSyncedAt(epochMillis: Long) {
        lastSyncedAt.value = epochMillis
    }

    val isAppBannerDismissed = MutableStateFlow(false)

    override fun observeAppBannerDismissed(): Flow<Boolean> = isAppBannerDismissed

    override suspend fun dismissAppBanner() {
        isAppBannerDismissed.value = true
    }

    override fun observeUsageDataSharing(): Flow<Boolean> = usageDataSharing

    override suspend fun updateUsageDataSharing(isOn: Boolean) {
        updatedUsageDataSharing += isOn
        usageDataSharing.value = isOn
    }

    override fun observeBetaReleaseNotesChoice(): Flow<Boolean?> = betaReleaseNotesChoice

    override suspend fun updateBetaReleaseNotesChoice(isOn: Boolean) {
        betaReleaseNotesChoice.value = isOn
    }
}
