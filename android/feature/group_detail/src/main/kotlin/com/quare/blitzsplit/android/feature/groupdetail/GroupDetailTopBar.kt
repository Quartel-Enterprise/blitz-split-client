package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader
import com.quare.blitzsplit.android.designsystem.R as DesignR

// Why: the canvas's medium flexible bar: the group's shape, name and totals, collapsing to one line as the list scrolls.
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
) {
    val members = pluralStringResource(R.plurals.group_detail_member_count, header.memberCount, header.memberCount)
    val meta = if (hasBills) {
        stringResource(R.string.group_detail_meta_total, members, money.toText(header.totalSpentMinor))
    } else {
        stringResource(R.string.group_detail_meta_empty, members)
    }
    MediumFlexibleTopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GroupBadge(
                    initials = header.groupInitials,
                    color = header.groupColor,
                    size = 40.dp,
                )
                Text(
                    text = header.groupName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        subtitle = {
            Text(
                text = meta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = { onAction(GroupAction.BACK) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.group_detail_back),
                )
            }
        },
        actions = {
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
                isOnline = isOnline,
                onAction = onAction,
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

// Why: the ⋮ menu keeps only what changes the group itself (6.1i); offline, Sair and Apagar need the internet (6.1j).
@Composable
private fun GroupMenu(
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
