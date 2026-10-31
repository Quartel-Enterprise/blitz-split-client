package com.quare.blitzsplit.feature.groupdetail

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiState
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DebtKind
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DebtRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BalancesUiStateTest {
    @Test
    fun `GIVEN Room not read yet WHEN reading the empty state THEN it does not show`() {
        // Given
        val state = state(isLoaded = false)

        // When
        val isEmpty = state.isEmpty

        // Then
        assertFalse(isEmpty)
    }

    @Test
    fun `GIVEN loaded before the first bill WHEN reading the empty state THEN it shows`() {
        // Given
        val state = state()

        // When
        val isEmpty = state.isEmpty

        // Then
        assertTrue(isEmpty)
    }

    @Test
    fun `GIVEN bills that everyone settled WHEN reading the empty state THEN it does not show`() {
        // Given
        val state = state(hasBills = true)

        // When
        val isEmpty = state.isEmpty

        // Then
        assertFalse(isEmpty)
    }

    @Test
    fun `GIVEN a debt with this device user and no bill WHEN reading the empty state THEN it does not show`() {
        // Given
        val state = state(myDebts = listOf(debt(DebtKind.OWES_YOU)))

        // When
        val isEmpty = state.isEmpty

        // Then
        assertFalse(isEmpty)
    }

    @Test
    fun `GIVEN a debt between others and no bill WHEN reading the empty state THEN it does not show`() {
        // Given
        val state = state(otherDebts = listOf(debt(DebtKind.BETWEEN_OTHERS)))

        // When
        val isEmpty = state.isEmpty

        // Then
        assertFalse(isEmpty)
    }

    @Test
    fun `GIVEN no debt WHEN reading the debt note THEN it does not show`() {
        // Given
        val state = state(hasBills = true)

        // When
        val showsDebtNote = state.showsDebtNote

        // Then
        assertFalse(showsDebtNote)
    }

    @Test
    fun `GIVEN a debt with this device user WHEN reading the debt note THEN it shows`() {
        // Given
        val state = state(
            hasBills = true,
            myDebts = listOf(debt(DebtKind.YOU_OWE)),
        )

        // When
        val showsDebtNote = state.showsDebtNote

        // Then
        assertTrue(showsDebtNote)
    }

    @Test
    fun `GIVEN only debts between others WHEN reading the debt note THEN it shows`() {
        // Given
        val state = state(
            hasBills = true,
            otherDebts = listOf(debt(DebtKind.BETWEEN_OTHERS)),
        )

        // When
        val showsDebtNote = state.showsDebtNote

        // Then
        assertTrue(showsDebtNote)
    }

    @Test
    fun `GIVEN Room not read yet WHEN reading the split button THEN it does not show`() {
        // Given
        val state = state(isLoaded = false)

        // When
        val showsSplitBillButton = state.showsSplitBillButton

        // Then
        assertFalse(showsSplitBillButton)
    }

    @Test
    fun `GIVEN loaded before the first bill WHEN reading the split button THEN it shows`() {
        // Given
        val state = state()

        // When
        val showsSplitBillButton = state.showsSplitBillButton

        // Then
        assertTrue(showsSplitBillButton)
    }

    private fun debt(kind: DebtKind): DebtRow = DebtRow(
        fromMemberId = "member-1",
        toMemberId = "member-2",
        fromName = "Ana",
        fromFace = null,
        toName = "Pierre",
        toFace = null,
        amountMinor = 5_000,
        kind = kind,
    )

    private fun state(
        isLoaded: Boolean = true,
        hasBills: Boolean = false,
        myDebts: List<DebtRow> = emptyList(),
        otherDebts: List<DebtRow> = emptyList(),
    ): BalancesUiState = BalancesUiState(
        header = GroupHeader(
            groupName = "Viagem de Floripa",
            groupInitials = "VF",
            groupColor = AvatarColor.LILAC,
            currency = GroupCurrency.BRL,
            faces = emptyList(),
            memberCount = 2,
            notJoinedCount = 0,
            totalSpentMinor = 0L,
            myBalance = null,
            isEveryoneSettled = false,
        ),
        myDebts = myDebts,
        otherDebts = otherDebts,
        hasBills = hasBills,
        recordedPayment = null,
        isLoaded = isLoaded,
    )
}
