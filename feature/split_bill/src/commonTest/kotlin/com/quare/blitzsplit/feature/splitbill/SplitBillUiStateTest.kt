package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SplitBillUiStateTest {
    @Test
    fun `GIVEN a bill filled in WHEN reading Salvar THEN it is enabled`() {
        // Given
        val state = state()

        // When
        val canSave = state.canSave

        // Then
        assertTrue(canSave)
    }

    @Test
    fun `GIVEN a payer who is not in the group WHEN reading Salvar THEN it is disabled`() {
        // Given
        val state = state(payerId = "someone-else")

        // When
        val canSave = state.canSave

        // Then
        assertNull(state.payer)
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN a save in flight WHEN reading Salvar THEN it is disabled`() {
        // Given
        val state = state(isSaving = true)

        // When
        val canSave = state.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN Room not read WHEN reading Salvar THEN it is disabled`() {
        // Given
        val state = state(isLoaded = false)

        // When
        val canSave = state.canSave

        // Then
        assertFalse(canSave)
    }

    @Test
    fun `GIVEN a split that does not add up WHEN reading Salvar and the help THEN it is disabled with no refund`() {
        // Given
        val state = state(splitMode = SplitMode.AMOUNT)

        // When
        val canSave = state.canSave

        // Then
        assertFalse(canSave)
        assertNull(state.refundMinor)
        assertNull(state.myShareMinor)
    }

    @Test
    fun `GIVEN this device user paid WHEN reading the help THEN shows their share and refund`() {
        // Given
        val state = state()

        // When
        val refund = state.refundMinor

        // Then
        assertEquals(5_000L, state.myShareMinor)
        assertEquals(5_000L, refund)
    }

    @Test
    fun `GIVEN nobody on the device in the group WHEN reading the help THEN shows no share`() {
        // Given
        val state = state(people = listOf(person(ANA, isMe = false), person(PIERRE, isMe = false)))

        // When
        val share = state.myShareMinor

        // Then
        assertNull(share)
        assertFalse(state.payerIsMe)
        assertNull(state.refundMinor)
    }

    @Test
    fun `GIVEN no internet WHEN reading the screen THEN shows the offline notice`() {
        // Given
        val state = state(isOnline = false)

        // When
        val showsOfflineNotice = state.showsOfflineNotice

        // Then
        assertTrue(showsOfflineNotice)
    }

    private fun person(
        id: String,
        isMe: Boolean,
    ): BillPerson = BillPerson(
        id = id,
        name = id,
        initials = "",
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = true,
    )

    // Why: a 100,00 bill between Ana and Pierre, who paid, split equally unless the mode says otherwise.
    private fun state(
        people: List<BillPerson> = listOf(person(ANA, isMe = false), person(PIERRE, isMe = true)),
        payerId: String = PIERRE,
        splitMode: SplitMode = SplitMode.EQUAL,
        isSaving: Boolean = false,
        isLoaded: Boolean = true,
        isOnline: Boolean = true,
    ): SplitBillUiState = SplitBillUiState(
        isEditing = false,
        groupName = "Viagem Floripa",
        groupInitials = "VF",
        groupColor = AvatarColor.GREEN,
        currency = GroupCurrency.BRL,
        people = people,
        amountText = "100,00",
        description = "Mercado",
        hasEditedAmount = true,
        hasEditedDescription = true,
        payerId = payerId,
        payerSelection = null,
        spentAt = 0L,
        spentDay = RelativeDay.TODAY,
        latestSpentAt = 0L,
        split = SplitEditor(
            mode = splitMode,
            rows = people.map {
                SplitRow(
                    it,
                    isIncluded = true,
                    valueText = "",
                    parts = 0L,
                    shareMinor = null,
                )
            },
            totalMinor = 10_000L,
            payerId = payerId,
            currency = GroupCurrency.BRL,
        ),
        splitEditor = null,
        conflict = null,
        deletion = null,
        isOnline = isOnline,
        isSaving = isSaving,
        isDeleting = false,
        isLoaded = isLoaded,
    )

    private companion object {
        const val ANA = "member-1"
        const val PIERRE = "member-2"
    }
}
