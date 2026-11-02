package com.quare.blitzsplit.core.model

/*
 * Why: the group screen's three tabs, Despesas first. Picking one only swaps what is under the group's header, never
 * the back stack; a route only says which one the screen opens on.
 */
enum class GroupTab {
    EXPENSES,
    BALANCES,
    MEMBERS,
}
