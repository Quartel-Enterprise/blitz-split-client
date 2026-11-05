package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.profile.domain.usecase.impl.DismissAppInviteUseCase
import com.quare.blitzsplit.core.profile.testing.FakeAppInviteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DismissAppInviteUseCaseTest {
    private val repository = FakeAppInviteRepository()
    private val useCase = DismissAppInviteUseCase(repository)

    @Test
    fun `GIVEN an open invite WHEN dismissing it THEN the repository dismisses it once and it waits to sync`() =
        runTest {
            // Given
            val wasDismissed = repository.status.value.isDismissed

            // When
            useCase()

            // Then
            assertEquals(false, wasDismissed)
            assertEquals(1, repository.dismissCount)
            assertTrue(repository.observeStatus().first().isDismissed)
            assertTrue(repository.observeHasPendingChanges().first())
        }
}
