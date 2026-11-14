package com.quare.blitzsplit.core.settings.domain

import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveUsageDataSharingUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateUsageDataSharingUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UsageDataSharingUseCasesTest {
    private lateinit var repository: FakeDeviceSettingsRepository
    private lateinit var observeUsageDataSharing: ObserveUsageDataSharingUseCase
    private lateinit var updateUsageDataSharing: UpdateUsageDataSharingUseCase

    @Test
    fun `GIVEN a device that never touched it WHEN observing usage data sharing THEN is on`() = runTest {
        // Given
        prepareScenario()

        // When
        val isOn = observeUsageDataSharing().first()

        // Then
        assertEquals(true, isOn)
    }

    @Test
    fun `GIVEN usage data sharing turned off on this device WHEN observing it THEN is off`() = runTest {
        // Given
        prepareScenario(storedOn = false)

        // When
        val isOn = observeUsageDataSharing().first()

        // Then
        assertEquals(false, isOn)
    }

    @Test
    fun `GIVEN usage data sharing being observed WHEN it is turned off THEN observers see it off`() = runTest {
        // Given
        prepareScenario(storedOn = true)
        val observed = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeUsageDataSharing().collect(observed::add)
        }

        // When
        updateUsageDataSharing(false)

        // Then
        assertEquals(listOf(true, false), observed)
    }

    @Test
    fun `GIVEN usage data sharing on WHEN turning it off THEN stores off`() = runTest {
        // Given
        prepareScenario(storedOn = true)

        // When
        updateUsageDataSharing(false)

        // Then
        assertEquals(listOf(false), repository.updatedUsageDataSharing)
        assertEquals(false, repository.usageDataSharing.value)
    }

    @Test
    fun `GIVEN usage data sharing off WHEN turning it on THEN stores on`() = runTest {
        // Given
        prepareScenario(storedOn = false)

        // When
        updateUsageDataSharing(true)

        // Then
        assertEquals(listOf(true), repository.updatedUsageDataSharing)
        assertEquals(true, repository.usageDataSharing.value)
    }

    private fun prepareScenario(storedOn: Boolean = true) {
        repository = FakeDeviceSettingsRepository().also { it.usageDataSharing.value = storedOn }
        observeUsageDataSharing = ObserveUsageDataSharingUseCase(repository)
        updateUsageDataSharing = UpdateUsageDataSharingUseCase(repository)
    }
}
