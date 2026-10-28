package com.quare.blitzsplit.core.settings.domain

import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveThemePreferenceUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveThemePreferenceUseCaseTest {
    private lateinit var repository: FakeDeviceSettingsRepository
    private lateinit var useCase: ObserveThemePreferenceUseCase

    @Test
    fun `GIVEN dark picked on this device WHEN observing the theme THEN is dark`() = runTest {
        // Given
        prepareScenario(stored = ThemePreference.DARK)

        // When
        val theme = useCase().first()

        // Then
        assertEquals(ThemePreference.DARK, theme)
    }

    @Test
    fun `GIVEN the theme being observed WHEN light is picked THEN observers see light`() = runTest {
        // Given
        prepareScenario(stored = ThemePreference.SYSTEM)
        val observed = mutableListOf<ThemePreference>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().collect(observed::add) }

        // When
        repository.updateTheme(ThemePreference.LIGHT)

        // Then
        assertEquals(listOf(ThemePreference.SYSTEM, ThemePreference.LIGHT), observed)
    }

    private fun prepareScenario(stored: ThemePreference) {
        repository = FakeDeviceSettingsRepository(stored)
        useCase = ObserveThemePreferenceUseCase(repository)
    }
}
