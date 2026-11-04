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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupBalancesSimplifyTest {
    @Test
    fun `GIVEN a group with simplifying off WHEN computing its balances THEN keeps the pairs as the bills make them`() {
        // Given
        val group = group(simplifiesDebts = false)
        val expenses = listOf(owes(ANA, BRUNO, 100L), owes(BRUNO, CARLA, 100L))

        // When
        val balances = GroupBalances.of(group, expenses, settlements = emptyList())

        // Then
        assertEquals(listOf(Debt(ANA, BRUNO, 100L), Debt(BRUNO, CARLA, 100L)), balances.debts)
        assertFalse(balances.isSimplified)
        assertNull(balances.unsimplifiedDebts)
        assertEquals(2, balances.unsimplifiedCount)
    }

    @Test
    fun `GIVEN a chain of debts and simplifying on WHEN computing the balances THEN the first pays the last`() {
        // Given
        val group = group(simplifiesDebts = true)
        val expenses = listOf(owes(ANA, BRUNO, 100L), owes(BRUNO, CARLA, 100L))

        // When
        val balances = GroupBalances.of(group, expenses, settlements = emptyList())

        // Then
        assertEquals(listOf(Debt(ANA, CARLA, 100L)), balances.debts)
        assertTrue(balances.isSimplified)
        assertEquals(2, balances.unsimplifiedCount)
        assertEquals(listOf(Debt(ANA, BRUNO, 100L), Debt(BRUNO, CARLA, 100L)), balances.unsimplifiedDebts)
    }

    @Test
    fun `GIVEN a chain of debts WHEN computing the balances without the group THEN never simplifies`() {
        // Given
        val expenses = listOf(owes(ANA, BRUNO, 100L), owes(BRUNO, CARLA, 100L))

        // When
        val balances = GroupBalances.of(expenses, settlements = emptyList())

        // Then
        assertEquals(listOf(Debt(ANA, BRUNO, 100L), Debt(BRUNO, CARLA, 100L)), balances.debts)
        assertFalse(balances.isSimplified)
    }

    @Test
    fun `GIVEN Viagem Floripa with 6 people and 7 pairs WHEN simplifying THEN 5 payments keep each one net`() {
        // Given
        val group = group(
            simplifiesDebts = true,
            memberIds = listOf(PIERRE, ANA, BRUNO, CARLA, DIEGO, ELISA),
        )
        val expenses = listOf(
            owes(ANA, PIERRE, 100L, id = "1"),
            owes(BRUNO, PIERRE, 50L, id = "2"),
            owes(CARLA, BRUNO, 80L, id = "3"),
            owes(DIEGO, BRUNO, 60L, id = "4"),
            owes(ELISA, CARLA, 40L, id = "5"),
            owes(DIEGO, ANA, 30L, id = "6"),
            owes(ELISA, PIERRE, 20L, id = "7"),
        )
        val raw = GroupBalances.of(expenses, settlements = emptyList())

        // When
        val balances = GroupBalances.of(group, expenses, settlements = emptyList())

        // Then
        assertEquals(
            listOf(
                Debt(DIEGO, PIERRE, 90L),
                Debt(ANA, BRUNO, 70L),
                Debt(ELISA, PIERRE, 60L),
                Debt(CARLA, BRUNO, 20L),
                Debt(CARLA, PIERRE, 20L),
            ),
            balances.debts,
        )
        assertEquals(7, balances.unsimplifiedCount)
        group.members.forEach { member ->
            assertEquals(netOf(raw.debts, member.id), netOf(balances.debts, member.id), member.id)
        }
    }

    @Test
    fun `GIVEN two debtors and two creditors WHEN simplifying THEN the greatest debtor pays the greatest creditor`() {
        // Given
        val balances = unsimplified(
            Debt(DIEGO, PIERRE, 50L),
            Debt(DIEGO, BRUNO, 40L),
            Debt(ELISA, PIERRE, 50L),
            Debt(ELISA, BRUNO, 10L),
        )

        // When
        val simplified = balances.toSimplified(listOf(PIERRE, BRUNO, DIEGO, ELISA))

        // Then
        assertEquals(
            listOf(
                Debt(DIEGO, PIERRE, 90L),
                Debt(ELISA, BRUNO, 50L),
                Debt(ELISA, PIERRE, 10L),
            ),
            simplified.debts,
        )
    }

    @Test
    fun `GIVEN debtors and creditors that tie WHEN simplifying THEN they go in the order they joined the group`() {
        // Given
        val balances = unsimplified(
            Debt(ANA, CARLA, 50L),
            Debt(ANA, DIEGO, 50L),
            Debt(BRUNO, CARLA, 50L),
            Debt(BRUNO, DIEGO, 50L),
        )

        // When
        val simplified = balances.toSimplified(listOf(DIEGO, ANA, CARLA, BRUNO))

        // Then
        assertEquals(listOf(Debt(ANA, DIEGO, 100L), Debt(BRUNO, CARLA, 100L)), simplified.debts)
    }

    @Test
    fun `GIVEN the same pairs listed in another order WHEN simplifying THEN every device gets the same payments`() {
        // Given
        val pairs = listOf(
            Debt(ANA, CARLA, 50L),
            Debt(ANA, DIEGO, 50L),
            Debt(BRUNO, CARLA, 50L),
            Debt(BRUNO, DIEGO, 50L),
        )
        val order = listOf(ANA, BRUNO, CARLA, DIEGO)
        val expected = unsimplified(*pairs.toTypedArray()).toSimplified(order).debts

        // When
        val simplified = unsimplified(*pairs.reversed().toTypedArray()).toSimplified(order)

        // Then
        assertEquals(expected, simplified.debts)
        assertEquals(listOf(Debt(ANA, CARLA, 100L), Debt(BRUNO, DIEGO, 100L)), simplified.debts)
    }

    @Test
    fun `GIVEN a member and an ex-member that tie WHEN simplifying THEN the ex-member comes after the members`() {
        // Given
        val balances = unsimplified(
            Debt(EX_MEMBER_A, CARLA, 50L),
            Debt(EX_MEMBER_A, DIEGO, 50L),
            Debt(ZOE, CARLA, 50L),
            Debt(ZOE, DIEGO, 50L),
        )

        // When
        val simplified = balances.toSimplified(listOf(CARLA, DIEGO, ZOE))

        // Then
        assertEquals(listOf(Debt(EX_MEMBER_A, DIEGO, 100L), Debt(ZOE, CARLA, 100L)), simplified.debts)
    }

    @Test
    fun `GIVEN two ex-members that tie WHEN simplifying THEN they go by id`() {
        // Given
        val balances = unsimplified(
            Debt(EX_MEMBER_B, CARLA, 50L),
            Debt(EX_MEMBER_B, DIEGO, 50L),
            Debt(EX_MEMBER_A, CARLA, 50L),
            Debt(EX_MEMBER_A, DIEGO, 50L),
        )

        // When
        val simplified = balances.toSimplified(listOf(CARLA, DIEGO))

        // Then
        assertEquals(listOf(Debt(EX_MEMBER_A, CARLA, 100L), Debt(EX_MEMBER_B, DIEGO, 100L)), simplified.debts)
    }

    @Test
    fun `GIVEN a circle of debts that nets to zero WHEN simplifying THEN no payment is left`() {
        // Given
        val group = group(simplifiesDebts = true)
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, id = "1"),
            owes(BRUNO, CARLA, 100L, id = "2"),
            owes(CARLA, ANA, 100L, id = "3"),
        )

        // When
        val balances = GroupBalances.of(group, expenses, settlements = emptyList())

        // Then
        assertTrue(balances.debts.isEmpty())
        assertEquals(3, balances.unsimplifiedCount)
        assertTrue(balances.isEveryoneSettled)
    }

    @Test
    fun `GIVEN two people WHEN simplifying THEN keeps their pair as it is`() {
        // Given
        val group = group(
            simplifiesDebts = true,
            memberIds = listOf(ANA, BRUNO),
        )
        val expenses = listOf(owes(ANA, BRUNO, 100L))

        // When
        val balances = GroupBalances.of(group, expenses, settlements = emptyList())

        // Then
        assertEquals(listOf(Debt(ANA, BRUNO, 100L)), balances.debts)
        assertTrue(balances.isSimplified)
        assertEquals(1, balances.unsimplifiedCount)
    }

    @Test
    fun `GIVEN pairs already at the fewest payments but routed otherwise WHEN simplifying THEN keeps the raw pairs`() {
        // Given
        val balances = unsimplified(Debt(ANA, DIEGO, 50L), Debt(BRUNO, CARLA, 50L))

        // When
        val simplified = balances.toSimplified(listOf(ANA, BRUNO, CARLA, DIEGO))

        // Then
        assertEquals(listOf(Debt(ANA, DIEGO, 50L), Debt(BRUNO, CARLA, 50L)), simplified.debts)
        assertTrue(simplified.isSimplified)
        assertEquals(2, simplified.unsimplifiedCount)
    }

    @Test
    fun `GIVEN a partial payment recorded on a chain WHEN simplifying THEN the payments come from the nets left`() {
        // Given
        val group = group(simplifiesDebts = true)
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, id = "1"),
            owes(BRUNO, CARLA, 100L, id = "2"),
            owes(CARLA, DIEGO, 100L, id = "3"),
        )
        val settlements = listOf(payment(ANA, BRUNO, 40L))

        // When
        val balances = GroupBalances.of(group, expenses, settlements)

        // Then
        assertEquals(listOf(Debt(ANA, DIEGO, 60L), Debt(BRUNO, DIEGO, 40L)), balances.debts)
        assertEquals(3, balances.unsimplifiedCount)
    }

    @Test
    fun `GIVEN a payment of a simplified pair recorded WHEN simplifying again THEN only what is left of it stays`() {
        // Given
        val group = group(simplifiesDebts = true)
        val expenses = listOf(
            owes(ANA, BRUNO, 100L, id = "1"),
            owes(BRUNO, CARLA, 100L, id = "2"),
            owes(CARLA, DIEGO, 100L, id = "3"),
        )
        val settlements = listOf(payment(ANA, DIEGO, 60L))

        // When
        val balances = GroupBalances.of(group, expenses, settlements)

        // Then
        assertEquals(listOf(Debt(ANA, DIEGO, 40L)), balances.debts)
    }

    @Test
    fun `GIVEN balances already simplified WHEN simplifying them again THEN they stay the same`() {
        // Given
        val order = listOf(ANA, BRUNO, CARLA)
        val once = unsimplified(Debt(ANA, BRUNO, 100L), Debt(BRUNO, CARLA, 100L)).toSimplified(order)

        // When
        val twice = once.toSimplified(order)

        // Then
        assertEquals(once, twice)
    }

    @Test
    fun `GIVEN simplifying off WHEN asking whether a pair was simplified THEN it never is`() {
        // Given
        val balances = unsimplified(Debt(ANA, CARLA, 100L))

        // When
        val isSimplified = balances.isSimplifiedPair(Debt(ANA, CARLA, 100L))

        // Then
        assertFalse(isSimplified)
    }

    @Test
    fun `GIVEN a pair no bill made WHEN asking whether it was simplified THEN it is`() {
        // Given
        val balances = chainSimplified()

        // When
        val isSimplified = balances.isSimplifiedPair(Debt(ANA, CARLA, 100L))

        // Then
        assertTrue(isSimplified)
    }

    @Test
    fun `GIVEN a pair a bill made with another amount WHEN asking whether it was simplified THEN it is not`() {
        // Given
        val balances = unsimplified(
            Debt(ANA, CARLA, 30L),
            Debt(ANA, BRUNO, 70L),
            Debt(BRUNO, CARLA, 70L),
        ).toSimplified(listOf(ANA, BRUNO, CARLA))

        // When
        val isSimplified = balances.isSimplifiedPair(balances.debts.single())

        // Then
        assertEquals(listOf(Debt(ANA, CARLA, 100L)), balances.debts)
        assertFalse(isSimplified)
    }

    @Test
    fun `GIVEN simplifying off WHEN asking whom a pair is instead of THEN there is nobody`() {
        // Given
        val balances = unsimplified(Debt(ANA, BRUNO, 100L), Debt(BRUNO, CARLA, 100L))

        // When
        val insteadOf = balances.insteadOfOrNull(ANA, BRUNO)

        // Then
        assertNull(insteadOf)
    }

    @Test
    fun `GIVEN a pair as the bills made it WHEN asking whom it is instead of THEN there is nobody`() {
        // Given
        val balances = unsimplified(Debt(ANA, DIEGO, 50L), Debt(BRUNO, CARLA, 50L)).toSimplified(
            listOf(ANA, BRUNO, CARLA, DIEGO),
        )

        // When
        val insteadOf = balances.insteadOfOrNull(ANA, DIEGO)

        // Then
        assertNull(insteadOf)
    }

    @Test
    fun `GIVEN a pair the simplification made WHEN asking whom it is instead of THEN names whom the payer owed`() {
        // Given
        val balances = chainSimplified()

        // When
        val insteadOf = balances.insteadOfOrNull(ANA, CARLA)

        // Then
        assertEquals(listOf(BRUNO), insteadOf)
    }

    @Test
    fun `GIVEN a pair the simplification grew WHEN asking whom it is instead of THEN leaves the receiver out`() {
        // Given
        val balances = unsimplified(
            Debt(ANA, CARLA, 30L),
            Debt(ANA, BRUNO, 70L),
            Debt(BRUNO, CARLA, 70L),
        ).toSimplified(listOf(ANA, BRUNO, CARLA))

        // When
        val insteadOf = balances.insteadOfOrNull(ANA, CARLA)

        // Then
        assertEquals(listOf(BRUNO), insteadOf)
    }

    @Test
    fun `GIVEN a bill debt that only shrank WHEN asking whom the new pair replaces THEN names that creditor`() {
        // Given
        val balances = unsimplified(
            Debt(ANA, BRUNO, 100L),
            Debt(BRUNO, CARLA, 60L),
            Debt(DIEGO, ELISA, 10L),
            Debt(ELISA, PIERRE, 10L),
        ).toSimplified(listOf(ANA, BRUNO, CARLA, DIEGO, ELISA, PIERRE))

        // When
        val insteadOf = balances.insteadOfOrNull(ANA, CARLA)

        // Then
        assertEquals(
            listOf(Debt(ANA, CARLA, 60L), Debt(ANA, BRUNO, 40L), Debt(DIEGO, PIERRE, 10L)),
            balances.debts,
        )
        assertEquals(listOf(BRUNO), insteadOf)
    }

    @Test
    fun `GIVEN a pair the simplification only shrank WHEN asking whom it is instead of THEN there is none`() {
        // Given
        val balances = unsimplified(
            Debt(ANA, BRUNO, 100L),
            Debt(BRUNO, CARLA, 60L),
            Debt(DIEGO, ELISA, 10L),
            Debt(ELISA, PIERRE, 10L),
        ).toSimplified(listOf(ANA, BRUNO, CARLA, DIEGO, ELISA, PIERRE))

        // When
        val insteadOf = balances.insteadOfOrNull(ANA, BRUNO)

        // Then
        assertNull(insteadOf)
    }

    @Test
    fun `GIVEN a pair that is not shown WHEN asking whom it is instead of THEN there is nobody`() {
        // Given
        val balances = chainSimplified()

        // When
        val insteadOf = balances.insteadOfOrNull(BRUNO, CARLA)

        // Then
        assertNull(insteadOf)
    }

    // Why: Ana owes Bruno and Bruno owes Carla the same, which simplifying turns into Ana paying Carla.
    private fun chainSimplified(): GroupBalances = unsimplified(
        Debt(ANA, BRUNO, 100L),
        Debt(BRUNO, CARLA, 100L),
    ).toSimplified(listOf(ANA, BRUNO, CARLA))

    private fun unsimplified(vararg debts: Debt): GroupBalances = GroupBalances(
        debts = debts.toList(),
        hasBills = true,
        unsimplifiedDebts = null,
    )

    private fun netOf(
        debts: List<Debt>,
        memberId: String,
    ): Long = debts.sumOf {
        when (memberId) {
            it.toMemberId -> it.amountMinor
            it.fromMemberId -> -it.amountMinor
            else -> 0L
        }
    }

    private fun group(
        simplifiesDebts: Boolean,
        memberIds: List<String> = listOf(ANA, BRUNO, CARLA, DIEGO),
    ): Group = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = memberIds.map(::member),
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
        isMe = id == PIERRE,
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
        id: String = "bill-$fromMemberId-$toMemberId",
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = "Conta",
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
    ): Settlement = Settlement(
        id = "payment-$fromMemberId-$toMemberId",
        groupId = GROUP_ID,
        fromMemberId = fromMemberId,
        toMemberId = toMemberId,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = 2_000L,
        isPending = false,
        updatedAt = 0L,
    )

    private companion object {
        const val GROUP_ID = "floripa"
        const val PIERRE = "pierre"
        const val ANA = "ana"
        const val BRUNO = "bruno"
        const val CARLA = "carla"
        const val DIEGO = "diego"
        const val ELISA = "elisa"
        const val ZOE = "zoe"

        // Why: ids of people no longer in the group, which sort before the members' by id.
        const val EX_MEMBER_A = "a-ex"
        const val EX_MEMBER_B = "b-ex"
    }
}
