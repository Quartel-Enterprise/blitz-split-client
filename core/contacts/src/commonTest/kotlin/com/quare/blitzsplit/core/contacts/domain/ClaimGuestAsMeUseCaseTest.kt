package com.quare.blitzsplit.core.contacts.domain

import com.quare.blitzsplit.core.contacts.contact
import com.quare.blitzsplit.core.contacts.contactGroup
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.ClaimGuestAsMeUseCase
import com.quare.blitzsplit.core.contacts.testing.FakeGuestMergeRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClaimGuestAsMeUseCaseTest {
    private lateinit var repository: FakeGuestMergeRepository
    private lateinit var useCase: ClaimGuestAsMeUseCase

    @Test
    fun `GIVEN a guest in two groups WHEN claiming them THEN claims their member in every group`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(diego)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(listOf("diego-ape", "diego-futebol")), repository.claimedMemberIds)
    }

    @Test
    fun `GIVEN a guest with the same member listed twice WHEN claiming them THEN claims each member once`() = runTest {
        // Given
        prepareScenario()
        val repeated = contact(
            key = "name:diego",
            name = "Diego",
            groups = listOf(contactGroup("ape", "diego-ape"), contactGroup("futebol", "diego-ape")),
        )

        // When
        val result = useCase(repeated)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(listOf("diego-ape")), repository.claimedMemberIds)
    }

    @Test
    fun `GIVEN someone with an account WHEN claiming them THEN fails without asking the server`() = runTest {
        // Given
        prepareScenario()
        val carla = contact(
            key = "user:carla",
            name = "Carla",
            userId = "carla",
            groups = listOf(contactGroup("ape", "carla-ape")),
        )

        // When
        val result = useCase(carla)

        // Then
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertTrue(repository.claimedMemberIds.isEmpty())
    }

    @Test
    fun `GIVEN the server fails WHEN claiming a guest THEN returns that failure`() = runTest {
        // Given
        prepareScenario(claimError = IllegalStateException("offline"))

        // When
        val result = useCase(diego)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a merge request waiting for the guest WHEN claiming them THEN the request is gone`() = runTest {
        // Given
        val waiting = GuestMerge(
            id = "merge-1",
            memberIds = listOf("diego-ape", "diego-futebol"),
            link = "https://blitzsplit.com/g/Code1234/dg1234",
            requestedAt = 1L,
        )
        prepareScenario(pending = waiting)

        // When
        val result = useCase(diego)

        // Then
        assertTrue(result.isSuccess)
        assertNull(repository.pending)
    }

    private val diego = contact(
        key = "name:diego",
        name = "Diego",
        groups = listOf(contactGroup("ape", "diego-ape"), contactGroup("futebol", "diego-futebol")),
    )

    private fun prepareScenario(
        claimError: Throwable? = null,
        pending: GuestMerge? = null,
    ) {
        repository = FakeGuestMergeRepository(pending = pending).also { it.claimError = claimError }
        useCase = ClaimGuestAsMeUseCase(repository)
    }
}
