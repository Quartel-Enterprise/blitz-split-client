@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiAction
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.RegisterPaymentViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

// Why: who paid or who received; the page says "Você" for isMe.
class JsPaymentPerson(
    val id: String,
    val name: String,
    val initials: String,
    val color: String,
    val isMe: Boolean,
)

// Why: amounts in the currency's minor unit and instants in epoch millis, as numbers JavaScript reads exactly.
class JsRegisterPaymentState(
    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator.
    val currency: String,
    val fractionDigits: Int,
    val from: JsPaymentPerson?,
    val to: JsPaymentPerson?,
    val debtMinor: Double,
    // Why: "Por grupo" of a settling up across groups from the global Saldos; empty for a pair of one group.
    val groupParts: Array<JsPersonGroup>,
    // Why: across several groups only the whole net settles, so the amount can't be changed.
    val isAmountLocked: Boolean,
    val amountText: String,
    // Why: the SettlementMethod's code: "pix", "cash", "transfer" or "other".
    val method: String,
    val paidAt: Double,
    // Why: "today", "yesterday" or "other", when the page writes the date.
    val paidDay: String,
    val latestPaidAt: Double,
    // Why: "received", "paid" or "between-others", for the title, the sentences and the button.
    val kind: String,
    val amountMinor: Double?,
    val isAmountValid: Boolean,
    val showsOverDebtError: Boolean,
    val remainingMinor: Double?,
    val isFullDebt: Boolean,
    val showsOfflineNotice: Boolean,
    val isSaving: Boolean,
    val canConfirm: Boolean,
    val isLoaded: Boolean,
)

class RegisterPaymentPresenter(
    route: JsRoute,
) {
    // Why: a pair of one group ("register-payment") or someone across groups ("settle-up").
    private val paymentRoute = route.route
    private val holder = ViewModelHolder(RegisterPaymentViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(paymentRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsRegisterPaymentState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsRegisterPaymentState(
                currency = state.currency.code,
                fractionDigits = state.currency.fractionDigits,
                from = state.from?.let(::toJsPaymentPerson),
                to = state.to?.let(::toJsPaymentPerson),
                debtMinor = state.debtMinor.toDouble(),
                groupParts = state.groupParts.map(::toJsPersonGroup).toTypedArray(),
                isAmountLocked = state.isAmountLocked,
                amountText = state.amountText,
                method = state.method.code,
                paidAt = state.paidAt.toDouble(),
                paidDay = toJsRelativeDay(state.paidDay),
                latestPaidAt = state.latestPaidAt.toDouble(),
                kind = when (state.kind) {
                    PaymentKind.RECEIVED -> "received"
                    PaymentKind.PAID -> "paid"
                    PaymentKind.BETWEEN_OTHERS -> "between-others"
                },
                amountMinor = state.amountMinor?.toDouble(),
                isAmountValid = state.isAmountValid,
                showsOverDebtError = state.showsOverDebtError,
                remainingMinor = state.remainingMinor?.toDouble(),
                isFullDebt = state.isFullDebt,
                showsOfflineNotice = state.showsOfflineNotice,
                isSaving = state.isSaving,
                canConfirm = state.canConfirm,
                isLoaded = state.isLoaded,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "save-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                RegisterPaymentUiAction.ShowSaveError -> "save-error"
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun changeAmount(text: String) {
        viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged(text))
    }

    // Why: the ViewModel filters the amount as it handles it, so the field reads it back in the same keystroke.
    fun getAmountText(): String = viewModel.uiState.value.amountText

    // Why: "Toda a dívida".
    fun fullDebt() {
        viewModel.onEvent(RegisterPaymentUiEvent.OnFullDebtClicked)
    }

    // Why: a SettlementMethod's code; an unknown one is ignored.
    fun pickMethod(code: String) {
        val method = SettlementMethod.entries.firstOrNull { it.code == code } ?: return
        viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(method))
    }

    fun openPaidAt() {
        viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAtOpened)
    }

    fun changePaidAt(epochMillis: Double) {
        viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAtChanged(epochMillis.toLong()))
    }

    fun confirm() {
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)
    }

    fun close() {
        viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

internal fun toJsPaymentPerson(person: PaymentPerson): JsPaymentPerson = JsPaymentPerson(
    id = person.id,
    name = person.name,
    initials = person.initials,
    color = person.avatarColor.code,
    isMe = person.isMe,
)
