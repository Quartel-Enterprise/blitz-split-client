package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSettlementUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteSettlementUseCaseTest {
    private lateinit var repository: FakeSettlementRepository
    private lateinit var recordedPayments: InMemoryRecordedPaymentRepository
    private lateinit var useCase: DeleteSettlementUseCase

    @Test
    fun `GIVEN a payment WHEN undoing it THEN deletes it at the current time and it leaves the group`() = runTest {
        // Given
        prepareScenario(now = 9_000L)

        // When
        val result = useCase(listOf(PAYMENT_ID))

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(PAYMENT_ID), repository.deletedIds)
        assertEquals(listOf(9_000L), repository.deletedAts)
        assertTrue(repository.observeSettlements(GROUP_ID).first().isEmpty())
    }

    @Test
    fun `GIVEN the payments of a settling up WHEN undoing them THEN deletes them all at once`() = runTest {
        // Given
        prepareScenario(stored = listOf(payment(PAYMENT_ID), payment(OTHER_PAYMENT_ID)))

        // When
        val result = useCase(listOf(PAYMENT_ID, OTHER_PAYMENT_ID))

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(PAYMENT_ID, OTHER_PAYMENT_ID), repository.deletedIds)
        assertTrue(repository.settlements.value.isEmpty())
    }

    @Test
    fun `GIVEN a payment owed its notice WHEN undoing it THEN no screen owes the notice anymore`() = runTest {
        // Given
        prepareScenario(noticed = payment(PAYMENT_ID))

        // When
        useCase(listOf(PAYMENT_ID))

        // Then
        assertNull(recordedPayments.observeRecorded().first())
    }

    @Test
    fun `GIVEN another payment owed its notice WHEN undoing this one THEN the other notice keeps waiting`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(payment(PAYMENT_ID), payment(OTHER_PAYMENT_ID)),
            noticed = payment(OTHER_PAYMENT_ID),
        )

        // When
        useCase(listOf(PAYMENT_ID))

        // Then
        assertEquals(OTHER_PAYMENT_ID, recordedPayments.observeRecorded().first()?.id)
    }

    @Test
    fun `GIVEN a payment already gone WHEN undoing it THEN succeeds and its notice is dropped`() = runTest {
        // Given
        prepareScenario(
            stored = emptyList(),
            noticed = payment(PAYMENT_ID),
        )

        // When
        val result = useCase(listOf(PAYMENT_ID))

        // Then
        assertTrue(result.isSuccess)
        assertNull(recordedPayments.observeRecorded().first())
    }

    @Test
    fun `GIVEN the device fails to save WHEN undoing a payment THEN fails and the payment stays`() = runTest {
        // Given
        prepareScenario(
            noticed = payment(PAYMENT_ID),
            deleteError = IllegalStateException("disk full"),
        )

        // When
        val result = useCase(listOf(PAYMENT_ID))

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(listOf(PAYMENT_ID), repository.settlements.value.map { it.id })
        assertEquals(PAYMENT_ID, recordedPayments.observeRecorded().first()?.id)
    }

    private fun payment(id: String): Settlement = Settlement(
        id = id,
        groupId = GROUP_ID,
        fromMemberId = "ana",
        toMemberId = "bruno",
        amountMinor = 5_000L,
        method = SettlementMethod.PIX,
        paidAt = 1_000L,
        isPending = true,
        updatedAt = 0L,
    )

    private fun prepareScenario(
        now: Long = 1_000L,
        stored: List<Settlement> = listOf(payment(PAYMENT_ID)),
        noticed: Settlement? = null,
        deleteError: Throwable? = null,
    ) {
        repository = FakeSettlementRepository(initialSettlements = stored).also {
            it.deleteError = deleteError
        }
        recordedPayments = InMemoryRecordedPaymentRepository().also { notices ->
            noticed?.let(notices::post)
        }
        useCase = DeleteSettlementUseCase(
            repository = repository,
            recordedPayments = recordedPayments,
            currentTimeMillis = { now },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val PAYMENT_ID = "payment-1"
        const val OTHER_PAYMENT_ID = "payment-2"
    }
}
