package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.SimplifyDebtsConflictNotice
import com.quare.blitzsplit.core.expenses.domain.model.SimplifyDebtsConflicts
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SimplifyDebtsConflictsTest {
    @Test
    fun `GIVEN no later change lost to WHEN reading the notice THEN there is none`() {
        // Given
        val group = group(
            conflictBy = null,
            simplifiesDebts = true,
        )

        // When
        val notice = SimplifyDebtsConflicts.noticeOrNull(group)

        // Then
        assertNull(notice)
    }

    @Test
    fun `GIVEN a member turned it off after this device WHEN reading the notice THEN names them and says it is off`() {
        // Given
        val group = group(
            conflictBy = "user-ana",
            simplifiesDebts = false,
        )

        // When
        val notice = SimplifyDebtsConflicts.noticeOrNull(group)

        // Then
        assertEquals(
            SimplifyDebtsConflictNotice(
                changedByName = "Ana",
                simplifiesDebts = false,
            ),
            notice,
        )
    }

    @Test
    fun `GIVEN someone no longer in the group changed it WHEN reading the notice THEN names nobody`() {
        // Given
        val group = group(
            conflictBy = "user-gone",
            simplifiesDebts = true,
        )

        // When
        val notice = SimplifyDebtsConflicts.noticeOrNull(group)

        // Then
        assertEquals(
            SimplifyDebtsConflictNotice(
                changedByName = null,
                simplifiesDebts = true,
            ),
            notice,
        )
    }

    private fun group(
        conflictBy: String?,
        simplifiesDebts: Boolean,
    ): Group = Group(
        id = "floripa",
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(
            member(
                id = "pierre",
                name = "Pierre",
                userId = "user-pierre",
            ),
            member(
                id = "ana",
                name = "Ana",
                userId = "user-ana",
            ),
            member(
                id = "bruno",
                name = "Bruno",
                userId = null,
            ),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = simplifiesDebts,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = conflictBy,
        simplifyDebtsConflictBy = conflictBy,
    )

    private fun member(
        id: String,
        name: String,
        userId: String?,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = id == "pierre",
        hasJoined = userId != null,
        userId = userId,
        inviteCode = null,
        photoUrl = null,
    )
}
