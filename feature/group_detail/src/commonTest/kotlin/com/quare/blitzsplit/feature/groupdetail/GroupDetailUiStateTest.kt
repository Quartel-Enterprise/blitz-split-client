package com.quare.blitzsplit.feature.groupdetail

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.expenses.domain.model.GroupFace
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRowPayer
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupDetailUiStateTest {
    @Test
    fun `GIVEN six members and four faces WHEN reading the extra count THEN it is two`() {
        // Given
        val state = state(
            memberCount = 6,
            faceCount = 4,
        )

        // When
        val extra = state.header.extraMemberCount

        // Then
        assertEquals(2, extra)
    }

    @Test
    fun `GIVEN as many members as faces WHEN reading the extra count THEN it is zero`() {
        // Given
        val state = state(
            memberCount = 3,
            faceCount = 3,
        )

        // When
        val extra = state.header.extraMemberCount

        // Then
        assertEquals(0, extra)
    }

    @Test
    fun `GIVEN fewer members than faces WHEN reading the extra count THEN it is never negative`() {
        // Given
        val state = state(
            memberCount = 0,
            faceCount = 2,
        )

        // When
        val extra = state.header.extraMemberCount

        // Then
        assertEquals(0, extra)
    }

    @Test
    fun `GIVEN Room not read yet WHEN reading the empty state and split button THEN neither shows`() {
        // Given
        val state = state(
            isLoaded = false,
            hasBills = false,
        )

        // When
        val showsEmptyOrButton = state.isEmpty || state.showsSplitBillButton

        // Then
        assertFalse(showsEmptyOrButton)
    }

    @Test
    fun `GIVEN loaded without bills WHEN reading the empty state and split button THEN only the empty state shows`() {
        // Given
        val state = state(
            isLoaded = true,
            hasBills = false,
        )

        // When
        val isEmpty = state.isEmpty

        // Then
        assertTrue(isEmpty)
        assertFalse(state.showsSplitBillButton)
    }

    @Test
    fun `GIVEN loaded with bills WHEN reading the empty state and split button THEN only the split button shows`() {
        // Given
        val state = state(
            isLoaded = true,
            hasBills = true,
        )

        // When
        val showsSplitBillButton = state.showsSplitBillButton

        // Then
        assertTrue(showsSplitBillButton)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `GIVEN just joined with an unprotected account WHEN checking the protect access invite THEN it shows`() {
        // Given
        val state = state(
            joinedAs = "Bruno",
            isAccountUnprotected = true,
        )

        // When
        val shows = state.showsProtectAccess

        // Then
        assertTrue(shows)
    }

    @Test
    fun `GIVEN just joined with a protected account WHEN checking the protect access invite THEN it hides`() {
        // Given
        val state = state(
            joinedAs = "Bruno",
            isAccountUnprotected = false,
        )

        // When
        val shows = state.showsProtectAccess

        // Then
        assertFalse(shows)
    }

    @Test
    fun `GIVEN not opened from a join with an unprotected account WHEN checking the invite THEN it hides`() {
        // Given
        val state = state(
            joinedAs = null,
            isAccountUnprotected = true,
        )

        // When
        val shows = state.showsProtectAccess

        // Then
        assertFalse(shows)
    }

    private fun state(
        memberCount: Int = 4,
        faceCount: Int = 4,
        isLoaded: Boolean = true,
        hasBills: Boolean = true,
        joinedAs: String? = null,
        isAccountUnprotected: Boolean = false,
    ): GroupDetailUiState = GroupDetailUiState(
        header = GroupHeader(
            groupName = "Viagem de Floripa",
            groupInitials = "VF",
            groupColor = AvatarColor.LILAC,
            currency = GroupCurrency.BRL,
            faces = List(faceCount) { GroupFace("A", AvatarColor.PEACH) },
            memberCount = memberCount,
            notJoinedCount = 0,
            totalSpentMinor = if (hasBills) 12_000 else 0,
            myBalance = null,
            isEveryoneSettled = false,
            isPinned = false,
        ),
        days = if (hasBills) listOf(day) else emptyList(),
        paymentToDelete = null,
        recordedPayment = null,
        showsBillDeletedNotice = false,
        exit = GroupExitState.idle,
        isLoaded = isLoaded,
        homeTab = HomeTab.GROUPS,
        joinedAs = joinedAs,
        isAccountUnprotected = isAccountUnprotected,
        activityUnreadCount = 0,
    )

    private val day = ExpenseDay(
        dayStartMillis = 0L,
        relativeDay = RelativeDay.TODAY,
        entries = listOf(
            ExpenseRow(
                id = "dinner",
                description = "Jantar",
                spentAt = 0L,
                payers = listOf(
                    ExpenseRowPayer(
                        name = "Ana",
                        isMe = false,
                    ),
                ),
                totalMinor = 12_000,
                myBalanceMinor = -6_000,
                category = CategoryTree.appOnly.other,
                isPending = false,
            ),
        ),
    )
}
