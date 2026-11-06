package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementLeg
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

class PeopleBalancesByCurrencyTest {
    private val me = member("me", "Pierre", isMe = true)
    private val carla = member("carla", "Carla", userId = "carla")

    @Test
    fun `GIVEN one group with bills in reais and dollars WHEN netting THEN shows the person once per currency`() {
        // Given
        val groups = listOf(group("floripa", me, carla))
        val money = mapOf(
            "floripa" to moneyOf(
                owes("floripa", carla.id, me.id, 1_000L, GroupCurrency.BRL, id = "b1"),
                owes("floripa", carla.id, me.id, 500L, GroupCurrency.USD, id = "b2"),
            ),
        )

        // When
        val people = PeopleBalances.of(groups, money).people

        // Then
        assertEquals(listOf("user:carla", "user:carla"), people.map { it.key })
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), people.map { it.currency })
        assertEquals(listOf(1_000L, 500L), people.map { it.netMinor })
        assertEquals(listOf(listOf("floripa"), listOf("floripa")), people.map { p -> p.groups.map { it.groupId } })
        assertEquals(listOf(1_000L, 500L), people.map { it.groups.single().amountMinor })
    }

    @Test
    fun `GIVEN one group with bills in two currencies WHEN looking a person up THEN finds each currency apart`() {
        // Given
        val groups = listOf(group("floripa", me, carla))
        val money = mapOf(
            "floripa" to moneyOf(
                owes("floripa", carla.id, me.id, 1_000L, GroupCurrency.BRL, id = "b1"),
                owes("floripa", me.id, carla.id, 500L, GroupCurrency.USD, id = "b2"),
            ),
        )
        val balances = PeopleBalances.of(groups, money)

        // When
        val dollars = balances.findPersonOrNull("user:carla", "USD")
        val reais = balances.findPersonOrNull("user:carla", "BRL")

        // Then
        assertEquals(-500L, dollars?.netMinor)
        assertEquals(1_000L, reais?.netMinor)
    }

    @Test
    fun `GIVEN one group with bills in two currencies WHEN settling up THEN each leg carries its own currency`() {
        // Given
        val groups = listOf(group("floripa", me, carla))
        val money = mapOf(
            "floripa" to moneyOf(
                owes("floripa", carla.id, me.id, 1_000L, GroupCurrency.BRL, id = "b1"),
                owes("floripa", me.id, carla.id, 500L, GroupCurrency.USD, id = "b2"),
            ),
        )

        // When
        val legs = PeopleBalances.of(groups, money).people.flatMap { it.legs }

        // Then
        assertEquals(
            listOf(
                SettlementLeg("floripa", carla.id, me.id, 1_000L, GroupCurrency.BRL),
                SettlementLeg("floripa", me.id, carla.id, 500L, GroupCurrency.USD),
            ),
            legs,
        )
    }

    @Test
    fun `GIVEN one group with bills in two currencies WHEN totaling THEN lists each currency once with its own sums`() {
        // Given
        val groups = listOf(group("floripa", me, carla))
        val money = mapOf(
            "floripa" to moneyOf(
                owes("floripa", carla.id, me.id, 1_000L, GroupCurrency.BRL, id = "b1"),
                owes("floripa", me.id, carla.id, 500L, GroupCurrency.USD, id = "b2"),
            ),
        )

        // When
        val totals = PeopleBalances.of(groups, money).totals

        // Then
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), totals.map { it.currency })
        assertEquals(listOf(1_000L, -500L), totals.map { it.netMinor })
        assertEquals(listOf(1_000L, 0L), totals.map { it.toReceiveMinor })
        assertEquals(listOf(0L, 500L), totals.map { it.toPayMinor })
        assertEquals(listOf(1, 0), totals.map { it.toReceiveCount })
        assertEquals(listOf(0, 1), totals.map { it.toPayCount })
    }

    @Test
    fun `GIVEN a currency in two groups and another in one WHEN totaling THEN the one in more groups comes first`() {
        // Given
        val groups = listOf(group("floripa", me, carla), group("lisboa", me, carla, currency = GroupCurrency.USD))
        val money = mapOf(
            "floripa" to moneyOf(owes("floripa", carla.id, me.id, 1_000L, GroupCurrency.USD)),
        )

        // When
        val totals = PeopleBalances.of(groups, money).totals

        // Then
        assertEquals(listOf(GroupCurrency.USD, GroupCurrency.BRL), totals.map { it.currency })
        assertEquals(listOf(1_000L, 0L), totals.map { it.netMinor })
    }

    @Test
    fun `GIVEN a group with no bills WHEN totaling THEN still lists its default currency with nothing owed`() {
        // Given
        val groups = listOf(group("lisboa", me, carla, currency = GroupCurrency.EUR))

        // When
        val balances = PeopleBalances.of(groups, emptyMap())

        // Then
        assertEquals(emptyList(), balances.people)
        assertEquals(listOf(GroupCurrency.EUR), balances.totals.map { it.currency })
        assertEquals(0L, balances.totals.single().netMinor)
    }

    @Test
    fun `GIVEN a group whose bills are all in other currencies WHEN totaling THEN lists its own as well`() {
        // Given
        val groups = listOf(group("lisboa", me, carla, currency = GroupCurrency.EUR))
        val money = mapOf(
            "lisboa" to moneyOf(
                owes("lisboa", carla.id, me.id, 1_000L, GroupCurrency.BRL, id = "b1"),
                owes("lisboa", carla.id, me.id, 300L, GroupCurrency.USD, id = "b2"),
            ),
        )

        // When
        val balances = PeopleBalances.of(groups, money)

        // Then
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), balances.people.map { it.currency })
        assertEquals(
            listOf(GroupCurrency.BRL, GroupCurrency.EUR, GroupCurrency.USD),
            balances.totals.map { it.currency },
        )
        assertEquals(listOf(1_000L, 0L, 300L), balances.totals.map { it.netMinor })
    }

    @Test
    fun `GIVEN a payment in a currency no bill uses WHEN netting THEN shows it as its own balance and total`() {
        // Given
        val groups = listOf(group("floripa", me, carla))
        val money = mapOf(
            "floripa" to GroupMoney(
                expenses = listOf(owes("floripa", carla.id, me.id, 1_000L, GroupCurrency.BRL)),
                settlements = listOf(paid("floripa", me.id, carla.id, 300L, GroupCurrency.CAD)),
                pendingDeletionCount = 0,
            ),
        )

        // When
        val balances = PeopleBalances.of(groups, money)

        // Then
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.CAD), balances.people.map { it.currency })
        assertEquals(listOf(1_000L, 300L), balances.people.map { it.netMinor })
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.CAD), balances.totals.map { it.currency })
    }

    @Test
    fun `GIVEN a payment in euros as large as a debt in reais WHEN netting THEN the debt in reais stays`() {
        // Given
        val groups = listOf(group("floripa", me, carla))
        val money = mapOf(
            "floripa" to GroupMoney(
                expenses = listOf(owes("floripa", carla.id, me.id, 1_000L, GroupCurrency.BRL)),
                settlements = listOf(paid("floripa", carla.id, me.id, 1_000L, GroupCurrency.EUR)),
                pendingDeletionCount = 0,
            ),
        )

        // When
        val people = PeopleBalances.of(groups, money).people

        // Then
        assertEquals(1_000L, people.single { it.currency == GroupCurrency.BRL }.netMinor)
        assertEquals(-1_000L, people.single { it.currency == GroupCurrency.EUR }.netMinor)
    }

    @Test
    fun `GIVEN bills in two currencies with someone who left WHEN netting THEN shows them gone once per currency`() {
        // Given
        val groups = listOf(group("floripa", me, carla))
        val money = mapOf(
            "floripa" to moneyOf(
                owes("floripa", "ex-member", me.id, 1_000L, GroupCurrency.BRL, id = "b1"),
                owes("floripa", "ex-member", me.id, 200L, GroupCurrency.USD, id = "b2"),
            ),
        )

        // When
        val people = PeopleBalances.of(groups, money).people

        // Then
        assertEquals(listOf("gone:floripa:ex-member", "gone:floripa:ex-member"), people.map { it.key })
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), people.map { it.currency })
        assertEquals(listOf(null, null), people.map { it.name })
    }

    private fun member(
        id: String,
        name: String,
        userId: String? = null,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = isMe || userId != null,
        userId = userId,
        inviteCode = null,
        photoUrl = null,
    )

    private fun group(
        id: String,
        vararg members: GroupMember,
        currency: GroupCurrency = GroupCurrency.BRL,
    ): Group = Group(
        id = id,
        name = id,
        type = GroupType.OTHER,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = currency,
        members = members.toList(),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private fun moneyOf(vararg expenses: Expense): GroupMoney = GroupMoney(
        expenses = expenses.toList(),
        settlements = emptyList(),
        pendingDeletionCount = 0,
    )

    // Why: a bill only one member paid, all of it on another, so the second owes the first that much.
    private fun owes(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
        currency: GroupCurrency,
        id: String = "bill-$groupId",
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = id,
        currency = currency,
        amountMinor = amountMinor,
        splitMode = SplitMode.AMOUNT,
        payers = listOf(ExpensePayer(toMemberId, amountMinor)),
        shares = listOf(ExpenseShare(fromMemberId, amountMinor, weight = amountMinor)),
        items = null,
        categoryId = null,
        spentAt = 0L,
        isPending = false,
        updatedAt = 0L,
        updatedByMemberId = null,
    )

    private fun paid(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
        currency: GroupCurrency,
    ): Settlement = Settlement(
        id = "payment-$groupId-${currency.code}",
        groupId = groupId,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        currency = currency,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = 0L,
        isPending = false,
        updatedAt = 0L,
    )
}
