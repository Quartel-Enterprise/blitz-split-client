package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillConflict
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

    @Test
    fun `GIVEN a saved bill being edited WHEN reading Apagar conta and the hint THEN both show`() {
        // Given
        val state = state(isEditing = true)

        // When
        val canDelete = state.canDelete

        // Then
        assertTrue(canDelete)
        assertTrue(state.showsAnyoneCanEditHint)
    }

    @Test
    fun `GIVEN a new bill WHEN reading Apagar conta and the hint THEN neither shows`() {
        // Given
        val state = state(isEditing = false)

        // When
        val canDelete = state.canDelete

        // Then
        assertFalse(canDelete)
        assertFalse(state.showsAnyoneCanEditHint)
    }

    @Test
    fun `GIVEN someone else saved the bill meanwhile WHEN reading the hint THEN the warning takes its place`() {
        // Given
        val state = state(
            isEditing = true,
            conflict = BillConflict(
                changerName = "Ana",
                changerIsMe = false,
                changedAt = 1L,
            ),
        )

        // When
        val showsHint = state.showsAnyoneCanEditHint

        // Then
        assertFalse(showsHint)
        assertTrue(state.canDelete)
        assertTrue(state.canSave)
    }

    @Test
    fun `GIVEN the edit being saved WHEN reading Apagar conta THEN it is disabled`() {
        // Given
        val state = state(
            isEditing = true,
            isSaving = true,
        )

        // When
        val canDelete = state.canDelete

        // Then
        assertFalse(canDelete)
    }

    @Test
    fun `GIVEN the bill being deleted WHEN reading Salvar and Apagar conta THEN both are disabled`() {
        // Given
        val state = state(
            isEditing = true,
            isDeleting = true,
        )

        // When
        val canDelete = state.canDelete

        // Then
        assertFalse(canDelete)
        assertFalse(state.canSave)
    }

    @Test
    fun `GIVEN Room not read while editing WHEN reading Apagar conta THEN it is disabled`() {
        // Given
        val state = state(
            isEditing = true,
            isLoaded = false,
        )

        // When
        val canDelete = state.canDelete

        // Then
        assertFalse(canDelete)
    }

    @Test
    fun `GIVEN a by-amount bill being edited WHEN reading the amount help THEN says a new total rescales it`() {
        // Given
        val state = state(
            isEditing = true,
            splitMode = SplitMode.AMOUNT,
        )

        // When
        val showsHint = state.showsProportionalAmountHint

        // Then
        assertTrue(showsHint)
    }

    @Test
    fun `GIVEN a by-amount bill with no valid total WHEN reading the amount help THEN only the error shows`() {
        // Given
        val state = state(
            isEditing = true,
            splitMode = SplitMode.AMOUNT,
            amountText = "",
        )

        // When
        val showsHint = state.showsProportionalAmountHint

        // Then
        assertFalse(showsHint)
    }

    @Test
    fun `GIVEN a new by-amount bill WHEN reading the amount help THEN says nothing of rescaling`() {
        // Given
        val state = state(splitMode = SplitMode.AMOUNT)

        // When
        val showsHint = state.showsProportionalAmountHint

        // Then
        assertFalse(showsHint)
    }

    @Test
    fun `GIVEN an edited bill whose payer left the group WHEN reading Salvar THEN a former member paid and it saves`() {
        // Given
        val state = state(
            isEditing = true,
            payerId = FORMER,
        )

        // When
        val canSave = state.canSave

        // Then
        assertTrue(state.isPayerFormerMember)
        assertTrue(canSave)
    }

    @Test
    fun `GIVEN a new bill whose payer is no member WHEN reading Salvar THEN it is disabled`() {
        // Given
        val state = state(payerId = FORMER)

        // When
        val canSave = state.canSave

        // Then
        assertFalse(state.isPayerFormerMember)
        assertFalse(canSave)
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
        isEditing: Boolean = false,
        isDeleting: Boolean = false,
        conflict: BillConflict? = null,
        amountText: String = "100,00",
    ): SplitBillUiState = SplitBillUiState(
        isEditing = isEditing,
        groupName = "Viagem Floripa",
        groupInitials = "VF",
        groupColor = AvatarColor.GREEN,
        currency = GroupCurrency.BRL,
        people = people,
        amountText = amountText,
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
        conflict = conflict,
        deletion = null,
        isOnline = isOnline,
        isSaving = isSaving,
        isDeleting = isDeleting,
        isLoaded = isLoaded,
    )

    private companion object {
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val FORMER = "member-9"
    }
}
