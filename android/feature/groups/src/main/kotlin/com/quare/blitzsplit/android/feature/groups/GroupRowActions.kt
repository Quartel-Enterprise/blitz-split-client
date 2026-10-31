package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.android.designsystem.R as DesignR

// Why: the M3 alpha of a disabled item, as the ⋮ menu's own DropdownMenuItem draws it.
private const val DISABLED_ALPHA = 0.38f

/*
 * Why: the ⋮ at the end of every row (5.4), with the same items as its long press, so nobody depends on the long
 * press. Offline, Sair and Apagar show disabled with "Precisa de internet".
 */
@Composable
internal fun GroupRowMenuButton(
    group: GroupRow,
    isOnline: Boolean,
    onEvent: (GroupsUiEvent) -> Unit,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isOpen = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.groups_row_more, group.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { isOpen = false },
        ) {
            groupActionItemsOf(group, isOnline).forEach { item ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(item.label)
                            item.hint?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    },
                    onClick = {
                        isOpen = false
                        onEvent(item.event)
                    },
                    enabled = item.isEnabled,
                    leadingIcon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                        )
                    },
                    colors = if (item.isDestructive) {
                        MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        MenuDefaults.itemColors()
                    },
                )
            }
        }
    }
}

// Why: the long press on a row (5.3): the group, its balance and the same three actions, in a bottom sheet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupActionsSheet(
    group: GroupRow,
    isOnline: Boolean,
    onEvent: (GroupsUiEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(R.string.groups_actions_title, group.name)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.semantics { paneTitle = title },
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            GroupActionsHeader(group = group)
            if (!isOnline) OfflineActionsBanner()
            groupActionItemsOf(group, isOnline).forEach { item ->
                GroupActionSheetRow(
                    item = item,
                    onClick = {
                        onDismiss()
                        onEvent(item.event)
                    },
                )
            }
        }
    }
}

@Composable
private fun GroupActionsHeader(group: GroupRow) {
    val money = rememberMoneyFormatter(group.currency)
    Row(
        modifier = Modifier.padding(
            start = 24.dp,
            end = 24.dp,
            bottom = 12.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GroupBadge(
            initials = group.initials,
            color = group.color,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!group.isSettled) {
                    Text(
                        text = money.toSignedText(group.netMinor),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = amountColorOf(group.netMinor),
                    )
                }
                Text(
                    text = stringResource(balanceLabelOf(group.balanceKind)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Why: "Sair e apagar precisam de internet, para conferir os saldos de todo mundo antes."
@Composable
private fun OfflineActionsBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 24.dp,
                end = 24.dp,
                bottom = 8.dp,
            ),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 10.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.groups_actions_offline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GroupActionSheetRow(
    item: GroupActionItem,
    onClick: () -> Unit,
) {
    val color = if (item.isDestructive && item.isEnabled) MaterialTheme.colorScheme.error else Color.Unspecified
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(
                enabled = item.isEnabled,
                onClick = onClick,
            ).alpha(if (item.isEnabled) 1f else DISABLED_ALPHA)
            .padding(
                horizontal = 24.dp,
                vertical = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyLarge,
                color = color,
            )
            item.hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/*
 * Why: offline, Sair and Apagar say they need the internet; online, what blocks them shows before the tap, which
 * still opens "Acerte antes de sair" or "Ainda há saldo em aberto" with what is open.
 */
@Composable
private fun groupActionItemsOf(
    group: GroupRow,
    isOnline: Boolean,
): List<GroupActionItem> {
    val money = rememberMoneyFormatter(group.currency)
    val needsInternet = stringResource(DesignR.string.designsystem_group_exit_needs_internet)
    val leaveHint = when {
        !isOnline -> needsInternet

        group.settleBeforeLeavingMinor > 0 -> stringResource(
            R.string.groups_action_leave_hint,
            money.toText(group.settleBeforeLeavingMinor),
        )

        else -> null
    }
    val deleteHint = when {
        !isOnline -> needsInternet
        group.hasOpenDebts -> stringResource(R.string.groups_action_delete_hint)
        else -> null
    }
    return listOf(
        GroupActionItem(
            label = stringResource(R.string.groups_action_invite),
            hint = stringResource(R.string.groups_action_invite_hint),
            icon = Icons.Outlined.PersonAdd,
            isEnabled = true,
            isDestructive = false,
            event = GroupsUiEvent.OnInviteClicked(group.id),
        ),
        GroupActionItem(
            label = stringResource(R.string.groups_action_leave),
            hint = leaveHint,
            icon = Icons.AutoMirrored.Outlined.Logout,
            isEnabled = isOnline,
            isDestructive = false,
            event = GroupsUiEvent.OnLeaveGroupClicked(group.id),
        ),
        GroupActionItem(
            label = stringResource(R.string.groups_action_delete),
            hint = deleteHint,
            icon = Icons.Outlined.Delete,
            isEnabled = isOnline,
            isDestructive = true,
            event = GroupsUiEvent.OnDeleteGroupClicked(group.id),
        ),
    )
}
