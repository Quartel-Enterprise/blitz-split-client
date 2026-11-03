package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent

// Why: the buttons that open a field of the form, which the lock while a receipt is read drops (7.1e).
private val lockedOpenings: Set<SplitBillUiEvent> = setOf(
    SplitBillUiEvent.OnGroupClicked,
    SplitBillUiEvent.OnPayerClicked,
    SplitBillUiEvent.OnSeveralPayersClicked,
    SplitBillUiEvent.OnSpentAtOpened,
    SplitBillUiEvent.OnSplitClicked,
    SplitBillUiEvent.OnCategoryClicked,
    SplitBillUiEvent.OnCategoryMapClicked,
)

/*
 * Why: what opens or changes a field of the form, dropped while `SplitBillUiState.isFormLocked`, so nothing typed
 * meanwhile is overwritten by the read; × on "Foto do cupom" and closing the form still work. The category's are here
 * too since the Mac's menu picks, creates and edits one straight from the form, with no picker open, and the payer's
 * since the Mac's pop-up picks one in one go (a pick opens the selection by itself). A new field's events belong here
 * as well.
 */
internal fun SplitBillUiEvent.isLockedWhileReading(): Boolean = this in lockedOpenings ||
    this is SplitBillUiEvent.OnAmountChanged ||
    this is SplitBillUiEvent.OnPayerPicked ||
    this is SplitBillUiEvent.OnDescriptionChanged ||
    this is SplitBillUiEvent.OnSpentAtChanged ||
    this is SplitBillUiEvent.OnCategoryPicked ||
    this is SplitBillUiEvent.OnNewCategoryClicked ||
    this is SplitBillUiEvent.OnEditCategoryClicked
