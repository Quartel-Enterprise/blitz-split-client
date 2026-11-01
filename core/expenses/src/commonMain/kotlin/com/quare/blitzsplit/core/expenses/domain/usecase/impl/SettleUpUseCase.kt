package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.SettledUp
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementLeg
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.repository.SettledUpRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.SettleUp
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlin.uuid.Uuid

/*
 * Why: each payment counts in its group's balances at once, offline included, and syncs like any other; one that
 * fails takes back those already recorded, so a group never closes while another stays open.
 */
class SettleUpUseCase(
    private val repository: SettlementRepository,
    private val settledUps: SettledUpRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : SettleUp {
    override suspend fun invoke(
        legs: List<SettlementLeg>,
        method: SettlementMethod,
        paidAt: Long,
        personName: String?,
        isPaidByMe: Boolean,
    ): Result<List<String>> {
        val recordedAt = currentTimeMillis()
        val recorded = mutableListOf<String>()
        legs.forEach { leg ->
            val settlement = Settlement(
                id = Uuid.random().toString(),
                groupId = leg.groupId,
                fromMemberId = leg.fromMemberId,
                toMemberId = leg.toMemberId,
                amountMinor = leg.amountMinor,
                method = method,
                paidAt = paidAt,
                isPending = true,
                updatedAt = recordedAt,
            )
            val result = repository.createSettlement(
                settlement = settlement,
                recordedAt = recordedAt,
            )
            result.exceptionOrNull()?.let { error ->
                recorded.forEach { id ->
                    repository.deleteSettlement(
                        id = id,
                        deletedAt = currentTimeMillis(),
                    )
                }
                return Result.failure(error)
            }
            recorded += settlement.id
        }
        settledUps.post(
            SettledUp(
                settlementIds = recorded.toList(),
                personName = personName,
                isPaidByMe = isPaidByMe,
            ),
        )
        return Result.success(recorded.toList())
    }
}
