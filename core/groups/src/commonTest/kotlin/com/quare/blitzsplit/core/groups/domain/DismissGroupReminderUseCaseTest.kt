package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.usecase.impl.DismissGroupReminderUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DismissGroupReminderUseCaseTest {
    private val repository = FakeGroupRepository()
    private val useCase = DismissGroupReminderUseCase(repository)

    @Test
    fun `GIVEN a reminder banner WHEN dismissing it THEN asks the repository to dismiss that group and occurrence`() =
        runTest {
            // Given
            val occurrenceAt = millisAt(2026, 11, 16, 10)

            // When
            val result = useCase(
                groupId = "group-1",
                occurrenceAt = occurrenceAt,
            )

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("group-1" to occurrenceAt), repository.dismissedReminders)
        }
}
