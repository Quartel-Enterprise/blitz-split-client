package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerSelection
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Why: "Quem pagou" picks one person, or "Várias pessoas", whose amounts add up to the total before Pronto takes them.
internal class PayerPicking(
    private val uiState: MutableStateFlow<SplitBillUiState>,
    private val separator: () -> Char,
    private val trackEvent: TrackEvent,
    // Why: "Paid with" belongs with who paid, though its own helper keeps it.
    private val onPaidWithPicked: (String?) -> Unit,
) {
    fun onEvent(event: SplitBillUiEvent.Payers) {
        when (event) {
            SplitBillUiEvent.OnPayerClicked -> uiState.update {
                it.copy(payerSelection = PayerSelection(memberId = it.payerId.ifEmpty { null }))
            }

            // Why: only a pick the sheet takes, never a late tap once it closed.
            is SplitBillUiEvent.OnPayerPicked -> {
                val before = uiState.value
                uiState.update { it.withPayerPicked(event.memberId) }
                if (uiState.value != before) trackEvent("split_bill_payer_picked", emptyMap())
            }

            // Why: only "Pronto" on a sheet still open counts.
            SplitBillUiEvent.OnPayerDone -> {
                if (uiState.value.payerSelection != null) trackEvent("split_bill_payer_confirmed", emptyMap())
                uiState.update { it.withPayerConfirmed() }
            }

            SplitBillUiEvent.OnPayerDismissed -> uiState.update { it.copy(payerSelection = null) }

            SplitBillUiEvent.OnSeveralPayersClicked -> uiState.update { it.withSeveralPayersOpened(separator()) }

            is SplitBillUiEvent.OnPayerAmountChanged -> uiState.update {
                it.withPayerAmount(event.memberId, event.text, separator())
            }

            // Why: only once the amounts add up, with how many paid.
            SplitBillUiEvent.OnSeveralPayersDone -> {
                uiState.value.payerAmounts?.takeIf { it.addsUp }?.let { amounts ->
                    trackEvent("split_bill_several_payers_confirmed", mapOf("payers" to amounts.payers.size))
                }
                uiState.update { it.withSeveralPayersConfirmed() }
            }

            SplitBillUiEvent.OnSeveralPayersDismissed -> uiState.update { it.copy(payerAmounts = null) }

            is SplitBillUiEvent.OnPaidWithPicked -> onPaidWithPicked(event.methodId)
        }
    }
}
