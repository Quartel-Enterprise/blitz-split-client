package com.quare.blitzsplit.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import com.quare.blitzsplit.android.designsystem.component.LocalHomeBarsEntryKey
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.navigation.Route

// Why: a fixed list, so each one's rememberDecoratedNavEntries keeps its place in the composition.
private val keptTabs: List<Route> = listOf(Route.Groups, Route.GlobalBalances(BalancesFilter.ALL), Route.Contacts)

// Why: tells each screen which back stack entry it draws in, so the home's one bar shows the one highest on the stack.
internal val homeBarsEntryDecorator: NavEntryDecorator<Route> = NavEntryDecorator(
    decorate = { entry ->
        CompositionLocalProvider(LocalHomeBarsEntryKey provides entry.contentKey) { entry.Content() }
    },
)

/*
 * Why: the tabs take each other's place right above Início ([Home, Groups], [Home, Contacts]), so a tab leaving the
 * back stack used to clear its ViewModel and saved state, and each switch came back on a skeleton, which flashed under
 * the tab's fade (the beta's flicker between Grupos and Saldos). The one back stack stays the source of truth; only
 * Android's decoration of it changes: Grupos, Saldos and Contatos are decorated on their own, where they stay while
 * another tab takes their place, as Apple keeps its tabs. Saldos is kept as its tab opens it (every filter shown,
 * BalancesFilter.ALL), whose ViewModel goes back to that filter each time it comes back; Saldos opened already filtered
 * by Início's "A receber" or "A pagar" is a route of its own, decorated as before. Anything else is decorated as
 * before, cleared once popped.
 */
@Composable
internal fun rememberAppNavEntries(
    backStack: List<Route>,
    decorators: List<NavEntryDecorator<Route>>,
    entryProvider: (Route) -> NavEntry<Route>,
): List<NavEntry<Route>> {
    val isHomeRoot = backStack.firstOrNull() == Route.Home
    val tab = backStack.getOrNull(1)?.takeIf { isHomeRoot && it in keptTabs }
    // Why: built again whenever the back stack changes, as NavDisplay does, so each entry reads the window's layout then.
    val mainEntries = rememberDecoratedNavEntries(
        entries = remember(backStack) {
            backStack.filterIndexed { index, _ -> tab == null || index != 1 }.map(entryProvider)
        },
        entryDecorators = decorators,
    )
    // Why: kept only while Início is the root, so leaving the home (the account deleted, a restore) clears them too.
    val keptEntries = keptTabs.associateWith { kept ->
        rememberDecoratedNavEntries(
            entries = remember(backStack) { if (isHomeRoot) listOf(entryProvider(kept)) else emptyList() },
            entryDecorators = decorators,
        ).singleOrNull()
    }
    val tabEntry = keptEntries[tab] ?: return mainEntries
    return listOf(mainEntries.first(), tabEntry) + mainEntries.drop(1)
}
