package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.DeviceLayout
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiState

// Why: a phone lying down's column of the actions and "Saldo geral", or Buscar's field, as the canvas draws it.
private val sideColumnWidth = 320.dp

// Why: the QR reader, Buscar once there is a group, and the profile: the top bar's, or the side column's lying down.
@Composable
internal fun GroupsTopActionsComponent(
    state: GroupsUiState,
    onEvent: (GroupsUiEvent) -> Unit,
) {
    IconButton(onClick = { onEvent(GroupsUiEvent.OnJoinWithLinkClicked) }) {
        Icon(
            imageVector = Icons.Outlined.QrCode2,
            contentDescription = stringResource(R.string.groups_join_with_link),
        )
    }
    // Why: only once there is a group to look in.
    if (state.canSearch) {
        IconButton(onClick = { onEvent(GroupsUiEvent.OnSearchOpened) }) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = stringResource(R.string.groups_search),
            )
        }
    }
    ProfileAvatarButton(
        initial = state.avatarInitial,
        color = state.avatarColor,
        photoUrl = state.photoUrl,
        onClick = { onEvent(GroupsUiEvent.OnProfileClicked) },
    )
}

/*
 * Why: a phone lying down (5.1, 5.2): on the left, 320 dp, the actions, the sync status while it asks for attention and
 * "Saldo geral", which stay still, or Buscar's field and scopes; on the right the list, or the results, scrolling on
 * their own. Pulling the list down syncs.
 */
@Composable
internal fun GroupsSideColumnLayout(
    state: GroupsUiState,
    onEvent: (GroupsUiEvent) -> Unit,
    layout: DeviceLayout,
    listState: LazyGridState,
    onLongPress: (GroupRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 12.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier
                .width(sideColumnWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isSearching) {
                GroupsSearchHeader(
                    search = state.search,
                    onEvent = onEvent,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GroupsTopActionsComponent(
                        state = state,
                        onEvent = onEvent,
                    )
                }
                SyncBadgeChip(
                    badge = state.syncBadge,
                    pendingChangeCount = state.pendingChangeCount,
                    canRetry = state.canRetrySync,
                    onRetry = { onEvent(GroupsUiEvent.OnRetrySyncClicked) },
                )
                GroupsSummaryCard(
                    summary = state.summary,
                    onEvent = onEvent,
                    isStacked = true,
                )
            }
        }
        if (state.isSearching) {
            GroupsSearchPane(
                search = state.search,
                onEvent = onEvent,
                layout = layout,
                modifier = Modifier.weight(1f),
                horizontalPadding = 0.dp,
            )
        } else {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(GroupsUiEvent.OnRefresh) },
                modifier = Modifier.weight(1f),
            ) {
                LazyVerticalGrid(
                    columns = gridCellsOf(state),
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groupsListItems(
                        state = state,
                        onEvent = onEvent,
                        onLongPress = onLongPress,
                        selectedGroupId = null,
                        showsSummary = false,
                    )
                }
            }
        }
    }
}
