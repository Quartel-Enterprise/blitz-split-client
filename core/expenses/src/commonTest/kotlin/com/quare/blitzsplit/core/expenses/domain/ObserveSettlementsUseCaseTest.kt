package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveSettlementsUseCaseTest {
    private lateinit var repository: FakeSettlementRepository
    private lateinit var useCase: ObserveSettlementsUseCase

    @Test
    fun `GIVEN payments of two groups WHEN observing one THEN shows only its payments with the most recent first`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    payment(
                        id = "older",
                        groupId = "group-1",
                        paidAt = 1_000L,
                    ),
                    payment(
                        id = "other-group",
                        groupId = "group-2",
                        paidAt = 2_000L,
                    ),
                    payment(
                        id = "newer",
                        groupId = "group-1",
                        paidAt = 3_000L,
                    ),
                ),
            )

            // When
            val settlements = useCase("group-1").first()

            // Then
            assertEquals(listOf("newer", "older"), settlements.map { it.id })
        }

    @Test
    fun `GIVEN an open group WHEN a payment is recorded in it THEN the group shows it`() = runTest {
        // Given
        prepareScenario()
        val settlements = useCase("group-1")

        // When
        repository.settlements.value = listOf(
            payment(
                id = "new",
                groupId = "group-1",
                paidAt = 1_000L,
            ),
        )

        // Then
        assertEquals(listOf("new"), settlements.first().map { it.id })
    }

    private fun payment(
        id: String,
        groupId: String,
        paidAt: Long,
    ): Settlement = Settlement(
        id = id,
        groupId = groupId,
        fromMemberId = "ana",
        toMemberId = "bruno",
        amountMinor = 5_000L,
        method = SettlementMethod.PIX,
        paidAt = paidAt,
        isPending = false,
        updatedAt = 0L,
    )

    private fun prepareScenario(stored: List<Settlement> = emptyList()) {
        repository = FakeSettlementRepository(initialSettlements = stored)
        useCase = ObserveSettlementsUseCase(repository)
    }
}
