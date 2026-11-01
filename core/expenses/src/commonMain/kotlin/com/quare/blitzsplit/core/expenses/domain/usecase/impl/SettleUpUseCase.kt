package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementLeg
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.repository.RecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.SettleUp
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlin.uuid.Uuid

/*
 * Why: each payment counts in its group's balances at once, offline included, and syncs like any other. They go in
 * together, so a failure leaves no group closed while another stays open, and nothing waiting to sync.
 */
class SettleUpUseCase(
    private val repository: SettlementRepository,
    private val recordedPayments: RecordedPaymentRepository,
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
        val settlements = legs.map { leg ->
            Settlement(
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
        }
        val ids = settlements.map { it.id }
        return repository
            .createSettlements(
                settlements = settlements,
                recordedAt = recordedAt,
            ).onSuccess {
                recordedPayments.post(
                    RecordedPayment.SettledUp(
                        settlementIds = ids,
                        personName = personName,
                        isPaidByMe = isPaidByMe,
                    ),
                )
            }.map { ids }
    }
}
