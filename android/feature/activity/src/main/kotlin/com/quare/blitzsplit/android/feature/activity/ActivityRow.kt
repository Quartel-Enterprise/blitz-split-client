package com.quare.blitzsplit.android.feature.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.component.selectableRowColorOf
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.activity.domain.model.ActivityType
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityItem
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityUiEvent

// Why: the small badge on the avatar says what kind of event it is; a deleted target shows the trash.
internal fun iconOf(item: ActivityItem): ImageVector = when {
    item.isTargetDeleted -> Icons.Outlined.Delete
    item.kind == ActivityKind.INVITE_LINK_CHANGED -> Icons.Outlined.Link
    item.kind == ActivityKind.GROUP_CREATED || item.kind == ActivityKind.GROUP_EDITED -> Icons.Outlined.Edit
    else -> iconOf(item.kind.type)
}

internal fun iconOf(type: ActivityType): ImageVector = when (type) {
    ActivityType.BILLS -> Icons.AutoMirrored.Outlined.ReceiptLong
    ActivityType.PAYMENTS -> Icons.Outlined.CheckCircle
    ActivityType.COMMENTS -> Icons.AutoMirrored.Outlined.Chat
    ActivityType.PEOPLE_AND_GROUPS -> Icons.Outlined.PersonAdd
    ActivityType.CHARGES -> Icons.AutoMirrored.Outlined.Send
}

@Composable
private fun badgeColorsOf(item: ActivityItem): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    return when {
        item.isTargetDeleted -> colors.errorContainer to colors.onErrorContainer

        item.kind.type == ActivityType.BILLS || item.kind.type == ActivityType.CHARGES ->
            colors.primaryContainer to colors.onPrimaryContainer

        item.kind.type == ActivityType.PAYMENTS -> colors.tertiaryContainer to colors.onTertiaryContainer

        else -> colors.surfaceContainerHigh to colors.onSurfaceVariant
    }
}

// Why: who did it, with the kind of event at its corner, as every row and the detail draw it.
@Composable
internal fun ActorAvatarIcon(
    item: ActivityItem,
    modifier: Modifier = Modifier,
) {
    val (container, content) = badgeColorsOf(item)
    val rowColor = MaterialTheme.colorScheme.surfaceContainerLowest
    Box(modifier = modifier) {
        AvatarBadge(
            initial = item.actor.initials,
            color = item.actor.color,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(
                    x = 5.dp,
                    y = 5.dp,
                ).size(20.dp)
                .background(container, CircleShape)
                .border(2.dp, rowColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconOf(item),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = content,
            )
        }
    }
}

// Why: the group's own shape and color from Grupos, small before its name.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GroupDotIcon(
    color: AvatarColor?,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = modifier
            .size(12.dp)
            .background(
                color?.toContainerColor(isDark) ?: MaterialTheme.colorScheme.outlineVariant,
                MaterialShapes.Cookie4Sided.toShape(),
            ),
    )
}

/*
 * Why: one event of the feed: the sentence (bold names, bolder while unread), its detail, the group and the time, the
 * blue dot while unread and the orange one while waiting to go up. A bill's edit opens its changes in the row.
 */
@Composable
internal fun ActivityRow(
    item: ActivityItem,
    index: Int,
    count: Int,
    isExpanded: Boolean,
    isSelected: Boolean,
    dates: DateTimeTextFormatter,
    onEvent: (ActivityUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val unread = stringResource(R.string.activity_unread)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (item.isUnread) stateDescription = unread
            },
        shape = groupedRowShapeOf(index, count),
        color = selectableRowColorOf(isSelected),
        onClick = { onEvent(ActivityUiEvent.OnItemClicked(item.id)) },
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = if (item.hasChangesToShow) 4.dp else 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActorAvatarIcon(item = item)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = sentenceTextOf(item),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (item.isUnread) FontWeight.Medium else FontWeight.Normal,
                    )
                    detailTextOf(item)?.let { detail ->
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    MetaLine(
                        item = item,
                        dates = dates,
                    )
                }
                StatusDotIndicator(item = item)
            }
            if (item.hasChangesToShow) {
                ChangesToggle(
                    item = item,
                    isExpanded = isExpanded,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun MetaLine(
    item: ActivityItem,
    dates: DateTimeTextFormatter,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GroupDotIcon(color = item.groupColor)
        Text(
            text = groupAndTimeTextOf(item, dates),
            style = MaterialTheme.typography.bodySmall,
            color = if (item.isPending) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusDotIndicator(item: ActivityItem) {
    val color = when {
        item.isUnread -> MaterialTheme.colorScheme.primary
        item.isPending -> MaterialTheme.colorScheme.tertiary
        else -> return
    }
    Box(
        modifier = Modifier
            .padding(top = 6.dp)
            .size(10.dp)
            .background(color, CircleShape),
    )
}

// Why: "Ver mudanças" opens the bill's changes in the row, "Abrir conta" beside it opens the bill.
@Composable
private fun ChangesToggle(
    item: ActivityItem,
    isExpanded: Boolean,
    onEvent: (ActivityUiEvent) -> Unit,
) {
    Column(modifier = Modifier.padding(start = 52.dp)) {
        if (isExpanded) {
            ChangeList(
                item = item,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onEvent(ActivityUiEvent.OnChangesToggled(item.id)) }) {
                Text(
                    stringResource(
                        if (isExpanded) R.string.activity_hide_changes else R.string.activity_show_changes,
                    ),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (isExpanded) 180f else 0f),
                )
            }
            if (isExpanded && !item.isTargetDeleted) {
                TextButton(onClick = { onEvent(ActivityUiEvent.OnOpenTargetClicked(item.id)) }) {
                    Text(openLabelOf(item))
                }
            }
        }
    }
}

// Why: "Valor  R$ 612,00 → R$ 642,00", one line per field, as the bill's history writes them.
@Composable
internal fun ChangeList(
    item: ActivityItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item.changes.forEach { change ->
            val field = fieldLabelOf(change.field)
            val before = changeValueTextOf(change.field, change.before)
            val after = changeValueTextOf(change.field, change.after)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { contentDescription = "$field: $before → $after" },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = field,
                    modifier = Modifier.width(96.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = before,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "→",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = after,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// Why: a row's place while a new device waits for its first page.
@Composable
internal fun PlaceholderRow(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val widths = listOf(0.88f, 0.72f, 0.8f, 0.64f, 0.76f)
    val line = MaterialTheme.colorScheme.surfaceContainerHigh
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = groupedRowShapeOf(index, count),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 14.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(line, CircleShape),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(widths[index % widths.size])
                        .height(14.dp)
                        .background(line, CircleShape),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(widths[(index + 2) % widths.size] * 0.7f)
                        .height(12.dp)
                        .background(line, CircleShape),
                )
            }
        }
    }
}
