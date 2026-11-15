package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BalanceDetail
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MemberBalanceTest {
    @Test
    fun `GIVEN a member who receives more than they owe WHEN reading the net THEN it is positive`() {
        // Given
        val balance = balance(
            receivesMinor = 33_290L,
            owesMinor = 11_800L,
        )

        // When
        val net = balance.netMinor

        // Then
        assertEquals(21_490L, net)
    }

    @Test
    fun `GIVEN a member who owes more than they receive WHEN reading the net THEN it is negative`() {
        // Given
        val balance = balance(
            receivesMinor = 0L,
            owesMinor = 11_800L,
        )

        // When
        val net = balance.netMinor

        // Then
        assertEquals(-11_800L, net)
    }

    @Test
    fun `GIVEN a member who neither receives nor owes WHEN checking it THEN they are settled`() {
        // Given
        val balance = balance(
            receivesMinor = 0L,
            owesMinor = 0L,
        )

        // When
        val settled = balance.isSettled

        // Then
        assertTrue(settled)
    }

    @Test
    fun `GIVEN a member who receives and owes the same WHEN checking it THEN they are not settled`() {
        // Given
        val balance = balance(
            receivesMinor = 500L,
            owesMinor = 500L,
        )

        // When
        val settled = balance.isSettled

        // Then
        assertFalse(settled)
        assertEquals(0L, balance.netMinor)
    }

    @Test
    fun `GIVEN a member who only receives WHEN checking it THEN they are not settled and cannot settle`() {
        // Given
        val balance = balance(
            receivesMinor = 500L,
            owesMinor = 0L,
        )

        // When
        val canSettle = balance.canSettle

        // Then
        assertFalse(canSettle)
        assertFalse(balance.isSettled)
    }

    @Test
    fun `GIVEN a member who owes WHEN checking it THEN they can settle`() {
        // Given
        val balance = balance(
            receivesMinor = 900L,
            owesMinor = 1L,
        )

        // When
        val canSettle = balance.canSettle

        // Then
        assertTrue(canSettle)
    }

    @Test
    fun `GIVEN a member who receives and owes WHEN reading the detail THEN it is the breakdown`() {
        // Given
        val balance = balance(
            receivesMinor = 33_290L,
            owesMinor = 11_800L,
        )

        // When
        val detail = balance.detailOf(isEveryoneSettled = false)

        // Then
        assertEquals(BalanceDetail.BREAKDOWN, detail)
    }

    @Test
    fun `GIVEN a member who receives and owes the same WHEN reading the detail THEN the breakdown explains the zero`() {
        // Given
        val balance = balance(
            receivesMinor = 500L,
            owesMinor = 500L,
        )

        // When
        val detail = balance.detailOf(isEveryoneSettled = false)

        // Then
        assertEquals(BalanceDetail.BREAKDOWN, detail)
    }

    @Test
    fun `GIVEN a member who only receives WHEN reading the detail THEN there is none`() {
        // Given
        val balance = balance(
            receivesMinor = 31_381L,
            owesMinor = 0L,
        )

        // When
        val detail = balance.detailOf(isEveryoneSettled = false)

        // Then
        assertEquals(BalanceDetail.NONE, detail)
    }

    @Test
    fun `GIVEN a member who only owes WHEN reading the detail THEN there is none`() {
        // Given
        val balance = balance(
            receivesMinor = 0L,
            owesMinor = 11_800L,
        )

        // When
        val detail = balance.detailOf(isEveryoneSettled = false)

        // Then
        assertEquals(BalanceDetail.NONE, detail)
    }

    @Test
    fun `GIVEN a settled member while others still owe WHEN reading the detail THEN others are settling`() {
        // Given
        val balance = balance(
            receivesMinor = 0L,
            owesMinor = 0L,
        )

        // When
        val detail = balance.detailOf(isEveryoneSettled = false)

        // Then
        assertEquals(BalanceDetail.OTHERS_SETTLING, detail)
    }

    @Test
    fun `GIVEN a settled member in a settled group WHEN reading the detail THEN everyone is settled`() {
        // Given
        val balance = balance(
            receivesMinor = 0L,
            owesMinor = 0L,
        )

        // When
        val detail = balance.detailOf(isEveryoneSettled = true)

        // Then
        assertEquals(BalanceDetail.EVERYONE_SETTLED, detail)
    }

    private fun balance(
        receivesMinor: Long,
        owesMinor: Long,
    ): MemberBalance = MemberBalance(
        receivesMinor = receivesMinor,
        owesMinor = owesMinor,
    )
}
