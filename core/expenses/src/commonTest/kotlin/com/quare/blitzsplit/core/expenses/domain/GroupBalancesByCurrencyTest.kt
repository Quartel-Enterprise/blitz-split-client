package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupBalancesByCurrencyTest {
    @Test
    fun `GIVEN bills in two currencies WHEN computing the balances in the group currency THEN counts only its bills`() {
        // Given
        val group = group(currency = GroupCurrency.BRL)
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, GroupCurrency.BRL),
            owes(BRUNO, CARLA, 700L, GroupCurrency.USD),
        )

        // When
        val balances = GroupBalances.of(
            group,
            expenses,
            settlements = emptyList(),
            currency = group.currency,
        )

        // Then
        assertEquals(listOf(Debt(ANA, BRUNO, 100L)), balances.debts)
    }

    @Test
    fun `GIVEN bills in two currencies WHEN computing the balances of the other currency THEN counts only its bills`() {
        // Given
        val group = group(currency = GroupCurrency.BRL)
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, GroupCurrency.BRL),
            owes(BRUNO, CARLA, 700L, GroupCurrency.USD),
        )

        // When
        val balances = GroupBalances.of(group, expenses, emptyList(), GroupCurrency.USD)

        // Then
        assertEquals(listOf(Debt(BRUNO, CARLA, 700L)), balances.debts)
        assertTrue(balances.hasBills)
    }

    @Test
    fun `GIVEN a debt in reais WHEN a payment in euros covers its amount THEN the debt in reais stays whole`() {
        // Given
        val group = group(currency = GroupCurrency.BRL)
        val expenses = listOf(owes(ANA, BRUNO, 100L, GroupCurrency.BRL))
        val settlements = listOf(payment(ANA, BRUNO, 100L, GroupCurrency.EUR))

        // When
        val balances = GroupBalances.of(group, expenses, settlements, currency = group.currency)

        // Then
        assertEquals(listOf(Debt(ANA, BRUNO, 100L)), balances.debts)
        assertFalse(balances.isEveryoneSettled)
    }

    @Test
    fun `GIVEN a payment in euros WHEN computing the balances in euros THEN it stands alone without any bill`() {
        // Given
        val group = group(currency = GroupCurrency.BRL)
        val expenses = listOf(owes(ANA, BRUNO, 100L, GroupCurrency.BRL))
        val settlements = listOf(payment(ANA, BRUNO, 100L, GroupCurrency.EUR))

        // When
        val balances = GroupBalances.of(group, expenses, settlements, GroupCurrency.EUR)

        // Then
        assertEquals(listOf(Debt(BRUNO, ANA, 100L)), balances.debts)
        assertFalse(balances.hasBills)
    }

    @Test
    fun `GIVEN a payment in dollars WHEN it covers the dollar debt THEN settles that currency and leaves reais open`() {
        // Given
        val group = group(currency = GroupCurrency.BRL)
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, GroupCurrency.BRL),
            owes(ANA, BRUNO, 100L, GroupCurrency.USD),
        )
        val settlements = listOf(payment(ANA, BRUNO, 100L, GroupCurrency.USD))

        // When
        val byCurrency = GroupBalances.getByCurrency(group, expenses, settlements)

        // Then
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), byCurrency.map { it.currency })
        assertEquals(listOf(Debt(ANA, BRUNO, 100L)), byCurrency[0].balances.debts)
        assertTrue(byCurrency[1].balances.isEveryoneSettled)
    }

    @Test
    fun `GIVEN a group with no bills WHEN listing by currency THEN lists its currency with nothing owed`() {
        // Given
        val group = group(currency = GroupCurrency.EUR)

        // When
        val byCurrency = GroupBalances.getByCurrency(
            group,
            expenses = emptyList(),
            settlements = emptyList(),
        )

        // Then
        assertEquals(listOf(GroupCurrency.EUR), byCurrency.map { it.currency })
        assertFalse(byCurrency.single().balances.hasBills)
        assertTrue(
            byCurrency
                .single()
                .balances.debts
                .isEmpty(),
        )
    }

    @Test
    fun `GIVEN a group whose bills are all in other currencies WHEN listing the currencies THEN its own comes first`() {
        // Given
        val group = group(currency = GroupCurrency.EUR)
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, GroupCurrency.USD),
            owes(ANA, BRUNO, 100L, GroupCurrency.BRL, id = "b2"),
            owes(ANA, BRUNO, 100L, GroupCurrency.BRL, id = "b3"),
        )

        // When
        val byCurrency = GroupBalances.getByCurrency(group, expenses, settlements = emptyList())

        // Then
        assertEquals(listOf(GroupCurrency.EUR, GroupCurrency.BRL, GroupCurrency.USD), byCurrency.map { it.currency })
        assertFalse(byCurrency.first().balances.hasBills)
        assertTrue(
            byCurrency
                .first()
                .balances.debts
                .isEmpty(),
        )
    }

    @Test
    fun `GIVEN other currencies with different numbers of bills WHEN listing them THEN the most used comes first`() {
        // Given
        val group = group(currency = GroupCurrency.BRL)
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, GroupCurrency.EUR, id = "b1"),
            owes(ANA, BRUNO, 100L, GroupCurrency.USD, id = "b2"),
            owes(ANA, BRUNO, 100L, GroupCurrency.USD, id = "b3"),
            owes(ANA, BRUNO, 100L, GroupCurrency.BRL, id = "b4"),
        )

        // When
        val currencies = GroupBalances.currenciesOf(group, expenses, settlements = emptyList())

        // Then
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD, GroupCurrency.EUR), currencies)
    }

    @Test
    fun `GIVEN other currencies with as many bills each WHEN listing them THEN orders them by code`() {
        // Given
        val group = group(currency = GroupCurrency.BRL)
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, GroupCurrency.USD, id = "b1"),
            owes(ANA, BRUNO, 100L, GroupCurrency.GBP, id = "b2"),
            owes(ANA, BRUNO, 100L, GroupCurrency.EUR, id = "b3"),
        )

        // When
        val currencies = GroupBalances.currenciesOf(group, expenses, settlements = emptyList())

        // Then
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.EUR, GroupCurrency.GBP, GroupCurrency.USD), currencies)
    }

    @Test
    fun `GIVEN a currency used only by a payment WHEN listing the currencies THEN lists it after those with bills`() {
        // Given
        val group = group(currency = GroupCurrency.BRL)
        val expenses = listOf(owes(ANA, BRUNO, 100L, GroupCurrency.USD))
        val settlements = listOf(payment(BRUNO, ANA, 50L, GroupCurrency.CAD))

        // When
        val byCurrency = GroupBalances.getByCurrency(group, expenses, settlements)

        // Then
        assertEquals(
            listOf(GroupCurrency.BRL, GroupCurrency.USD, GroupCurrency.CAD),
            byCurrency.map { it.currency },
        )
        assertEquals(listOf(Debt(ANA, BRUNO, 50L)), byCurrency.last().balances.debts)
    }

    @Test
    fun `GIVEN a currency used by bills and payments WHEN listing the currencies THEN lists it once`() {
        // Given
        val group = group(currency = GroupCurrency.BRL)
        val expenses = listOf(owes(ANA, BRUNO, 100L, GroupCurrency.USD))
        val settlements =
            listOf(payment(ANA, BRUNO, 40L, GroupCurrency.USD), payment(ANA, BRUNO, 10L, GroupCurrency.BRL))

        // When
        val currencies = GroupBalances.currenciesOf(group, expenses, settlements)

        // Then
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), currencies)
    }

    @Test
    fun `GIVEN simplifying on and a chain in one currency WHEN listing by currency THEN simplifies that currency`() {
        // Given
        val group = group(
            currency = GroupCurrency.BRL,
            simplifiesDebts = true,
        )
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, GroupCurrency.BRL, id = "b1"),
            owes(BRUNO, CARLA, 100L, GroupCurrency.BRL, id = "b2"),
            owes(ANA, CARLA, 30L, GroupCurrency.USD, id = "b3"),
        )

        // When
        val byCurrency = GroupBalances.getByCurrency(group, expenses, settlements = emptyList())

        // Then
        val reais = byCurrency.first { it.currency == GroupCurrency.BRL }.balances
        assertEquals(listOf(Debt(ANA, CARLA, 100L)), reais.debts)
        assertEquals(listOf(Debt(ANA, BRUNO, 100L), Debt(BRUNO, CARLA, 100L)), reais.unsimplifiedDebts)
        val dollars = byCurrency.first { it.currency == GroupCurrency.USD }.balances
        assertEquals(listOf(Debt(ANA, CARLA, 30L)), dollars.debts)
    }

    @Test
    fun `GIVEN simplifying on and a chain across two currencies WHEN listing by currency THEN never chains them`() {
        // Given
        val group = group(
            currency = GroupCurrency.BRL,
            simplifiesDebts = true,
        )
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, GroupCurrency.BRL),
            owes(BRUNO, CARLA, 100L, GroupCurrency.USD),
        )

        // When
        val byCurrency = GroupBalances.getByCurrency(group, expenses, settlements = emptyList())

        // Then
        assertEquals(listOf(Debt(ANA, BRUNO, 100L)), byCurrency[0].balances.debts)
        assertEquals(listOf(Debt(BRUNO, CARLA, 100L)), byCurrency[1].balances.debts)
    }

    @Test
    fun `GIVEN a payer who left the group WHEN listing by currency THEN keeps their debts in each currency`() {
        // Given
        val group = group(currency = GroupCurrency.BRL)
        val expenses = listOf(
            owes(ANA, EX_MEMBER, 100L, GroupCurrency.BRL, id = "b1"),
            owes(ANA, EX_MEMBER, 20L, GroupCurrency.USD, id = "b2"),
        )

        // When
        val byCurrency = GroupBalances.getByCurrency(group, expenses, settlements = emptyList())

        // Then
        assertEquals(listOf(Debt(ANA, EX_MEMBER, 100L)), byCurrency[0].balances.debts)
        assertEquals(listOf(Debt(ANA, EX_MEMBER, 20L)), byCurrency[1].balances.debts)
    }

    @Test
    fun `GIVEN a group gone from the device with no bills or payments WHEN listing by currency THEN lists nothing`() {
        // Given
        val expenses = emptyList<Expense>()

        // When
        val byCurrency = GroupBalances.getUnsimplifiedByCurrency(expenses, settlements = emptyList())

        // Then
        assertTrue(byCurrency.isEmpty())
    }

    @Test
    fun `GIVEN a group gone with two currencies WHEN listing by currency THEN lists one balance per currency`() {
        // Given
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, GroupCurrency.BRL),
            owes(BRUNO, CARLA, 700L, GroupCurrency.USD),
        )

        // When
        val byCurrency = GroupBalances.getUnsimplifiedByCurrency(expenses, settlements = emptyList())

        // Then
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), byCurrency.map { it.currency })
        assertEquals(listOf(Debt(ANA, BRUNO, 100L)), byCurrency[0].balances.debts)
        assertEquals(listOf(Debt(BRUNO, CARLA, 700L)), byCurrency[1].balances.debts)
    }

    @Test
    fun `GIVEN a group gone with more bills in dollars WHEN listing by currency THEN lists the most used first`() {
        // Given
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, GroupCurrency.BRL, id = "b1"),
            owes(ANA, BRUNO, 100L, GroupCurrency.USD, id = "b2"),
            owes(ANA, BRUNO, 100L, GroupCurrency.USD, id = "b3"),
        )

        // When
        val byCurrency = GroupBalances.getUnsimplifiedByCurrency(expenses, settlements = emptyList())

        // Then
        assertEquals(listOf(GroupCurrency.USD, GroupCurrency.BRL), byCurrency.map { it.currency })
    }

    @Test
    fun `GIVEN a group gone with as many bills in each currency WHEN listing by currency THEN orders them by code`() {
        // Given
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, GroupCurrency.USD),
            owes(ANA, BRUNO, 100L, GroupCurrency.EUR),
            owes(ANA, BRUNO, 100L, GroupCurrency.BRL),
        )

        // When
        val byCurrency = GroupBalances.getUnsimplifiedByCurrency(expenses, settlements = emptyList())

        // Then
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.EUR, GroupCurrency.USD), byCurrency.map { it.currency })
    }

    @Test
    fun `GIVEN a group gone with a payment in a currency without bills WHEN listing by currency THEN lists it last`() {
        // Given
        val expenses = listOf(owes(ANA, BRUNO, 100L, GroupCurrency.USD))
        val settlements = listOf(payment(BRUNO, ANA, 50L, GroupCurrency.BRL))

        // When
        val byCurrency = GroupBalances.getUnsimplifiedByCurrency(expenses, settlements)

        // Then
        assertEquals(listOf(GroupCurrency.USD, GroupCurrency.BRL), byCurrency.map { it.currency })
        assertEquals(listOf(Debt(ANA, BRUNO, 50L)), byCurrency.last().balances.debts)
        assertFalse(byCurrency.last().balances.hasBills)
    }

    @Test
    fun `GIVEN a group gone with a chain WHEN listing by currency THEN keeps the pairs as the bills make them`() {
        // Given
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, GroupCurrency.BRL, id = "b1"),
            owes(BRUNO, CARLA, 100L, GroupCurrency.BRL, id = "b2"),
        )

        // When
        val byCurrency = GroupBalances.getUnsimplifiedByCurrency(expenses, settlements = emptyList())

        // Then
        val balances = byCurrency.single().balances
        assertEquals(listOf(Debt(ANA, BRUNO, 100L), Debt(BRUNO, CARLA, 100L)), balances.debts)
        assertFalse(balances.isSimplified)
    }

    @Test
    fun `GIVEN a payment in euros WHEN creating the unsimplified balance in reais THEN ignores the payment`() {
        // Given
        val expenses = listOf(owes(ANA, BRUNO, 100L, GroupCurrency.BRL))
        val settlements = listOf(payment(ANA, BRUNO, 100L, GroupCurrency.EUR))

        // When
        val balances = GroupBalances.createUnsimplified(expenses, settlements, GroupCurrency.BRL)

        // Then
        assertEquals(listOf(Debt(ANA, BRUNO, 100L)), balances.debts)
        assertTrue(balances.hasBills)
    }

    @Test
    fun `GIVEN a bill in dollars WHEN creating the unsimplified balance in reais THEN has no bills and nothing owed`() {
        // Given
        val expenses = listOf(owes(ANA, BRUNO, 100L, GroupCurrency.USD))

        // When
        val balances = GroupBalances.createUnsimplified(expenses, settlements = emptyList(), GroupCurrency.BRL)

        // Then
        assertFalse(balances.hasBills)
        assertTrue(balances.debts.isEmpty())
    }

    @Test
    fun `GIVEN a payment in dollars covering the debt WHEN creating the balance in dollars THEN settles it`() {
        // Given
        val expenses = listOf(owes(ANA, BRUNO, 100L, GroupCurrency.USD), owes(ANA, BRUNO, 80L, GroupCurrency.BRL))
        val settlements = listOf(payment(ANA, BRUNO, 100L, GroupCurrency.USD))

        // When
        val balances = GroupBalances.createUnsimplified(expenses, settlements, GroupCurrency.USD)

        // Then
        assertTrue(balances.isEveryoneSettled)
    }

    private fun group(
        currency: GroupCurrency,
        simplifiesDebts: Boolean = false,
    ): Group = Group(
        id = GROUP_ID,
        name = "Viagem",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = currency,
        members = listOf(ANA, BRUNO, CARLA).map(::member),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = simplifiesDebts,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private fun member(id: String): GroupMember = GroupMember(
        id = id,
        name = id,
        avatarColor = AvatarColor.BLUE,
        isMe = id == ANA,
        hasJoined = true,
        userId = id,
        inviteCode = null,
        photoUrl = null,
    )

    // Why: a bill all on one member, which another paid, so the first owes the second that much.
    private fun owes(
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
        currency: GroupCurrency,
        id: String = "bill-$fromMemberId-$toMemberId-${currency.code}",
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = "Conta",
        currency = currency,
        amountMinor = amountMinor,
        splitMode = SplitMode.AMOUNT,
        payers = listOf(ExpensePayer(toMemberId, amountMinor)),
        shares = listOf(ExpenseShare(fromMemberId, amountMinor, weight = amountMinor)),
        items = null,
        categoryId = null,
        spentAt = 1_000L,
        isPending = false,
        updatedAt = 1_000L,
        updatedByMemberId = null,
    )

    private fun payment(
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
        currency: GroupCurrency,
    ): Settlement = Settlement(
        id = "payment-$fromMemberId-$toMemberId-${currency.code}-$amountMinor",
        groupId = GROUP_ID,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        currency = currency,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = 2_000L,
        isPending = false,
        updatedAt = 0L,
    )

    private companion object {
        const val GROUP_ID = "viagem"
        const val ANA = "ana"
        const val BRUNO = "bruno"
        const val CARLA = "carla"
        const val EX_MEMBER = "ex-member"
    }
}
