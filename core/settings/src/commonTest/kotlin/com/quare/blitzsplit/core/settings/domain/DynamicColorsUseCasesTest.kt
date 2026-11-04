package com.quare.blitzsplit.core.settings.domain

import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveDynamicColorsUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateDynamicColorsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicColorsUseCasesTest {
    private lateinit var repository: FakeDeviceSettingsRepository
    private lateinit var observeDynamicColors: ObserveDynamicColorsUseCase
    private lateinit var updateDynamicColors: UpdateDynamicColorsUseCase

    @Test
    fun `GIVEN a device that draws Material You with it turned on WHEN observing Material You THEN is on`() = runTest {
        // Given
        prepareScenario(
            isSupported = true,
            storedOn = true,
        )

        // When
        val isOn = observeDynamicColors().first()

        // Then
        assertEquals(true, isOn)
    }

    @Test
    fun `GIVEN a device that draws Material You with it off WHEN observing Material You THEN is off`() = runTest {
        // Given
        prepareScenario(
            isSupported = true,
            storedOn = false,
        )

        // When
        val isOn = observeDynamicColors().first()

        // Then
        assertEquals(false, isOn)
    }

    @Test
    fun `GIVEN a device that cannot draw Material You with it stored on WHEN observing Material You THEN is off`() =
        runTest {
            // Given
            prepareScenario(
                isSupported = false,
                storedOn = true,
            )

            // When
            val isOn = observeDynamicColors().first()

            // Then
            assertEquals(false, isOn)
        }

    @Test
    fun `GIVEN a device that draws Material You being observed WHEN it is turned on THEN observers see it on`() =
        runTest {
            // Given
            prepareScenario(
                isSupported = true,
                storedOn = false,
            )
            val observed = mutableListOf<Boolean>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                observeDynamicColors().collect(observed::add)
            }

            // When
            repository.updateDynamicColors(true)

            // Then
            assertEquals(listOf(false, true), observed)
        }

    @Test
    fun `GIVEN a device that cannot draw Material You being observed WHEN it is turned on THEN stays off`() = runTest {
        // Given
        prepareScenario(
            isSupported = false,
            storedOn = false,
        )
        val observed = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeDynamicColors().collect(observed::add)
        }

        // When
        repository.updateDynamicColors(true)

        // Then
        assertEquals(listOf(false), observed)
    }

    @Test
    fun `GIVEN Material You off WHEN turning it on THEN stores on`() = runTest {
        // Given
        prepareScenario(storedOn = false)

        // When
        updateDynamicColors(true)

        // Then
        assertEquals(listOf(true), repository.updatedDynamicColors)
        assertEquals(true, repository.dynamicColors.value)
    }

    @Test
    fun `GIVEN Material You on WHEN turning it off THEN stores off`() = runTest {
        // Given
        prepareScenario(storedOn = true)

        // When
        updateDynamicColors(false)

        // Then
        assertEquals(listOf(false), repository.updatedDynamicColors)
        assertEquals(false, repository.dynamicColors.value)
    }

    private fun prepareScenario(
        isSupported: Boolean = true,
        storedOn: Boolean,
    ) {
        repository = FakeDeviceSettingsRepository().also { it.dynamicColors.value = storedOn }
        observeDynamicColors = ObserveDynamicColorsUseCase(
            repository = repository,
            dynamicColorSupport = { isSupported },
        )
        updateDynamicColors = UpdateDynamicColorsUseCase(repository)
    }
}
