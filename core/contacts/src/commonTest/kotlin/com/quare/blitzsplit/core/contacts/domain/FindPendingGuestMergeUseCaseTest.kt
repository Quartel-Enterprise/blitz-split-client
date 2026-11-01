package com.quare.blitzsplit.core.contacts.domain

import com.quare.blitzsplit.core.contacts.contact
import com.quare.blitzsplit.core.contacts.contactGroup
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.FindPendingGuestMergeUseCase
import com.quare.blitzsplit.core.contacts.testing.FakeGuestMergeRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FindPendingGuestMergeUseCaseTest {
    private lateinit var repository: FakeGuestMergeRepository
    private lateinit var useCase: FindPendingGuestMergeUseCase

    @Test
    fun `GIVEN a request waiting for the guest WHEN looking for it THEN finds it by their members`() = runTest {
        // Given
        prepareScenario(pending = waiting)

        // When
        val result = useCase(diego)

        // Then
        assertEquals(waiting, result.getOrNull())
        assertEquals(listOf(listOf("diego-ape", "diego-futebol")), repository.pendingLookups)
    }

    @Test
    fun `GIVEN no request waiting WHEN looking for one THEN finds none`() = runTest {
        // Given
        prepareScenario(pending = null)

        // When
        val result = useCase(diego)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `GIVEN someone with an account WHEN looking for a request THEN finds none without asking the server`() =
        runTest {
            // Given
            prepareScenario(pending = waiting)
            val carla = contact(
                key = "user:carla",
                name = "Carla",
                userId = "carla",
                groups = listOf(contactGroup("ape", "diego-ape")),
            )

            // When
            val result = useCase(carla)

            // Then
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
            assertTrue(repository.pendingLookups.isEmpty())
        }

    @Test
    fun `GIVEN the server fails WHEN looking for a request THEN returns that failure`() = runTest {
        // Given
        prepareScenario(findError = IllegalStateException("offline"))

        // When
        val result = useCase(diego)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private val diego = contact(
        key = "name:diego",
        name = "Diego",
        groups = listOf(contactGroup("ape", "diego-ape"), contactGroup("futebol", "diego-futebol")),
    )
    private val waiting = GuestMerge(
        id = "merge-1",
        memberIds = listOf("diego-ape", "diego-futebol"),
        link = "https://blitzsplit.com/g/Code1234/dg1234",
        requestedAt = 100L,
    )

    private fun prepareScenario(
        pending: GuestMerge? = null,
        findError: Throwable? = null,
    ) {
        repository = FakeGuestMergeRepository(pending).also { it.findError = findError }
        useCase = FindPendingGuestMergeUseCase(repository)
    }
}
