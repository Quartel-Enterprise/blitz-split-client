package com.quare.blitzsplit.core.contacts.domain

import com.quare.blitzsplit.core.contacts.contact
import com.quare.blitzsplit.core.contacts.contactGroup
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.RequestGuestMergeUseCase
import com.quare.blitzsplit.core.contacts.testing.FakeGuestMergeRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RequestGuestMergeUseCaseTest {
    private lateinit var repository: FakeGuestMergeRepository
    private lateinit var useCase: RequestGuestMergeUseCase

    @Test
    fun `GIVEN a guest in two groups WHEN asking to merge them THEN asks for their member in every group`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(diego)

        // Then
        assertEquals(listOf("diego-ape", "diego-futebol"), result.getOrNull()?.memberIds)
        assertEquals(listOf(listOf("diego-ape", "diego-futebol")), repository.requestedMemberIds)
        assertEquals(result.getOrNull(), repository.pending)
    }

    @Test
    fun `GIVEN someone with an account WHEN asking to merge them THEN fails without asking the server`() = runTest {
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
        assertTrue(result.isFailure)
        assertTrue(repository.requestedMemberIds.isEmpty())
    }

    @Test
    fun `GIVEN the server fails WHEN asking to merge a guest THEN returns that failure`() = runTest {
        // Given
        prepareScenario(requestError = IllegalStateException("offline"))

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

    private fun prepareScenario(requestError: Throwable? = null) {
        repository = FakeGuestMergeRepository().also { it.requestError = requestError }
        useCase = RequestGuestMergeUseCase(repository)
    }
}
