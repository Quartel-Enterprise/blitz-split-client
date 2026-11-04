package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.toSaturatedProduct
import com.quare.blitzsplit.core.expenses.domain.model.toSaturatedSum
import kotlin.test.Test
import kotlin.test.assertEquals

class SaturatingLongTest {
    @Test
    fun `GIVEN 999 units at the bitcoin cap in sats WHEN multiplying THEN it is exact`() {
        // Given
        val quantity = 999L
        val bitcoinCapInSats = 2_100_000_000_000_000L

        // When
        val product = quantity.toSaturatedProduct(bitcoinCapInSats)

        // Then
        assertEquals(2_097_900_000_000_000_000L, product)
    }

    @Test
    fun `GIVEN zero on either side WHEN multiplying by the largest Long THEN it is zero`() {
        // Given
        val zero = 0L

        // When
        val zeroFirst = zero.toSaturatedProduct(Long.MAX_VALUE)
        val zeroLast = Long.MAX_VALUE.toSaturatedProduct(zero)

        // Then
        assertEquals(0L, zeroFirst)
        assertEquals(0L, zeroLast)
    }

    @Test
    fun `GIVEN 999 units at a price of 16 digits WHEN multiplying THEN holds at the largest Long`() {
        // Given
        val quantity = 999L
        val price = 9_999_999_999_999_999L

        // When
        val product = quantity.toSaturatedProduct(price)

        // Then
        assertEquals(Long.MAX_VALUE, product)
    }

    @Test
    fun `GIVEN half the largest Long WHEN doubling it THEN it is exact just under the largest Long`() {
        // Given
        val half = Long.MAX_VALUE / 2

        // When
        val product = half.toSaturatedProduct(2)

        // Then
        assertEquals(Long.MAX_VALUE - 1, product)
    }

    @Test
    fun `GIVEN one over half the largest Long WHEN doubling it THEN holds at the largest Long`() {
        // Given
        val overHalf = Long.MAX_VALUE / 2 + 1

        // When
        val product = overHalf.toSaturatedProduct(2)

        // Then
        assertEquals(Long.MAX_VALUE, product)
    }

    @Test
    fun `GIVEN a negative amount WHEN multiplying THEN it is exact`() {
        // Given
        val difference = -300L

        // When
        val product = difference.toSaturatedProduct(3)

        // Then
        assertEquals(-900L, product)
    }

    @Test
    fun `GIVEN two amounts whose sum fits WHEN adding THEN it is exact`() {
        // Given
        val first = 4_995_000_000_000_000_000L
        val second = 4_000_000_000_000_000_000L

        // When
        val sum = first.toSaturatedSum(second)

        // Then
        assertEquals(8_995_000_000_000_000_000L, sum)
    }

    @Test
    fun `GIVEN one under the largest Long WHEN adding one THEN it is exactly the largest Long`() {
        // Given
        val almost = Long.MAX_VALUE - 1

        // When
        val sum = almost.toSaturatedSum(1)

        // Then
        assertEquals(Long.MAX_VALUE, sum)
    }

    @Test
    fun `GIVEN two items of 999 at the ether cap WHEN adding them THEN holds at the largest Long`() {
        // Given
        val itemTotal = 8_991_000_000_000_000_000L

        // When
        val sum = itemTotal.toSaturatedSum(itemTotal)

        // Then
        assertEquals(Long.MAX_VALUE, sum)
    }

    @Test
    fun `GIVEN the largest Long WHEN adding one more THEN holds at the largest Long`() {
        // Given
        val largest = Long.MAX_VALUE

        // When
        val sum = largest.toSaturatedSum(1)

        // Then
        assertEquals(Long.MAX_VALUE, sum)
    }

    @Test
    fun `GIVEN the largest Long WHEN adding a negative amount THEN it is exact`() {
        // Given
        val largest = Long.MAX_VALUE

        // When
        val sum = largest.toSaturatedSum(-1)

        // Then
        assertEquals(Long.MAX_VALUE - 1, sum)
    }
}
