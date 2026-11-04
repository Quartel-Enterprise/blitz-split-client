package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.SPLIT_BILL_SOURCE
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.coroutines.flow.StateFlow

/*
 * Why: Rachar conta's Manual events (docs/analytics/README.md), each read from the form before it changes, so a
 * pick or a confirmation counts only when the form takes it, never a late tap on a sheet already closed.
 */
internal class SplitFormTracking(
    private val uiState: StateFlow<SplitBillUiState>,
    private val trackEvent: TrackEvent,
) {
    // Why: as GroupPicking.confirm takes it: the group still listed, the bill not saving.
    fun trackGroupConfirmed() {
        val state = uiState.value
        val picked = state.groupSelection?.groupId ?: return
        if (state.isSaving || state.groups.none { it.id == picked }) return
        trackEvent("split_bill_group_confirmed", mapOf("is_another" to (picked != state.groupId)))
    }

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

    // Why: as Pronto takes it: a split that adds up, Itens having its own Pronto.
    fun trackSplitConfirmed() {
        val state = uiState.value
        val editor = state.splitEditor?.takeIf { it.addsUp && it.mode != SplitMode.ITEMS } ?: return
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
                "is_category_picked" to state.isCategoryPicked,
                "is_split_as_default" to (isNew && state.splitAsDefault != null),
            ),
        )
    }
}
