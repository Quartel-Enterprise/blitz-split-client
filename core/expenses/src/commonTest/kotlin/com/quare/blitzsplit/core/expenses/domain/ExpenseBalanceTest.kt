package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExpenseBalanceTest {
    @Test
    fun `GIVEN the payer has the cents remainder in the split WHEN reading their balance THEN gets back the rest`() {
        // Given
        val expense = splitInThree

        // When
        val balance = expense.balanceOf(PIERRE)

        // Then
        assertEquals(666L, balance)
    }

    @Test
    fun `GIVEN a member in the split who did not pay WHEN reading their balance THEN owes their share`() {
        // Given
        val expense = splitInThree

        // When
        val balance = expense.balanceOf(ANA)

        // Then
        assertEquals(-333L, balance)
    }

    @Test
    fun `GIVEN a split with a cents remainder WHEN reading every balance THEN they add up to zero`() {
        // Given
        val expense = splitInThree

        // When
        val balances = listOf(PIERRE, ANA, BIA).map { expense.balanceOf(it) ?: 0L }

        // Then
        assertEquals(0L, balances.sum())
    }

    @Test
    fun `GIVEN a payer not in the split WHEN reading their balance THEN gets back the whole total`() {
        // Given
        val expense = bill(
            payerId = PIERRE,
            shares = listOf(
                share(ANA, 500L),
                share(BIA, 500L),
            ),
        )

        // When
        val balance = expense.balanceOf(PIERRE)

        // Then
        assertEquals(1_000L, balance)
    }

    @Test
    fun `GIVEN a member who paid exactly their share WHEN reading their balance THEN it is zero`() {
        // Given
        val expense = bill(
            payerId = PIERRE,
            shares = listOf(share(PIERRE, 1_000L)),
        )

        // When
        val balance = expense.balanceOf(PIERRE)

        // Then
        assertEquals(0L, balance)
    }

    @Test
    fun `GIVEN a member neither paying nor in the split WHEN reading their balance THEN has none`() {
        // Given
        val expense = splitInThree

        // When
        val balance = expense.balanceOf("carla")

        // Then
        assertNull(balance)
    }

    private fun share(
        memberId: String,
        amountMinor: Long,
    ): ExpenseShare = ExpenseShare(
        memberId = memberId,
        amountMinor = amountMinor,
        weight = 1L,
    )

    private fun bill(
        payerId: String,
        shares: List<ExpenseShare>,
    ): Expense = Expense(
        id = "bill-1",
        groupId = "group-1",
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = SplitMode.EQUAL,
        payers = listOf(
            ExpensePayer(
                memberId = payerId,
                amountMinor = 1_000L,
            ),
        ),
        shares = shares,
        items = null,
        spentAt = 5_000L,
        isPending = false,
        updatedAt = 1L,
        updatedByMemberId = null,
    )

    // Why: 10,00 in three, the cent that can't be shared going to whoever paid.
    private val splitInThree = bill(
        payerId = PIERRE,
        shares = listOf(
            share(PIERRE, 334L),
            share(ANA, 333L),
            share(BIA, 333L),
        ),
    )

    private companion object {
        const val PIERRE = "pierre"
        const val ANA = "ana"
        const val BIA = "bia"
    }
}
