package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.core.model.HomeTab

/*
 * Why: what the home's one bar or rail shows while a screen is in front: the tab selected (null for Search), where each
 * tab goes, "Buscar" (null hides it) and the rail's header ("Rachar conta" on Início and Grupos). Each screen sends its
 * own ViewModel's events, so the navigation stays in the shared code.
 */
@Stable
class HomeBars(
    val kind: HomeBarsKind,
    val selected: HomeTab?,
    val onTabClick: (HomeTab) -> Unit,
    val onSearchClick: (() -> Unit)? = null,
    // Why: beside a screen the selected tab opened (a group's), that tab goes back to it.
    val reselects: Boolean = false,
    val railHeader: @Composable (isShort: Boolean) -> Unit = {},
)

/*
 * Why: the bars are drawn once, around the navigation, so switching tabs swaps only the content and the selected tab
 * moves within the one bar. The screens in front each say what the bar shows (HomeBarsScaffold); the one highest on the
 * back stack wins, so a tab coming in takes the bar as the one leaving still fades out under it.
 */
@Stable
internal class HomeBarsHost {
    // Why: in the order the screens came in, so the latest one leads once none of them is on the back stack any more.
    private val registered = mutableStateListOf<Pair<Any, HomeBars>>()

    // Why: the bar's measured height or the rail's width, insets included, which the screens leave free for it.
    var extent: Dp by mutableStateOf(0.dp)

    fun put(
        key: Any,
        bars: HomeBars,
    ) {
        val index = registered.indexOfFirst { (registeredKey, _) -> registeredKey == key }
        if (index == -1) registered.add(key to bars) else registered[index] = key to bars
    }

    fun remove(key: Any) {
        registered.removeAll { (registeredKey, _) -> registeredKey == key }
    }

    fun barsOf(stackKeys: List<Any>): HomeBars? {
        val keyOnTop = stackKeys.lastOrNull { key -> registered.any { (registeredKey, _) -> registeredKey == key } }
        return registered.lastOrNull { (key, _) -> keyOnTop == null || key == keyOnTop }?.second
    }
}

internal val LocalHomeBarsHost = staticCompositionLocalOf<HomeBarsHost?> { null }

// Why: the key of the back stack entry a screen draws in, which the navigation provides to each entry.
val LocalHomeBarsEntryKey = staticCompositionLocalOf<Any?> { null }

/*
 * Why: draws the bar or rail of the screen in front under the navigation (content), which each screen's HomeBarsScaffold
 * leaves free. Under it, so a screen without the bars covers them as it comes in, and they go once no screen with
 * them is left. stackKeys are the back stack's entry keys, bottom to top.
 */
@Composable
fun HomeBarsLayout(
    stackKeys: List<Any>,
    content: @Composable () -> Unit,
) {
    val host = remember { HomeBarsHost() }
    val bars = host.barsOf(stackKeys)
    Box(modifier = Modifier.fillMaxSize()) {
        if (bars != null) {
            val measureExtent = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                host.extent = when (bars.kind) {
                    HomeBarsKind.BOTTOM_BAR -> placeable.height
                    HomeBarsKind.RAIL -> placeable.width
                }.toDp()
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            when (bars.kind) {
                HomeBarsKind.BOTTOM_BAR -> HomeNavigationBar(
                    selected = bars.selected,
                    onTabClick = bars.onTabClick,
                    onSearchClick = bars.onSearchClick,
                    modifier = Modifier.align(Alignment.BottomCenter).then(measureExtent),
                )

                HomeBarsKind.RAIL -> HomeNavigationRail(
                    selected = bars.selected,
                    onTabClick = bars.onTabClick,
                    onSearchClick = bars.onSearchClick,
                    reselects = bars.reselects,
                    /*
                     * Why: "Rachar conta" comes and goes with the screen (Início and Grupos have it, a group doesn't),
                     * so the tabs under it slide to their place instead of jumping.
                     */
                    header = { isShort -> Box(modifier = Modifier.animateContentSize()) { bars.railHeader(isShort) } },
                    modifier = Modifier.align(Alignment.CenterStart).then(measureExtent),
                )
            }
        }
        CompositionLocalProvider(LocalHomeBarsHost provides host, content = content)
    }
}

/*
 * Why: a screen with the home's bars (null: none) leaves their place free, its insets included, and tells the bar
 * what to show. Outside the navigation (a preview) it draws them itself, around content. modifier goes on the content
 * beside the bars, never over them: a screen's paneTitle spanning the window would hide the bar under it from TalkBack,
 * which leaves out what a node drawn above covers.
 */
@Composable
fun HomeBarsScaffold(
    bars: HomeBars?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val host = LocalHomeBarsHost.current
    val key = LocalHomeBarsEntryKey.current
    if (bars != null && (host == null || key == null)) {
        InlineHomeBarsScaffold(bars, modifier, content)
        return
    }
    if (bars != null && host != null && key != null) {
        DisposableEffect(host, key) { onDispose { host.remove(key) } }
        SideEffect { host.put(key, bars) }
    }
    val kind = bars?.kind
    val padding = remember(host, kind) {
        if (host != null && kind != null) HomeBarsPadding(host, kind) else PaddingValues()
    }
    // Why: one call site whether the bars show or not, so a rail coming or going keeps the screen's state.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding)
            .then(modifier),
    ) { content() }
}

// Why: read as the screen is measured, so the bar measured just before it in the same pass already counts.
private class HomeBarsPadding(
    private val host: HomeBarsHost,
    private val kind: HomeBarsKind,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        if (kind == HomeBarsKind.RAIL && layoutDirection == LayoutDirection.Ltr) host.extent else 0.dp

    override fun calculateTopPadding(): Dp = 0.dp

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        if (kind == HomeBarsKind.RAIL && layoutDirection == LayoutDirection.Rtl) host.extent else 0.dp

    override fun calculateBottomPadding(): Dp = if (kind == HomeBarsKind.BOTTOM_BAR) host.extent else 0.dp
}

// Why: a preview draws the screen alone, so its bars come with it, as the canvas draws them.
@Composable
private fun InlineHomeBarsScaffold(
    bars: HomeBars,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    when (bars.kind) {
        HomeBarsKind.BOTTOM_BAR -> Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .then(modifier),
            ) { content() }
            HomeNavigationBar(
                selected = bars.selected,
                onTabClick = bars.onTabClick,
                onSearchClick = bars.onSearchClick,
            )
        }

        HomeBarsKind.RAIL -> Row(modifier = Modifier.fillMaxSize()) {
            HomeNavigationRail(
                selected = bars.selected,
                onTabClick = bars.onTabClick,
                onSearchClick = bars.onSearchClick,
                reselects = bars.reselects,
                header = bars.railHeader,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                    .then(modifier),
            ) { content() }
        }
    }
}
