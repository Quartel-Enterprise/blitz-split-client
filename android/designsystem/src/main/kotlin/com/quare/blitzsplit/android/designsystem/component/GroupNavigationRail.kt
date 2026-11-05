package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.LocalInDetailPane
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.core.model.HomeTab

/*
 * Why: a group's screens (Despesas, Saldos, Membros, Estatísticas) drawn alone anywhere wider than a phone upright (a
 * phone lying down, a tablet or a foldable upright, Estatísticas over a whole book) keep the home's rail
 * with the tab they were opened from selected, as the canvas draws them ("No Android o trilho fica"), without "Rachar
 * conta": the group has its own. That tab goes back to it, as the others open theirs. Beside the home's list the
 * list's rail is already there, and the tabletop draws none. The rail is the home's one (HomeBarsScaffold), so going
 * between a tab and a group swaps only what is beside it.
 */
@Composable
fun WithGroupNavigationRail(
    selected: HomeTab,
    onTabClick: (HomeTab) -> Unit,
    // Why: "Buscar", the rail's last item as beside the home's tabs, which opens the search over the tab underneath.
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val showsRail = !LocalInDetailPane.current && rememberUsesGroupRail()
    HomeBarsScaffold(
        bars = if (showsRail) {
            HomeBars(
                kind = HomeBarsKind.RAIL,
                selected = selected,
                onTabClick = onTabClick,
                onSearchClick = onSearchClick,
                reselects = true,
            )
        } else {
            null
        },
        modifier = modifier,
        content = content,
    )
}

// Why: the window draws the home's rail beside a group's screens drawn alone, except on a tabletop.
@Composable
private fun rememberUsesGroupRail(): Boolean =
    rememberDeviceLayout().usesNavigationRail && rememberFoldLayout().formFactor != FormFactor.Tabletop
