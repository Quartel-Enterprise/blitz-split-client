package com.quare.blitzsplit.core.settings.domain

import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateThemePreferenceUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateThemePreferenceUseCaseTest {
    private lateinit var repository: FakeDeviceSettingsRepository
    private lateinit var useCase: UpdateThemePreferenceUseCase

    @Test
    fun `GIVEN the theme follows the system WHEN picking dark THEN stores dark`() = runTest {
        // Given
        prepareScenario(stored = ThemePreference.SYSTEM)

        // When
        useCase(ThemePreference.DARK)

        // Then
        assertEquals(listOf(ThemePreference.DARK), repository.updatedThemes)
        assertEquals(ThemePreference.DARK, repository.theme.value)
    }

    @Test
    fun `GIVEN dark picked WHEN picking system again THEN stores system`() = runTest {
        // Given
        prepareScenario(stored = ThemePreference.DARK)

        // When
        useCase(ThemePreference.SYSTEM)

        // Then
        assertEquals(listOf(ThemePreference.SYSTEM), repository.updatedThemes)
        assertEquals(ThemePreference.SYSTEM, repository.theme.value)
    }

    private fun prepareScenario(stored: ThemePreference) {
        repository = FakeDeviceSettingsRepository(stored)
        useCase = UpdateThemePreferenceUseCase(repository)
    }
}
