@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeDraft
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeUiAction
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeUiEvent
import com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel.ChargeViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

/*
 * Why: a group where the person pays you only because "Simplificar dívidas" is on, which Cobrar's message explains
 * (13.6b): "A Viagem Floripa simplificou as dívidas, então em vez de pagar Bruno você paga direto para mim…".
 */
class JsChargeReroute(
    val groupName: String,
    val insteadOfNames: Array<String>,
)

// Why: Cobrar's fields; the page writes the message from them until it is edited.
class JsChargeDraft(
    val personId: String,
    val name: String?,
    val initials: String,
    val color: String?,
    val photoUrl: String?,
    val currency: String,
    val fractionDigits: Int,
    val amountMinor: Double,
    val groupNames: Array<String>,
    val isGuest: Boolean,
    val guestBills: Array<String>,
    val guestPaidMinor: Double?,
    val includesPixKey: Boolean,
    val canTogglePixKey: Boolean,
    val editedMessage: String?,
    val reroutes: Array<JsChargeReroute>,
)

class JsChargeState(
    // Why: null until Room is read; the dialog draws once the person's message is known.
    val charge: JsChargeDraft?,
    // Why: the PaymentKeyType's name ("EMAIL") and the value of the Pix key Cobrar offers, null without one.
    val pixKeyType: String?,
    val pixKeyValue: String?,
)

// Why: "Cobrar", its own route, a dialog over the page that opened it (Saldos, a contact's profile).
class ChargePresenter(
    route: JsRoute,
) {
    private val chargeRoute = route.route as Route.Charge
    private val holder = ViewModelHolder(ChargeViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(chargeRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsChargeState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsChargeState(
                charge = state.charge?.let(::toJsChargeDraft),
                pixKeyType = state.pixKey?.type?.name,
                pixKeyValue = state.pixKey?.value,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "Enviar": the browser's share sheet when it has one, else the message copied.
    fun observeShare(onShare: (String) -> Unit): JsWatch {
        val messages = viewModel.uiAction.filterIsInstance<ChargeUiAction.ShareMessage>().map { it.message }
        val watch = FlowWatcher(messages).observe(onShare)
        return JsWatch(watch::cancel)
    }

    fun observeCopy(onCopy: (String) -> Unit): JsWatch {
        val messages = viewModel.uiAction.filterIsInstance<ChargeUiAction.CopyMessage>().map { it.message }
        val watch = FlowWatcher(messages).observe(onCopy)
        return JsWatch(watch::cancel)
    }

    fun togglePixKey(isIncluded: Boolean) {
        viewModel.onEvent(ChargeUiEvent.OnPixKeyToggled(isIncluded))
    }

    fun changeMessage(message: String) {
        viewModel.onEvent(ChargeUiEvent.OnMessageChanged(message))
    }

    // Why: the ViewModel keeps what was typed, so the field reads it back in the same keystroke.
    fun getEditedMessage(): String? = viewModel.uiState.value.charge
        ?.editedMessage

    fun shareMessage(message: String) {
        viewModel.onEvent(ChargeUiEvent.OnShareClicked(message))
    }

    fun copyMessage(message: String) {
        viewModel.onEvent(ChargeUiEvent.OnCopyClicked(message))
    }

    fun close() {
        viewModel.onEvent(ChargeUiEvent.OnDismissed)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsChargeDraft(charge: ChargeDraft): JsChargeDraft = JsChargeDraft(
    personId = charge.personId,
    name = charge.name,
    initials = charge.initials,
    color = charge.color?.code,
    photoUrl = charge.photoUrl,
    currency = charge.currency.code,
    fractionDigits = charge.currency.fractionDigits,
    amountMinor = charge.amountMinor.toDouble(),
    groupNames = charge.groupNames.toTypedArray(),
    isGuest = charge.isGuest,
    guestBills = charge.guestBills.toTypedArray(),
    guestPaidMinor = charge.guestPaidMinor?.toDouble(),
    includesPixKey = charge.includesPixKey,
    canTogglePixKey = charge.canTogglePixKey,
    editedMessage = charge.editedMessage,
    reroutes = charge.reroutes
        .map {
            JsChargeReroute(
                groupName = it.groupName,
                insteadOfNames = it.insteadOfNames.toTypedArray(),
            )
        }.toTypedArray(),
)
