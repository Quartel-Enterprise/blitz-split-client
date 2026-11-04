package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.DebtSimplificationSummary
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DebtSimplificationSummaryTest {
    @Test
    fun `GIVEN simplifying off WHEN summing up THEN there is no summary`() {
        // Given
        val balances = raw(Debt(ANA, BRUNO, 100L), Debt(BRUNO, CARLA, 100L))

        // When
        val summary = DebtSimplificationSummary.ofOrNull(balances)

        // Then
        assertNull(summary)
    }

    @Test
    fun `GIVEN simplifying saves payments WHEN summing up THEN counts the payments now and without it`() {
        // Given
        val balances = raw(Debt(ANA, BRUNO, 100L), Debt(BRUNO, CARLA, 100L)).toSimplified(listOf(ANA, BRUNO, CARLA))

        // When
        val summary = DebtSimplificationSummary.ofOrNull(balances)

        // Then
        assertEquals(
            DebtSimplificationSummary(
                paymentCount = 1,
                unsimplifiedPaymentCount = 2,
            ),
            summary,
        )
        assertEquals(false, summary?.isAlreadyMinimal)
    }

    @Test
    fun `GIVEN pairs already at the fewest payments WHEN summing up THEN says it is already minimal`() {
        // Given
        val balances = raw(Debt(ANA, BRUNO, 100L)).toSimplified(listOf(ANA, BRUNO))

        // When
        val summary = DebtSimplificationSummary.ofOrNull(balances)

        // Then
        assertEquals(
            DebtSimplificationSummary(
                paymentCount = 1,
                unsimplifiedPaymentCount = 1,
            ),
            summary,
        )
        assertEquals(true, summary?.isAlreadyMinimal)
    }

    private fun raw(vararg debts: Debt): GroupBalances = GroupBalances(
        debts = debts.toList(),
        hasBills = true,
        unsimplifiedDebts = null,
    )

    private companion object {
        const val ANA = "ana"
        const val BRUNO = "bruno"
        const val CARLA = "carla"
    }
}
