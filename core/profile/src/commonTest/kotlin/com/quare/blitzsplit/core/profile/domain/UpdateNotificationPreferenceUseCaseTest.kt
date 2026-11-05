package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.profile.domain.usecase.impl.UpdateNotificationPreferenceUseCase
import com.quare.blitzsplit.core.profile.testing.FakeNotificationPreferencesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateNotificationPreferenceUseCaseTest {
    @Test
    fun `GIVEN the defaults WHEN turning off marked payments THEN asks the repository to turn off that type`() =
        runTest {
            // Given
            val repository = FakeNotificationPreferencesRepository()
            val useCase = UpdateNotificationPreferenceUseCase(repository)

            // When
            useCase(
                type = NotificationType.PAYMENTS,
                channel = NotificationChannel.PUSH,
                isEnabled = false,
            )

            // Then
            assertEquals(listOf(Triple(NotificationType.PAYMENTS, NotificationChannel.PUSH, false)), repository.updates)
        }

    @Test
    fun `GIVEN the defaults WHEN turning on invites THEN asks the repository to turn on that type`() = runTest {
        // Given
        val repository = FakeNotificationPreferencesRepository()
        val useCase = UpdateNotificationPreferenceUseCase(repository)

        // When
        useCase(
            type = NotificationType.PEOPLE_AND_GROUPS,
            channel = NotificationChannel.PUSH,
            isEnabled = true,
        )

        // Then
        assertEquals(
            listOf(Triple(NotificationType.PEOPLE_AND_GROUPS, NotificationChannel.PUSH, true)),
            repository.updates,
        )
    }
}
