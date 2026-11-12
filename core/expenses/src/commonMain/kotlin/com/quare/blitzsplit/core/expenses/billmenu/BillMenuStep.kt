package com.quare.blitzsplit.core.expenses.billmenu

/*
 * Why: what a bill's ⋮ did, which each screen sends to Analytics with its source; the screen names each event itself,
 * so the analytics catalog finds the call that sends it next to the Manual that declares it.
 */
enum class BillMenuStep {
    EDIT_OPENED,
    DUPLICATE_OPENED,
    SHARE_OPENED,
    DELETE_ASKED,
    DELETE_CONFIRMED,
    DELETE_CANCELLED,
}
