package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BalanceSnapshot
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.OpenGroupBalance
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BalanceSnapshotTest {
    @Test
    fun `GIVEN no group WHEN building the snapshot THEN has no overall balance no open group and is all settled`() {
        // Given
        val groups = emptyList<Group>()

        // When
        val snapshot = BalanceSnapshot.of(groups, emptyMap())

        // Then
        assertNull(snapshot.overall)
        assertTrue(snapshot.openGroups.isEmpty())
        assertTrue(snapshot.isSettled)
    }

    @Test
    fun `GIVEN groups with no bills WHEN building THEN shows a zero balance in their currency and all settled`() {
        // Given
        val groups = listOf(group("ape", "Apê 302", me, ana), group("floripa", "Viagem Floripa", me, bruno))

        // When
        val snapshot = BalanceSnapshot.of(groups, emptyMap())

        // Then
        assertEquals(total(GroupCurrency.BRL), snapshot.overall)
        assertTrue(snapshot.openGroups.isEmpty())
        assertTrue(snapshot.isSettled)
    }

    @Test
    fun `GIVEN Ana owes you in a group WHEN building THEN shows what you are owed and the group as positive`() {
        // Given
        val groups = listOf(group("ape", "Apê 302", me, ana))
        val money = mapOf("ape" to moneyOf(owes("ape", ana.id, me.id, 1_000L)))

        // When
        val snapshot = BalanceSnapshot.of(groups, money)

        // Then
        assertEquals(
            total(
                GroupCurrency.BRL,
                netMinor = 1_000L,
                toReceiveMinor = 1_000L,
                toReceiveCount = 1,
            ),
            snapshot.overall,
        )
        assertEquals(listOf(OpenGroupBalance("ape", "Apê 302", GroupCurrency.BRL, 1_000L)), snapshot.openGroups)
        assertFalse(snapshot.isSettled)
    }

    @Test
    fun `GIVEN you owe Bruno in a group WHEN building THEN shows what you owe and the group as negative`() {
        // Given
        val groups = listOf(group("floripa", "Viagem Floripa", me, bruno))
        val money = mapOf("floripa" to moneyOf(owes("floripa", me.id, bruno.id, 2_500L)))

        // When
        val snapshot = BalanceSnapshot.of(groups, money)

        // Then
        assertEquals(
            total(
                GroupCurrency.BRL,
                netMinor = -2_500L,
                toPayMinor = 2_500L,
                toPayCount = 1,
            ),
            snapshot.overall,
        )
        assertEquals(
            listOf(OpenGroupBalance("floripa", "Viagem Floripa", GroupCurrency.BRL, -2_500L)),
            snapshot.openGroups,
        )
        assertFalse(snapshot.isSettled)
    }

    @Test
    fun `GIVEN a bill you paid for three WHEN building the snapshot THEN the group shows your own net in it`() {
        // Given
        val groups = listOf(group("churrasco", "Churrasco", me, ana, bruno))
        val bill = bill(
            id = "carne",
            groupId = "churrasco",
            payers = listOf(ExpensePayer(me.id, 3_000L)),
            shares = listOf(share(me.id, 1_000L), share(ana.id, 1_000L), share(bruno.id, 1_000L)),
        )
        val money = mapOf("churrasco" to moneyOf(bill))

        // When
        val snapshot = BalanceSnapshot.of(groups, money)

        // Then
        assertEquals(
            listOf(OpenGroupBalance("churrasco", "Churrasco", GroupCurrency.BRL, 2_000L)),
            snapshot.openGroups,
        )
        assertEquals(
            total(
                GroupCurrency.BRL,
                netMinor = 2_000L,
                toReceiveMinor = 2_000L,
                toReceiveCount = 2,
            ),
            snapshot.overall,
        )
    }

    @Test
    fun `GIVEN Ana owes you what you owe Bruno elsewhere WHEN building THEN the net is zero but not settled`() {
        // Given
        val groups = listOf(group("ape", "Apê 302", me, ana), group("floripa", "Viagem Floripa", me, bruno))
        val money = mapOf(
            "ape" to moneyOf(owes("ape", ana.id, me.id, 1_000L)),
            "floripa" to moneyOf(owes("floripa", me.id, bruno.id, 1_000L)),
        )

        // When
        val snapshot = BalanceSnapshot.of(groups, money)

        // Then
        assertEquals(
            total(
                GroupCurrency.BRL,
                netMinor = 0L,
                toReceiveMinor = 1_000L,
                toPayMinor = 1_000L,
                toReceiveCount = 1,
                toPayCount = 1,
            ),
            snapshot.overall,
        )
        assertEquals(
            listOf(
                OpenGroupBalance("ape", "Apê 302", GroupCurrency.BRL, 1_000L),
                OpenGroupBalance("floripa", "Viagem Floripa", GroupCurrency.BRL, -1_000L),
            ),
            snapshot.openGroups,
        )
        assertFalse(snapshot.isSettled)
    }

    @Test
    fun `GIVEN a group already paid back WHEN building the snapshot THEN it is not an open group and all is settled`() {
        // Given
        val groups = listOf(group("ape", "Apê 302", me, ana), group("floripa", "Viagem Floripa", me, bruno))
        val money = mapOf(
            "ape" to GroupMoney(
                expenses = listOf(owes("ape", ana.id, me.id, 1_000L)),
                settlements = listOf(paid("ape", ana.id, me.id, 1_000L)),
                pendingDeletionCount = 0,
            ),
        )

        // When
        val snapshot = BalanceSnapshot.of(groups, money)

        // Then
        assertTrue(snapshot.openGroups.isEmpty())
        assertTrue(snapshot.isSettled)
        assertEquals(total(GroupCurrency.BRL), snapshot.overall)
    }

    @Test
    fun `GIVEN a group with bills in two currencies WHEN building THEN lists the group once per open currency`() {
        // Given
        val groups = listOf(group("viagem", "Eurotrip", me, ana))
        val money = mapOf(
            "viagem" to moneyOf(
                owes(
                    "viagem",
                    ana.id,
                    me.id,
                    1_000L,
                    id = "hotel",
                    currency = GroupCurrency.EUR,
                ),
                owes(
                    "viagem",
                    me.id,
                    ana.id,
                    500L,
                    id = "taxi",
                    currency = GroupCurrency.USD,
                ),
            ),
        )

        // When
        val snapshot = BalanceSnapshot.of(groups, money)

        // Then
        assertEquals(
            listOf(
                OpenGroupBalance("viagem", "Eurotrip", GroupCurrency.EUR, 1_000L),
                OpenGroupBalance("viagem", "Eurotrip", GroupCurrency.USD, -500L),
            ),
            snapshot.openGroups,
        )
        assertFalse(snapshot.isSettled)
    }

    @Test
    fun `GIVEN one of the currencies of a group paid back WHEN building THEN lists it only in the other`() {
        // Given
        val groups = listOf(group("viagem", "Eurotrip", me, ana))
        val money = mapOf(
            "viagem" to GroupMoney(
                expenses = listOf(
                    owes("viagem", ana.id, me.id, 1_000L, id = "jantar"),
                    owes(
                        "viagem",
                        ana.id,
                        me.id,
                        700L,
                        id = "hotel",
                        currency = GroupCurrency.EUR,
                    ),
                ),
                settlements = listOf(paid("viagem", ana.id, me.id, 1_000L)),
                pendingDeletionCount = 0,
            ),
        )

        // When
        val snapshot = BalanceSnapshot.of(groups, money)

        // Then
        assertEquals(
            listOf(OpenGroupBalance("viagem", "Eurotrip", GroupCurrency.EUR, 700L)),
            snapshot.openGroups,
        )
    }

    @Test
    fun `GIVEN the main currency settled and another open WHEN building THEN shows the open one`() {
        // Given
        val groups = listOf(
            group("ape", "Apê 302", me, ana),
            group("churrasco", "Churrasco", me, bruno),
            group("nyc", "Nova York", me, ana, currency = GroupCurrency.USD),
        )
        val money = mapOf("nyc" to moneyOf(owes("nyc", ana.id, me.id, 4_000L, currency = GroupCurrency.USD)))

        // When
        val snapshot = BalanceSnapshot.of(groups, money)

        // Then
        assertEquals(
            total(
                GroupCurrency.USD,
                netMinor = 4_000L,
                toReceiveMinor = 4_000L,
                toReceiveCount = 1,
            ),
            snapshot.overall,
        )
    }

    @Test
    fun `GIVEN two currencies with a balance WHEN building the snapshot THEN shows the one of the most groups`() {
        // Given
        val groups = listOf(
            group("nyc", "Nova York", me, ana, currency = GroupCurrency.USD),
            group("ape", "Apê 302", me, ana),
            group("churrasco", "Churrasco", me, bruno),
        )
        val money = mapOf(
            "nyc" to moneyOf(owes("nyc", ana.id, me.id, 4_000L, currency = GroupCurrency.USD)),
            "ape" to moneyOf(owes("ape", me.id, ana.id, 300L)),
        )

        // When
        val snapshot = BalanceSnapshot.of(groups, money)

        // Then
        assertEquals(
            total(
                GroupCurrency.BRL,
                netMinor = -300L,
                toPayMinor = 300L,
                toPayCount = 1,
            ),
            snapshot.overall,
        )
    }

    @Test
    fun `GIVEN a group you are not a member of WHEN building THEN adds no open group and all is settled`() {
        // Given
        val groups = listOf(group("outro", "Grupo dos outros", ana, bruno))
        val money = mapOf("outro" to moneyOf(owes("outro", ana.id, bruno.id, 1_000L)))

        // When
        val snapshot = BalanceSnapshot.of(groups, money)

        // Then
        assertTrue(snapshot.openGroups.isEmpty())
        assertTrue(snapshot.isSettled)
    }

    @Test
    fun `GIVEN open groups in a given order WHEN building the snapshot THEN keeps that order`() {
        // Given
        val groups = listOf(
            group("c", "Churrasco", me, ana),
            group("a", "Apê 302", me, ana),
            group("b", "Viagem Floripa", me, bruno),
        )
        val money = mapOf(
            "a" to moneyOf(owes("a", ana.id, me.id, 100L)),
            "b" to moneyOf(owes("b", me.id, bruno.id, 200L)),
            "c" to moneyOf(owes("c", ana.id, me.id, 300L)),
        )

        // When
        val snapshot = BalanceSnapshot.of(groups, money)

        // Then
        assertEquals(listOf("c", "a", "b"), snapshot.openGroups.map { it.groupId })
    }

    @Test
    fun `GIVEN Ana owes you in one group what you owe her in another WHEN building THEN both groups stay open`() {
        // Given
        val groups = listOf(group("ape", "Apê 302", me, ana), group("floripa", "Viagem Floripa", me, ana))
        val money = mapOf(
            "ape" to moneyOf(owes("ape", ana.id, me.id, 1_000L)),
            "floripa" to moneyOf(owes("floripa", me.id, ana.id, 1_000L)),
        )

        // When
        val snapshot = BalanceSnapshot.of(groups, money)

        // Then
        assertEquals(
            listOf(
                OpenGroupBalance("ape", "Apê 302", GroupCurrency.BRL, 1_000L),
                OpenGroupBalance("floripa", "Viagem Floripa", GroupCurrency.BRL, -1_000L),
            ),
            snapshot.openGroups,
        )
        assertFalse(snapshot.isSettled)
    }

    @Test
    fun `GIVEN the main currency settled and another offsetting WHEN building THEN its totals are the open ones`() {
        // Given
        val groups = listOf(
            group("ape", "Apê 302", me, ana),
            group("churrasco", "Churrasco", me, bruno),
            group("nyc", "Nova York", me, ana, bruno, currency = GroupCurrency.USD),
        )
        val money = mapOf(
            "nyc" to moneyOf(
                owes(
                    "nyc",
                    ana.id,
                    me.id,
                    1_000L,
                    id = "dinner",
                    currency = GroupCurrency.USD,
                ),
                owes(
                    "nyc",
                    me.id,
                    bruno.id,
                    1_000L,
                    id = "taxi",
                    currency = GroupCurrency.USD,
                ),
            ),
        )

        // When
        val snapshot = BalanceSnapshot.of(groups, money)

        // Then
        assertEquals(total(GroupCurrency.BRL), snapshot.overall)
        val usd = total(
            GroupCurrency.USD,
            toReceiveMinor = 1_000L,
            toPayMinor = 1_000L,
            toReceiveCount = 1,
            toPayCount = 1,
        )
        assertEquals(usd, snapshot.toReceive)
        assertEquals(usd, snapshot.toPay)
        assertFalse(snapshot.isSettled)
    }

    private val me = member("me", "Pierre", isMe = true)
    private val ana = member("ana", "Ana", userId = "ana")
    private val bruno = member("bruno", "Bruno", userId = "bruno")

    private fun total(
        currency: GroupCurrency,
        netMinor: Long = 0L,
        toReceiveMinor: Long = 0L,
        toPayMinor: Long = 0L,
        toReceiveCount: Int = 0,
        toPayCount: Int = 0,
    ): PeopleTotal = PeopleTotal(
        currency = currency,
        netMinor = netMinor,
        toReceiveMinor = toReceiveMinor,
        toPayMinor = toPayMinor,
        toReceiveCount = toReceiveCount,
        toPayCount = toPayCount,
    )

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
        name: String,
        vararg members: GroupMember,
        currency: GroupCurrency = GroupCurrency.BRL,
    ): Group = Group(
        id = id,
        name = name,
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
        reminder = GroupReminder.Default,
        reminderDismissedAt = 0L,
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
        id: String = "bill-$groupId",
        currency: GroupCurrency = GroupCurrency.BRL,
    ): Expense = bill(
        id = id,
        groupId = groupId,
        payers = listOf(ExpensePayer(toMemberId, amountMinor)),
        shares = listOf(share(fromMemberId, amountMinor)),
        currency = currency,
    )

    private fun bill(
        id: String,
        groupId: String,
        payers: List<ExpensePayer>,
        shares: List<ExpenseShare>,
        currency: GroupCurrency = GroupCurrency.BRL,
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = id,
        currency = currency,
        amountMinor = payers.sumOf { it.amountMinor },
        splitMode = SplitMode.AMOUNT,
        payers = payers,
        shares = shares,
        items = null,
        categoryId = null,
        spentAt = 0L,
        isPending = false,
        updatedAt = 0L,
        updatedByMemberId = null,
    )

    private fun share(
        memberId: String,
        amountMinor: Long,
    ): ExpenseShare = ExpenseShare(
        memberId = memberId,
        amountMinor = amountMinor,
        weight = amountMinor,
    )

    private fun paid(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
    ): Settlement = Settlement(
        id = "payment-$groupId",
        groupId = groupId,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        currency = GroupCurrency.BRL,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = 0L,
        isPending = false,
        updatedAt = 0L,
    )
}
