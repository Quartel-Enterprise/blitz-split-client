package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.DebtKind
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.debtRowOf
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

class DebtRowTest {
    @Test
    fun `GIVEN a pair the simplification made WHEN drawing its row THEN it is marked simplified`() {
        // Given
        val balances = chain().toSimplified(memberOrder)
        val debt = balances.debts.single()

        // When
        val row = debtRowOf(
            debt,
            group,
            meId = CARLA,
            balances = balances,
        )

        // Then
        assertEquals(Debt(ANA, CARLA, 100L), debt)
        assertTrue(row.isSimplified)
        assertEquals(DebtKind.OWES_YOU, row.kind)
        assertEquals("Ana", row.fromName)
    }

    @Test
    fun `GIVEN a pair a bill made in a simplified group WHEN drawing its row THEN it is not marked simplified`() {
        // Given
        val balances = raw(Debt(ANA, DIEGO, 50L), Debt(BRUNO, CARLA, 50L)).toSimplified(memberOrder)
        val debt = Debt(ANA, DIEGO, 50L)

        // When
        val row = debtRowOf(
            debt,
            group,
            meId = PIERRE,
            balances = balances,
        )

        // Then
        assertFalse(row.isSimplified)
    }

    @Test
    fun `GIVEN simplifying off WHEN drawing a row THEN it is not marked simplified`() {
        // Given
        val balances = chain()
        val debt = Debt(ANA, BRUNO, 100L)

        // When
        val row = debtRowOf(
            debt,
            group,
            meId = PIERRE,
            balances = balances,
        )

        // Then
        assertFalse(row.isSimplified)
    }

    private val group = Group(
        id = "floripa",
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(
            member(PIERRE, "Pierre"),
            member(ANA, "Ana"),
            member(BRUNO, "Bruno"),
            member(CARLA, "Carla"),
            member(DIEGO, "Diego"),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = true,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    // Why: Ana owes Bruno and Bruno owes Carla the same, which simplifying turns into Ana paying Carla.
    private fun chain(): GroupBalances = raw(Debt(ANA, BRUNO, 100L), Debt(BRUNO, CARLA, 100L))

    private fun raw(vararg debts: Debt): GroupBalances = GroupBalances(
        debts = debts.toList(),
        hasBills = true,
        unsimplifiedDebts = null,
    )

    private fun member(
        id: String,
        name: String,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = id == PIERRE,
        hasJoined = true,
        userId = id,
        inviteCode = null,
        photoUrl = null,
    )

    private val memberOrder = listOf(PIERRE, ANA, BRUNO, CARLA, DIEGO)

    private companion object {
        const val PIERRE = "pierre"
        const val ANA = "ana"
        const val BRUNO = "bruno"
        const val CARLA = "carla"
        const val DIEGO = "diego"
    }
}
