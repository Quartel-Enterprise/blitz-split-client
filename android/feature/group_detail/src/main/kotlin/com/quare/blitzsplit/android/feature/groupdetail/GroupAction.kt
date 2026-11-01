package com.quare.blitzsplit.android.feature.groupdetail

/*
 * Why: Despesas and Saldos draw the same header, tabs, balance card and "Rachar conta"; each screen turns what was
 * tapped there into its own UiEvent.
 */
internal enum class GroupAction {
    BACK,
    INVITE,
    STATS,
    EDIT_GROUP,
    LEAVE_GROUP,
    DELETE_GROUP,
    MEMBERS,
    EXPENSES_TAB,
    BALANCES_TAB,
    SPLIT_BILL,
    SETTLE,
}
