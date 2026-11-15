package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

// Why: the modifiers a bill's category badge, title, total and share take to fly between its row and its screen.
@Immutable
class BillSharedModifiers(
    val category: Modifier,
    val title: Modifier,
    val total: Modifier,
    // Why: the row's "-R$ 98,26", which lands on "Your share"'s amount.
    val share: Modifier,
) {
    companion object {
        val None = BillSharedModifiers(
            category = Modifier,
            title = Modifier,
            total = Modifier,
            share = Modifier,
        )
    }
}

/*
 * Why: a bill's category badge, title, total and share move from Home's or "All expenses"' row into the bill's screen
 * and back (beta feedback of 2026-11-15), as a group's avatar and name do. Keyed by the bill and the screen it opened
 * from; where the bill opens as a dialog over the group, in previews and in a bill's link page nothing is shared.
 */
@Composable
fun rememberBillSharedModifiers(billId: String?): BillSharedModifiers {
    val scopes = rememberSharedScopes(billId) ?: return BillSharedModifiers.None
    return BillSharedModifiers(
        category = scopes.element("bill-category-${scopes.key}"),
        title = scopes.text("bill-title-${scopes.key}"),
        total = scopes.text("bill-total-${scopes.key}"),
        share = scopes.text("bill-share-${scopes.key}"),
    )
}
