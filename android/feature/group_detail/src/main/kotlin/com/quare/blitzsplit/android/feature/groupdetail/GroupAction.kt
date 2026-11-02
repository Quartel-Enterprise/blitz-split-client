package com.quare.blitzsplit.android.feature.groupdetail

// Why: what was tapped on the group's header, its tabs and "Rachar conta", turned into the screen's UiEvent.
internal enum class GroupAction {
    BACK,
    INVITE,
    STATS,
    PIN,
    EDIT_GROUP,
    LEAVE_GROUP,
    DELETE_GROUP,
    MEMBERS,
    EXPENSES_TAB,
    BALANCES_TAB,
    SPLIT_BILL,
    SETTLE,
}
