package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RecordSettlementUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordSettlementUseCaseTest {
    private lateinit var repository: FakeSettlementRepository
    private lateinit var recordedPayments: InMemoryRecordedPaymentRepository
    private lateinit var useCase: RecordSettlementUseCase

    @Test
    fun `GIVEN a debt WHEN recording a payment THEN saves it pending at the current time and returns its id`() =
        runTest {
            // Given
            prepareScenario(now = 9_000L)

            // When
            val result = recordAnaPaysBruno()

            // Then
            val saved = repository.recordedSettlements.single()
            assertEquals(saved.id, result.getOrNull())
            assertEquals(
                Settlement(
                    id = saved.id,
                    groupId = GROUP_ID,
                    fromMemberId = ANA,
                    toMemberId = BRUNO,
                    amountMinor = 5_000L,
                    method = SettlementMethod.PIX,
                    paidAt = 3_000L,
                    isPending = true,
                    updatedAt = 9_000L,
                ),
                saved,
            )
            assertEquals(listOf(9_000L), repository.recordedAts)
        }

    @Test
    fun `GIVEN a payment already recorded WHEN recording another THEN it gets a new id`() = runTest {
        // Given
        prepareScenario()
        val firstId = recordAnaPaysBruno().getOrThrow()

        // When
        val secondId = recordAnaPaysBruno().getOrThrow()

        // Then
        assertNotEquals(firstId, secondId)
        assertTrue(secondId.isNotBlank())
    }

    @Test
    fun `GIVEN a debt WHEN recording a payment THEN the group screen is owed its notice`() = runTest {
        // Given
        prepareScenario()

        // When
        val id = recordAnaPaysBruno().getOrThrow()

        // Then
        val notice = recordedPayments.observeRecorded().first()
        assertEquals(listOf(id), notice?.settlementIds)
        assertEquals(RecordedPayment.InGroup(repository.recordedSettlements.single()), notice)
    }

    @Test
    fun `GIVEN the device fails to save WHEN recording a payment THEN fails and no notice is owed`() = runTest {
        // Given
        prepareScenario(recordError = IllegalStateException("No group"))

        // When
        val result = recordAnaPaysBruno()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(repository.recordedSettlements.isEmpty())
        assertNull(recordedPayments.observeRecorded().first())
    }

    @Test
    fun `GIVEN a save in flight WHEN reading the notice THEN none is owed before the payment is saved`() = runTest {
        // Given
        prepareScenario(recordGate = CompletableDeferred())
        backgroundScope.launch { recordAnaPaysBruno() }
        runCurrent()

        // When
        val notice = recordedPayments.observeRecorded().first()

        // Then
        assertNull(notice)
    }

    private suspend fun recordAnaPaysBruno(): Result<String> = useCase(
        groupId = GROUP_ID,
        fromMemberId = ANA,
        toMemberId = BRUNO,
        amountMinor = 5_000L,
        method = SettlementMethod.PIX,
        paidAt = 3_000L,
    )

    private fun prepareScenario(
        now: Long = 1_000L,
        recordError: Throwable? = null,
        recordGate: CompletableDeferred<Unit>? = null,
    ) {
        repository = FakeSettlementRepository().also {
            it.recordError = recordError
            it.recordGate = recordGate
        }
        recordedPayments = InMemoryRecordedPaymentRepository()
        useCase = RecordSettlementUseCase(
            repository = repository,
            recordedPayments = recordedPayments,
            currentTimeMillis = { now },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val ANA = "ana"
        const val BRUNO = "bruno"
    }
}
