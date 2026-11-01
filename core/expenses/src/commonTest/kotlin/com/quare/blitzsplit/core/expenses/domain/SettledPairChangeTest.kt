package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairOutcome
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
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

// Why: Bruno paid R$ 300 split equally among Bruno, Elisa and Pierre (this device's user); Elisa then paid him back.
class SettledPairChangeTest {
    @Test
    fun `GIVEN Elisa settled with Bruno after the bill WHEN her part grows THEN she owes him the difference again`() {
        // Given
        val changed = jantar.withShares(BRUNO to 8_000L, ELISA to 12_000L, PIERRE to 10_000L)

        // When
        val change = SettledPairChange.findOrNull(jantar, changed, group, listOf(jantar), listOf(elisaPaidBruno))

        // Then
        assertEquals(
            SettledPairChange(
                payerName = "Elisa",
                payerIsMe = false,
                receiverName = "Bruno",
                receiverIsMe = false,
                outcome = SettledPairOutcome.OWES_AGAIN,
                debtorIsPayer = true,
                amountMinor = 2_000L,
            ),
            change,
        )
    }

    @Test
    fun `GIVEN Elisa settled with Bruno after the bill WHEN deleting it THEN Bruno comes to owe her the payment`() {
        // Given
        val expenses = listOf(jantar)

        // When
        val change = SettledPairChange.findOrNull(jantar, null, group, expenses, listOf(elisaPaidBruno))

        // Then
        assertEquals(SettledPairOutcome.OWES, change?.outcome)
        assertEquals("Bruno", change?.debtorName)
        assertEquals("Elisa", change?.creditorName)
        assertEquals(10_000L, change?.amountMinor)
    }

    @Test
    fun `GIVEN Elisa settled with Bruno WHEN the change only moves the part of Pierre THEN says nothing`() {
        // Given
        val changed = jantar.withShares(BRUNO to 8_000L, ELISA to 10_000L, PIERRE to 12_000L)

        // When
        val change = SettledPairChange.findOrNull(jantar, changed, group, listOf(jantar), listOf(elisaPaidBruno))

        // Then
        assertNull(change)
    }

    @Test
    fun `GIVEN a payment dated before the bill WHEN the bill changes THEN says nothing`() {
        // Given
        val earlier = elisaPaidBruno.copy(paidAt = BILL_AT - 1L)
        val changed = jantar.withShares(BRUNO to 8_000L, ELISA to 12_000L, PIERRE to 10_000L)

        // When
        val change = SettledPairChange.findOrNull(jantar, changed, group, listOf(jantar), listOf(earlier))

        // Then
        assertNull(change)
    }

    @Test
    fun `GIVEN a payment at the same time as the bill WHEN the bill changes THEN counts it as after the bill`() {
        // Given
        val sameTime = elisaPaidBruno.copy(paidAt = BILL_AT)
        val changed = jantar.withShares(BRUNO to 8_000L, ELISA to 12_000L, PIERRE to 10_000L)

        // When
        val change = SettledPairChange.findOrNull(jantar, changed, group, listOf(jantar), listOf(sameTime))

        // Then
        assertEquals(SettledPairOutcome.OWES_AGAIN, change?.outcome)
    }

    @Test
    fun `GIVEN nothing changed in the split WHEN comparing THEN says nothing`() {
        // Given
        val changed = jantar.copy(description = "Jantar no Ostradamus")

        // When
        val change = SettledPairChange.findOrNull(jantar, changed, group, listOf(jantar), listOf(elisaPaidBruno))

        // Then
        assertNull(change)
    }

    @Test
    fun `GIVEN Elisa paid half of her part WHEN her part becomes that half THEN they are even`() {
        // Given
        val half = elisaPaidBruno.copy(amountMinor = 5_000L)
        val changed = jantar.withShares(BRUNO to 15_000L, ELISA to 5_000L, PIERRE to 10_000L)

        // When
        val change = SettledPairChange.findOrNull(jantar, changed, group, listOf(jantar), listOf(half))

        // Then
        assertEquals(SettledPairOutcome.EVEN, change?.outcome)
        assertEquals(0L, change?.amountMinor)
        assertEquals("Elisa", change?.debtorName)
    }

    @Test
    fun `GIVEN Elisa paid half of her part WHEN her part grows THEN she owes more rather than again`() {
        // Given
        val half = elisaPaidBruno.copy(amountMinor = 5_000L)
        val changed = jantar.withShares(BRUNO to 8_000L, ELISA to 12_000L, PIERRE to 10_000L)

        // When
        val change = SettledPairChange.findOrNull(jantar, changed, group, listOf(jantar), listOf(half))

        // Then
        assertEquals(SettledPairOutcome.OWES, change?.outcome)
        assertEquals(true, change?.debtorIsPayer)
        assertEquals(7_000L, change?.amountMinor)
    }

    @Test
    fun `GIVEN Elisa paid half of her part WHEN her part shrinks THEN she is left owing less`() {
        // Given
        val half = elisaPaidBruno.copy(amountMinor = 5_000L)
        val changed = jantar.withShares(BRUNO to 12_000L, ELISA to 8_000L, PIERRE to 10_000L)

        // When
        val change = SettledPairChange.findOrNull(jantar, changed, group, listOf(jantar), listOf(half))

        // Then
        assertEquals(SettledPairOutcome.OWES_LESS, change?.outcome)
        assertEquals(true, change?.debtorIsPayer)
        assertEquals(3_000L, change?.amountMinor)
    }

    @Test
    fun `GIVEN Elisa paid more than her part WHEN her part grows THEN Bruno is left owing her less`() {
        // Given
        val more = elisaPaidBruno.copy(amountMinor = 15_000L)
        val changed = jantar.withShares(BRUNO to 8_000L, ELISA to 12_000L, PIERRE to 10_000L)

        // When
        val change = SettledPairChange.findOrNull(jantar, changed, group, listOf(jantar), listOf(more))

        // Then
        assertEquals(SettledPairOutcome.OWES_LESS, change?.outcome)
        assertEquals("Bruno", change?.debtorName)
        assertEquals(3_000L, change?.amountMinor)
    }

    @Test
    fun `GIVEN Elisa paid half of her part WHEN her part drops below it THEN Bruno comes to owe her`() {
        // Given
        val half = elisaPaidBruno.copy(amountMinor = 5_000L)
        val changed = jantar.withShares(BRUNO to 18_000L, ELISA to 2_000L, PIERRE to 10_000L)

        // When
        val change = SettledPairChange.findOrNull(jantar, changed, group, listOf(jantar), listOf(half))

        // Then
        assertEquals(SettledPairOutcome.OWES, change?.outcome)
        assertEquals("Bruno", change?.debtorName)
        assertEquals(3_000L, change?.amountMinor)
    }

    @Test
    fun `GIVEN a payment after the bill WHEN Quando moves past it THEN says nothing`() {
        // Given
        val changed = jantar
            .withShares(BRUNO to 8_000L, ELISA to 12_000L, PIERRE to 10_000L)
            .copy(spentAt = PAID_AT + 1L)

        // When
        val change = SettledPairChange.findOrNull(jantar, changed, group, listOf(jantar), listOf(elisaPaidBruno))

        // Then
        assertNull(change)
    }

    @Test
    fun `GIVEN a payment dated before the bill WHEN Quando moves before it THEN warns of it`() {
        // Given
        val earlier = elisaPaidBruno.copy(paidAt = BILL_AT - 1L)
        val changed = jantar
            .withShares(BRUNO to 8_000L, ELISA to 12_000L, PIERRE to 10_000L)
            .copy(spentAt = BILL_AT - 2L)

        // When
        val change = SettledPairChange.findOrNull(jantar, changed, group, listOf(jantar), listOf(earlier))

        // Then
        assertEquals(SettledPairOutcome.OWES_AGAIN, change?.outcome)
    }

    @Test
    fun `GIVEN two changes that differ only in the amount WHEN comparing their announcement keys THEN they match`() {
        // Given
        val first = settledPairChange(SettledPairOutcome.OWES_AGAIN, amountMinor = 2_000L)
        val second = settledPairChange(SettledPairOutcome.OWES_AGAIN, amountMinor = 3_000L)

        // When
        val matches = first.announcementKey == second.announcementKey

        // Then
        assertTrue(matches)
    }

    @Test
    fun `GIVEN two changes with another outcome WHEN comparing their announcement keys THEN they differ`() {
        // Given
        val first = settledPairChange(SettledPairOutcome.OWES_AGAIN, amountMinor = 2_000L)
        val second = settledPairChange(SettledPairOutcome.EVEN, amountMinor = 2_000L)

        // When
        val matches = first.announcementKey == second.announcementKey

        // Then
        assertFalse(matches)
    }

    @Test
    fun `GIVEN two pairs settled after the bill WHEN both change THEN names the latest payment which is mine`() {
        // Given
        val iPaidBruno = elisaPaidBruno.copy(
            id = "settlement-2",
            fromMemberId = PIERRE,
            paidAt = PAID_AT + 1L,
        )
        val changed = jantar.withShares(BRUNO to 6_000L, ELISA to 12_000L, PIERRE to 12_000L)

        // When
        val change = SettledPairChange.findOrNull(
            jantar,
            changed,
            group,
            listOf(jantar),
            listOf(iPaidBruno, elisaPaidBruno),
        )

        // Then
        assertEquals(true, change?.payerIsMe)
        assertEquals(true, change?.debtorIsMe)
        assertEquals("Bruno", change?.creditorName)
        assertEquals(2_000L, change?.amountMinor)
    }

    @Test
    fun `GIVEN another bill between the pair WHEN this one changes THEN counts the whole balance of the pair`() {
        // Given
        val cafe = bill(
            id = "cafe",
            payerId = ELISA,
            shares = listOf(share(ELISA, 1_000L), share(BRUNO, 1_000L)),
        )
        val changed = jantar.withShares(BRUNO to 8_000L, ELISA to 12_000L, PIERRE to 10_000L)
        val payment = elisaPaidBruno.copy(amountMinor = 9_000L)

        // When
        val change = SettledPairChange.findOrNull(jantar, changed, group, listOf(cafe, jantar), listOf(payment))

        // Then
        assertEquals(SettledPairOutcome.OWES_AGAIN, change?.outcome)
        assertEquals(2_000L, change?.amountMinor)
    }

    @Test
    fun `GIVEN the payer left the group WHEN the bill changes THEN names them as nobody`() {
        // Given
        val withoutElisa = group.copy(members = group.members.filter { it.id != ELISA })
        val changed = jantar.withShares(BRUNO to 8_000L, ELISA to 12_000L, PIERRE to 10_000L)

        // When
        val change = SettledPairChange.findOrNull(
            jantar,
            changed,
            withoutElisa,
            listOf(jantar),
            listOf(elisaPaidBruno),
        )

        // Then
        assertNull(change?.payerName)
        assertEquals("Bruno", change?.receiverName)
    }

    @Test
    fun `GIVEN the bill on the device WHEN finding what deleting it unsettles THEN compares without it`() {
        // Given
        val expenses = listOf(jantar)

        // When
        val change = SettledPairChange.findDeletionOrNull(JANTAR, group, expenses, listOf(elisaPaidBruno))

        // Then
        assertEquals(SettledPairOutcome.OWES, change?.outcome)
        assertEquals(false, change?.debtorIsPayer)
    }

    @Test
    fun `GIVEN the bill is not on the device WHEN finding what deleting it unsettles THEN finds nothing`() {
        // Given
        val expenses = emptyList<Expense>()

        // When
        val change = SettledPairChange.findDeletionOrNull(JANTAR, group, expenses, listOf(elisaPaidBruno))

        // Then
        assertNull(change)
    }

    @Test
    fun `GIVEN the group is not read yet WHEN finding what deleting the bill unsettles THEN finds nothing`() {
        // Given
        val noGroup: Group? = null

        // When
        val change = SettledPairChange.findDeletionOrNull(JANTAR, noGroup, listOf(jantar), listOf(elisaPaidBruno))

        // Then
        assertNull(change)
    }

    private val group = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        currency = GroupCurrency.BRL,
        members = listOf(
            member(PIERRE, "Pierre", isMe = true),
            member(BRUNO, "Bruno"),
            member(ELISA, "Elisa"),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    private val jantar = bill(
        id = JANTAR,
        payerId = BRUNO,
        shares = listOf(share(BRUNO, 10_000L), share(ELISA, 10_000L), share(PIERRE, 10_000L)),
    )

    private val elisaPaidBruno = Settlement(
        id = "settlement-1",
        groupId = GROUP_ID,
        fromMemberId = ELISA,
        toMemberId = BRUNO,
        amountMinor = 10_000L,
        method = SettlementMethod.PIX,
        paidAt = PAID_AT,
        isPending = false,
        updatedAt = PAID_AT,
    )

    private fun settledPairChange(
        outcome: SettledPairOutcome,
        amountMinor: Long,
    ): SettledPairChange = SettledPairChange(
        payerName = "Elisa",
        payerIsMe = false,
        receiverName = "Bruno",
        receiverIsMe = false,
        outcome = outcome,
        debtorIsPayer = true,
        amountMinor = amountMinor,
    )

    private fun Expense.withShares(vararg parts: Pair<String, Long>): Expense =
        copy(shares = parts.map { (memberId, amountMinor) -> share(memberId, amountMinor) })

    private fun bill(
        id: String,
        payerId: String,
        shares: List<ExpenseShare>,
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = "Jantar",
        amountMinor = shares.sumOf { it.amountMinor },
        splitMode = SplitMode.AMOUNT,
        payers = listOf(ExpensePayer(payerId, shares.sumOf { it.amountMinor })),
        shares = shares,
        items = null,
        spentAt = BILL_AT,
        isPending = false,
        updatedAt = BILL_AT,
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

    private fun member(
        id: String,
        name: String,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.PEACH,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
    )

    private companion object {
        const val GROUP_ID = "group-1"
        const val JANTAR = "jantar"
        const val PIERRE = "pierre"
        const val BRUNO = "bruno"
        const val ELISA = "elisa"
        const val BILL_AT = 1_000L
        const val PAID_AT = 2_000L
    }
}
