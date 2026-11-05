package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.core.model.HomeTab

// Why: M3's NavigationBar height and NavigationRail width, which a screen leaves free until the bar is first measured.
private val estimatedBarExtent = 80.dp

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

// Why: one HomeBarsScaffold's bars, under the back stack entry it draws in.
private class HomeBarsRegistration(
    val owner: Any,
    val entryKey: Any,
    val bars: HomeBars,
)

/*
 * Why: the bars are drawn once, around the navigation, so switching tabs swaps only the content and the selected tab
 * moves within the one bar. The screens in front each say what the bar shows (HomeBarsScaffold); the one highest on the
 * back stack wins, so a tab coming in takes the bar as the one leaving still fades out under it. Each scaffold adds and
 * removes only its own, so an entry drawn twice during a scene change never takes the bars of the copy that stays.
 */
@Stable
internal class HomeBarsHost {
    // Why: in the order the screens came in, so the latest one leads once none of them is on the back stack any more.
    private val registered = mutableStateListOf<HomeBarsRegistration>()

    // Why: the bar's measured height and the rail's width, insets included, each kept apart so one never pads the other.
    private val extents = mutableStateMapOf<HomeBarsKind, Dp>()

    fun extentOf(kind: HomeBarsKind): Dp? = extents[kind]

    fun onMeasured(
        kind: HomeBarsKind,
        extent: Dp,
    ) {
        if (extents[kind] != extent) extents[kind] = extent
    }

    fun put(
        owner: Any,
        entryKey: Any,
        bars: HomeBars,
    ) {
        val registration = HomeBarsRegistration(owner, entryKey, bars)
        val index = registered.indexOfFirst { it.owner === owner }
        if (index == -1) registered.add(registration) else registered[index] = registration
    }

    fun remove(owner: Any) {
        registered.removeAll { it.owner === owner }
    }

    fun barsOf(stackKeys: List<Any>): HomeBars? {
        val keyOnTop = stackKeys.lastOrNull { key -> registered.any { it.entryKey == key } }
        return registered.lastOrNull { keyOnTop == null || it.entryKey == keyOnTop }?.bars
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
                val extent = when (bars.kind) {
                    HomeBarsKind.BOTTOM_BAR -> placeable.height
                    HomeBarsKind.RAIL -> placeable.width
                }
                host.onMeasured(bars.kind, extent.toDp())
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
    if (bars != null) {
        if (host == null || key == null) {
            InlineHomeBarsScaffold(bars, modifier, content)
            return
        }
        val owner = remember { Any() }
        DisposableEffect(host, owner) { onDispose { host.remove(owner) } }
        SideEffect { host.put(owner, key, bars) }
    }
    val kind = bars?.kind
    // Why: until the bar is first measured (the first frame after a cold start), M3's size and the system bar under it.
    val systemBars = WindowInsets.systemBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val estimate by rememberUpdatedState(
        when (kind) {
            HomeBarsKind.BOTTOM_BAR -> estimatedBarExtent + systemBars.calculateBottomPadding()
            HomeBarsKind.RAIL -> estimatedBarExtent + systemBars.calculateStartPadding(layoutDirection)
            null -> 0.dp
        },
    )
    val padding = remember(host, kind) {
        if (host != null && kind != null) HomeBarsPadding(host, kind) { estimate } else PaddingValues()
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
    private val estimate: () -> Dp,
) : PaddingValues {
    private val extent: Dp
        get() = host.extentOf(kind) ?: estimate()

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        if (kind == HomeBarsKind.RAIL && layoutDirection == LayoutDirection.Ltr) extent else 0.dp

    override fun calculateTopPadding(): Dp = 0.dp

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        if (kind == HomeBarsKind.RAIL && layoutDirection == LayoutDirection.Rtl) extent else 0.dp

    override fun calculateBottomPadding(): Dp = if (kind == HomeBarsKind.BOTTOM_BAR) extent else 0.dp
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
