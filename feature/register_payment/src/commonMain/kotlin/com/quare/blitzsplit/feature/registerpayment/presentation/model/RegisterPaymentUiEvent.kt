package com.quare.blitzsplit.feature.registerpayment.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
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

    data object OnCloseClicked : RegisterPaymentUiEvent
}
