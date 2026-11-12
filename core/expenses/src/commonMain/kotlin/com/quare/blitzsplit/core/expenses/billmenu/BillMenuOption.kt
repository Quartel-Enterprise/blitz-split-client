package com.quare.blitzsplit.core.expenses.billmenu

// Why: what a bill's ⋮ opens without opening the bill first; "Apagar" asks first, so it is the controller's own.
enum class BillMenuOption(
    // Why: what Analytics hears once its screen opened.
    val openedStep: BillMenuStep,
    // Why: a bill in a currency this version doesn't know is read-only; the bill's own screen says so.
    val needsKnownCurrency: Boolean,
) {
    EDIT(openedStep = BillMenuStep.EDIT_OPENED, needsKnownCurrency = true),
    DUPLICATE(openedStep = BillMenuStep.DUPLICATE_OPENED, needsKnownCurrency = true),
    SHARE(openedStep = BillMenuStep.SHARE_OPENED, needsKnownCurrency = false),
}
