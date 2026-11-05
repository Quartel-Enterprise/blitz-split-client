package com.quare.blitzsplit.android.feature.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DeviceLayout
import com.quare.blitzsplit.android.designsystem.layout.ListDetailLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberListDetailLayout
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityUiEvent
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityUiState

private val columnMaxWidth = 640.dp
private val sideColumnWidth = 320.dp

/*
 * Why: Atividade, which Início's bell opens over it (no tab anymore, Pierre's call), titled like the canvas with back on
 * the left. Upright, one column; a phone lying down puts the tools on the left and the feed on the right; a tablet
 * lying down and an unfolded book list the feed beside the item picked, nothing on the hinge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityContent(
    state: ActivityUiState,
    onEvent: (ActivityUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(R.string.activity_title)
    val back = stringResource(R.string.activity_back)
    val listDetail = rememberListDetailLayout()
    val dates = rememberDateTimeTextFormatter()
    LaunchedEffect(listDetail != null) { onEvent(ActivityUiEvent.OnDetailPaneChanged(listDetail != null)) }
    AdaptiveScreen(modifier = modifier.fillMaxSize().semantics { paneTitle = title }) { layout ->
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(ActivityUiEvent.OnBackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = back,
                            )
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets.safeDrawing,
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(ActivityUiEvent.OnRefresh) },
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            ) {
                if (listDetail != null) {
                    ActivityListDetailLayout(
                        state = state,
                        listDetail = listDetail,
                        dates = dates,
                        onEvent = onEvent,
                    )
                } else {
                    ActivityPane(
                        state = state,
                        layout = layout,
                        dates = dates,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityPane(
    state: ActivityUiState,
    layout: DeviceLayout,
    dates: DateTimeTextFormatter,
    onEvent: (ActivityUiEvent) -> Unit,
) {
    if (layout.usesTwoColumns) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActivityToolsSection(
                state = state,
                onEvent = onEvent,
                modifier = Modifier
                    .widthIn(max = sideColumnWidth)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        end = 8.dp,
                        top = 4.dp,
                    ),
            )
            FeedList(
                state = state,
                dates = dates,
                selectedId = null,
                onEvent = onEvent,
                modifier = Modifier.weight(1f),
                header = {},
            )
        }
        return
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        FeedList(
            state = state,
            dates = dates,
            selectedId = null,
            onEvent = onEvent,
            modifier = Modifier.widthIn(max = columnMaxWidth),
            header = {
                item(key = "tools") {
                    ActivityToolsSection(
                        state = state,
                        onEvent = onEvent,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            },
        )
    }
}

/*
 * Why: the list keeps the rail's half (520dp with the rail on a tablet, the half before the hinge on a book), the item
 * picked fills the other.
 */
@Composable
private fun ActivityListDetailLayout(
    state: ActivityUiState,
    listDetail: ListDetailLayout,
    dates: DateTimeTextFormatter,
    onEvent: (ActivityUiEvent) -> Unit,
) {
    val selected = state.selectedItem
    Row(modifier = Modifier.fillMaxSize()) {
        FeedList(
            state = state,
            dates = dates,
            selectedId = selected?.id,
            onEvent = onEvent,
            modifier = Modifier
                .width(listDetail.listPaneWidth)
                .fillMaxHeight(),
            header = {
                item(key = "tools") {
                    ActivityToolsSection(
                        state = state,
                        onEvent = onEvent,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            },
        )
        ActivityDetailPane(
            item = selected.takeIf { state.emptyState == null },
            related = state.relatedItems,
            dates = dates,
            nowMillis = System.currentTimeMillis(),
            onEvent = onEvent,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(
                    start = listDetail.paneSpacing,
                    end = if (listDetail.isBook) 16.dp else 32.dp,
                    top = 8.dp,
                ),
        )
    }
}

@Composable
private fun FeedList(
    state: ActivityUiState,
    dates: DateTimeTextFormatter,
    selectedId: String?,
    onEvent: (ActivityUiEvent) -> Unit,
    header: LazyListScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    EndReachedEffect(
        listState = listState,
        hasMore = state.hasMore,
        onEndReached = { onEvent(ActivityUiEvent.OnEndReached) },
    )
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        header()
        feedItems(
            state = state,
            dates = dates,
            selectedId = selectedId,
            onEvent = onEvent,
        )
    }
}
