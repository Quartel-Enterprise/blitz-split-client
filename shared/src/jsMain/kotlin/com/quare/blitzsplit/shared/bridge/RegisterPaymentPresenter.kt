@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.ReceiverKey
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiAction
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.model.SimplifiedPairNote
import com.quare.blitzsplit.feature.registerpayment.presentation.model.SimplifyChangedNotice
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
    val photoUrl: String?,
    val isMe: Boolean,
)

// Why: a key of the receiver for "Pagar Ana" (4.5): value is what is copied, text how it reads.
class JsReceiverKey(
    val id: String,
    // Why: the PaymentKeyType's name ("PHONE").
    val type: String,
    val value: String,
    val text: String,
)

/*
 * Why: "Dívida simplificada: Diego paga a você o que devia a Bruno." for a pair no bill made (isNewPair), "…: inclui o
 * que Ana devia a você." for one that grew (13.5); insteadOf is whom the payer owed by the bills.
 */
class JsSimplifiedPairNote(
    val isNewPair: Boolean,
    val insteadOf: Array<JsSimplifyName>,
)

/*
 * Why: 13.5c, in place of the form or "Pagar Ana": someone turned "Simplificar dívidas" the other way and the pair owes
 * nothing anymore. A null name is "Alguém"; previousDebtMinor is what "Já paguei mesmo assim" records.
 */
class JsSimplifyChange(
    val changedByName: String?,
    val simplifiesDebts: Boolean,
    val groupName: String,
    val previousDebtMinor: Double,
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
    // Why: "Pagar Ana" (4.5) takes the form's place while this person pays someone with a key.
    val showsReceiverKeys: Boolean,
    // Why: neither "Pagar Ana" nor the form yet: the keys' shimmer until the account's full sync brings them.
    val isLoadingReceiverKeys: Boolean,
    val groupName: String?,
    // Why: the PaymentMethod's names, "PIX" or "BITCOIN": those the receiver has keys of, and the one shown.
    val keyMethods: Array<String>,
    val showsKeyMethodPicker: Boolean,
    val keyMethod: String,
    val defaultKey: JsReceiverKey?,
    val otherKeys: Array<JsReceiverKey>,
    // Why: "Pix copia e cola" with the whole debt in it, and its QR code's modules row by row, pixQrSize by pixQrSize.
    val pixCode: String?,
    val pixQrSize: Int,
    val pixQrModules: Array<Boolean>,
    val canMarkAsPaid: Boolean,
    val simplifiedNote: JsSimplifiedPairNote?,
    val simplifyChange: JsSimplifyChange?,
    val canPayAnyway: Boolean,
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
                showsReceiverKeys = state.showsReceiverKeys,
                isLoadingReceiverKeys = state.isLoadingReceiverKeys,
                groupName = state.groupName,
                keyMethods = state.keyMethods.map(PaymentMethod::name).toTypedArray(),
                showsKeyMethodPicker = state.showsKeyMethodPicker,
                keyMethod = state.shownKeyMethod.name,
                defaultKey = state.defaultKey?.let(::toJsReceiverKey),
                otherKeys = state.otherKeys.map(::toJsReceiverKey).toTypedArray(),
                pixCode = state.pixCode,
                pixQrSize = state.pixQrCode?.size ?: 0,
                pixQrModules = state.pixQrCode
                    ?.modules
                    .orEmpty()
                    .toTypedArray(),
                canMarkAsPaid = state.canMarkAsPaid,
                simplifiedNote = state.simplifiedNote?.let(::toJsSimplifiedPairNote),
                simplifyChange = state.simplifyChange?.let(::toJsSimplifyChange),
                canPayAnyway = state.canPayAnyway,
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

    // Why: a PaymentMethod's name; an unknown one is ignored.
    fun pickKeyMethod(name: String) {
        val method = PaymentMethod.entries.firstOrNull { it.name == name } ?: return
        viewModel.onEvent(RegisterPaymentUiEvent.OnKeyMethodPicked(method))
    }

    // Why: "Marcar como pago", once paid in the bank or the wallet.
    fun markAsPaid() {
        viewModel.onEvent(RegisterPaymentUiEvent.OnMarkAsPaidClicked)
    }

    // Why: "Paguei outro valor ou de outro jeito", which opens the form in place of "Pagar Ana".
    fun payOtherwise() {
        viewModel.onEvent(RegisterPaymentUiEvent.OnPayOtherwiseClicked)
    }

    // Why: who paid or who received, by JsPaymentPerson.id, whose face opens their profile over the dialog.
    fun openPerson(personId: String) {
        viewModel.onEvent(RegisterPaymentUiEvent.OnPersonClicked(personId))
    }

    fun close() {
        viewModel.onEvent(RegisterPaymentUiEvent.OnCloseClicked)
    }

    // Why: "Já paguei mesmo assim" of 13.5c.
    fun payAnyway() {
        viewModel.onEvent(RegisterPaymentUiEvent.OnPaidAnywayClicked)
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
    photoUrl = person.photoUrl,
    isMe = person.isMe,
)

private fun toJsReceiverKey(key: ReceiverKey): JsReceiverKey = JsReceiverKey(
    id = key.id,
    type = key.type.name,
    value = key.value,
    text = key.text,
)

private fun toJsSimplifiedPairNote(note: SimplifiedPairNote): JsSimplifiedPairNote = JsSimplifiedPairNote(
    isNewPair = note.isNewPair,
    insteadOf = note.insteadOf.map { JsSimplifyName(it.name, it.isMe) }.toTypedArray(),
)

private fun toJsSimplifyChange(notice: SimplifyChangedNotice): JsSimplifyChange = JsSimplifyChange(
    changedByName = notice.changedByName,
    simplifiesDebts = notice.simplifiesDebts,
    groupName = notice.groupName,
    previousDebtMinor = notice.previousDebtMinor.toDouble(),
)
