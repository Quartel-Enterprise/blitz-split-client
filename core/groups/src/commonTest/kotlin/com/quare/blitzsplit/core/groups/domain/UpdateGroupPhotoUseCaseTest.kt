package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.UpdateGroupPhotoUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.ProfilePhoto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UpdateGroupPhotoUseCaseTest {
    private lateinit var repository: FakeGroupRepository
    private lateinit var useCase: UpdateGroupPhotoUseCase

    private val trip = Group(
        id = "group-1",
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.PEACH,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(
            GroupMember(
                id = "m-1",
                name = "Pierre",
                avatarColor = AvatarColor.LILAC,
                isMe = true,
                hasJoined = true,
                userId = null,
                inviteCode = null,
                photoUrl = null,
            ),
        ),
        isPending = false,
        inviteCode = "Xy7kQ2",
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    @Test
    fun `GIVEN a group WHEN saving its photo THEN writes that group at the current time`() = runTest {
        // Given
        prepareScenario(now = 3_000L)

        // When
        val result = useCase(
            groupId = "group-1",
            photo = ProfilePhoto.Remote("https://example.com/photo.jpg"),
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("group-1"), repository.updatedGroupIds)
        assertEquals(listOf(3_000L), repository.groupUpdateTimes)
    }

    @Test
    fun `GIVEN a repository that fails WHEN saving its photo THEN returns the failure`() = runTest {
        // Given
        prepareScenario(updateError = IllegalStateException("room failed"))

        // When
        val result = useCase(
            groupId = "group-1",
            photo = null,
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun prepareScenario(
        now: Long = 0L,
        updateError: Throwable? = null,
    ) {
        repository = FakeGroupRepository(listOf(trip)).also { it.updateGroupError = updateError }
        useCase = UpdateGroupPhotoUseCase(
            repository = repository,
            currentTimeMillis = { now },
        )
    }
}
