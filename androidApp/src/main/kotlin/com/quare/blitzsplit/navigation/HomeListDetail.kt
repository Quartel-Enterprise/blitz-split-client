package com.quare.blitzsplit.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.EmptyGroupsImage
import com.quare.blitzsplit.android.designsystem.layout.ListDetailLayout
import com.quare.blitzsplit.android.designsystem.layout.ListDetailPaneSpacing
import com.quare.blitzsplit.android.designsystem.layout.LocalInDetailPane
import com.quare.blitzsplit.core.navigation.Route

/*
 * Why: the home's list and detail (5.1, a tablet lying down and an unfolded book): Grupos in the list pane and the
 * group's tab right above it in the detail pane, both entries of the one back stack. Material's list-detail scene
 * claims them only while the window has room for two panes, so a phone and anything upright draw the group over
 * the list as before.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun rememberHomeListDetailStrategy(listDetail: ListDetailLayout?): ListDetailSceneStrategy<Route> {
    val windowDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
    val directive = remember(windowDirective, listDetail) { homeDirectiveOf(windowDirective, listDetail) }
    return rememberListDetailSceneStrategy(directive = directive)
}

/*
 * Why: two panes side by side only where the canvas draws them, never three on the widest tablet nor stacked on a
 * tabletop; the hinge stays out of both, as the window's own directive says.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun homeDirectiveOf(
    windowDirective: PaneScaffoldDirective,
    listDetail: ListDetailLayout?,
): PaneScaffoldDirective = windowDirective.copy(
    maxHorizontalPartitions = if (listDetail != null) 2 else 1,
    horizontalPartitionSpacerSize = if (listDetail != null) ListDetailPaneSpacing else 0.dp,
    maxVerticalPartitions = 1,
    verticalPartitionSpacerSize = 0.dp,
)

// Why: the groups in the list pane, 520dp on a tablet and the half before the hinge on a book.
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun homeListPaneMetadataOf(listDetail: ListDetailLayout?): Map<String, Any> {
    val listPane = ListDetailSceneStrategy.listPane(detailPlaceholder = { GroupPanePlaceholderBox() })
    if (listDetail == null) return listPane
    return listPane + ListDetailSceneStrategy.preferredPaneSize(width = listDetail.listPaneWidth)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val homeDetailPaneMetadata: Map<String, Any> = ListDetailSceneStrategy.detailPane()

// Why: a group's tab tells its layout it is in the pane beside the list, which only a list and detail draws.
@Composable
internal fun InGroupPane(
    listDetail: ListDetailLayout?,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalInDetailPane provides (listDetail != null), content = content)
}

// Why: before a group is tapped, or once it closed, the pane holds the canvas' shapes; it has no words of its own.
@Composable
private fun GroupPanePlaceholderBox() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(contentAlignment = Alignment.Center) {
            EmptyGroupsImage(scale = 1.25f)
        }
    }
}
