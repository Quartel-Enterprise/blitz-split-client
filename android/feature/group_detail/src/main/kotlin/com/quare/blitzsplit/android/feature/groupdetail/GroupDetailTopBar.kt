package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.quare.blitzsplit.android.designsystem.component.GroupAvatarShape
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.component.rememberGroupSharedModifiers
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.LocalInDetailPane
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader
import com.quare.blitzsplit.android.designsystem.R as DesignR

/*
 * Why: the header's avatar, big while the bar is open and shrinking with it into the one-line bar (beta feedback of
 * 2026-11-12), and the open bar's height, room under the one-line bar for it beside the name and the totals under them.
 */
private val expandedBadgeSize = 64.dp
private val foldingBadgeSize = 40.dp
private val collapsedBadgeSize = 32.dp
private val expandedBarHeight = 168.dp

/*
 * Why: the canvas's medium flexible bar: the group's square, name and totals, collapsing to one line as the list
 * scrolls. Its open title draws the avatar at 64 dp, shrinking as the bar folds while the one-line title, 32 dp, fades
 * in over it; only the open one is the group's shared avatar and name, which fly in from the row tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupTopBar(
    header: GroupHeader,
    // Why: "nenhuma despesa ainda" instead of the total before the first bill.
    hasBills: Boolean,
    isOnline: Boolean,
    money: MoneyFormatter,
    scrollBehavior: TopAppBarScrollBehavior,
    onAction: (GroupAction) -> Unit,
    // Why: the group whose row the avatar and name fly from; null where nothing flies (previews, screenshots).
    sharedGroupId: String? = null,
) {
    val members = pluralStringResource(R.plurals.group_detail_member_count, header.memberCount, header.memberCount)
    val meta = if (hasBills) {
        stringResource(R.string.group_detail_meta_total, members, money.toText(header.totalSpentMinor))
    } else {
        stringResource(R.string.group_detail_meta_empty, members)
    }
    TwoRowsTopAppBar(
        title = { isExpanded ->
            if (isExpanded) {
                GroupTitle(
                    header = header,
                    badgeSize = lerp(expandedBadgeSize, foldingBadgeSize, scrollBehavior.state.collapsedFraction),
                    onAction = onAction,
                    sharedGroupId = sharedGroupId,
                )
            } else {
                GroupTitle(
                    header = header,
                    badgeSize = collapsedBadgeSize,
                    onAction = onAction,
                )
            }
        },
        // Why: the totals ellipsize short of the bar's actions instead of running into the chart button.
        subtitle = {
            Text(
                text = meta,
                modifier = Modifier.padding(end = 16.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = { GroupBackButton(onAction) },
        actions = {
            GroupBarActionsComponent(
                header = header,
                isOnline = isOnline,
                onAction = onAction,
            )
        },
        collapsedHeight = TopAppBarDefaults.MediumAppBarCollapsedHeight,
        expandedHeight = expandedBarHeight,
        scrollBehavior = scrollBehavior,
    )
}

// Why: a tabletop's one-line bar (6.2), which leaves the top half to the balance; the same back arrow and actions.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupCompactTopBar(
    header: GroupHeader,
    isOnline: Boolean,
    onAction: (GroupAction) -> Unit,
) {
    TopAppBar(
        title = {
            GroupTitle(
                header = header,
                badgeSize = 32.dp,
                onAction = onAction,
            )
        },
        navigationIcon = { GroupBackButton(onAction) },
        actions = {
            GroupBarActionsComponent(
                header = header,
                isOnline = isOnline,
                onAction = onAction,
            )
        },
    )
}

@Composable
private fun GroupTitle(
    header: GroupHeader,
    badgeSize: Dp,
    onAction: (GroupAction) -> Unit,
    sharedGroupId: String? = null,
) {
    val shared = rememberGroupSharedModifiers(sharedGroupId)
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        /*
         * Why: the avatar opens the group's photo big, as the profile's avatar opens its own; the touch target keeps
         * the 48 dp minimum around the smaller badge.
         */
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(GroupAvatarShape)
                .clickable(
                    onClickLabel = stringResource(R.string.group_detail_photo_open),
                    role = Role.Button,
                ) { onAction(GroupAction.PHOTO) },
            contentAlignment = Alignment.Center,
        ) {
            GroupBadge(
                initials = header.groupInitials,
                color = header.groupColor,
                photoUrl = header.groupPhotoUrl,
                size = badgeSize,
                modifier = shared.avatar,
            )
        }
        Text(
            text = header.groupName,
            modifier = shared.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Why: beside the home's list the group has nothing to go back to; another row takes its place.
@Composable
internal fun GroupBackButton(onAction: (GroupAction) -> Unit) {
    if (LocalInDetailPane.current) return
    IconButton(onClick = { onAction(GroupAction.BACK) }) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.group_detail_back),
        )
    }
}

@Composable
private fun GroupBarActionsComponent(
    header: GroupHeader,
    isOnline: Boolean,
    onAction: (GroupAction) -> Unit,
) {
    IconButton(onClick = { onAction(GroupAction.STATS) }) {
        Icon(
            imageVector = Icons.Outlined.BarChart,
            contentDescription = stringResource(R.string.group_detail_stats),
        )
    }
    IconButton(onClick = { onAction(GroupAction.INVITE) }) {
        Icon(
            imageVector = Icons.Outlined.PersonAdd,
            contentDescription = stringResource(R.string.group_detail_invite),
        )
    }
    GroupMenu(
        isPinned = header.isPinned,
        isOnline = isOnline,
        onAction = onAction,
    )
}

/*
 * Why: the ⋮ menu keeps only what changes the group itself (6.1i), with "Fixar na Início" first, which works offline;
 * offline, Sair and Apagar need the internet (6.1j).
 */
@Composable
private fun GroupMenu(
    isPinned: Boolean,
    isOnline: Boolean,
    onAction: (GroupAction) -> Unit,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isOpen = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.group_detail_more),
            )
        }
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { isOpen = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(if (isPinned) R.string.group_detail_unpin else R.string.group_detail_pin))
                },
                onClick = {
                    isOpen = false
                    onAction(GroupAction.PIN)
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = null,
                    )
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.group_detail_edit_group)) },
                onClick = {
                    isOpen = false
                    onAction(GroupAction.EDIT_GROUP)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                    )
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    MenuItemText(
                        label = stringResource(R.string.group_detail_leave_group),
                        isOnline = isOnline,
                    )
                },
                onClick = {
                    isOpen = false
                    onAction(GroupAction.LEAVE_GROUP)
                },
                enabled = isOnline,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                    )
                },
            )
            DropdownMenuItem(
                text = {
                    MenuItemText(
                        label = stringResource(R.string.group_detail_delete_group),
                        isOnline = isOnline,
                    )
                },
                onClick = {
                    isOpen = false
                    onAction(GroupAction.DELETE_GROUP)
                },
                enabled = isOnline,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                    )
                },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
            )
        }
    }
}

// Why: "Precisa de internet" under the label while offline.
@Composable
private fun MenuItemText(
    label: String,
    isOnline: Boolean,
) {
    Column {
        Text(label)
        if (!isOnline) {
            Text(
                text = stringResource(DesignR.string.designsystem_group_exit_needs_internet),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
