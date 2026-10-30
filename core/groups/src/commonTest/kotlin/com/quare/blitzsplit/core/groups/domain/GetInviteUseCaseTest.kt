package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteTarget
import com.quare.blitzsplit.core.groups.domain.usecase.impl.GetInviteUseCase
import com.quare.blitzsplit.core.groups.testing.FakeInviteRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class GetInviteUseCaseTest {
    private lateinit var repository: FakeInviteRepository
    private lateinit var useCase: GetInviteUseCase

    @Test
    fun `GIVEN a code of a group WHEN getting its invite THEN returns the invite of that code`() = runTest {
        // Given
        prepareScenario(invite = invite)

        // When
        val result = useCase(groupLink)

        // Then
        assertEquals(invite, result.getOrThrow())
        assertEquals(listOf("aB3dE5gH"), repository.fetchedCodes)
    }

    @Test
    fun `GIVEN a code no group has WHEN getting its invite THEN succeeds with no invite`() = runTest {
        // Given
        prepareScenario(invite = null)

        // When
        val result = useCase(groupLink)

        // Then
        assertNull(result.getOrThrow())
    }

    @Test
    fun `GIVEN the server can not be reached WHEN getting an invite THEN returns the error`() = runTest {
        // Given
        prepareScenario(fetchError = IllegalStateException("offline"))

        // When
        val result = useCase(groupLink)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private val groupLink = InviteTarget(
        code = "aB3dE5gH",
        memberCode = null,
        inviterCode = null,
    )

    private val invite = Invite(
        code = "aB3dE5gH",
        groupId = "group-1",
        groupName = "Viagem Floripa",
        groupType = GroupType.TRIP,
        groupColor = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        creatorName = "Pierre",
        isMember = false,
        members = emptyList(),
    )

    private fun prepareScenario(
        invite: Invite? = null,
        fetchError: Throwable? = null,
    ) {
        repository = FakeInviteRepository(invite = invite).also { it.fetchError = fetchError }
        useCase = GetInviteUseCase(repository)
    }
}
