package com.quare.blitzsplit.feature.registerpayment.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "register_payment"

sealed interface RegisterPaymentUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: typed letter by letter; what was typed shows in the payment recorded.
            is OnAmountChanged -> EventAnalytics.NotTracked

            OnFullDebtClicked -> EventAnalytics.Automatic("payment_full_debt_clicked")

            // Why: only another method, never the one picked, as Apple's Picker reports only a change.
            is OnMethodPicked -> EventAnalytics.Manual("payment_method_selected")

            OnPaidAtOpened -> EventAnalytics.Automatic("payment_date_clicked")

            /*
             * Why: Apple's and the web's pickers send each step (a day, the time wheel) as it turns, like typing; the
             * tap that opened it counts, and the date shows in the payment recorded.
             */
            is OnPaidAtChanged -> EventAnalytics.NotTracked

            // Why: the tap, then the payment once it is saved.
            OnConfirmClicked -> EventAnalytics.Manual(setOf("payment_confirm_clicked", "payment_recorded"))

            is OnKeyMethodPicked -> EventAnalytics.Manual("payment_key_method_selected")

            OnMarkAsPaidClicked -> EventAnalytics.Manual(setOf("payment_mark_as_paid_clicked", "payment_recorded"))

            OnPayOtherwiseClicked -> EventAnalytics.Automatic("payment_pay_otherwise_clicked")

            is OnPersonClicked -> EventAnalytics.Automatic("person_clicked", mapOf("source" to SOURCE))

            // Why: a close, which each platform also does its own way: where it went is its screen view.
            OnCloseClicked -> EventAnalytics.NotTracked

            OnPaidAnywayClicked -> EventAnalytics.Manual(setOf("payment_paid_anyway_clicked", "payment_recorded"))
        }

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

    // Why: "Já paguei mesmo assim" of 13.5c: the payment is recorded as it was owed and the app redoes the pairs.
    data object OnPaidAnywayClicked : RegisterPaymentUiEvent
}
