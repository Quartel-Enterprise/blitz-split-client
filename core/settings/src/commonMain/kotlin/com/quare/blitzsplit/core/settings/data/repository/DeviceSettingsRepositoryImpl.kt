package com.quare.blitzsplit.core.settings.data.repository

import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.core.model.StatsChartPreferences
import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.core.model.ThemePreference
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
        local.update { entity -> entity.copy(theme = theme.code) }
    }

    // Why: nothing picked yet, or a code from a newer version, is the list and the standard grid, each on its own.
    override fun observeGroupsLayout(): Flow<GroupsLayout> = local
        .observe()
        .map { entity ->
            GroupsLayout(
                display = entity?.groupsDisplay?.let(GroupsDisplay::fromCodeOrNull) ?: GroupsLayout.initial.display,
                gridSize = entity?.groupGridSize?.let(GroupGridSize::fromCodeOrNull) ?: GroupsLayout.initial.gridSize,
            )
        }.distinctUntilChanged()

    override suspend fun updateGroupsLayout(layout: GroupsLayout) {
        local.update { entity ->
            entity.copy(
                groupsDisplay = layout.display.code,
                groupGridSize = layout.gridSize.code,
            )
        }
    }

    // Why: nothing picked yet, or a code from a newer version, is bars, grouped by what fits the period.
    override fun observeStatsChart(): Flow<StatsChartPreferences> = local
        .observe()
        .map { entity ->
            StatsChartPreferences(
                chartType = entity?.statsChartType?.let(StatsChartType::fromCodeOrNull)
                    ?: StatsChartPreferences.initial.chartType,
                grain = entity?.statsGrain?.let(StatsGrain::fromCodeOrNull),
            )
        }.distinctUntilChanged()

    override suspend fun updateStatsChart(preferences: StatsChartPreferences) {
        local.update { entity ->
            entity.copy(
                statsChartType = preferences.chartType.code,
                statsGrain = preferences.grain?.code,
            )
        }
    }

    // Why: no row yet keeps the brand's colors.
    override fun observeDynamicColors(): Flow<Boolean> = local
        .observe()
        .map { entity -> entity?.usesDynamicColors ?: false }
        .distinctUntilChanged()

    override suspend fun updateDynamicColors(isOn: Boolean) {
        local.update { entity -> entity.copy(usesDynamicColors = isOn) }
    }

    // Why: no row yet shows bitcoin in bitcoin.
    override fun observeBitcoinInSats(): Flow<Boolean> = local
        .observe()
        .map { entity -> entity?.showsBitcoinInSats ?: false }
        .distinctUntilChanged()

    override suspend fun updateBitcoinInSats(isOn: Boolean) {
        local.update { entity -> entity.copy(showsBitcoinInSats = isOn) }
    }

    override fun observeLastUpdatePromptAt(): Flow<Long?> = local
        .observe()
        .map { entity -> entity?.lastUpdatePromptAt }
        .distinctUntilChanged()

    override suspend fun updateLastUpdatePromptAt(epochMillis: Long) {
        local.update { entity -> entity.copy(lastUpdatePromptAt = epochMillis) }
    }

    override fun observeLastNotificationPromptAt(): Flow<Long?> = local
        .observe()
        .map { entity -> entity?.lastNotificationPromptAt }
        .distinctUntilChanged()

    override suspend fun updateLastNotificationPromptAt(epochMillis: Long) {
        local.update { entity -> entity.copy(lastNotificationPromptAt = epochMillis) }
    }

    override fun observeAppBannerDismissed(): Flow<Boolean> = local
        .observe()
        .map { entity -> entity?.isAppBannerDismissed == true }
        .distinctUntilChanged()

    override suspend fun dismissAppBanner() {
        local.update { entity -> entity.copy(isAppBannerDismissed = true) }
    }

    // Why: on for a fresh install, so the default the policy states (legitimate interest) holds before any row exists.
    override fun observeUsageDataSharing(): Flow<Boolean> = local
        .observe()
        .map { entity -> entity?.sharesUsageData ?: true }
        .distinctUntilChanged()

    override suspend fun updateUsageDataSharing(isOn: Boolean) {
        local.update { entity -> entity.copy(sharesUsageData = isOn) }
    }
}
