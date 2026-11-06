package com.quare.blitzsplit.feature.editgroup

import com.quare.blitzsplit.core.expenses.domain.model.CurrencyBalances
import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyAmount
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyName
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.simplifyConfirmationOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SimplifyConfirmationsTest {
    @Test
    fun `GIVEN this device user joined last WHEN turning on THEN lists them first and the others in join order`() {
        // Given
        val group = group(members = listOf(ana, bruno, pierre))

        // When
        val confirmation = simplifyConfirmationOf(group, balancesOf(anaOwesBruno, brunoOwesPierre), emptyList())

        // Then
        assertEquals(listOf(PIERRE, ANA, BRUNO), confirmation.people.map { it.memberId })
    }

    @Test
    fun `GIVEN debts in two currencies WHEN turning on THEN counts the payments of each currency apart`() {
        // Given
        val group = group(members = listOf(pierre, ana, bruno))
        val raws = listOf(
            CurrencyBalances(GroupCurrency.BRL, rawOf(anaOwesBruno, brunoOwesPierre)),
            CurrencyBalances(GroupCurrency.USD, rawOf(anaOwesBruno)),
        )

        // When
        val confirmation = simplifyConfirmationOf(group, raws, emptyList())

        // Then
        assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), confirmation.currencies.map { it.currency })
        assertEquals(listOf(2, 1), confirmation.currencies.map { it.paymentsBefore })
        assertEquals(listOf(1, 1), confirmation.currencies.map { it.paymentsAfter })
        assertEquals(setOf(GroupCurrency.BRL), confirmation.people.map { it.currency }.toSet())
    }

    @Test
    fun `GIVEN a payment queued in another currency WHEN turning on THEN lists it with its own currency`() {
        // Given
        val group = group(members = listOf(pierre, ana, bruno))
        val queued = settlement(GroupCurrency.EUR)

        // When
        val confirmation = simplifyConfirmationOf(group, balancesOf(anaOwesBruno), listOf(queued))

        // Then
        assertEquals(listOf(GroupCurrency.EUR), confirmation.queuedPayments.map { it.currency })
    }

    @Test
    fun `GIVEN someone no longer in the group WHEN turning on THEN lists them last with no name and not as a guest`() {
        // Given
        val group = group(members = listOf(pierre, ana, bruno))
        val exMemberOwesAna = Debt(
            fromMemberId = EX_MEMBER,
            toMemberId = ANA,
            amountMinor = 1_000,
        )

        // When
        val confirmation = simplifyConfirmationOf(
            group,
            balancesOf(anaOwesBruno, brunoOwesPierre, exMemberOwesAna),
            emptyList(),
        )

        // Then
        val exMember = confirmation.people.last()
        assertEquals(EX_MEMBER, exMember.memberId)
        assertEquals(
            SimplifyName(
                name = null,
                isMe = false,
            ),
            exMember.person,
        )
        assertFalse(exMember.isGuest)
        assertEquals("", exMember.initials)
        assertEquals(AvatarColor.default, exMember.avatarColor)
        assertNull(exMember.photoUrl)
        assertEquals(
            listOf(
                SimplifyAmount(
                    to = SimplifyName(
                        name = "Pierre",
                        isMe = true,
                    ),
                    amountMinor = 1_000,
                ),
            ),
            exMember.pays,
        )
    }

    @Test
    fun `GIVEN nothing changes for this device user WHEN turning off THEN names nobody and counts everyone else`() {
        // Given
        val group = group(
            members = listOf(pierre, ana, bruno, carla),
            simplifiesDebts = true,
        )
        val brunoOwesCarla = Debt(
            fromMemberId = BRUNO,
            toMemberId = CARLA,
            amountMinor = 3_000,
        )

        // When
        val confirmation = simplifyConfirmationOf(group, balancesOf(anaOwesBruno, brunoOwesCarla), emptyList())

        // Then
        assertFalse(confirmation.isTurningOn)
        assertEquals(emptyList(), confirmation.people)
        assertEquals(3, confirmation.othersChangingCount)
        assertEquals(1, confirmation.currencies.single().paymentsBefore)
        assertEquals(2, confirmation.currencies.single().paymentsAfter)
    }

    private val anaOwesBruno = Debt(
        fromMemberId = ANA,
        toMemberId = BRUNO,
        amountMinor = 3_000,
    )

    private val brunoOwesPierre = Debt(
        fromMemberId = BRUNO,
        toMemberId = PIERRE,
        amountMinor = 3_000,
    )

    private val pierre = member(
        id = PIERRE,
        name = "Pierre",
        isMe = true,
        hasJoined = true,
    )

    private val ana = member(
        id = ANA,
        name = "Ana",
        isMe = false,
        hasJoined = false,
    )

    private val bruno = member(
        id = BRUNO,
        name = "Bruno",
        isMe = false,
        hasJoined = true,
    )

    private val carla = member(
        id = CARLA,
        name = "Carla",
        isMe = false,
        hasJoined = true,
    )

    private fun balancesOf(vararg debts: Debt): List<CurrencyBalances> = listOf(
        CurrencyBalances(GroupCurrency.BRL, rawOf(*debts)),
    )

    private fun rawOf(vararg debts: Debt): GroupBalances = GroupBalances(
        debts = debts.toList(),
        hasBills = true,
        unsimplifiedDebts = null,
    )

    private fun settlement(currency: GroupCurrency): Settlement = Settlement(
        id = "settlement-1",
        groupId = "group-1",
        fromMemberId = ANA,
        toMemberId = BRUNO,
        currency = currency,
        amountMinor = 1_000,
        method = SettlementMethod.PIX,
        paidAt = 2_000L,
        isPending = true,
        updatedAt = 2_000L,
    )

    private fun member(
        id: String,
        name: String,
        isMe: Boolean,
        hasJoined: Boolean,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = hasJoined,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun group(
        members: List<GroupMember>,
        simplifiesDebts: Boolean = false,
    ): Group = Group(
        id = "group-1",
        name = "Futebol de quinta",
        type = GroupType.OTHER,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = members,
        isPending = false,
        inviteCode = "AB12CD",
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = simplifiesDebts,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private companion object {
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val BRUNO = "member-3"
        const val CARLA = "member-4"
        const val EX_MEMBER = "member-9"
    }
}
