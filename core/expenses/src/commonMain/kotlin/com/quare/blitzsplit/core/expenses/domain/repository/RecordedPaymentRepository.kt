package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import kotlinx.coroutines.flow.Flow

/*
 * Why: "Pagamento de Ana registrado", with Desfazer, which the screen under Registrar pagamento shows once it is on
 * top again: a group's screen or the home for a pair, the global Saldos for a settling up. Only while the app runs.
 */
interface RecordedPaymentRepository {
    // Why: the payments just recorded, null when no screen owes the notice.
    fun observeRecorded(): Flow<RecordedPayment?>

    fun post(recorded: RecordedPayment)

    // Why: only the notice holding that payment, so one recorded meanwhile keeps its own.
    fun consume(settlementId: String)
}
