package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettledUpUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveSettledUpUseCaseTest {
    private val recordedPayments = InMemoryRecordedPaymentRepository()
    private val useCase = ObserveSettledUpUseCase(recordedPayments)

    @Test
    fun `GIVEN a settling up just recorded WHEN observing it THEN the global Saldos owes its notice`() = runTest {
        // Given
        val settledUp = RecordedPayment.SettledUp(
            settlementIds = listOf("p1", "p2"),
            personName = "Carla",
            isPaidByMe = false,
        )
        recordedPayments.post(settledUp)

        // When
        val recorded = useCase().first()

        // Then
        assertEquals(settledUp, recorded)
    }

    @Test
    fun `GIVEN a pair of one group just recorded WHEN observing the settling up THEN none is owed`() = runTest {
        // Given
        recordedPayments.post(
            RecordedPayment.InGroup(
                Settlement(
                    id = "p1",
                    groupId = "group-1",
                    fromMemberId = "ana",
                    toMemberId = "bruno",
                    amountMinor = 5_000L,
                    method = SettlementMethod.PIX,
                    paidAt = 1_000L,
                    isPending = true,
                    updatedAt = 0L,
                ),
            ),
        )

        // When
        val recorded = useCase().first()

        // Then
        assertNull(recorded)
    }
}
