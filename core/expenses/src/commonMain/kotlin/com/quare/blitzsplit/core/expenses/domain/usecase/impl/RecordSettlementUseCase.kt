package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.repository.RecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.RecordSettlement
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlin.uuid.Uuid

/*
 * Why: the payment counts in the balances at once, offline included, and the group's screen under Registrar
 * pagamento then offers to undo it.
 */
class RecordSettlementUseCase(
    private val repository: SettlementRepository,
    private val recordedPayments: RecordedPaymentRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : RecordSettlement {
    override suspend fun invoke(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
        method: SettlementMethod,
        paidAt: Long,
    ): Result<String> {
        val recordedAt = currentTimeMillis()
        val settlement = Settlement(
            id = Uuid.random().toString(),
            groupId = groupId,
            fromMemberId = fromMemberId,
            toMemberId = toMemberId,
            amountMinor = amountMinor,
            method = method,
            paidAt = paidAt,
            isPending = true,
            updatedAt = recordedAt,
        )
        return repository
            .createSettlements(
                settlements = listOf(settlement),
                recordedAt = recordedAt,
            ).onSuccess { recordedPayments.post(settlement) }
            .map { settlement.id }
    }
}
