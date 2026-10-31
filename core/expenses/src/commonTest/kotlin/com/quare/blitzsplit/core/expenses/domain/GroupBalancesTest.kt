package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupBalancesTest {
    @Test
    fun `GIVEN no bills WHEN computing the balances THEN nobody owes and the group is not settled yet`() {
        // Given
        val expenses = emptyList<Expense>()

        // When
        val balances = GroupBalances.of(expenses, settlements = emptyList())

        // Then
        assertTrue(balances.debts.isEmpty())
        assertFalse(balances.hasBills)
        assertFalse(balances.isEveryoneSettled)
    }

    @Test
    fun `GIVEN one bill paid by one member WHEN computing the balances THEN everyone else owes the payer their part`() {
        // Given
        val expenses = listOf(
            bill(
                id = "dinner",
                payers = listOf(paid(PIERRE, 1_000L)),
                shares = listOf(
                    share(PIERRE, 334L),
                    share(ANA, 333L),
                    share(BIA, 333L),
                ),
            ),
        )

        // When
        val balances = GroupBalances.of(expenses, settlements = emptyList())

        // Then
        assertEquals(
            listOf(
                Debt(ANA, PIERRE, 333L),
                Debt(BIA, PIERRE, 333L),
            ),
            balances.debts,
        )
        assertTrue(balances.hasBills)
        assertFalse(balances.isEveryoneSettled)
    }

    @Test
    fun `GIVEN a bill with two payers WHEN computing THEN who paid less owes who paid more in the bill order`() {
        // Given
        val expenses = listOf(
            bill(
                id = "trip",
                payers = listOf(
                    paid(PIERRE, 700L),
                    paid(ANA, 300L),
                ),
                shares = listOf(
                    share(PIERRE, 250L),
                    share(ANA, 250L),
                    share(BIA, 250L),
                    share(CARLA, 250L),
                ),
            ),
        )

        // When
        val balances = GroupBalances.of(expenses, settlements = emptyList())

        // Then
        assertEquals(
            listOf(
                Debt(BIA, PIERRE, 250L),
                Debt(CARLA, PIERRE, 200L),
                Debt(CARLA, ANA, 50L),
            ),
            balances.debts,
        )
    }

    @Test
    fun `GIVEN two payers and a cents remainder WHEN computing THEN the debts add up to the cent`() {
        // Given
        val expenses = listOf(
            bill(
                id = "trip",
                payers = listOf(
                    paid(PIERRE, 600L),
                    paid(ANA, 401L),
                ),
                shares = listOf(
                    share(PIERRE, 334L),
                    share(ANA, 334L),
                    share(BIA, 333L),
                ),
            ),
        )

        // When
        val balances = GroupBalances.of(expenses, settlements = emptyList())

        // Then
        assertEquals(266L, balances.balanceOf(PIERRE).receivesMinor)
        assertEquals(67L, balances.balanceOf(ANA).receivesMinor)
        assertEquals(333L, balances.balanceOf(BIA).owesMinor)
        assertEquals(333L, balances.debts.sumOf { it.amountMinor })
    }

    @Test
    fun `GIVEN two members owing each other WHEN computing the balances THEN only the difference shows`() {
        // Given
        val expenses = listOf(
            bill(
                id = "dinner",
                payers = listOf(paid(PIERRE, 100L)),
                shares = listOf(share(ANA, 100L)),
            ),
            bill(
                id = "taxi",
                payers = listOf(paid(ANA, 30L)),
                shares = listOf(share(PIERRE, 30L)),
            ),
        )

        // When
        val balances = GroupBalances.of(expenses, settlements = emptyList())

        // Then
        assertEquals(listOf(Debt(ANA, PIERRE, 70L)), balances.debts)
    }

    @Test
    fun `GIVEN two members owing each other the same WHEN computing THEN the pair disappears`() {
        // Given
        val expenses = listOf(
            bill(
                id = "dinner",
                payers = listOf(paid(PIERRE, 100L)),
                shares = listOf(share(ANA, 100L)),
            ),
            bill(
                id = "taxi",
                payers = listOf(paid(ANA, 100L)),
                shares = listOf(share(PIERRE, 100L)),
            ),
        )

        // When
        val balances = GroupBalances.of(expenses, settlements = emptyList())

        // Then
        assertTrue(balances.debts.isEmpty())
        assertTrue(balances.isEveryoneSettled)
    }

    @Test
    fun `GIVEN Ana owes Bia and Bia owes Carla WHEN computing the balances THEN both debts stay without simplifying`() {
        // Given
        val expenses = listOf(
            bill(
                id = "dinner",
                payers = listOf(paid(BIA, 100L)),
                shares = listOf(share(ANA, 100L)),
            ),
            bill(
                id = "taxi",
                payers = listOf(paid(CARLA, 100L)),
                shares = listOf(share(BIA, 100L)),
            ),
        )

        // When
        val balances = GroupBalances.of(expenses, settlements = emptyList())

        // Then
        assertEquals(
            listOf(
                Debt(ANA, BIA, 100L),
                Debt(BIA, CARLA, 100L),
            ),
            balances.debts,
        )
    }

    @Test
    fun `GIVEN debts of different amounts WHEN computing THEN the largest come first and ties follow the ids`() {
        // Given
        val expenses = listOf(
            bill(
                id = "small",
                payers = listOf(paid(PIERRE, 50L)),
                shares = listOf(share(CARLA, 50L)),
            ),
            bill(
                id = "large",
                payers = listOf(paid(PIERRE, 300L)),
                shares = listOf(share(BIA, 300L)),
            ),
            bill(
                id = "tie-b",
                payers = listOf(paid(CARLA, 100L)),
                shares = listOf(share(BIA, 100L)),
            ),
            bill(
                id = "tie-a",
                payers = listOf(paid(PIERRE, 100L)),
                shares = listOf(share(ANA, 100L)),
            ),
        )

        // When
        val balances = GroupBalances.of(expenses, settlements = emptyList())

        // Then
        assertEquals(
            listOf(
                Debt(BIA, PIERRE, 300L),
                Debt(ANA, PIERRE, 100L),
                Debt(BIA, CARLA, 100L),
                Debt(CARLA, PIERRE, 50L),
            ),
            balances.debts,
        )
    }

    @Test
    fun `GIVEN Ana owes Pierre WHEN she records a partial payment THEN she owes only the rest`() {
        // Given
        val expenses = listOf(anaOwesPierre100)
        val settlements = listOf(
            payment(
                from = ANA,
                to = PIERRE,
                amountMinor = 40L,
            ),
        )

        // When
        val balances = GroupBalances.of(expenses, settlements)

        // Then
        assertEquals(listOf(Debt(ANA, PIERRE, 60L)), balances.debts)
    }

    @Test
    fun `GIVEN Ana owes Pierre WHEN she pays all of it THEN everyone is settled`() {
        // Given
        val expenses = listOf(anaOwesPierre100)
        val settlements = listOf(
            payment(
                from = ANA,
                to = PIERRE,
                amountMinor = 100L,
            ),
        )

        // When
        val balances = GroupBalances.of(expenses, settlements)

        // Then
        assertTrue(balances.debts.isEmpty())
        assertTrue(balances.isEveryoneSettled)
    }

    @Test
    fun `GIVEN Ana owes Pierre WHEN she pays more than she owes THEN Pierre owes her the difference`() {
        // Given
        val expenses = listOf(anaOwesPierre100)
        val settlements = listOf(
            payment(
                from = ANA,
                to = PIERRE,
                amountMinor = 150L,
            ),
        )

        // When
        val balances = GroupBalances.of(expenses, settlements)

        // Then
        assertEquals(listOf(Debt(PIERRE, ANA, 50L)), balances.debts)
    }

    @Test
    fun `GIVEN Ana owes Pierre WHEN she records two payments THEN both count`() {
        // Given
        val expenses = listOf(anaOwesPierre100)
        val settlements = listOf(
            payment(
                from = ANA,
                to = PIERRE,
                amountMinor = 30L,
            ),
            payment(
                from = ANA,
                to = PIERRE,
                amountMinor = 20L,
            ),
        )

        // When
        val balances = GroupBalances.of(expenses, settlements)

        // Then
        assertEquals(listOf(Debt(ANA, PIERRE, 50L)), balances.debts)
    }

    @Test
    fun `GIVEN only payments and no bill WHEN computing the balances THEN the group has no bills and is not settled`() {
        // Given
        val settlements = listOf(
            payment(
                from = ANA,
                to = PIERRE,
                amountMinor = 40L,
            ),
        )

        // When
        val balances = GroupBalances.of(
            expenses = emptyList(),
            settlements = settlements,
        )

        // Then
        assertFalse(balances.hasBills)
        assertFalse(balances.isEveryoneSettled)
    }

    @Test
    fun `GIVEN a member owed by one and owing another WHEN reading their balance THEN sums both`() {
        // Given
        val balances = GroupBalances.of(listOf(anaOwesBia100, biaOwesCarla30), settlements = emptyList())

        // When
        val balance = balances.balanceOf(BIA)

        // Then
        assertEquals(
            MemberBalance(
                receivesMinor = 100L,
                owesMinor = 30L,
            ),
            balance,
        )
    }

    @Test
    fun `GIVEN a member in no debt WHEN reading their balance THEN receives and owes nothing`() {
        // Given
        val balances = GroupBalances.of(listOf(anaOwesBia100), settlements = emptyList())

        // When
        val balance = balances.balanceOf(PIERRE)

        // Then
        assertEquals(
            MemberBalance(
                receivesMinor = 0L,
                owesMinor = 0L,
            ),
            balance,
        )
    }

    @Test
    fun `GIVEN Ana owes Pierre in netted bills WHEN reading the debt from Ana to Pierre THEN gets the netted amount`() {
        // Given
        val pierreOwesAna30 = bill(
            id = "taxi",
            payers = listOf(paid(ANA, 30L)),
            shares = listOf(share(PIERRE, 30L)),
        )
        val balances = GroupBalances.of(listOf(anaOwesPierre100, pierreOwesAna30), settlements = emptyList())

        // When
        val debt = balances.debtOf(
            fromMemberId = ANA,
            toMemberId = PIERRE,
        )

        // Then
        assertEquals(70L, debt)
    }

    @Test
    fun `GIVEN Ana owes Pierre WHEN reading the debt from Pierre to Ana THEN it is zero`() {
        // Given
        val balances = GroupBalances.of(listOf(anaOwesPierre100), settlements = emptyList())

        // When
        val debt = balances.debtOf(
            fromMemberId = PIERRE,
            toMemberId = ANA,
        )

        // Then
        assertEquals(0L, debt)
    }

    @Test
    fun `GIVEN two members with no debt between them WHEN reading their debt THEN it is zero`() {
        // Given
        val balances = GroupBalances.of(listOf(anaOwesPierre100), settlements = emptyList())

        // When
        val debt = balances.debtOf(
            fromMemberId = BIA,
            toMemberId = CARLA,
        )

        // Then
        assertEquals(0L, debt)
    }

    @Test
    fun `GIVEN a member who owes two others WHEN finding their largest creditor THEN gets whom they owe the most`() {
        // Given
        val anaOwesCarla300 = bill(
            id = "hotel",
            payers = listOf(paid(CARLA, 300L)),
            shares = listOf(share(ANA, 300L)),
        )
        val balances = GroupBalances.of(listOf(anaOwesPierre100, anaOwesCarla300), settlements = emptyList())

        // When
        val creditor = balances.findLargestCreditorOrNull(ANA)

        // Then
        assertEquals(CARLA, creditor)
    }

    @Test
    fun `GIVEN a member who is only owed WHEN finding their largest creditor THEN there is none`() {
        // Given
        val balances = GroupBalances.of(listOf(anaOwesPierre100), settlements = emptyList())

        // When
        val creditor = balances.findLargestCreditorOrNull(PIERRE)

        // Then
        assertNull(creditor)
    }

    @Test
    fun `GIVEN no bills nor payments WHEN reading the balance or null of a member THEN there is none`() {
        // Given
        val balances = GroupBalances.of(
            expenses = emptyList(),
            settlements = emptyList(),
        )

        // When
        val balance = balances.balanceOrNull(ANA)

        // Then
        assertNull(balance)
    }

    @Test
    fun `GIVEN only a payment WHEN reading the balance or null of the payer THEN gets what the payment moved`() {
        // Given
        val balances = GroupBalances.of(
            expenses = emptyList(),
            settlements = listOf(
                payment(
                    from = ANA,
                    to = PIERRE,
                    amountMinor = 40L,
                ),
            ),
        )

        // When
        val balance = balances.balanceOrNull(ANA)

        // Then
        assertEquals(
            MemberBalance(
                receivesMinor = 40L,
                owesMinor = 0L,
            ),
            balance,
        )
    }

    @Test
    fun `GIVEN bills WHEN reading the balance or null of no member THEN there is none`() {
        // Given
        val balances = GroupBalances.of(listOf(anaOwesPierre100), settlements = emptyList())

        // When
        val balance = balances.balanceOrNull(null)

        // Then
        assertNull(balance)
    }

    private val anaOwesPierre100 = bill(
        id = "dinner",
        payers = listOf(paid(PIERRE, 100L)),
        shares = listOf(share(ANA, 100L)),
    )

    private val anaOwesBia100 = bill(
        id = "market",
        payers = listOf(paid(BIA, 100L)),
        shares = listOf(share(ANA, 100L)),
    )

    private val biaOwesCarla30 = bill(
        id = "coffee",
        payers = listOf(paid(CARLA, 30L)),
        shares = listOf(share(BIA, 30L)),
    )

    private fun bill(
        id: String,
        payers: List<ExpensePayer>,
        shares: List<ExpenseShare>,
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = "Conta",
        amountMinor = payers.sumOf { it.amountMinor },
        splitMode = SplitMode.AMOUNT,
        payers = payers,
        shares = shares,
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

    private fun payment(
        from: String,
        to: String,
        amountMinor: Long,
    ): Settlement = Settlement(
        id = "payment-$from-$to-$amountMinor",
        groupId = GROUP_ID,
        fromMemberId = from,
        toMemberId = to,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = 2_000L,
        isPending = false,
    )

    private companion object {
        const val GROUP_ID = "group-1"
        const val PIERRE = "pierre"
        const val ANA = "ana"
        const val BIA = "bia"
        const val CARLA = "carla"
    }
}
