package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementLeg
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeopleBalancesTest {
    @Test
    fun `GIVEN someone who owes you in one group and whom you owe in another WHEN netting THEN shows them once`() {
        // Given
        val groups = listOf(
            group("floripa", "Viagem Floripa", me, carla),
            group("ape", "Apê 302", me, carla),
            group("churrasco", "Churrasco de sábado", me, carla),
        )
        val money = mapOf(
            "floripa" to moneyOf(owes("floripa", carla.id, me.id, 9_840L)),
            "ape" to moneyOf(owes("ape", me.id, carla.id, 9_800L)),
            "churrasco" to moneyOf(owes("churrasco", carla.id, me.id, 3_250L)),
        )

        // When
        val people = PeopleBalances.of(groups, money).people

        // Then
        val person = people.single()
        assertEquals("user:carla", person.key)
        assertEquals("Carla", person.name)
        assertEquals(3_290L, person.netMinor)
        assertEquals(listOf("floripa", "ape", "churrasco"), person.groups.map { it.groupId })
        assertEquals(listOf(9_840L, -9_800L, 3_250L), person.groups.map { it.amountMinor })
        assertFalse(person.isGuest)
    }

    @Test
    fun `GIVEN someone across groups WHEN settling up THEN each group pays the way its pair owes`() {
        // Given
        val groups = listOf(group("floripa", "Viagem Floripa", me, carla), group("ape", "Apê 302", me, carla))
        val money = mapOf(
            "floripa" to moneyOf(owes("floripa", carla.id, me.id, 9_840L)),
            "ape" to moneyOf(owes("ape", me.id, carla.id, 9_800L)),
        )

        // When
        val legs = PeopleBalances
            .of(groups, money)
            .people
            .single()
            .legs

        // Then
        assertEquals(
            listOf(
                SettlementLeg(
                    "floripa",
                    fromMemberId = carla.id,
                    toMemberId = me.id,
                    amountMinor = 9_840L,
                ),
                SettlementLeg(
                    "ape",
                    fromMemberId = me.id,
                    toMemberId = carla.id,
                    amountMinor = 9_800L,
                ),
            ),
            legs,
        )
    }

    @Test
    fun `GIVEN a payment recorded WHEN netting THEN counts what is still open`() {
        // Given
        val groups = listOf(group("floripa", "Viagem Floripa", me, carla))
        val money = mapOf(
            "floripa" to GroupMoney(
                expenses = listOf(owes("floripa", carla.id, me.id, 9_840L)),
                settlements = listOf(paid("floripa", carla.id, me.id, 4_000L)),
                pendingDeletionCount = 0,
            ),
        )

        // When
        val people = PeopleBalances.of(groups, money).people

        // Then
        assertEquals(5_840L, people.single().netMinor)
    }

    @Test
    fun `GIVEN a name nobody joined as in two groups WHEN netting THEN it is one guest written either way`() {
        // Given
        val diegoHere = member("d1", "Diego")
        val diegoThere = member("d2", " diego ")
        val groups = listOf(group("churrasco", "Churrasco", me, diegoHere), group("futebol", "Futebol", me, diegoThere))
        val money = mapOf(
            "churrasco" to moneyOf(owes("churrasco", diegoHere.id, me.id, 4_500L)),
            "futebol" to moneyOf(owes("futebol", diegoThere.id, me.id, 1_000L)),
        )

        // When
        val people = PeopleBalances.of(groups, money).people

        // Then
        val person = people.single()
        assertEquals("name:diego", person.key)
        assertEquals("Diego", person.name)
        assertEquals(5_500L, person.netMinor)
        assertTrue(person.isGuest)
    }

    @Test
    fun `GIVEN someone who joined and a name nobody joined as WHEN they share a name THEN they stay apart`() {
        // Given
        val anaGuest = member("a2", "Ana")
        val groups = listOf(group("floripa", "Floripa", me, ana), group("ape", "Apê", me, anaGuest))
        val money = mapOf(
            "floripa" to moneyOf(owes("floripa", ana.id, me.id, 2_000L)),
            "ape" to moneyOf(owes("ape", anaGuest.id, me.id, 1_000L)),
        )

        // When
        val people = PeopleBalances.of(groups, money).people

        // Then
        assertEquals(listOf("user:ana", "name:ana"), people.map { it.key })
    }

    @Test
    fun `GIVEN a debt with someone no longer in the group WHEN netting THEN shows them nameless on their own`() {
        // Given
        val groups = listOf(group("floripa", "Floripa", me))
        val money = mapOf("floripa" to moneyOf(owes("floripa", "gone-member", me.id, 1_500L)))

        // When
        val person = PeopleBalances.of(groups, money).people.single()

        // Then
        assertEquals("gone:floripa:gone-member", person.key)
        assertNull(person.name)
        assertNull(person.face)
        assertFalse(person.isGuest)
    }

    @Test
    fun `GIVEN groups that offset each other to zero WHEN netting THEN the person is not listed`() {
        // Given
        val groups = listOf(group("floripa", "Floripa", me, carla), group("ape", "Apê", me, carla))
        val money = mapOf(
            "floripa" to moneyOf(owes("floripa", carla.id, me.id, 2_000L)),
            "ape" to moneyOf(owes("ape", me.id, carla.id, 2_000L)),
        )

        // When
        val people = PeopleBalances.of(groups, money).people

        // Then
        assertTrue(people.isEmpty())
    }

    @Test
    fun `GIVEN debts between other members WHEN netting THEN only your pairs count`() {
        // Given
        val groups = listOf(group("floripa", "Floripa", me, ana, carla))
        val money = mapOf("floripa" to moneyOf(owes("floripa", ana.id, carla.id, 2_000L)))

        // When
        val people = PeopleBalances.of(groups, money).people

        // Then
        assertTrue(people.isEmpty())
    }

    @Test
    fun `GIVEN several people WHEN listing THEN the largest net comes first whichever its direction`() {
        // Given
        val groups = listOf(group("floripa", "Floripa", me, ana, carla, bruno))
        val money = mapOf(
            "floripa" to GroupMoney(
                expenses = listOf(
                    owes("floripa", ana.id, me.id, 1_000L, id = "e1"),
                    owes("floripa", me.id, bruno.id, 5_000L, id = "e2"),
                    owes("floripa", carla.id, me.id, 3_000L, id = "e3"),
                ),
                settlements = emptyList(),
                pendingDeletionCount = 0,
            ),
        )

        // When
        val people = PeopleBalances.of(groups, money).people

        // Then
        assertEquals(listOf("Bruno", "Carla", "Ana"), people.map { it.name })
    }

    @Test
    fun `GIVEN groups in two currencies WHEN netting THEN each currency has its own row and total`() {
        // Given
        val groups = listOf(
            group("floripa", "Floripa", me, carla),
            group("ape", "Apê", me, carla),
            group("lisboa", "Lisboa", me, carla, currency = GroupCurrency.EUR),
        )
        val money = mapOf(
            "floripa" to moneyOf(owes("floripa", carla.id, me.id, 1_000L)),
            "ape" to moneyOf(owes("ape", me.id, ana.id, 0L)),
            "lisboa" to moneyOf(owes("lisboa", me.id, carla.id, 700L)),
        )

        // When
        val balances = PeopleBalances.of(groups, money)

        // Then
        assertEquals(listOf(1_000L, -700L), balances.people.map { it.netMinor })
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.EUR), balances.people.map { it.currency })
        assertEquals(
            listOf(
                PeopleTotal(
                    GroupCurrency.BRL,
                    netMinor = 1_000L,
                    toReceiveMinor = 1_000L,
                    toPayMinor = 0L,
                    toReceiveCount = 1,
                    toPayCount = 0,
                ),
                PeopleTotal(
                    GroupCurrency.EUR,
                    netMinor = -700L,
                    toReceiveMinor = 0L,
                    toPayMinor = 700L,
                    toReceiveCount = 0,
                    toPayCount = 1,
                ),
            ),
            balances.totals,
        )
        assertEquals(balances.people.first(), balances.findPersonOrNull("user:carla", "BRL"))
    }

    @Test
    fun `GIVEN people you owe and people who owe you WHEN totalling THEN the net is what the groups add up to`() {
        // Given
        val groups = listOf(group("floripa", "Floripa", me, ana, bruno, carla))
        val money = mapOf(
            "floripa" to GroupMoney(
                expenses = listOf(
                    owes("floripa", me.id, bruno.id, 12_600L, id = "e1"),
                    owes("floripa", ana.id, me.id, 16_230L, id = "e2"),
                    owes("floripa", carla.id, me.id, 3_290L, id = "e3"),
                ),
                settlements = emptyList(),
                pendingDeletionCount = 0,
            ),
        )

        // When
        val total = PeopleBalances.of(groups, money).totals.single()

        // Then
        assertEquals(
            PeopleTotal(
                currency = GroupCurrency.BRL,
                netMinor = 6_920L,
                toReceiveMinor = 19_520L,
                toPayMinor = 12_600L,
                toReceiveCount = 2,
                toPayCount = 1,
            ),
            total,
        )
    }

    @Test
    fun `GIVEN a guest who paid part of the bills WHEN netting THEN names their bills and what they paid`() {
        // Given
        val diego = member("d1", "Diego")
        val groups = listOf(group("churrasco", "Churrasco", me, diego))
        val money = mapOf(
            "churrasco" to GroupMoney(
                expenses = listOf(
                    bill(
                        "bebidas",
                        "churrasco",
                        description = "Bebidas",
                        payers = listOf(ExpensePayer(diego.id, 18_250L)),
                        shares = listOf(share(me.id, 5_500L), share(diego.id, 12_750L)),
                    ),
                    bill(
                        "carne",
                        "churrasco",
                        description = "Carne",
                        payers = listOf(ExpensePayer(me.id, 20_000L)),
                        shares = listOf(share(me.id, 10_000L), share(diego.id, 10_000L)),
                    ),
                ),
                settlements = emptyList(),
                pendingDeletionCount = 0,
            ),
        )

        // When
        val person = PeopleBalances.of(groups, money).people.single()

        // Then
        assertEquals(4_500L, person.netMinor)
        assertEquals(listOf("Bebidas", "Carne"), person.guestBills)
        assertEquals(18_250L, person.guestPaidMinor)
    }

    @Test
    fun `GIVEN a guest whose bills also involve others WHEN netting THEN leaves out what they paid`() {
        // Given
        val diego = member("d1", "Diego")
        val groups = listOf(group("churrasco", "Churrasco", me, diego, ana))
        val money = mapOf(
            "churrasco" to moneyOf(
                bill(
                    "carne",
                    "churrasco",
                    description = "Carne",
                    payers = listOf(ExpensePayer(me.id, 10_000L)),
                    shares = listOf(share(me.id, 5_000L), share(diego.id, 5_000L)),
                ),
                bill(
                    "gelo",
                    "churrasco",
                    description = "Gelo",
                    payers = listOf(ExpensePayer(ana.id, 5_000L), ExpensePayer(diego.id, 1_000L)),
                    shares = listOf(share(ana.id, 3_000L), share(diego.id, 3_000L)),
                ),
            ),
        )

        // When
        val person = PeopleBalances.of(groups, money).people.first { it.key == "name:diego" }

        // Then
        assertEquals(5_000L, person.netMinor)
        assertEquals(listOf("Carne", "Gelo"), person.guestBills)
        assertNull(person.guestPaidMinor)
    }

    @Test
    fun `GIVEN a guest with bills in two groups WHEN netting THEN names their bills the most recent first`() {
        // Given
        val diegoHere = member("d1", "Diego")
        val diegoThere = member("d2", "Diego")
        val groups = listOf(group("churrasco", "Churrasco", me, diegoHere), group("futebol", "Futebol", me, diegoThere))
        val money = mapOf(
            "churrasco" to moneyOf(
                guestBill("carne", "churrasco", "Carne", diegoHere.id, spentAt = 1_000L),
                guestBill("bebidas", "churrasco", "Bebidas", diegoHere.id, spentAt = 2_000L),
            ),
            "futebol" to moneyOf(guestBill("pizza", "futebol", "Pizza", diegoThere.id, spentAt = 3_000L)),
        )

        // When
        val person = PeopleBalances.of(groups, money).people.single()

        // Then
        assertEquals(listOf("Pizza", "Bebidas", "Carne"), person.guestBills)
    }

    @Test
    fun `GIVEN someone who joined WHEN netting THEN has no guest details`() {
        // Given
        val groups = listOf(group("floripa", "Floripa", me, ana))
        val money = mapOf("floripa" to moneyOf(owes("floripa", ana.id, me.id, 1_000L)))

        // When
        val person = PeopleBalances.of(groups, money).people.single()

        // Then
        assertTrue(person.guestBills.isEmpty())
        assertNull(person.guestPaidMinor)
    }

    @Test
    fun `GIVEN groups WHEN netting THEN this device user is as the most recent group has them`() {
        // Given
        val renamedMe = me.copy(
            id = "me-2",
            name = "Pierre V.",
        )
        val groups = listOf(group("ape", "Apê", renamedMe), group("floripa", "Floripa", me))

        // When
        val balances = PeopleBalances.of(groups, emptyMap())

        // Then
        assertEquals(renamedMe, balances.me)
        assertTrue(balances.people.isEmpty())
        assertEquals(listOf(GroupCurrency.BRL), balances.totals.map { it.currency })
    }

    private val me = member("me", "Pierre", isMe = true)
    private val ana = member("ana", "Ana", userId = "ana")
    private val bruno = member("bruno", "Bruno", userId = "bruno")
    private val carla = member("carla", "Carla", userId = "carla")

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
        currency = currency,
        members = members.toList(),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        updatedAt = 0L,
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
    ): Expense = bill(
        id = id,
        groupId = groupId,
        description = id,
        payers = listOf(ExpensePayer(toMemberId, amountMinor)),
        shares = listOf(share(fromMemberId, amountMinor)),
    )

    private fun bill(
        id: String,
        groupId: String,
        description: String,
        payers: List<ExpensePayer>,
        shares: List<ExpenseShare>,
        spentAt: Long = 0L,
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = description,
        amountMinor = payers.sumOf { it.amountMinor },
        splitMode = SplitMode.AMOUNT,
        payers = payers,
        shares = shares,
        spentAt = spentAt,
        isPending = false,
        updatedAt = 0L,
        updatedByMemberId = null,
    )

    // Why: this device's user paid a bill all on the guest.
    private fun guestBill(
        id: String,
        groupId: String,
        description: String,
        guestId: String,
        spentAt: Long,
    ): Expense = bill(
        id = id,
        groupId = groupId,
        description = description,
        payers = listOf(ExpensePayer(me.id, 1_000L)),
        shares = listOf(share(guestId, 1_000L)),
        spentAt = spentAt,
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
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = 0L,
        isPending = false,
        updatedAt = 0L,
    )
}
