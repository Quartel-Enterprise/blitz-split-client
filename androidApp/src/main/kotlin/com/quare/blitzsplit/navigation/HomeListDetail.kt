package com.quare.blitzsplit.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.quare.blitzsplit.android.designsystem.component.EmptyGroupsImage
import com.quare.blitzsplit.android.designsystem.layout.ListDetailLayout
import com.quare.blitzsplit.android.designsystem.layout.LocalInDetailPane
import com.quare.blitzsplit.android.feature.groups.NoGroupOpenPane
import com.quare.blitzsplit.core.navigation.Route

/*
 * Why: the home's list and detail (5.1, a tablet lying down and an unfolded book): Grupos in the list pane and the
 * group's tab right above it in the detail pane, both entries of the one back stack. Material's list-detail scene
 * claims them only while the window has room for two panes, so a phone and anything upright draw the group over
 * the list as before.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun rememberHomeListDetailStrategy(listDetail: ListDetailLayout?): SceneStrategy<Route> {
    val windowDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
    val directive = remember(windowDirective, listDetail) { homeDirectiveOf(windowDirective, listDetail) }
    val listDetailStrategy = rememberListDetailSceneStrategy<Route>(directive = directive)
    return remember(listDetailStrategy) { BesideGroupsSceneStrategy(listDetailStrategy) }
}

/*
 * Why: a group's tab opened from Início has no Grupos under it, and Material's scene would still draw it beside an
 * empty list pane. Only the group's tabs right above Grupos go in the list and detail; anything else covers the
 * window, as the group does over Início.
 */
private class BesideGroupsSceneStrategy(
    private val delegate: SceneStrategy<Route>,
) : SceneStrategy<Route> {
    override fun SceneStrategyScope<Route>.calculateScene(entries: List<NavEntry<Route>>): Scene<Route>? {
        val underTabs = entries.dropLastWhile { it.metadata[HomeDetailPaneKey] == true }.lastOrNull()
        if (underTabs == null || underTabs.metadata[HomeListPaneKey] != true) return null
        return with(delegate) { calculateScene(entries) }
    }
}

// Why: marks Grupos and the group's tabs, telling a group above Grupos from one above Início.
private object HomeListPaneKey : NavMetadataKey<Boolean>

private object HomeDetailPaneKey : NavMetadataKey<Boolean>

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
    horizontalPartitionSpacerSize = listDetail?.paneSpacing ?: 0.dp,
    maxVerticalPartitions = 1,
    verticalPartitionSpacerSize = 0.dp,
)

/*
 * Why: a tablet lying down draws the empty home in one pane, the detail only once the first group exists; a book
 * keeps its halves, the empty detail in the one after the hinge (decision 5, each size's board).
 */
internal fun homeListDetailOf(
    listDetail: ListDetailLayout?,
    isHomeEmpty: Boolean,
): ListDetailLayout? = listDetail?.takeUnless { isHomeEmpty && !it.isBook }

// Why: the groups in the list pane, 520dp on a tablet and the half before the hinge on a book.
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun homeListPaneMetadataOf(
    listDetail: ListDetailLayout?,
    isHomeEmpty: Boolean,
): Map<String, Any> {
    val listPane = ListDetailSceneStrategy.listPane(
        detailPlaceholder = {
            if (isHomeEmpty) NoGroupOpenPane() else GroupPanePlaceholderBox()
        },
    ) + metadata { put(HomeListPaneKey, true) }
    if (listDetail == null) return listPane
    return listPane + ListDetailSceneStrategy.preferredPaneSize(width = listDetail.listPaneWidth)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val homeDetailPaneMetadata: Map<String, Any> =
    ListDetailSceneStrategy.detailPane() + metadata { put(HomeDetailPaneKey, true) }

/*
 * Why: a group's tab tells its layout it is in the pane beside the list. Only Material's list-detail scene says so: it
 * provides its scope to the panes it draws, and only while the list shows beside them, so a tab the scene leaves out
 * (drawn alone, full screen) keeps its back arrow and "Rachar conta" whatever the window's size.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun InGroupPane(content: @Composable () -> Unit) {
    val sceneScope = LocalListDetailSceneScope.current
    val isBesideList = sceneScope != null &&
        sceneScope.scaffoldTransitionScope.scaffoldStateTransition.targetState[ListDetailPaneScaffoldRole.List] ==
        PaneAdaptedValue.Expanded
    CompositionLocalProvider(LocalInDetailPane provides isBesideList, content = content)
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
