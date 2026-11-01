package com.quare.blitzsplit.core.settings.domain

import com.quare.blitzsplit.core.model.StatsChartPreferences
import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveStatsChartPreferencesUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateStatsChartPreferencesUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsChartUseCasesTest {
    private lateinit var repository: FakeDeviceSettingsRepository
    private lateinit var observeStatsChart: ObserveStatsChartPreferencesUseCase
    private lateinit var updateStatsChart: UpdateStatsChartPreferencesUseCase

    @Test
    fun `GIVEN the line by week picked on this device WHEN observing the chart THEN is the line by week`() = runTest {
        // Given
        prepareScenario()
        repository.statsChart.value = StatsChartPreferences(StatsChartType.LINE, StatsGrain.WEEK)

        // When
        val chart = observeStatsChart().first()

        // Then
        assertEquals(StatsChartPreferences(StatsChartType.LINE, StatsGrain.WEEK), chart)
    }

    @Test
    fun `GIVEN bars WHEN picking the line by month THEN stores it`() = runTest {
        // Given
        prepareScenario()
        val lineByMonth = StatsChartPreferences(StatsChartType.LINE, StatsGrain.MONTH)

        // When
        updateStatsChart(lineByMonth)

        // Then
        assertEquals(listOf(lineByMonth), repository.updatedStatsCharts)
    }

    private fun prepareScenario() {
        repository = FakeDeviceSettingsRepository()
        observeStatsChart = ObserveStatsChartPreferencesUseCase(repository)
        updateStatsChart = UpdateStatsChartPreferencesUseCase(repository)
    }
}
