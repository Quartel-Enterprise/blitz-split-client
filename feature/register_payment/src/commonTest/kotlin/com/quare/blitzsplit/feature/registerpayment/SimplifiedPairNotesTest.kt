package com.quare.blitzsplit.feature.registerpayment

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.registerpayment.presentation.model.NotePerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.SimplifiedPairNote
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.simplifiedPairNoteOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SimplifiedPairNotesTest {
    @Test
    fun `GIVEN the switch off WHEN reading the note of a pair THEN there is none`() {
        // Given
        val group = group(simplifiesDebts = false)
        val balances = GroupBalances.of(group, grownBills, emptyList())

        // When
        val note = simplifiedPairNoteOrNull(pair(DIEGO, BRUNO), group, balances)

        // Then
        assertNull(note)
    }

    @Test
    fun `GIVEN the switch on and a pair no bill made WHEN reading its note THEN it is new naming the old creditor`() {
        // Given
        val group = group()
        val balances = GroupBalances.of(group, simplifiedBills, emptyList())

        // When
        val note = simplifiedPairNoteOrNull(pair(DIEGO, PIERRE), group, balances)

        // Then
        assertEquals(
            SimplifiedPairNote(
                isNewPair = true,
                insteadOf = listOf(
                    NotePerson(
                        name = "Bruno",
                        isMe = false,
                    ),
                ),
            ),
            note,
        )
    }

    @Test
    fun `GIVEN the switch on and a pair as the bills made it WHEN reading its note THEN there is none`() {
        // Given
        val group = group()
        val balances = GroupBalances.of(group, simplifiedBills, emptyList())

        // When
        val note = simplifiedPairNoteOrNull(pair(ANA, PIERRE), group, balances)

        // Then
        assertNull(note)
    }

    @Test
    fun `GIVEN a pair that grew WHEN reading its note THEN names each creditor owed less the largest first`() {
        // Given
        val group = group()
        val bills = grownBills + listOf(
            bill(
                "bar",
                payer = ANA,
                debtor = DIEGO,
                2_000L,
            ),
            bill(
                "ferry",
                payer = BRUNO,
                debtor = ANA,
                2_000L,
            ),
        )
        val balances = GroupBalances.of(group, bills, emptyList())

        // When
        val note = simplifiedPairNoteOrNull(pair(DIEGO, BRUNO), group, balances)

        // Then
        assertEquals(
            SimplifiedPairNote(
                isNewPair = false,
                insteadOf = listOf(
                    NotePerson(
                        name = "Pierre Vieira",
                        isMe = true,
                    ),
                    NotePerson(
                        name = "Ana",
                        isMe = false,
                    ),
                ),
            ),
            note,
        )
    }

    @Test
    fun `GIVEN a creditor no longer in the group WHEN reading the note THEN names them as nobody`() {
        // Given
        val group = group(members = listOf(pierre, bruno, diego))
        val bills = listOf(
            bill(
                "hotel",
                payer = BRUNO,
                debtor = DIEGO,
                5_000L,
            ),
            bill(
                "dinner",
                payer = ANA,
                debtor = DIEGO,
                3_000L,
            ),
            bill(
                "taxi",
                payer = BRUNO,
                debtor = ANA,
                3_000L,
            ),
        )
        val balances = GroupBalances.of(group, bills, emptyList())

        // When
        val note = simplifiedPairNoteOrNull(pair(DIEGO, BRUNO), group, balances)

        // Then
        assertEquals(
            SimplifiedPairNote(
                isNewPair = false,
                insteadOf = listOf(
                    NotePerson(
                        name = null,
                        isMe = false,
                    ),
                ),
            ),
            note,
        )
    }

    @Test
    fun `GIVEN the switch on and a pair the simplification only shrank WHEN reading its note THEN there is none`() {
        // Given
        val group = group()
        // Why: Ana owes Bruno 100,00, Bruno owes Diego 30,00 and Diego owes Ana 30,00; simplified, Ana pays Bruno 70.
        val bills = listOf(
            bill(
                "hotel",
                payer = BRUNO,
                debtor = ANA,
                10_000L,
            ),
            bill(
                "dinner",
                payer = DIEGO,
                debtor = BRUNO,
                3_000L,
            ),
            bill(
                "taxi",
                payer = ANA,
                debtor = DIEGO,
                3_000L,
            ),
        )
        val balances = GroupBalances.of(group, bills, emptyList())

        // When
        val note = simplifiedPairNoteOrNull(pair(ANA, BRUNO), group, balances)

        // Then
        assertEquals(7_000L, balances.debtOf(ANA, BRUNO))
        assertNull(note)
    }

    private val ana = member(ANA, "Ana")
    private val pierre = member(PIERRE, "Pierre Vieira", isMe = true)
    private val bruno = member(BRUNO, "Bruno")
    private val diego = member(DIEGO, "Diego")

    // Why: Diego owes Bruno 50,00 and you 30,00, and you owe Bruno 30,00; simplified, Diego pays Bruno 80,00.
    private val grownBills = listOf(
        bill(
            "hotel",
            payer = BRUNO,
            debtor = DIEGO,
            5_000L,
        ),
        bill(
            "dinner",
            payer = PIERRE,
            debtor = DIEGO,
            3_000L,
        ),
        bill(
            "taxi",
            payer = BRUNO,
            debtor = PIERRE,
            3_000L,
        ),
    )

    // Why: Ana owes you 100,00, Diego owes Bruno 80,00 and Bruno owes you 80,00; simplified, Diego pays you instead.
    private val simplifiedBills = listOf(
        bill(
            "market",
            payer = PIERRE,
            debtor = ANA,
            10_000L,
        ),
        bill(
            "hotel",
            payer = BRUNO,
            debtor = DIEGO,
            8_000L,
        ),
        bill(
            "boat",
            payer = PIERRE,
            debtor = BRUNO,
            8_000L,
        ),
    )

    private fun pair(
        from: String,
        to: String,
    ): Route.RegisterPayment = Route.RegisterPayment(
        GROUP_ID,
        fromMemberId = from,
        toMemberId = to,
        offersReceiverKeys = true,
    )

    private fun member(
        id: String,
        name: String,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun group(
        simplifiesDebts: Boolean = true,
        members: List<GroupMember> = listOf(ana, pierre, bruno, diego),
    ): Group = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = members,
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

    // Why: a bill the payer paid whole for the debtor alone, so the debtor owes the payer all of it.
    private fun bill(
        id: String,
        payer: String,
        debtor: String,
        amountMinor: Long,
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = id,
        amountMinor = amountMinor,
        splitMode = SplitMode.AMOUNT,
        payers = listOf(ExpensePayer(payer, amountMinor)),
        shares = listOf(ExpenseShare(debtor, amountMinor, weight = amountMinor)),
        items = null,
        categoryId = null,
        spentAt = 0L,
        isPending = false,
        updatedAt = 0L,
        updatedByMemberId = null,
    )

    private companion object {
        const val GROUP_ID = "group-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val DIEGO = "member-4"
    }
}
