package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebtsWithinBillTest {
    @Test
    fun `GIVEN one payer in the split WHEN finding the debts THEN everyone else owes the payer their part`() {
        // Given
        val expense = bill(
            payers = listOf(paid(PIERRE, 1_000L)),
            shares = listOf(
                share(ANA, 333L),
                share(PIERRE, 334L),
                share(BIA, 333L),
            ),
        )

        // When
        val debts = GroupBalances.findDebtsWithin(expense)

        // Then
        assertEquals(
            listOf(
                Debt(ANA, PIERRE, 333L),
                Debt(BIA, PIERRE, 333L),
            ),
            debts,
        )
    }

    @Test
    fun `GIVEN one payer out of the split WHEN finding the debts THEN each owes their whole part in the split order`() {
        // Given
        val expense = bill(
            payers = listOf(paid(PIERRE, 900L)),
            shares = listOf(
                share(CARLA, 300L),
                share(ANA, 300L),
                share(BIA, 300L),
            ),
        )

        // When
        val debts = GroupBalances.findDebtsWithin(expense)

        // Then
        assertEquals(
            listOf(
                Debt(CARLA, PIERRE, 300L),
                Debt(ANA, PIERRE, 300L),
                Debt(BIA, PIERRE, 300L),
            ),
            debts,
        )
    }

    @Test
    fun `GIVEN a payer who paid exactly their part WHEN finding the debts THEN they neither owe nor are owed`() {
        // Given
        val expense = bill(
            payers = listOf(
                paid(PIERRE, 700L),
                paid(ANA, 300L),
            ),
            shares = listOf(
                share(PIERRE, 400L),
                share(ANA, 300L),
                share(BIA, 300L),
            ),
        )

        // When
        val debts = GroupBalances.findDebtsWithin(expense)

        // Then
        assertEquals(listOf(Debt(BIA, PIERRE, 300L)), debts)
    }

    @Test
    fun `GIVEN two payers who paid more than their part WHEN finding the debts THEN who paid nothing owes both`() {
        // Given
        val expense = bill(
            payers = listOf(
                paid(PIERRE, 160L),
                paid(ANA, 140L),
            ),
            shares = listOf(
                share(PIERRE, 100L),
                share(ANA, 100L),
                share(BIA, 100L),
            ),
        )

        // When
        val debts = GroupBalances.findDebtsWithin(expense)

        // Then
        assertEquals(
            listOf(
                Debt(BIA, PIERRE, 60L),
                Debt(BIA, ANA, 40L),
            ),
            debts,
        )
    }

    @Test
    fun `GIVEN two payers out of the split WHEN finding the debts THEN the member in it owes each what they paid`() {
        // Given
        val expense = bill(
            payers = listOf(
                paid(PIERRE, 150L),
                paid(ANA, 150L),
            ),
            shares = listOf(share(BIA, 300L)),
        )

        // When
        val debts = GroupBalances.findDebtsWithin(expense)

        // Then
        assertEquals(
            listOf(
                Debt(BIA, PIERRE, 150L),
                Debt(BIA, ANA, 150L),
            ),
            debts,
        )
    }

    @Test
    fun `GIVEN two debtors and two creditors WHEN finding the debts THEN they are matched in the split order`() {
        // Given
        val expense = bill(
            payers = listOf(
                paid(PIERRE, 250L),
                paid(ANA, 150L),
            ),
            shares = listOf(
                share(PIERRE, 100L),
                share(ANA, 100L),
                share(BIA, 100L),
                share(CARLA, 100L),
            ),
        )

        // When
        val debts = GroupBalances.findDebtsWithin(expense)

        // Then
        assertEquals(
            listOf(
                Debt(BIA, PIERRE, 100L),
                Debt(CARLA, PIERRE, 50L),
                Debt(CARLA, ANA, 50L),
            ),
            debts,
        )
    }

    @Test
    fun `GIVEN a payer out of the split listed first WHEN finding the debts THEN the one in the split is paid first`() {
        // Given
        val expense = bill(
            payers = listOf(
                paid(EDU, 150L),
                paid(ANA, 150L),
            ),
            shares = listOf(
                share(ANA, 100L),
                share(BIA, 100L),
                share(CARLA, 100L),
            ),
        )

        // When
        val debts = GroupBalances.findDebtsWithin(expense)

        // Then
        assertEquals(
            listOf(
                Debt(BIA, ANA, 50L),
                Debt(BIA, EDU, 50L),
                Debt(CARLA, EDU, 100L),
            ),
            debts,
        )
    }

    @Test
    fun `GIVEN two payers and a cents remainder WHEN finding the debts THEN they add up to what the others owe`() {
        // Given
        val expense = bill(
            payers = listOf(
                paid(PIERRE, 601L),
                paid(ANA, 400L),
            ),
            shares = listOf(
                share(PIERRE, 334L),
                share(ANA, 334L),
                share(BIA, 333L),
            ),
        )

        // When
        val debts = GroupBalances.findDebtsWithin(expense)

        // Then
        assertEquals(
            listOf(
                Debt(BIA, PIERRE, 267L),
                Debt(BIA, ANA, 66L),
            ),
            debts,
        )
    }

    @Test
    fun `GIVEN every payer paid exactly their part WHEN finding the debts within the bill THEN there are none`() {
        // Given
        val expense = bill(
            payers = listOf(
                paid(PIERRE, 500L),
                paid(ANA, 500L),
            ),
            shares = listOf(
                share(PIERRE, 500L),
                share(ANA, 500L),
            ),
        )

        // When
        val debts = GroupBalances.findDebtsWithin(expense)

        // Then
        assertTrue(debts.isEmpty())
    }

    @Test
    fun `GIVEN 100 million paid by two WHEN finding the debts within the bill THEN they add up to the cent`() {
        // Given
        val expense = bill(
            payers = listOf(
                paid(PIERRE, 6_000_000_000L),
                paid(ANA, 4_000_000_000L),
            ),
            shares = listOf(
                share(PIERRE, 3_333_333_334L),
                share(ANA, 3_333_333_333L),
                share(BIA, 3_333_333_333L),
            ),
        )

        // When
        val debts = GroupBalances.findDebtsWithin(expense)

        // Then
        assertEquals(
            listOf(
                Debt(BIA, PIERRE, 2_666_666_666L),
                Debt(BIA, ANA, 666_666_667L),
            ),
            debts,
        )
    }

    private fun bill(
        payers: List<ExpensePayer>,
        shares: List<ExpenseShare>,
    ): Expense = Expense(
        id = "trip",
        groupId = "group-1",
        description = "Viagem",
        amountMinor = payers.sumOf { it.amountMinor },
        splitMode = SplitMode.AMOUNT,
        payers = payers,
        shares = shares,
        items = null,
        categoryId = null,
        spentAt = 1_000L,
        isPending = false,
        updatedAt = 1_000L,
        updatedByMemberId = null,
    )

    private fun paid(
        memberId: String,
        amountMinor: Long,
    ): ExpensePayer = ExpensePayer(
        memberId = memberId,
        amountMinor = amountMinor,
    )

    private fun share(
        memberId: String,
        amountMinor: Long,
    ): ExpenseShare = ExpenseShare(
        memberId = memberId,
        amountMinor = amountMinor,
        weight = amountMinor,
    )

    private companion object {
        const val PIERRE = "pierre"
        const val ANA = "ana"
        const val BIA = "bia"
        const val CARLA = "carla"
        const val EDU = "edu"
    }
}
