package com.quare.blitzsplit.core.expenses.data

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryPickedReceiptRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.HoldPickedReceiptUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.TakePickedReceiptUseCase
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class InMemoryPickedReceiptRepositoryTest {
    private val repository = InMemoryPickedReceiptRepository()
    private val hold = HoldPickedReceiptUseCase(repository)
    private val take = TakePickedReceiptUseCase(repository)

    @Test
    fun `GIVEN a photo Inicio held WHEN Rachar conta takes it twice THEN gets it once and nothing after`() {
        // Given
        hold(byteArrayOf(1, 2, 3))

        // When
        val first = take()
        val second = take()

        // Then
        assertContentEquals(byteArrayOf(1, 2, 3), first)
        assertNull(second)
    }

    @Test
    fun `GIVEN a newer photo held over an older one WHEN taking it THEN gets the newer one`() {
        // Given
        hold(byteArrayOf(1))
        hold(byteArrayOf(2))

        // When
        val photo = take()

        // Then
        assertContentEquals(byteArrayOf(2), photo)
    }
}
