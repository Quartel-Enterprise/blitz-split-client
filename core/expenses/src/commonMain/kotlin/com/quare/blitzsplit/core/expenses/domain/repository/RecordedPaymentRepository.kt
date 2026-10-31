package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import kotlinx.coroutines.flow.Flow

/*
 * Why: "Pagamento de Ana registrado", with Desfazer, which the group's screen under Registrar pagamento shows once
 * it is on top again. Only while the app runs.
 */
interface RecordedPaymentRepository {
    // Why: the payment just recorded, null when no screen owes the notice.
    fun observeRecorded(): Flow<Settlement?>

    fun post(settlement: Settlement)

    // Why: only that payment's notice, so one recorded meanwhile keeps its own.
    fun consume(settlementId: String)
}
