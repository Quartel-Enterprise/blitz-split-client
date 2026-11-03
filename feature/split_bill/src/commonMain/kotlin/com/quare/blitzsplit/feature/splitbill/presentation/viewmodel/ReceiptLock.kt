package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

// Why: closing a sheet or a dialog changes no field, so a UI catching up with editors the read closed still gets through.
private val dismissals: Set<SplitBillUiEvent> = setOf(
    SplitBillUiEvent.OnGroupDismissed,
    SplitBillUiEvent.OnPayerDismissed,
    SplitBillUiEvent.OnSeveralPayersDismissed,
    SplitBillUiEvent.OnSplitDismissed,
    SplitBillUiEvent.OnItemsDismissed,
    SplitBillUiEvent.OnItemDismissed,
    SplitBillUiEvent.OnItemDeleteDismissed,
    SplitBillUiEvent.OnFeeDismissed,
    SplitBillUiEvent.OnCategoryDismissed,
    SplitBillUiEvent.OnCategoryFormDismissed,
    SplitBillUiEvent.OnCategoryDeleteDismissed,
    SplitBillUiEvent.OnDeleteDismissed,
)

/*
 * Why: while `SplitBillUiState.isFormLocked` only the receipt's own events (× on "Foto do cupom" cancels the read),
 * closing the form, Salvar (which waits for the read anyway) and the dismissals get through; every other event, a
 * field added later included, is dropped, so nothing typed meanwhile is overwritten by the read (7.1e).
 */
internal fun SplitBillUiEvent.isAllowedWhileReading(): Boolean = this is SplitBillUiEvent.Receipt ||
    this == SplitBillUiEvent.OnCloseClicked ||
    this == SplitBillUiEvent.OnSaveClicked ||
    this in dismissals

/*
 * Why: a photo picked from a file arrives a while later (the Mac's and the web's), so an editor opened meanwhile would
 * stay open under the lock taking no taps; the read closes it, with what was marked in it unsaved.
 */
internal fun SplitBillUiState.withEditorsClosed(): SplitBillUiState = copy(
    groupSelection = null,
    payerSelection = null,
    payerAmounts = null,
    splitEditor = null,
    itemsEditor = null,
    itemEditor = null,
    itemDeletion = null,
    feeEditor = null,
    categoryPicker = null,
    categoryForm = null,
    categoryDeletion = null,
)
