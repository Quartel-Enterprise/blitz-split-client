package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/*
 * Why: whether a bill opens as a dialog over the screen under it (anywhere wider than a phone), where nothing flies, so
 * a list of bills there (the group's Expenses) sets up no shared element at all.
 */
val LocalBillOpensAsDialog = staticCompositionLocalOf { false }

// Why: the modifiers a bill's category badge, title, total and share take to fly between its row and its screen.
@Immutable
class BillSharedModifiers(
    val category: Modifier,
    val title: Modifier,
    val total: Modifier,
    // Why: the row's "-R$ 98,26", which lands on "Your share"'s amount.
    val share: Modifier,
    /*
     * Why: the group's square in the bill's top bar: from Home's and "All expenses"' rows' small square, and from the
     * group's own top bar when the bill opened from the group's Expenses (beta feedback of 2026-11-16).
     */
    val group: Modifier,
    // Why: the group's name beside it, which flies from and back to the group's top bar; the group's Expenses only.
    val groupName: Modifier,
    // Why: the row's payer faces, which land on "Larissa paid"'s faces (beta feedback of 2026-11-16).
    val payers: Modifier,
    // Why: the group's Expenses' "19:22", which lands on the bill's date and time (beta feedback of 2026-11-16).
    val time: Modifier,
) {
    companion object {
        val None = BillSharedModifiers(
            category = Modifier,
            title = Modifier,
            total = Modifier,
            share = Modifier,
            group = Modifier,
            groupName = Modifier,
            payers = Modifier,
            time = Modifier,
        )
    }
}

/*
 * Why: a bill's category badge, title, total and share move from Home's, "All expenses"' or the group's Expenses' row
 * into the bill's screen and back (beta feedback of 2026-11-15), as a group's avatar and name do, the group's square
 * from Home's rows into the top bar, the payers' faces from Home's rows and the time from the group's Expenses' rows
 * (beta feedback of 2026-11-16). Keyed by the bill and the screen it opened from (`origin`, the screen's own unless a
 * list inside it says otherwise, as the group's Expenses do under the group's header); where the bill opens as a dialog
 * over the group, in previews and in a bill's link page nothing is shared. Opened from the group, the top bar's square
 * and name are keyed by the group instead, as the group's own top bar knows no bill (`rememberGroupBillSharedModifiers`).
 */
@Composable
fun rememberBillSharedModifiers(
    billId: String?,
    origin: SharedOrigin? = LocalSharedOrigin.current,
    groupId: String? = null,
): BillSharedModifiers {
    val scopes = rememberSharedScopes(billId, origin) ?: return BillSharedModifiers.None
    val groupKey = "$origin-$groupId".takeIf { origin == SharedOrigin.GROUP && groupId != null }
    return BillSharedModifiers(
        category = scopes.element("bill-category-${scopes.key}"),
        title = scopes.scaled("bill-title-${scopes.key}"),
        total = scopes.scaled("bill-total-${scopes.key}"),
        share = scopes.scaled("bill-share-${scopes.key}"),
        group = scopes.element(groupKey?.let { "group-bill-avatar-$it" } ?: "bill-group-${scopes.key}"),
        groupName = groupKey?.let { scopes.scaled("group-bill-name-$it") } ?: Modifier,
        payers = scopes.scaled("bill-payers-${scopes.key}"),
        time = scopes.scaled("bill-time-${scopes.key}"),
    )
}

// Why: the group's square and name in the group's top bar, which fly into the top bar of a bill opened from it.
@Immutable
class GroupBillSharedModifiers(
    val avatar: Modifier,
    val name: Modifier,
) {
    companion object {
        val None = GroupBillSharedModifiers(
            avatar = Modifier,
            name = Modifier,
        )
    }
}

/*
 * Why: the group's square and name go from the group's top bar into the top bar of a bill opened from its Expenses and
 * back (beta feedback of 2026-11-16). Keyed by the group: one bill opens at a time over it. Nothing where the bill
 * opens as a dialog over the group.
 */
@Composable
fun rememberGroupBillSharedModifiers(groupId: String?): GroupBillSharedModifiers {
    val origin = SharedOrigin.GROUP.takeUnless { LocalBillOpensAsDialog.current }
    val scopes = rememberSharedScopes(groupId, origin) ?: return GroupBillSharedModifiers.None
    return GroupBillSharedModifiers(
        avatar = scopes.element("group-bill-avatar-${scopes.key}"),
        name = scopes.scaled("group-bill-name-${scopes.key}"),
    )
}
