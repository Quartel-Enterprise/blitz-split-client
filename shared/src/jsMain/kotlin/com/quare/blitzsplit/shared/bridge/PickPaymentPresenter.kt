@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PickPaymentGroup
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PickPaymentPair
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PickPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.PickPaymentViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

// Why: who owes whom how much, in the currency's minor unit; a null person is someone no longer in the group.
class JsPickPaymentPair(
    val from: JsPaymentPerson?,
    val to: JsPaymentPerson?,
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Double,
)

class JsPickPaymentGroup(
    val groupId: String,
    val name: String,
    val initials: String,
    val color: String,
    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator.
    val currency: String,
    val fractionDigits: Int,
    val pairs: Array<JsPickPaymentPair>,
)

class JsPickPaymentState(
    val isLoaded: Boolean,
    // Why: the groups with an open pair, the most recently used first.
    val groups: Array<JsPickPaymentGroup>,
    // Why: "Ninguém deve nada agora".
    val showsEmpty: Boolean,
)

// Why: "Quem pagou a quem?", Início's "Registrar pagamento" before the form.
class PickPaymentPresenter {
    private val holder = ViewModelHolder(PickPaymentViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsPickPaymentState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsPickPaymentState(
                isLoaded = state.isLoaded,
                groups = state.groups.map(::toJsPickPaymentGroup).toTypedArray(),
                showsEmpty = state.showsEmpty,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: a pair, which opens Registrar pagamento for it in this dialog's place.
    fun pickPair(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
    ) {
        viewModel.onEvent(PickPaymentUiEvent.OnPairClicked(groupId, fromMemberId, toMemberId))
    }

    // Why: ✕, Esc, a click outside or a swipe down.
    fun close() {
        viewModel.onEvent(PickPaymentUiEvent.OnDismissed)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsPickPaymentGroup(group: PickPaymentGroup): JsPickPaymentGroup = JsPickPaymentGroup(
    groupId = group.groupId,
    name = group.name,
    initials = group.initials,
    color = group.color.code,
    currency = group.currency.code,
    fractionDigits = group.currency.fractionDigits,
    pairs = group.pairs.map(::toJsPickPaymentPair).toTypedArray(),
)

private fun toJsPickPaymentPair(pair: PickPaymentPair): JsPickPaymentPair = JsPickPaymentPair(
    from = pair.from?.let(::toJsPaymentPerson),
    to = pair.to?.let(::toJsPaymentPerson),
    fromMemberId = pair.fromMemberId,
    toMemberId = pair.toMemberId,
    amountMinor = pair.amountMinor.toDouble(),
)
