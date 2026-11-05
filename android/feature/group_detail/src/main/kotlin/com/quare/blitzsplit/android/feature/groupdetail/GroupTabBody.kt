package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.graphics.vector.ImageVector

/*
 * Why: what a tab of the group draws under the group's header, which stays put while the tabs swap: its rows in the
 * group's list and the button floating over it, if any ("Rachar conta" on every tab but the empty Despesas).
 */
internal class GroupTabBody(
    val listContent: LazyListScope.() -> Unit,
    val floatingButton: GroupTabFloatingButton?,
)

internal class GroupTabFloatingButton(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)
