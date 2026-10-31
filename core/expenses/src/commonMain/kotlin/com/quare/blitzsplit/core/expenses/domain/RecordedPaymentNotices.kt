package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
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
 * Why: "Pagamento de Ana registrado." with "Desfazer", which Despesas, Saldos and Membros show alike once Registrar
 * pagamento closes over them.
 */
class RecordedPaymentNotices(
    private val consumeRecordedPayment: ConsumeRecordedPayment,
    private val deleteSettlement: DeleteSettlement,
) {
    /*
     * Why: the notice waits until the screen is on top again, under Registrar pagamento, and for the group's names;
     * once shown, no other screen shows it again.
     */
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
        combine(recorded, backStack, isLoaded.distinctUntilChanged(), ::Triple)
            .onEach { (settlement, _, isLoaded) ->
                if (settlement == null || !isLoaded || !isOnTop()) return@onEach
                consumeRecordedPayment(settlement.id)
                show(
                    PaymentNotice.of(
                        settlement = settlement,
                        meId = meId(),
                        nameOf = nameOf,
                    ),
                )
            }.launchIn(scope)
    }

    // Why: a payment already gone is what was asked; any other failure leaves it counting, which the screen says.
    fun delete(
        scope: CoroutineScope,
        settlementId: String,
        onFailure: suspend () -> Unit,
    ) {
        scope.launch { deleteSettlement(settlementId).onFailure { onFailure() } }
    }
}
