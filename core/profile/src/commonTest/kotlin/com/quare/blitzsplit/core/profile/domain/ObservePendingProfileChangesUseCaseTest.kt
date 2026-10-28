package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePendingProfileChangesUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ObservePendingProfileChangesUseCaseTest {
    @Test
    fun `GIVEN a change waiting to be pushed WHEN observing THEN reports it`() = runTest {
        // Given
        val repository = FakeProfileRepository().apply { hasPendingChanges.value = true }
        val useCase = ObservePendingProfileChangesUseCase(repository)

        // When
        val hasPendingChanges = useCase().first()

        // Then
        assertTrue(hasPendingChanges)
    }
}
