package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.GroupFace

/*
 * Why: a person "All expenses"' Who paid offers (Pierre's call, 2026-11-24): this device's user first, whom each UI
 * calls "You", then everyone of the groups, once each, as the global Balances counts people (PeopleBalances.keyOf).
 */
data class FilterPayerOption(
    // Why: what AllExpensesFilters.payerKeys holds: PAYER_KEY_ME for the user, PeopleBalances.keyOf for anyone else.
    val key: String,
    // Why: as the first group in the device's order names them; the user's own name, which no UI writes.
    val name: String,
    val isMe: Boolean,
    // Why: drawn as a member's face everywhere (faceOf): one initial, their color, their photo over it.
    val face: GroupFace,
)

// Why: the user's key in every group, also before the first sync gives them an account (and so a userId).
const val PAYER_KEY_ME = "me"
