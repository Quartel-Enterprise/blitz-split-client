package com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.usecase.RecordSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.SettleUp
import com.quare.blitzsplit.core.support.domain.model.ReviewMoment
import com.quare.blitzsplit.core.support.domain.usecase.NoteReviewMoment
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentTarget
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiState

/*
 * Why: "Save" on the form: the one payment of a pair, or a settling up across groups, one payment each; the ids of
 * the payments recorded. A payment saved is a moment the app may ask for a review after, in the groups it was in.
 */
internal class PaymentSaving(
    private val recordSettlement: RecordSettlement,
    private val settleUp: SettleUp,
    private val noteReviewMoment: NoteReviewMoment,
) {
    suspend fun save(
        isSettleUp: Boolean,
        target: PaymentTarget,
        state: RegisterPaymentUiState,
        amountMinor: Long,
        method: SettlementMethod,
        paidAt: Long,
    ): Result<List<String>> {
        // Why: a pair of one group may pay part of the debt; a settling up of a single group too, through its one payment.
        val legs = target.legs
            .singleOrNull()
            ?.let { leg -> listOf(leg.copy(amountMinor = amountMinor)) }
            ?: target.legs
        val result = if (isSettleUp) {
            settleUp(
                legs = legs,
                method = method,
                paidAt = paidAt,
                personName = (if (state.kind == PaymentKind.PAID) state.to else state.from)?.name,
                isPaidByMe = state.kind == PaymentKind.PAID,
            )
        } else {
            val leg = target.legs.first()
            recordSettlement(
                groupId = leg.groupId,
                fromMemberId = leg.fromMemberId,
                toMemberId = leg.toMemberId,
                amountMinor = amountMinor,
                currency = leg.currency,
                method = method,
                paidAt = paidAt,
            ).map { listOf(it) }
        }
        val groupIds = if (isSettleUp) legs.map { it.groupId }.distinct() else listOf(target.legs.first().groupId)
        return result.onSuccess { noteReviewMoment(ReviewMoment.PaymentRecorded(groupIds)) }
    }
}
