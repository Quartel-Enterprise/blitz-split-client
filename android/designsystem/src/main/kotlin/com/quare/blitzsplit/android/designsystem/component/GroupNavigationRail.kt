package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.LocalInDetailPane
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.core.model.HomeTab

/*
 * Why: a group's screens (Despesas, Saldos, Membros, Estatísticas) drawn alone anywhere wider than a phone upright (a
 * phone lying down, a tablet or a foldable upright, Estatísticas over a whole tablet or book) keep the home's rail
 * with the tab they were opened from selected, as the canvas draws them ("No Android o trilho fica"), without "Rachar
 * conta": the group has its own. That tab goes back to it, as the others open theirs. Beside the home's list the
 * list's rail is already there, and the tabletop draws none. content gets whether the rail shows, so its Scaffold
 * leaves out the start inset the rail already takes (toGroupRailInsets).
 */
@Composable
fun WithGroupNavigationRail(
    selected: HomeTab,
    onTabClick: (HomeTab) -> Unit,
    // Why: Atividade's unread badge, as on every home tab.
    activityBadgeCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable (showsRail: Boolean) -> Unit,
) {
    val showsRail = !LocalInDetailPane.current &&
        rememberDeviceLayout().usesNavigationRail &&
        rememberFoldLayout().formFactor != FormFactor.Tabletop
    Row(modifier = modifier.fillMaxSize()) {
        if (showsRail) {
            HomeNavigationRail(
                selected = selected,
                onTabClick = onTabClick,
                activityBadgeCount = activityBadgeCount,
                reselects = true,
            )
        }
        Box(modifier = Modifier.weight(1f)) { content(showsRail) }
    }
}

// Why: beside the rail, the screen's own insets without the start the rail takes.
fun WindowInsets.toGroupRailInsets(showsRail: Boolean): WindowInsets = if (showsRail) {
    only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
} else {
    this
}
