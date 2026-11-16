package com.quare.blitzsplit.core.settings.domain.repository

import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.model.StatsChartPreferences
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.model.TimeFormatPreference
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

    fun observeTimeFormat(): Flow<TimeFormatPreference>

    suspend fun updateTimeFormat(timeFormat: TimeFormatPreference)

    fun observeLastUpdatePromptAt(): Flow<Long?>

    suspend fun updateLastUpdatePromptAt(epochMillis: Long)

    fun observeLastNotificationPromptAt(): Flow<Long?>

    suspend fun updateLastNotificationPromptAt(epochMillis: Long)

    fun observeAppBannerDismissed(): Flow<Boolean>

    suspend fun dismissAppBanner()

    fun observeUsageDataSharing(): Flow<Boolean>

    suspend fun updateUsageDataSharing(isOn: Boolean)

    // Why: Release notes' "Show beta versions" as switched on this device, null until it was.
    fun observeBetaReleaseNotesChoice(): Flow<Boolean?>

    suspend fun updateBetaReleaseNotesChoice(isOn: Boolean)
}
