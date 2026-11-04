package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.SPLIT_BILL_SOURCE
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
import kotlinx.coroutines.flow.StateFlow

/*
 * Why: Rachar conta's Manual events (docs/analytics/README.md). A pick or a toggle is read from the form before it
 * changes, so it counts only when the form takes it; bill_saved is sent once the save succeeds, from the form as
 * Salvar found it.
 */
internal class SplitFormTracking(
    private val uiState: StateFlow<SplitBillUiState>,
    private val trackEvent: TrackEvent,
) {
    fun trackSplitModeSelected(mode: SplitMode) {
        val editor = uiState.value.splitEditor ?: return
        if (editor.mode != mode) trackEvent("split_bill_split_mode_selected", mapOf("mode" to mode))
    }

    fun trackPersonToggled(memberId: String) {
        val row = uiState.value.splitEditor
            ?.rows
            ?.firstOrNull { it.person.id == memberId } ?: return
        trackEvent("split_bill_person_toggled", mapOf("is_included" to !row.isIncluded))
    }

    fun trackEveryoneToggled() {
        val editor = uiState.value.splitEditor ?: return
        trackEvent("split_bill_everyone_toggled", mapOf("is_included" to !editor.isEveryoneIncluded))
    }

    // Why: only while "Usar como padrão do grupo" shows, as it unchecks once it hides.
    fun trackSplitAsDefaultToggled() {
        val state = uiState.value
        if (state.splitEditor == null || !state.offersSplitAsDefault) return
        trackEvent("split_bill_split_as_default_toggled", mapOf("is_checked" to !state.isSplitEditorAsDefaultChecked))
    }

    // Why: the split Pronto took, once it added up (Itens has its own Pronto).
    fun trackSplitConfirmed(editor: SplitEditor) {
        val state = uiState.value
        trackEvent(
            "split_bill_split_confirmed",
            mapOf(
                "mode" to editor.mode,
                "is_split_as_default" to (state.isSplitEditorAsDefaultChecked && state.offersSplitAsDefault),
            ),
        )
    }

    fun trackDeleteAnswered(isConfirmed: Boolean) {
        val state = uiState.value
        if (state.deletion == null || state.isDeleting) return
        val params = mapOf("source" to SPLIT_BILL_SOURCE)
        if (isConfirmed) trackEvent("bill_delete_confirmed", params) else trackEvent("bill_delete_cancelled", params)
    }

    // Why: the form as Salvar found it, which is what was saved.
    fun trackSaved(
        state: SplitBillUiState,
        isNew: Boolean,
        payers: Int,
    ) {
        trackEvent(
            "bill_saved",
            mapOf(
                "is_new" to isNew,
                "split_mode" to state.split.mode,
                "payers" to payers,
                "is_from_receipt" to (state.receipt?.status == ReceiptStatus.READ),
                // Why: an edited bill opens with its category as picked, so it says something only for a new one.
                "is_category_picked" to (isNew && state.isCategoryPicked),
                "is_split_as_default" to (isNew && state.splitAsDefault != null),
            ),
        )
    }
}
