package com.quare.blitzsplit.android.feature.groups

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.HomeBars
import com.quare.blitzsplit.android.designsystem.component.HomeBarsKind
import com.quare.blitzsplit.android.designsystem.component.HomeBarsScaffold
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DeviceLayout
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearch
import com.quare.blitzsplit.feature.groups.presentation.model.SearchUiEvent
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.SearchViewModel
import org.koin.compose.viewmodel.koinViewModel

/*
 * Why: "Buscar" (5.2) over Início, Saldos or Contatos (Route.Search), drawn as Grupos draws its own: the search bar in
 * the top bar's place and the results under it, the home's bar or rail around them with Search, their last item,
 * selected; a phone lying down puts the field and the scopes in a 320 dp column on the left and the results on the
 * right. Back closes it, back to the tab, as its tab does; any other tab takes its place.
 */
@Composable
fun SearchScreen(viewModel: SearchViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { viewModel.onEvent(SearchUiEvent.OnClosed) }
    SearchContent(
        search = state.search,
        onEvent = viewModel::onEvent,
    )
}

@Composable
internal fun SearchContent(
    search: GroupsSearch,
    onEvent: (SearchUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.groups_search)
    val onTabClick: (HomeTab) -> Unit = { tab -> onEvent(SearchUiEvent.OnHomeTabClicked(tab)) }
    AdaptiveScreen(modifier = modifier.fillMaxSize()) { layout ->
        val usesSideColumn = layout == DeviceLayout.PhoneLandscape
        HomeBarsScaffold(
            bars = HomeBars(
                kind = if (layout.usesNavigationRail) HomeBarsKind.RAIL else HomeBarsKind.BOTTOM_BAR,
                selected = null,
                onTabClick = onTabClick,
            ),
            // Why: the pane's title on the content beside the bars, which TalkBack then still reaches.
            modifier = Modifier.semantics { paneTitle = title },
        ) {
            SearchScaffold(
                search = search,
                onEvent = onEvent,
                layout = layout,
                usesSideColumn = usesSideColumn,
            )
        }
    }
}

@Composable
private fun SearchScaffold(
    search: GroupsSearch,
    onEvent: (SearchUiEvent) -> Unit,
    layout: DeviceLayout,
    usesSideColumn: Boolean,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!usesSideColumn) {
                GroupsSearchBar(
                    search = search,
                    onEvent = onEvent,
                    layout = layout,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        val contentModifier = Modifier
            .padding(padding)
            .consumeWindowInsets(padding)
        if (usesSideColumn) {
            Row(
                modifier = contentModifier
                    .fillMaxSize()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 12.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                GroupsSearchHeader(
                    search = search,
                    onEvent = onEvent,
                    modifier = Modifier
                        .width(sideColumnWidth)
                        .fillMaxHeight(),
                )
                GroupsSearchPane(
                    search = search,
                    onEvent = onEvent,
                    layout = layout,
                    modifier = Modifier.weight(1f),
                    horizontalPadding = 0.dp,
                )
            }
        } else {
            GroupsSearchPane(
                search = search,
                onEvent = onEvent,
                layout = layout,
                modifier = contentModifier,
            )
        }
    }
}
