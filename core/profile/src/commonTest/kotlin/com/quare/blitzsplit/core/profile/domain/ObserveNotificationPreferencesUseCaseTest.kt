package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveNotificationPreferencesUseCase
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveNotificationPreferencesUseCaseTest {
    @Test
    fun `GIVEN only invites on WHEN observing THEN shows only invites enabled`() = runTest {
        // Given
        val onlyInvites = NotificationPreferences(setOf(NotificationType.INVITES))
        val repository = FakeNotificationPreferencesRepository(initialPreferences = onlyInvites)
        val useCase = ObserveNotificationPreferencesUseCase(repository)

        // When
        val preferences = useCase().first()

        // Then
        assertEquals(onlyInvites, preferences)
    }
}
