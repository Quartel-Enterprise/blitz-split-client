package com.quare.blitzsplit.feature.registerpayment.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface RegisterPaymentUiEvent : UiEvent {
    // Why: "Valor pago", as typed.
    data class OnAmountChanged(
        val text: String,
    ) : RegisterPaymentUiEvent

    // Why: "Toda a dívida", which fills in the whole debt again.
    data object OnFullDebtClicked : RegisterPaymentUiEvent

    // Why: "Como foi pago": Pix, Dinheiro, Transferência or Outro.
    data class OnMethodPicked(
        val method: SettlementMethod,
    ) : RegisterPaymentUiEvent

    // Why: "Quando" opening its picker, which then stops at the day it is now, also once midnight passed meanwhile.
    data object OnPaidAtOpened : RegisterPaymentUiEvent

    // Why: "Quando", picked in each platform's date and time picker.
    data class OnPaidAtChanged(
        val epochMillis: Long,
    ) : RegisterPaymentUiEvent

    data object OnConfirmClicked : RegisterPaymentUiEvent

    // Why: "Forma de pagamento" of "Pagar Ana" (4.5): Pix or Bitcoin.
    data class OnKeyMethodPicked(
        val method: PaymentMethod,
    ) : RegisterPaymentUiEvent

    // Why: "Marcar como pago" of "Pagar Ana", which records the whole debt, paid now.
    data object OnMarkAsPaidClicked : RegisterPaymentUiEvent

    // Why: "Paguei outro valor ou de outro jeito" of "Pagar Ana", which opens the form in its place.
    data object OnPayOtherwiseClicked : RegisterPaymentUiEvent

    // Why: who paid or who received, whose face opens their profile over the form (beta feedback 18), as Conta's do.
    data class OnPersonClicked(
        val personId: String,
    ) : RegisterPaymentUiEvent

    data object OnCloseClicked : RegisterPaymentUiEvent
}
