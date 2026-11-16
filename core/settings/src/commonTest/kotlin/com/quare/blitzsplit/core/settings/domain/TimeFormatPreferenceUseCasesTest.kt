package com.quare.blitzsplit.core.settings.domain

import com.quare.blitzsplit.core.model.TimeFormatPreference
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveTimeFormatPreferenceUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateTimeFormatPreferenceUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeFormatPreferenceUseCasesTest {
    private lateinit var repository: FakeDeviceSettingsRepository
    private lateinit var observeTimeFormat: ObserveTimeFormatPreferenceUseCase
    private lateinit var updateTimeFormat: UpdateTimeFormatPreferenceUseCase

    @Test
    fun `GIVEN nothing picked on this device WHEN observing the time format THEN follows the system`() = runTest {
        // Given
        prepareScenario(stored = TimeFormatPreference.SYSTEM)

        // When
        val timeFormat = observeTimeFormat().first()

        // Then
        assertEquals(TimeFormatPreference.SYSTEM, timeFormat)
    }

    @Test
    fun `GIVEN 24h picked on this device WHEN observing the time format THEN is 24 hours`() = runTest {
        // Given
        prepareScenario(stored = TimeFormatPreference.TWENTY_FOUR_HOUR)

        // When
        val timeFormat = observeTimeFormat().first()

        // Then
        assertEquals(TimeFormatPreference.TWENTY_FOUR_HOUR, timeFormat)
    }

    @Test
    fun `GIVEN the time format being observed WHEN 12h is picked THEN observers see 12 hours`() = runTest {
        // Given
        prepareScenario(stored = TimeFormatPreference.SYSTEM)
        val observed = mutableListOf<TimeFormatPreference>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeTimeFormat().collect(observed::add)
        }

        // When
        updateTimeFormat(TimeFormatPreference.TWELVE_HOUR)

        // Then
        assertEquals(listOf(TimeFormatPreference.SYSTEM, TimeFormatPreference.TWELVE_HOUR), observed)
    }

    @Test
    fun `GIVEN the time format follows the system WHEN picking 24h THEN stores 24 hours`() = runTest {
        // Given
        prepareScenario(stored = TimeFormatPreference.SYSTEM)

        // When
        updateTimeFormat(TimeFormatPreference.TWENTY_FOUR_HOUR)

        // Then
        assertEquals(listOf(TimeFormatPreference.TWENTY_FOUR_HOUR), repository.updatedTimeFormats)
        assertEquals(TimeFormatPreference.TWENTY_FOUR_HOUR, repository.timeFormat.value)
    }

    @Test
    fun `GIVEN 12h picked WHEN picking system again THEN stores system`() = runTest {
        // Given
        prepareScenario(stored = TimeFormatPreference.TWELVE_HOUR)

        // When
        updateTimeFormat(TimeFormatPreference.SYSTEM)

        // Then
        assertEquals(listOf(TimeFormatPreference.SYSTEM), repository.updatedTimeFormats)
        assertEquals(TimeFormatPreference.SYSTEM, repository.timeFormat.value)
    }

    private fun prepareScenario(stored: TimeFormatPreference) {
        repository = FakeDeviceSettingsRepository().also { it.timeFormat.value = stored }
        observeTimeFormat = ObserveTimeFormatPreferenceUseCase(repository)
        updateTimeFormat = UpdateTimeFormatPreferenceUseCase(repository)
    }
}
