package com.quare.blitzsplit.android.feature.groups

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.feature.groups.presentation.model.ActivityAge
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRowDetail
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiState
import com.quare.blitzsplit.feature.groups.presentation.model.LastActivity

/*
 * Why: the home's list: "Saldo geral", the open groups by their latest activity and, collapsed, the settled ones. A
 * row opens the group; its ⋮ menu and its long press hold the group's actions. The grid and the search come with their
 * own flows.
 */
internal fun LazyListScope.groupsListItems(
    state: GroupsUiState,
    onEvent: (GroupsUiEvent) -> Unit,
    onLongPress: (GroupRow) -> Unit,
) {
    item(
        key = "summary",
        contentType = "summary",
    ) {
        GroupsSummaryCard(
            summary = state.summary,
            onEvent = onEvent,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
    val open = state.openGroups
    item(
        key = "open-header",
        contentType = "header",
    ) {
        OpenHeader(
            count = open.size,
            onEvent = onEvent,
        )
    }
    if (state.showsNoOpenGroups) {
        item(
            key = "no-open",
            contentType = "empty",
        ) {
            Text(
                text = stringResource(R.string.groups_no_open),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    itemsIndexed(
        open,
        key = { _, group -> group.id },
        contentType = { _, _ -> "group" },
    ) { index, group ->
        GroupListRow(
            group = group,
            shape = groupedRowShapeOf(index, open.size),
            isOnline = state.exit.isOnline,
            onEvent = onEvent,
            onLongPress = { onLongPress(group) },
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
    val settled = state.settledGroups
    if (settled.isEmpty()) return
    item(
        key = "settled-header",
        contentType = "header",
    ) {
        SettledToggle(
            count = settled.size,
            isExpanded = state.showsSettledGroups,
            onToggle = { onEvent(GroupsUiEvent.OnShowSettledChanged(!state.showsSettledGroups)) },
        )
    }
    if (!state.showsSettledGroups) return
    itemsIndexed(
        settled,
        key = { _, group -> group.id },
        contentType = { _, _ -> "group" },
    ) { index, group ->
        GroupListRow(
            group = group,
            shape = groupedRowShapeOf(index, settled.size),
            isOnline = state.exit.isOnline,
            onEvent = onEvent,
            onLongPress = { onLongPress(group) },
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

@Composable
private fun OpenHeader(
    count: Int,
    onEvent: (GroupsUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 4.dp,
                top = 12.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.groups_open_count, count),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(onClick = { onEvent(GroupsUiEvent.OnCreateGroupClicked) }) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.groups_new_group),
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun SettledToggle(
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f)
    TextButton(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 12.dp,
                bottom = 4.dp,
            ).heightIn(min = 48.dp),
    ) {
        Text(
            text = stringResource(R.string.groups_settled_count, count),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.Outlined.ExpandMore,
            contentDescription = null,
            modifier = Modifier.rotate(rotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupListRow(
    group: GroupRow,
    shape: Shape,
    isOnline: Boolean,
    onEvent: (GroupsUiEvent) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter(group.currency)
    val dates = rememberDateTimeTextFormatter()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = { onEvent(GroupsUiEvent.OnGroupClicked(group.id)) },
                    onLongClickLabel = stringResource(R.string.groups_actions_title, group.name),
                    onLongClick = onLongPress,
                ).padding(
                    start = 16.dp,
                    end = 4.dp,
                    top = 14.dp,
                    bottom = 14.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GroupBadge(
                initials = group.initials,
                color = group.color,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (group.usesTwoNameLines) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                GroupMetaRow(
                    group = group,
                    detail = detailTextOf(
                        group = group,
                        amountText = money::toAmountText,
                        dates = dates,
                    ),
                )
            }
            if (group.isSettled) {
                SettledLabel()
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = money.toSignedText(group.netMinor),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColorOf(group.netMinor),
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(balanceLabelOf(group.balanceKind)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            GroupRowMenuButton(
                group = group,
                isOnline = isOnline,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun SettledLabel() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.groups_balance_everyone_settled),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupMetaRow(
    group: GroupRow,
    detail: String,
) {
    val members = membersTextOf(group.memberCount)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Why: the faces only show who is in it; TalkBack reads the member count instead.
        Row(
            modifier = Modifier.clearAndSetSemantics { contentDescription = members },
            horizontalArrangement = Arrangement.spacedBy((-6).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            group.faces.forEach { face ->
                AvatarBadge(
                    initial = face.initial,
                    color = face.color,
                    size = 22.dp,
                )
            }
            if (group.extraMemberCount > 0) {
                Text(
                    text = stringResource(R.string.groups_extra_members, group.extraMemberCount),
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (group.isPending) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
            )
        }
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun membersTextOf(count: Int): String = if (count == 1) {
    stringResource(R.string.groups_only_you)
} else {
    pluralStringResource(R.plurals.groups_member_count, count, count)
}

// Why: "na fila · Mercado", "você recebe 120,00 e deve 218,00", "outros ainda acertam" or the last activity.
@Composable
private fun detailTextOf(
    group: GroupRow,
    amountText: (Long) -> String,
    dates: DateTimeTextFormatter,
): String = when (group.detail) {
    GroupRowDetail.QUEUED ->
        group.pendingBillDescription
            ?.let { stringResource(R.string.groups_meta_queued_bill, it) }
            ?: stringResource(R.string.groups_meta_queued)

    GroupRowDetail.MIXED -> stringResource(
        R.string.groups_meta_mixed,
        amountText(group.receivesMinor),
        amountText(group.owesMinor),
    )

    GroupRowDetail.OTHERS_STILL_SETTLING -> stringResource(R.string.groups_meta_others_settling)

    GroupRowDetail.INVITE_SOMEONE -> stringResource(R.string.groups_meta_invite)

    GroupRowDetail.EVERYONE_SETTLED -> stringResource(
        R.string.groups_meta_everyone_settled,
        membersTextOf(group.memberCount),
        dates.toDateText(group.lastActivity.epochMillis),
    )

    GroupRowDetail.ACTIVITY -> activityTextOf(group.lastActivity, dates)
}

// Why: "hoje", "ontem", "3 dias", "1 semana" or "3 de out.".
@Composable
private fun activityTextOf(
    activity: LastActivity,
    dates: DateTimeTextFormatter,
): String = when (activity.age) {
    ActivityAge.TODAY -> stringResource(R.string.groups_activity_today)
    ActivityAge.YESTERDAY -> stringResource(R.string.groups_activity_yesterday)
    ActivityAge.DAYS -> pluralStringResource(R.plurals.groups_activity_days, activity.count, activity.count)
    ActivityAge.WEEKS -> pluralStringResource(R.plurals.groups_activity_weeks, activity.count, activity.count)
    ActivityAge.DATE -> dates.toDateText(activity.epochMillis)
}

internal fun balanceLabelOf(kind: GroupBalanceKind): Int = when (kind) {
    GroupBalanceKind.TO_RECEIVE -> R.string.groups_balance_to_receive
    GroupBalanceKind.YOU_OWE -> R.string.groups_balance_you_owe
    GroupBalanceKind.YOU_ARE_SETTLED -> R.string.groups_balance_you_are_settled
    GroupBalanceKind.NO_EXPENSES -> R.string.groups_no_expenses
    GroupBalanceKind.EVERYONE_SETTLED -> R.string.groups_balance_everyone_settled
}

// Why: the accent for what comes back, the error tone for what you owe, plain for zero.
@Composable
internal fun amountColorOf(netMinor: Long): Color = when {
    netMinor > 0 -> MaterialTheme.colorScheme.primary
    netMinor < 0 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
