package com.quare.blitzsplit.android.feature.groups

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.EmptyGroupsImage
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DeviceLayout
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
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
 * Why: the first home, before any group exists. Its only actions are the two of the card (no
 * search here); the other tabs exist in the design but are not built yet.
 */
@Composable
fun GroupsContent(
    state: GroupsUiState,
    onEvent: (GroupsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    AdaptiveScreen(modifier = modifier.fillMaxSize()) { layout ->
        /*
         * Why: the rail runs edge to edge beside the scaffold, which then leaves out the start inset
         * the rail already takes.
         */
        Row(modifier = Modifier.fillMaxSize()) {
            if (layout.usesNavigationRail) GroupsNavigationRail(onEvent = onEvent)
            Scaffold(
                modifier = Modifier.weight(1f),
                snackbarHost = { SnackbarHost(snackbarHostState) },
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
                GroupsPane(
                    state = state,
                    onEvent = onEvent,
                    layout = layout,
                    modifier = Modifier
                        .padding(padding)
                        .consumeWindowInsets(padding),
                )
            }
        }
    }
}

@Composable
private fun GroupsPane(
    state: GroupsUiState,
    onEvent: (GroupsUiEvent) -> Unit,
    layout: DeviceLayout,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = if (layout.isTablet) {
        40.dp
    } else if (layout.usesNavigationRail) {
        32.dp
    } else {
        16.dp
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            // Why: a tablet centres the card in the space left, which needs a bounded height.
            .then(if (layout.isTablet) Modifier else Modifier.verticalScroll(rememberScrollState()))
            .padding(horizontal = horizontalPadding, vertical = if (layout.usesNavigationRail) 28.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GroupsHeader(state = state, onEvent = onEvent, layout = layout)
        when {
            layout.isTablet -> Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                EmptyGroupsCard(onEvent = onEvent, isHorizontal = false, isLarge = true)
            }

            layout.isLandscape -> EmptyGroupsCard(onEvent = onEvent, isHorizontal = true, isLarge = false)

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
private fun GroupsHeader(
    state: GroupsUiState,
    onEvent: (GroupsUiEvent) -> Unit,
    layout: DeviceLayout,
) {
    val profileButton: @Composable () -> Unit = {
        IconButton(
            onClick = { onEvent(GroupsUiEvent.OnProfileClicked) },
            modifier = Modifier.size(48.dp),
        ) {
            val profileLabel = stringResource(R.string.groups_profile)
            AvatarBadge(
                initial = state.avatarInitial,
                color = state.avatarColor,
                modifier = Modifier.semantics { contentDescription = profileLabel },
            )
        }
    }
    val title: @Composable () -> Unit = {
        Text(
            text = stringResource(R.string.groups_title),
            style = if (layout.isLandscape && !layout.isTablet) {
                MaterialTheme.typography.displaySmall
            } else {
                MaterialTheme.typography.displayMedium
            },
            fontWeight = FontWeight.Bold,
        )
    }
    if (layout.usesNavigationRail) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                title()
                SyncBadgeChip(
                    badge = state.syncBadge,
                    canRetry = state.canRetrySync,
                    onRetry = { onEvent(GroupsUiEvent.OnRetrySyncClicked) },
                )
            }
            profileButton()
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { profileButton() }
            title()
            SyncBadgeChip(
                badge = state.syncBadge,
                canRetry = state.canRetrySync,
                onRetry = { onEvent(GroupsUiEvent.OnRetrySyncClicked) },
            )
        }
    }
}

@Composable
private fun SyncBadgeChip(
    badge: SyncBadge?,
    canRetry: Boolean,
    onRetry: () -> Unit,
) {
    val (icon, label) = when (badge) {
        null -> return
        SyncBadge.OFFLINE -> Icons.Outlined.CloudOff to R.string.groups_sync_offline
        SyncBadge.SYNCING -> Icons.Outlined.CloudSync to R.string.groups_sync_syncing
        SyncBadge.FAILED -> Icons.Outlined.Refresh to R.string.groups_sync_failed
    }
    val shape = MaterialTheme.shapes.small
    // Why: only the failed badge is a button; the others stay plain text for TalkBack, not a disabled button.
    val retryModifier = if (canRetry) {
        Modifier.clip(shape).clickable(role = Role.Button, onClick = onRetry)
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
        Row(
            modifier = Modifier.height(32.dp).padding(start = 8.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = stringResource(label), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EmptyGroupsCard(
    onEvent: (GroupsUiEvent) -> Unit,
    isHorizontal: Boolean,
    isLarge: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .then(if (isLarge) Modifier.widthIn(max = 600.dp) else Modifier)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        if (isHorizontal) {
            Row(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmptyGroupsImage(scale = 0.94f)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EmptyGroupsText(textAlign = TextAlign.Start, isLarge = false)
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CreateGroupButton(onEvent = onEvent)
                        JoinWithLinkButton(onEvent = onEvent)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(
                    horizontal = if (isLarge) 40.dp else 24.dp,
                    vertical = if (isLarge) 40.dp else 32.dp,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EmptyGroupsImage(scale = if (isLarge) 1.18f else 1f)
                EmptyGroupsText(textAlign = TextAlign.Center, isLarge = isLarge)
                if (isLarge) {
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CreateGroupButton(onEvent = onEvent)
                        JoinWithLinkButton(onEvent = onEvent)
                    }
                } else {
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CreateGroupButton(onEvent = onEvent, modifier = Modifier.fillMaxWidth())
                        JoinWithLinkButton(onEvent = onEvent, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGroupsText(
    textAlign: TextAlign,
    isLarge: Boolean,
) {
    Text(
        text = stringResource(R.string.groups_empty_title),
        modifier = Modifier.fillMaxWidth(),
        textAlign = textAlign,
        style = if (isLarge) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = stringResource(R.string.groups_empty_body),
        modifier = Modifier.fillMaxWidth(),
        textAlign = textAlign,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CreateGroupButton(
    onEvent: (GroupsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(onClick = { onEvent(GroupsUiEvent.OnCreateGroupClicked) }, modifier = modifier.height(56.dp)) {
        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
        Text(text = stringResource(R.string.groups_create), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun JoinWithLinkButton(
    onEvent: (GroupsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(onClick = { onEvent(GroupsUiEvent.OnJoinWithLinkClicked) }, modifier = modifier.height(56.dp)) {
        Icon(imageVector = Icons.Outlined.QrCode2, contentDescription = null)
        Text(text = stringResource(R.string.groups_join_with_link), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun GroupsNavigationBar(onEvent: (GroupsUiEvent) -> Unit) {
    NavigationBar {
        GroupsTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == GroupsTab.GROUPS,
                onClick = { tab.event?.let(onEvent) },
                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
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
    ) {
        Column(modifier = Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GroupsTab.entries.forEach { tab ->
                NavigationRailItem(
                    selected = tab == GroupsTab.GROUPS,
                    onClick = { tab.event?.let(onEvent) },
                    icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.label)) },
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun GroupsOfflinePreview() {
    BlitzSplitTheme {
        GroupsContent(
            state = GroupsUiState(
                avatarInitial = "P",
                avatarColor = AvatarColor.BLUE,
                syncBadge = SyncBadge.OFFLINE,
                showsRecoveryCodePrompt = false,
            ),
            onEvent = {},
        )
    }
}
