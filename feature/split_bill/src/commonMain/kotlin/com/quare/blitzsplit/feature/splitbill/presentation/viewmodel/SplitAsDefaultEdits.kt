package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.groups.domain.usecase.SaveSplitAsDefault
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitAsDefault
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor

// Why: Dividir opens with the bill's split, "Usar como padrão do grupo" checked while the bill already takes it.
internal fun SplitBillUiState.withSplitOpened(): SplitBillUiState = copy(
    splitEditor = split,
    isSplitEditorAsDefaultChecked = splitAsDefault != null,
)

// Why: "Usar como padrão do grupo" unchecks once it hides, so it never comes back checked by itself.
internal fun SplitBillUiState.withSplitAsDefaultSettled(): SplitBillUiState =
    if (isSplitEditorAsDefaultChecked && !offersSplitAsDefault) copy(isSplitEditorAsDefaultChecked = false) else this

/*
 * Why: "Pronto" picks the bill's split, and "Usar como padrão do grupo" goes with it while checked and shown: for the
 * exception of the category the bill opened with, else the group's own default.
 */
internal fun SplitBillUiState.withSplitConfirmed(editor: SplitEditor): SplitBillUiState {
    val makesDefault = isSplitEditorAsDefaultChecked && offersSplitAsDefault
    return copy(
        split = editor,
        splitEditor = null,
        splitAsDefault = if (makesDefault) SplitAsDefault(defaultSplit?.exceptionCategoryId) else null,
    )
}

/*
 * Why: "Usar como padrão do grupo" goes with the bill. The bill is saved by then, so a default that fails to save
 * leaves it as it was rather than asking to save the bill again, which would make a second one.
 */
internal suspend fun SaveSplitAsDefault.saveOf(state: SplitBillUiState) {
    val asDefault = state.splitAsDefault ?: return
    val mode = state.split.mode.defaultMode ?: return
    invoke(
        groupId = state.groupId,
        categoryId = asDefault.exceptionCategoryId,
        mode = mode,
        weights = state.split.entries.associate { it.memberId to it.value },
    )
}
