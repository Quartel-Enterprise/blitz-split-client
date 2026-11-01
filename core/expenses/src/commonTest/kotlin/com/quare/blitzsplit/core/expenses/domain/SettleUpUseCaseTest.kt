package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.data.repository.InMemorySettledUpRepository
import com.quare.blitzsplit.core.expenses.domain.model.SettledUp
import com.quare.blitzsplit.core.expenses.domain.model.SettlementLeg
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SettleUpUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettleUpUseCaseTest {
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var settledUps: InMemorySettledUpRepository
    private lateinit var useCase: SettleUpUseCase

    @Test
    fun `GIVEN a person across two groups WHEN settling up THEN records one pending payment per group`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(
            legs = legs,
            method = SettlementMethod.CASH,
            paidAt = PAID_AT,
            personName = "Carla",
            isPaidByMe = false,
        )

        // Then
        val recorded = settlements.recordedSettlements
        assertEquals(recorded.map { it.id }, result.getOrThrow())
        assertEquals(
            legs,
            recorded.map { SettlementLeg(it.groupId, it.fromMemberId, it.toMemberId, it.amountMinor) },
        )
        assertTrue(recorded.all { it.method == SettlementMethod.CASH && it.paidAt == PAID_AT && it.isPending })
        assertTrue(recorded.all { it.updatedAt == NOW })
    }

    @Test
    fun `GIVEN a person across groups WHEN settling up THEN posts the notice for every payment`() = runTest {
        // Given
        prepareScenario()

        // When
        val ids = useCase(
            legs,
            SettlementMethod.PIX,
            PAID_AT,
            personName = "Bruno",
            isPaidByMe = true,
        ).getOrThrow()

        // Then
        assertEquals(
            SettledUp(
                settlementIds = ids,
                personName = "Bruno",
                isPaidByMe = true,
            ),
            settledUps.observeSettledUp().first(),
        )
    }

    @Test
    fun `GIVEN the device fails to save WHEN settling up THEN nothing is recorded nor waits to sync`() = runTest {
        // Given
        prepareScenario(recordError = IllegalStateException("No group ape"))

        // When
        val result = useCase(
            legs,
            SettlementMethod.PIX,
            PAID_AT,
            personName = "Carla",
            isPaidByMe = false,
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(settlements.settlements.value.isEmpty())
        assertTrue(settlements.deletedIds.isEmpty())
        assertTrue(settlements.pendingDeletionGroupIds.value.isEmpty())
        assertNull(settledUps.observeSettledUp().first())
    }

    private val legs = listOf(
        SettlementLeg(
            "floripa",
            fromMemberId = "carla-1",
            toMemberId = "me-1",
            amountMinor = 9_840L,
        ),
        SettlementLeg(
            "ape",
            fromMemberId = "me-2",
            toMemberId = "carla-2",
            amountMinor = 9_800L,
        ),
    )

    private fun prepareScenario(recordError: Throwable? = null) {
        settlements = FakeSettlementRepository().also { it.recordError = recordError }
        settledUps = InMemorySettledUpRepository()
        useCase = SettleUpUseCase(
            repository = settlements,
            settledUps = settledUps,
            currentTimeMillis = { NOW },
        )
    }

    private companion object {
        const val NOW = 1_000L
        const val PAID_AT = 500L
    }
}
