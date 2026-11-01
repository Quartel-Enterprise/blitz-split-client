package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.data.repository.InMemorySettledUpRepository
import com.quare.blitzsplit.core.expenses.domain.model.SettledUp
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementLeg
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
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
    fun `GIVEN the second payment fails WHEN settling up THEN takes back the first and posts nothing`() = runTest {
        // Given
        prepareScenario(failingCall = 2)

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
        assertEquals(settlements.recordedSettlements.map { it.id }, settlements.deletedIds)
        assertTrue(settlements.settlements.value.isEmpty())
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

    private fun prepareScenario(failingCall: Int? = null) {
        settlements = FakeSettlementRepository()
        settledUps = InMemorySettledUpRepository()
        useCase = SettleUpUseCase(
            repository = failingCall?.let { FailingOnCallRepository(settlements, it) } ?: settlements,
            settledUps = settledUps,
            currentTimeMillis = { NOW },
        )
    }

    private companion object {
        const val NOW = 1_000L
        const val PAID_AT = 500L
    }
}

// Why: the shared fake fails every record or none; settling up needs one that fails partway through.
private class FailingOnCallRepository(
    private val delegate: FakeSettlementRepository,
    private val failingCall: Int,
) : SettlementRepository by delegate {
    private var calls = 0

    override suspend fun createSettlement(
        settlement: Settlement,
        recordedAt: Long,
    ): Result<Unit> {
        calls++
        if (calls == failingCall) return Result.failure(IllegalStateException("disk full"))
        return delegate.createSettlement(settlement, recordedAt)
    }
}
