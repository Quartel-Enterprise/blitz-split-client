package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState

// Why: the canvas's medium flexible bar: the group's shape, name and totals, collapsing to one line as the list scrolls.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupDetailTopBar(
    state: GroupDetailUiState,
    money: MoneyFormatter,
    scrollBehavior: TopAppBarScrollBehavior,
    onEvent: (GroupDetailUiEvent) -> Unit,
) {
    val members = pluralStringResource(R.plurals.group_detail_member_count, state.memberCount, state.memberCount)
    val meta = if (state.days.isEmpty()) {
        stringResource(R.string.group_detail_meta_empty, members)
    } else {
        stringResource(R.string.group_detail_meta_total, members, money.toText(state.totalSpentMinor))
    }
    MediumFlexibleTopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GroupBadge(
                    initials = state.groupInitials,
                    color = state.groupColor,
                    size = 40.dp,
                )
                Text(
                    text = state.groupName,
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
            IconButton(onClick = { onEvent(GroupDetailUiEvent.OnBackClicked) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.group_detail_back),
                )
            }
        },
        actions = {
            IconButton(onClick = { onEvent(GroupDetailUiEvent.OnInviteClicked) }) {
                Icon(
                    imageVector = Icons.Outlined.PersonAdd,
                    contentDescription = stringResource(R.string.group_detail_invite),
                )
            }
            GroupMenu(
                isEnabled = state.isLoaded,
                onEvent = onEvent,
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

// Why: the ⋮ menu keeps only what changes the group itself (6.1i).
@Composable
private fun GroupMenu(
    // Why: the group's own fields are only there to edit once Room is read.
    isEnabled: Boolean,
    onEvent: (GroupDetailUiEvent) -> Unit,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { isOpen = true },
            enabled = isEnabled,
        ) {
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
                    onEvent(GroupDetailUiEvent.OnEditGroupClicked)
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
                text = { Text(stringResource(R.string.group_detail_leave_group)) },
                onClick = {
                    isOpen = false
                    onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                    )
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.group_detail_delete_group),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    isOpen = false
                    onEvent(GroupDetailUiEvent.OnDeleteGroupClicked)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
            )
        }
    }
}
