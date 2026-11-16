package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.graphics.vector.ImageVector

/*
 * Why: what a tab of the group draws under the group's header, which stays put while the tabs swap: its rows in the
 * group's list and its main button, if any ("Rachar conta" on every tab but the empty Despesas), floating over the list
 * on a phone upright and in the top bar anywhere wider.
 */
internal class GroupTabBody(
    val listContent: LazyListScope.() -> Unit,
    val splitBillButton: GroupTabFloatingButton?,
)

internal class GroupTabFloatingButton(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)
