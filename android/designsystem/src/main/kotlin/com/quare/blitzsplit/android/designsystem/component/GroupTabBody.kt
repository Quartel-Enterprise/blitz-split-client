package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.FabPosition
import androidx.compose.ui.graphics.vector.ImageVector

/*
 * Why: what a tab of the group draws under the group's header, which stays put while the tabs swap: its rows in the
 * group's list and the button floating over it, if any. Membros comes from another module, so the type lives here.
 */
class GroupTabBody(
    val listContent: LazyListScope.() -> Unit,
    val floatingButton: GroupTabFloatingButton?,
)

class GroupTabFloatingButton(
    val icon: ImageVector,
    val label: String,
    val position: FabPosition,
    val onClick: () -> Unit,
)
