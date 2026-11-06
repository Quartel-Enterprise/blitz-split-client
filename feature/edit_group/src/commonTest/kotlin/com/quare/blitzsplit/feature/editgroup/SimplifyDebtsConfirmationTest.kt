package com.quare.blitzsplit.feature.editgroup

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyCurrencyChange
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyDebtsConfirmation
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyName
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyPersonChange
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimplifyDebtsConfirmationTest {
    @Test
    fun `GIVEN turning on saves a payment WHEN reading the confirmation THEN it is not already at the minimum`() {
        // Given
        val confirmation = confirmation(
            isTurningOn = true,
            currencies = listOf(
                SimplifyCurrencyChange(
                    currency = GroupCurrency.BRL,
                    paymentsBefore = 7,
                    paymentsAfter = 5,
                ),
            ),
        )

        // When
        val isAlreadyMinimal = confirmation.isAlreadyMinimal

        // Then
        assertFalse(isAlreadyMinimal)
    }

    @Test
    fun `GIVEN turning on saves no payment WHEN reading the confirmation THEN it is already at the minimum`() {
        // Given
        val confirmation = confirmation(
            isTurningOn = true,
            currencies = listOf(
                SimplifyCurrencyChange(
                    currency = GroupCurrency.BRL,
                    paymentsBefore = 2,
                    paymentsAfter = 2,
                ),
            ),
        )

        // When
        val isAlreadyMinimal = confirmation.isAlreadyMinimal

        // Then
        assertTrue(isAlreadyMinimal)
    }

    @Test
    fun `GIVEN turning on saves a payment in only one currency WHEN reading the confirmation THEN it is not minimal`() {
        // Given
        val confirmation = confirmation(
            currencies = listOf(
                SimplifyCurrencyChange(
                    currency = GroupCurrency.BRL,
                    paymentsBefore = 2,
                    paymentsAfter = 2,
                ),
                SimplifyCurrencyChange(
                    currency = GroupCurrency.USD,
                    paymentsBefore = 3,
                    paymentsAfter = 2,
                ),
            ),
        )

        // When
        val isAlreadyMinimal = confirmation.isAlreadyMinimal

        // Then
        assertFalse(isAlreadyMinimal)
    }

    @Test
    fun `GIVEN turning off WHEN reading the confirmation THEN never says it is already at the minimum`() {
        // Given
        val confirmation = confirmation(
            isTurningOn = false,
            currencies = listOf(
                SimplifyCurrencyChange(
                    currency = GroupCurrency.BRL,
                    paymentsBefore = 5,
                    paymentsAfter = 7,
                ),
            ),
        )

        // When
        val isAlreadyMinimal = confirmation.isAlreadyMinimal

        // Then
        assertFalse(isAlreadyMinimal)
    }

    @Test
    fun `GIVEN a guest among whoever changes WHEN reading the confirmation THEN says a guest changes`() {
        // Given
        val confirmation = confirmation(
            people = listOf(
                person(
                    memberId = "member-1",
                    isGuest = false,
                ),
                person(
                    memberId = "member-2",
                    isGuest = true,
                ),
            ),
        )

        // When
        val hasGuestChanging = confirmation.hasGuestChanging

        // Then
        assertTrue(hasGuestChanging)
    }

    @Test
    fun `GIVEN only people who joined change WHEN reading the confirmation THEN says no guest changes`() {
        // Given
        val confirmation = confirmation(
            people = listOf(
                person(
                    memberId = "member-1",
                    isGuest = false,
                ),
                person(
                    memberId = "member-3",
                    isGuest = false,
                ),
            ),
        )

        // When
        val hasGuestChanging = confirmation.hasGuestChanging

        // Then
        assertFalse(hasGuestChanging)
    }

    private fun person(
        memberId: String,
        isGuest: Boolean,
    ): SimplifyPersonChange = SimplifyPersonChange(
        memberId = memberId,
        currency = GroupCurrency.BRL,
        person = SimplifyName(
            name = memberId,
            isMe = false,
        ),
        initials = "",
        avatarColor = AvatarColor.default,
        photoUrl = null,
        isGuest = isGuest,
        stopsPaying = emptyList(),
        pays = emptyList(),
        stopsReceivingFrom = emptyList(),
        receivesFrom = emptyList(),
    )

    private fun confirmation(
        isTurningOn: Boolean = true,
        currencies: List<SimplifyCurrencyChange> = listOf(
            SimplifyCurrencyChange(
                currency = GroupCurrency.BRL,
                paymentsBefore = 2,
                paymentsAfter = 1,
            ),
        ),
        people: List<SimplifyPersonChange> = emptyList(),
    ): SimplifyDebtsConfirmation = SimplifyDebtsConfirmation(
        isTurningOn = isTurningOn,
        groupName = "Futebol de quinta",
        currencies = currencies,
        people = people,
        othersChangingCount = 0,
        queuedPayments = emptyList(),
    )
}
