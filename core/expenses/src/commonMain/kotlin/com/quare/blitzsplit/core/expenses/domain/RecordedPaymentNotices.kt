package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSettlement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/*
 * Why: "Pagamento de Ana registrado." with "Desfazer", which Despesas, Saldos, Membros, the home and the global Saldos
 * show alike once Registrar pagamento closes over them.
 */
class RecordedPaymentNotices(
    private val consumeRecordedPayment: ConsumeRecordedPayment,
    private val deleteSettlement: DeleteSettlement,
) {
    // Why: a pair of one group, named as the group has its members.
    fun watch(
        scope: CoroutineScope,
        recorded: Flow<Settlement?>,
        backStack: Flow<*>,
        isLoaded: Flow<Boolean>,
        isOnTop: () -> Boolean,
        meId: () -> String?,
        nameOf: (memberId: String) -> String?,
        show: (PaymentNotice) -> Unit,
    ) {
        watchNotices(
            scope = scope,
            recorded = recorded,
            backStack = backStack,
            isLoaded = isLoaded,
            isOnTop = isOnTop,
            noticeOf = { settlement -> PaymentNotice.of(settlement, meId(), nameOf) },
            show = show,
        )
    }

    // Why: a settling up across groups, named as Registrar pagamento named the person.
    fun watchSettledUp(
        scope: CoroutineScope,
        recorded: Flow<RecordedPayment.SettledUp?>,
        backStack: Flow<*>,
        isLoaded: Flow<Boolean>,
        isOnTop: () -> Boolean,
        show: (PaymentNotice) -> Unit,
    ) {
        watchNotices(
            scope = scope,
            recorded = recorded,
            backStack = backStack,
            isLoaded = isLoaded,
            isOnTop = isOnTop,
            noticeOf = PaymentNotice::of,
            show = show,
        )
    }

    /*
     * Why: "Desfazer" and "Apagar este acerto?". A payment already gone is what was asked; any other failure leaves
     * them all counting, which the screen says.
     */
    fun delete(
        scope: CoroutineScope,
        settlementIds: List<String>,
        onFailure: suspend () -> Unit,
    ) {
        scope.launch { deleteSettlement(settlementIds).onFailure { onFailure() } }
    }

    /*
     * Why: the notice waits until the screen is on top again, under Registrar pagamento, and for what names it; once
     * shown, no other screen shows it again.
     */
    private fun <T : Any> watchNotices(
        scope: CoroutineScope,
        recorded: Flow<T?>,
        backStack: Flow<*>,
        isLoaded: Flow<Boolean>,
        isOnTop: () -> Boolean,
        noticeOf: (T) -> PaymentNotice,
        show: (PaymentNotice) -> Unit,
    ) {
        combine(recorded, backStack, isLoaded.distinctUntilChanged(), ::Triple)
            .onEach { (payment, _, isLoaded) ->
                if (payment == null || !isLoaded || !isOnTop()) return@onEach
                val notice = noticeOf(payment)
                consumeRecordedPayment(notice.settlementId)
                show(notice)
            }.launchIn(scope)
    }
}
