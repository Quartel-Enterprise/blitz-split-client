package com.quare.blitzsplit.android.feature.groups

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DeviceLayout
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiState
import com.quare.blitzsplit.feature.groups.presentation.model.SyncBadge

private enum class GroupsTab(
    @StringRes val label: Int,
    val icon: ImageVector,
    val event: GroupsUiEvent?,
) {
    GROUPS(R.string.groups_tab_groups, Icons.Outlined.Groups, null),
    BALANCES(R.string.groups_tab_balances, Icons.Outlined.AccountBalanceWallet, GroupsUiEvent.OnBalancesTabClicked),
    CONTACTS(R.string.groups_tab_contacts, Icons.Outlined.Person, GroupsUiEvent.OnContactsTabClicked),
    ACTIVITY(R.string.groups_tab_activity, Icons.AutoMirrored.Outlined.ReceiptLong, GroupsUiEvent.OnActivityTabClicked),
}

/*
 * Why: the home. No visible title, since the selected Groups tab already names it: the top bar only holds the QR
 * reader and the profile, and takes a color once the list scrolls under it, while "Rachar conta" shrinks to its icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsContent(
    state: GroupsUiState,
    onEvent: (GroupsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(R.string.groups_title)
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    AdaptiveScreen(modifier = modifier.fillMaxSize().semantics { paneTitle = title }) { layout ->
        /*
         * Why: the rail runs edge to edge beside the scaffold, which then leaves out the start inset
         * the rail already takes.
         */
        Row(modifier = Modifier.fillMaxSize()) {
            if (layout.usesNavigationRail) GroupsNavigationRail(onEvent = onEvent)
            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    GroupsTopBar(
                        state = state,
                        onEvent = onEvent,
                        scrollBehavior = scrollBehavior,
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    if (!layout.usesNavigationRail) {
                        ExtendedFloatingActionButton(
                            onClick = { onEvent(GroupsUiEvent.OnSplitBillClicked) },
                            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                            text = { Text(stringResource(R.string.groups_split_bill)) },
                            expanded = !listState.canScrollBackward,
                        )
                    }
                },
                bottomBar = {
                    if (!layout.usesNavigationRail) GroupsNavigationBar(onEvent = onEvent)
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentWindowInsets = if (layout.usesNavigationRail) {
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                } else {
                    WindowInsets.safeDrawing
                },
            ) { padding ->
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onEvent(GroupsUiEvent.OnRefresh) },
                    modifier = Modifier
                        .padding(padding)
                        .consumeWindowInsets(padding),
                ) {
                    if (state.groups.isEmpty()) {
                        EmptyGroupsPane(
                            state = state,
                            onEvent = onEvent,
                            layout = layout,
                        )
                    } else {
                        GroupsListPane(
                            state = state,
                            onEvent = onEvent,
                            layout = layout,
                            listState = listState,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupsTopBar(
    state: GroupsUiState,
    onEvent: (GroupsUiEvent) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = {},
        actions = {
            IconButton(onClick = { onEvent(GroupsUiEvent.OnJoinWithLinkClicked) }) {
                Icon(
                    imageVector = Icons.Outlined.QrCode2,
                    contentDescription = stringResource(R.string.groups_join_with_link),
                )
            }
            IconButton(
                onClick = { onEvent(GroupsUiEvent.OnProfileClicked) },
                modifier = Modifier.size(48.dp),
            ) {
                val profileLabel = stringResource(R.string.groups_profile)
                AvatarBadge(
                    initial = state.avatarInitial,
                    color = state.avatarColor,
                    modifier = Modifier.semantics { contentDescription = profileLabel },
                    photoUrl = state.photoUrl,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior,
    )
}

// Why: a tablet keeps the list in one centered column, 560dp at most.
@Composable
private fun GroupsListPane(
    state: GroupsUiState,
    onEvent: (GroupsUiEvent) -> Unit,
    layout: DeviceLayout,
    listState: LazyListState,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            state = listState,
            // Why: the cap comes before fillMaxSize, which would otherwise fix the width to the whole pane.
            modifier = Modifier
                .then(if (layout.isTablet) Modifier.widthIn(max = 560.dp) else Modifier)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPaddingOf(layout),
                end = horizontalPaddingOf(layout),
                top = 4.dp,
                // Why: room for the FAB over the last row.
                bottom = 96.dp,
            ),
        ) {
            if (state.syncBadge != null) {
                item(
                    key = "sync",
                    contentType = "sync",
                ) {
                    Box(modifier = Modifier.padding(bottom = 12.dp)) {
                        SyncBadgeChip(
                            badge = state.syncBadge,
                            pendingChangeCount = state.pendingChangeCount,
                            canRetry = state.canRetrySync,
                            onRetry = { onEvent(GroupsUiEvent.OnRetrySyncClicked) },
                        )
                    }
                }
            }
            groupsListItems(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

private fun horizontalPaddingOf(layout: DeviceLayout): Dp = if (layout.isTablet) {
    40.dp
} else if (layout.usesNavigationRail) {
    32.dp
} else {
    16.dp
}

@Composable
private fun EmptyGroupsPane(
    state: GroupsUiState,
    onEvent: (GroupsUiEvent) -> Unit,
    layout: DeviceLayout,
) {
    /*
     * Why: always scrollable, so a pull feeds PullToRefreshBox's nested scroll on every layout and before the groups
     * are read; at least the pane's height, so a tablet still centres the card in the space left.
     */
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        EmptyGroupsColumn(
            state = state,
            onEvent = onEvent,
            layout = layout,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight),
        )
    }
}

@Composable
private fun EmptyGroupsColumn(
    state: GroupsUiState,
    onEvent: (GroupsUiEvent) -> Unit,
    layout: DeviceLayout,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            horizontal = horizontalPaddingOf(layout),
            vertical = 4.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SyncBadgeChip(
            badge = state.syncBadge,
            pendingChangeCount = state.pendingChangeCount,
            canRetry = state.canRetrySync,
            onRetry = { onEvent(GroupsUiEvent.OnRetrySyncClicked) },
        )
        when {
            // Why: nothing until the groups are read, so the empty card never flashes before the list.
            !state.showsEmptyState -> Unit

            // Why: the spacers split what the card leaves, and shrink to nothing once it needs the scroll.
            layout.isTablet -> {
                Spacer(modifier = Modifier.weight(1f))
                EmptyGroupsCard(
                    onEvent = onEvent,
                    isHorizontal = false,
                    isLarge = true,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            layout.isLandscape -> EmptyGroupsCard(
                onEvent = onEvent,
                isHorizontal = true,
                isLarge = false,
            )

            else -> EmptyGroupsCard(
                onEvent = onEvent,
                isHorizontal = false,
                isLarge = false,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun SyncBadgeChip(
    badge: SyncBadge?,
    pendingChangeCount: Int,
    canRetry: Boolean,
    onRetry: () -> Unit,
) {
    val (icon, label) = when (badge) {
        null -> return

        SyncBadge.OFFLINE -> Icons.Outlined.CloudOff to if (pendingChangeCount > 0) {
            pluralStringResource(R.plurals.groups_sync_offline_queued, pendingChangeCount, pendingChangeCount)
        } else {
            stringResource(R.string.groups_sync_offline)
        }

        SyncBadge.SYNCING -> Icons.Outlined.CloudSync to stringResource(R.string.groups_sync_syncing)

        SyncBadge.FAILED -> Icons.Outlined.Refresh to stringResource(R.string.groups_sync_failed)
    }
    val shape = MaterialTheme.shapes.small
    // Why: only the failed badge is a button; the others stay plain text for TalkBack, not a disabled button.
    val retryModifier = if (canRetry) {
        Modifier.minimumInteractiveComponentSize().clip(shape).clickable(
            role = Role.Button,
            onClick = onRetry,
        )
    } else {
        Modifier
    }
    Surface(
        modifier = retryModifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        // Why: a long label may wrap to two lines on a narrow phone.
        Row(
            modifier = Modifier.heightIn(min = 32.dp).padding(
                start = 8.dp,
                end = 12.dp,
                top = 6.dp,
                bottom = 6.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun GroupsNavigationBar(onEvent: (GroupsUiEvent) -> Unit) {
    NavigationBar {
        GroupsTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == GroupsTab.GROUPS,
                onClick = { tab.event?.let(onEvent) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

@Composable
private fun GroupsNavigationRail(onEvent: (GroupsUiEvent) -> Unit) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        header = {
            FloatingActionButton(
                onClick = { onEvent(GroupsUiEvent.OnSplitBillClicked) },
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.groups_split_bill),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier.padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GroupsTab.entries.forEach { tab ->
                NavigationRailItem(
                    selected = tab == GroupsTab.GROUPS,
                    onClick = { tab.event?.let(onEvent) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(tab.label)) },
                )
            }
        }
    }
}
