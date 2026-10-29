package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePendingNotificationPreferenceChangesUseCase
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObservePendingNotificationPreferenceChangesUseCaseTest {
    @Test
    fun `GIVEN a toggle change waiting to be pushed WHEN observing THEN reports it`() = runTest {
        // Given
        val repository = FakeNotificationPreferencesRepository().apply { hasPendingChanges.value = true }
        val useCase = ObservePendingNotificationPreferenceChangesUseCase(repository)

        // When
        val hasPendingChanges = useCase().first()

        // Then
        assertTrue(hasPendingChanges)
    }

    @Test
    fun `GIVEN nothing waiting to be pushed WHEN observing THEN reports none`() = runTest {
        // Given
        val repository = FakeNotificationPreferencesRepository()
        val useCase = ObservePendingNotificationPreferenceChangesUseCase(repository)

        // When
        val hasPendingChanges = useCase().first()

        // Then
        assertFalse(hasPendingChanges)
    }
}
