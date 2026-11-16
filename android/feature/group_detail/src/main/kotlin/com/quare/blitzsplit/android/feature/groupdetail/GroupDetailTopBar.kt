package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.quare.blitzsplit.android.designsystem.component.GroupAvatarShape
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.component.rememberGroupBillSharedModifiers
import com.quare.blitzsplit.android.designsystem.component.rememberGroupSharedModifiers
import com.quare.blitzsplit.android.designsystem.component.splitBillIcon
import com.quare.blitzsplit.android.designsystem.layout.LocalInDetailPane
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader
import com.quare.blitzsplit.android.designsystem.R as DesignR

/*
 * Why: the header's avatar, big while the bar is open and shrinking with it into the one-line bar (beta feedback of
 * 2026-11-12), and the open bar's height, room under the one-line bar for it beside the name.
 */
private val expandedBadgeSize = 64.dp
private val foldingBadgeSize = 40.dp
private val collapsedBadgeSize = 32.dp
private val expandedBarHeight = 148.dp

/*
 * Why: the canvas's medium flexible bar: the group's square and name, with no line under them (the faces under the bar
 * tell how many people, Statistics the total; beta feedback of 2026-11-16), collapsing to one line of the square and
 * the name as the list scrolls. Its open title draws the avatar at 64 dp, shrinking as the bar folds while the one-line
 * title, 32 dp, fades in over it; only the open one is the group's shared avatar and name, which fly in from the row
 * tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupTopBar(
    header: GroupHeader,
    isOnline: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    onAction: (GroupAction) -> Unit,
    // Why: "Rachar conta" in the bar wherever it doesn't float (anywhere wider than a phone upright); null otherwise.
    splitBillButton: GroupTabFloatingButton? = null,
    // Why: keeps the button's room while a tab has none (the empty Despesas), so the bar's icons never jump sideways.
    reservesSplitBillSlot: Boolean = false,
    // Why: the group whose row the avatar and name fly from; null where nothing flies (previews, screenshots).
    sharedGroupId: String? = null,
) {
    /*
     * Why: the title in sight carries the shared avatar and name, so back from a folded bar flies from the one line.
     * Derived, and the fold read only by the open title's badge, so a scroll redraws the badge, not the whole bar.
     */
    val isMostlyOpen by remember(scrollBehavior) {
        derivedStateOf { scrollBehavior.state.collapsedFraction < 0.5f }
    }
    TwoRowsTopAppBar(
        title = { isExpanded ->
            if (isExpanded) {
                GroupTitle(
                    header = header,
                    badgeSize = lerp(expandedBadgeSize, foldingBadgeSize, scrollBehavior.state.collapsedFraction),
                    onAction = onAction,
                    sharedGroupId = sharedGroupId.takeIf { isMostlyOpen },
                )
            } else {
                GroupTitle(
                    header = header,
                    badgeSize = collapsedBadgeSize,
                    onAction = onAction,
                    sharedGroupId = sharedGroupId.takeUnless { isMostlyOpen },
                )
            }
        },
        navigationIcon = { GroupBackButton(onAction) },
        actions = {
            when {
                splitBillButton != null -> GroupBarSplitBillButton(
                    icon = splitBillButton.icon,
                    label = splitBillButton.label,
                    onClick = splitBillButton.onClick,
                )

                reservesSplitBillSlot -> GroupBarSplitBillButton(
                    icon = splitBillIcon(),
                    label = stringResource(R.string.group_detail_split_bill),
                    onClick = {},
                    modifier = Modifier
                        .alpha(0f)
                        .clearAndSetSemantics {},
                    isEnabled = false,
                )
            }
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
    // Why: the square and name also fly into the top bar of a bill opened from the Expenses (beta feedback of 2026-11-16).
    val billShared = rememberGroupBillSharedModifiers(sharedGroupId)
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
                modifier = shared.avatar.then(billShared.avatar),
            )
        }
        Text(
            text = header.groupName,
            modifier = shared.name.then(billShared.name),
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

// Why: the floating button's icon and words, as a filled button ahead of the bar's icons.
@Composable
private fun GroupBarSplitBillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.padding(horizontal = 8.dp),
        enabled = isEnabled,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
 * Why: the ⋮ menu keeps what is done to the group itself (6.1i): Edit group, Pin to Home and Export group data, which
 * work offline, then Leave and Delete, which need the internet (6.1j).
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
                text = { Text(stringResource(R.string.group_detail_export)) },
                onClick = {
                    isOpen = false
                    onAction(GroupAction.EXPORT)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
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
