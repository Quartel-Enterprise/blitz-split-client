package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveRecordedPaymentUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveRecordedPaymentUseCaseTest {
    private val recordedPayments = InMemoryRecordedPaymentRepository()
    private val useCase = ObserveRecordedPaymentUseCase(recordedPayments)

    @Test
    fun `GIVEN no payment recorded WHEN observing a group THEN it owes no notice`() = runTest {
        // Given
        val groupId = GROUP_ID

        // When
        val recorded = useCase(groupId).first()

        // Then
        assertNull(recorded)
    }

    @Test
    fun `GIVEN a payment recorded in the group WHEN observing it THEN the group owes its notice`() = runTest {
        // Given
        recordedPayments.post(payment(groupId = GROUP_ID))

        // When
        val recorded = useCase(GROUP_ID).first()

        // Then
        assertEquals(payment(groupId = GROUP_ID), recorded)
    }

    @Test
    fun `GIVEN a payment recorded in another group WHEN observing this group THEN it owes no notice`() = runTest {
        // Given
        recordedPayments.post(payment(groupId = OTHER_GROUP_ID))

        // When
        val recorded = useCase(GROUP_ID).first()

        // Then
        assertNull(recorded)
    }

    @Test
    fun `GIVEN an open group WHEN a payment is recorded in another group THEN it hears nothing new`() = runTest {
        // Given
        val seen = mutableListOf<Settlement?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase(GROUP_ID).toList(seen) }

        // When
        recordedPayments.post(payment(groupId = OTHER_GROUP_ID))

        // Then
        assertEquals(listOf<Settlement?>(null), seen)
    }

    @Test
    fun `GIVEN the group owes a notice WHEN the notice is consumed THEN it owes none`() = runTest {
        // Given
        recordedPayments.post(payment(groupId = GROUP_ID))

        // When
        recordedPayments.consume(PAYMENT_ID)

        // Then
        assertNull(useCase(GROUP_ID).first())
    }

    private fun payment(
        groupId: String,
        id: String = PAYMENT_ID,
    ): Settlement = Settlement(
        id = id,
        groupId = groupId,
        fromMemberId = "ana",
        toMemberId = "bruno",
        amountMinor = 5_000L,
        method = SettlementMethod.CASH,
        paidAt = 1_000L,
        isPending = true,
    )

    private companion object {
        const val PAYMENT_ID = "payment-1"
        const val GROUP_ID = "group-1"
        const val OTHER_GROUP_ID = "group-2"
    }
}
