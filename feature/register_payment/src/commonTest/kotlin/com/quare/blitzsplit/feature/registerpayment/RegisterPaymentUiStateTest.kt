package com.quare.blitzsplit.feature.registerpayment

import com.quare.blitzsplit.core.expenses.domain.model.PersonGroupBalance
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegisterPaymentUiStateTest {
    @Test
    fun `GIVEN the device user receives WHEN reading the kind THEN it is received`() {
        // Given
        val state = state(
            from = ana,
            to = me,
        )

        // When
        val kind = state.kind

        // Then
        assertEquals(PaymentKind.RECEIVED, kind)
    }

    @Test
    fun `GIVEN the device user pays WHEN reading the kind THEN it is paid`() {
        // Given
        val state = state(
            from = me,
            to = ana,
        )

        // When
        val kind = state.kind

        // Then
        assertEquals(PaymentKind.PAID, kind)
    }

    @Test
    fun `GIVEN two other members WHEN reading the kind THEN it is between others`() {
        // Given
        val state = state(
            from = diego,
            to = ana,
        )

        // When
        val kind = state.kind

        // Then
        assertEquals(PaymentKind.BETWEEN_OTHERS, kind)
    }

    @Test
    fun `GIVEN a valid amount but nothing loaded WHEN reading whether it can confirm THEN it cannot`() {
        // Given
        val state = state(
            amountText = "100,00",
            isLoaded = false,
        )

        // When
        val canConfirm = state.canConfirm

        // Then
        assertFalse(canConfirm)
    }

    @Test
    fun `GIVEN a valid amount while saving WHEN reading whether it can confirm THEN it cannot`() {
        // Given
        val state = state(
            amountText = "100,00",
            isSaving = true,
        )

        // When
        val canConfirm = state.canConfirm

        // Then
        assertFalse(canConfirm)
    }

    @Test
    fun `GIVEN a partial amount WHEN reading the state THEN it can confirm and shows what is left`() {
        // Given
        val state = state(amountText = "100,00")

        // When
        val remaining = state.remainingMinor

        // Then
        assertEquals(6_230L, remaining)
        assertTrue(state.canConfirm)
        assertFalse(state.isFullDebt)
        assertFalse(state.showsOverDebtError)
    }

    @Test
    fun `GIVEN text that is no amount WHEN reading the state THEN it is neither the whole debt nor over it`() {
        // Given
        val state = state(amountText = ".")

        // When
        val amount = state.amountMinor

        // Then
        assertNull(amount)
        assertFalse(state.isAmountValid)
        assertFalse(state.isFullDebt)
        assertFalse(state.showsOverDebtError)
        assertNull(state.remainingMinor)
    }

    @Test
    fun `GIVEN an amount over the debt WHEN reading the state THEN shows the error and nothing left`() {
        // Given
        val state = state(amountText = "500")

        // When
        val showsError = state.showsOverDebtError

        // Then
        assertTrue(showsError)
        assertFalse(state.isAmountValid)
        assertNull(state.remainingMinor)
        assertFalse(state.canConfirm)
    }

    @Test
    fun `GIVEN a settling up across groups WHEN typing less than the net THEN the amount is not valid`() {
        // Given
        val state = state(
            amountText = "100,00",
            groupParts = twoGroups,
        )

        // When
        val isValid = state.isAmountValid

        // Then
        assertTrue(state.isAmountLocked)
        assertFalse(isValid)
        assertFalse(state.canConfirm)
    }

    @Test
    fun `GIVEN a settling up across groups WHEN the amount is the whole net THEN it can be confirmed`() {
        // Given
        val state = state(
            amountText = "162,30",
            groupParts = twoGroups,
        )

        // When
        val canConfirm = state.canConfirm

        // Then
        assertTrue(canConfirm)
    }

    @Test
    fun `GIVEN a pair of one group WHEN reading the amount THEN it is not locked`() {
        // Given
        val state = state()

        // When
        val isLocked = state.isAmountLocked

        // Then
        assertFalse(isLocked)
    }

    @Test
    fun `GIVEN a group in yen WHEN typing the debt with a thousands dot THEN it is the whole debt`() {
        // Given
        val state = state(
            currency = GroupCurrency.JPY,
            debtMinor = 1_500L,
            amountText = "1.500",
        )

        // When
        val amount = state.amountMinor

        // Then
        assertEquals(1_500L, amount)
        assertTrue(state.isFullDebt)
        assertTrue(state.canConfirm)
    }

    @Test
    fun `GIVEN offline WHEN reading the state THEN shows the offline notice`() {
        // Given
        val state = state(isOnline = false)

        // When
        val showsNotice = state.showsOfflineNotice

        // Then
        assertTrue(showsNotice)
    }

    private val me = PaymentPerson("member-2", "Pierre", "PI", AvatarColor.BLUE, isMe = true)
    private val ana = PaymentPerson("member-1", "Ana", "AN", AvatarColor.PEACH, isMe = false)
    private val twoGroups = listOf("floripa", "ape").map { groupId ->
        PersonGroupBalance(
            groupId = groupId,
            groupName = groupId,
            groupInitials = "GR",
            groupColor = AvatarColor.BLUE,
            memberId = "member-1",
            meMemberId = "member-2",
            amountMinor = 8_115L,
        )
    }

    private val diego = PaymentPerson("member-4", "Diego", "DI", AvatarColor.SAND, isMe = false)

    private fun state(
        from: PaymentPerson? = ana,
        to: PaymentPerson? = me,
        currency: GroupCurrency = GroupCurrency.BRL,
        debtMinor: Long = 16_230L,
        amountText: String = "162,30",
        isOnline: Boolean = true,
        isSaving: Boolean = false,
        isLoaded: Boolean = true,
        groupParts: List<PersonGroupBalance> = emptyList(),
    ): RegisterPaymentUiState = RegisterPaymentUiState(
        currency = currency,
        from = from,
        to = to,
        debtMinor = debtMinor,
        groupParts = groupParts,
        amountText = amountText,
        hasEditedAmount = false,
        method = SettlementMethod.PIX,
        paidAt = 0L,
        paidDay = RelativeDay.TODAY,
        latestPaidAt = 0L,
        isOnline = isOnline,
        isSaving = isSaving,
        isLoaded = isLoaded,
    )
}
