package com.quare.blitzsplit.core.expenses.data

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryRecordedPaymentRepositoryTest {
    private val repository = InMemoryRecordedPaymentRepository()

    @Test
    fun `GIVEN no payment recorded WHEN observing the notice THEN no screen owes one`() = runTest {
        // Given
        val recorded = repository.observeRecorded()

        // When
        val settlement = recorded.first()

        // Then
        assertNull(settlement)
    }

    @Test
    fun `GIVEN no notice WHEN posting a payment THEN it is owed the notice`() = runTest {
        // Given
        val settlement = payment(PAYMENT_ID)

        // When
        repository.post(settlement)

        // Then
        assertEquals(settlement, repository.observeRecorded().first())
    }

    @Test
    fun `GIVEN a payment owed the notice WHEN posting another THEN only the newer one is owed it`() = runTest {
        // Given
        repository.post(payment(PAYMENT_ID))

        // When
        repository.post(payment(OTHER_PAYMENT_ID))

        // Then
        assertEquals(OTHER_PAYMENT_ID, repository.observeRecorded().first()?.id)
    }

    @Test
    fun `GIVEN a payment owed the notice WHEN consuming it THEN no screen owes one`() = runTest {
        // Given
        repository.post(payment(PAYMENT_ID))

        // When
        repository.consume(PAYMENT_ID)

        // Then
        assertNull(repository.observeRecorded().first())
    }

    @Test
    fun `GIVEN a payment owed the notice WHEN consuming another payment THEN the notice keeps waiting`() = runTest {
        // Given
        repository.post(payment(PAYMENT_ID))

        // When
        repository.consume(OTHER_PAYMENT_ID)

        // Then
        assertEquals(PAYMENT_ID, repository.observeRecorded().first()?.id)
    }

    private fun payment(id: String): Settlement = Settlement(
        id = id,
        groupId = "group-1",
        fromMemberId = "ana",
        toMemberId = "bruno",
        amountMinor = 5_000L,
        method = SettlementMethod.PIX,
        paidAt = 1_000L,
        isPending = true,
        updatedAt = 0L,
    )

    private companion object {
        const val PAYMENT_ID = "payment-1"
        const val OTHER_PAYMENT_ID = "payment-2"
    }
}
