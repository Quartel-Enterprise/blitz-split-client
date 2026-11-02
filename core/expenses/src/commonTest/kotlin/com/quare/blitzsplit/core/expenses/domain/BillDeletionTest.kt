package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import kotlin.test.Test
import kotlin.test.assertEquals

class BillDeletionTest {
    @Test
    fun `GIVEN a bill I owe my share in WHEN asking to delete it THEN shows its description and what I stop owing`() {
        // Given
        val myMemberId = ANA

        // When
        val deletion = BillDeletion.of(mercado, myMemberId, null)

        // Then
        assertEquals(
            BillDeletion(
                description = "Mercado",
                myBalanceMinor = -400L,
                settledPair = null,
            ),
            deletion,
        )
    }

    @Test
    fun `GIVEN a bill I paid WHEN asking to delete it THEN shows what I stop getting back`() {
        // Given
        val myMemberId = PIERRE

        // When
        val deletion = BillDeletion.of(mercado, myMemberId, null)

        // Then
        assertEquals(400L, deletion.myBalanceMinor)
    }

    @Test
    fun `GIVEN a bill I paid only for myself WHEN asking to delete it THEN my balance does not change`() {
        // Given
        val onlyMine = mercado.copy(
            shares = listOf(
                ExpenseShare(
                    memberId = PIERRE,
                    amountMinor = 1_000L,
                    weight = 1L,
                ),
            ),
        )

        // When
        val deletion = BillDeletion.of(onlyMine, PIERRE, null)

        // Then
        assertEquals(0L, deletion.myBalanceMinor)
    }

    @Test
    fun `GIVEN a bill I am not in WHEN asking to delete it THEN shows no balance line`() {
        // Given
        val myMemberId = "carla"

        // When
        val deletion = BillDeletion.of(mercado, myMemberId, null)

        // Then
        assertEquals(
            BillDeletion(
                description = "Mercado",
                myBalanceMinor = null,
                settledPair = null,
            ),
            deletion,
        )
    }

    @Test
    fun `GIVEN no member of the group is me WHEN asking to delete a bill THEN shows no balance line`() {
        // Given
        val myMemberId: String? = null

        // When
        val deletion = BillDeletion.of(mercado, myMemberId, null)

        // Then
        assertEquals(
            BillDeletion(
                description = "Mercado",
                myBalanceMinor = null,
                settledPair = null,
            ),
            deletion,
        )
    }

    private val mercado = Expense(
        id = "bill-1",
        groupId = "group-1",
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = SplitMode.PERCENT,
        payers = listOf(
            ExpensePayer(
                memberId = PIERRE,
                amountMinor = 1_000L,
            ),
        ),
        shares = listOf(
            ExpenseShare(
                memberId = PIERRE,
                amountMinor = 600L,
                weight = 60L,
            ),
            ExpenseShare(
                memberId = ANA,
                amountMinor = 400L,
                weight = 40L,
            ),
        ),
        items = null,
        categoryId = null,
        spentAt = 5_000L,
        isPending = false,
        updatedAt = 1L,
        updatedByMemberId = null,
    )

    private companion object {
        const val PIERRE = "pierre"
        const val ANA = "ana"
    }
}
