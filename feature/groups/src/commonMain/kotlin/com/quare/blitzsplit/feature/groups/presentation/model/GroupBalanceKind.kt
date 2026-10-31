package com.quare.blitzsplit.feature.groups.presentation.model

/*
 * Why: the word after the balance of a group's row. EVERYONE_SETTLED goes to "Quitados" ("Quitado"); the others stay
 * in "Em aberto": "a receber", "você deve", "você está quitado" while others still owe, or "sem despesas".
 */
enum class GroupBalanceKind {
    TO_RECEIVE,
    YOU_OWE,
    YOU_ARE_SETTLED,
    NO_EXPENSES,
    EVERYONE_SETTLED,
}
