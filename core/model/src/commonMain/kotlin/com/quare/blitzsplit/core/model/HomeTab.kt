package com.quare.blitzsplit.core.model

/*
 * Why: the home's four tabs, Início first, which Android's rail and bottom bar draw on the home, and its rail beside a
 * group's screens drawn alone too. Atividade is no tab: its bell on Início opens it (Pierre's call), which leaves the
 * room iOS's search tab needs at the end of the tab bar.
 */
enum class HomeTab {
    HOME,
    GROUPS,
    BALANCES,
    CONTACTS,
}
