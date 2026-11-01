package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent

private val cardShape = RoundedCornerShape(24.dp)

// Why: the canvas's sizes of each card: its shape, padding, name and balance.
private class CardMetrics(
    val badge: Dp,
    val padding: Dp,
    val name: TextUnit,
    val nameLineHeight: TextUnit,
    val amount: TextUnit,
)

private fun cardMetricsOf(size: GroupGridSize): CardMetrics = when (size) {
    GroupGridSize.COMPACT -> CardMetrics(
        badge = 40.dp,
        padding = 12.dp,
        name = 14.sp,
        nameLineHeight = 18.sp,
        amount = 15.sp,
    )

    GroupGridSize.STANDARD -> CardMetrics(
        badge = 48.dp,
        padding = 16.dp,
        name = 16.sp,
        nameLineHeight = 20.sp,
        amount = 17.sp,
    )

    GroupGridSize.LARGE -> CardMetrics(
        badge = 56.dp,
        padding = 20.dp,
        name = 18.sp,
        nameLineHeight = 24.sp,
        amount = 22.sp,
    )
}

/*
 * Why: the minimum width of a card of each size; the columns are as many as fit the list's width (a phone 3, 2 and
 * 1; a tablet's centred column 4, 3 and 2).
 */
internal fun minCardWidthOf(size: GroupGridSize): Dp = when (size) {
    GroupGridSize.COMPACT -> 104.dp
    GroupGridSize.STANDARD -> 144.dp
    GroupGridSize.LARGE -> 220.dp
}

/*
 * Why: a card of the grid (5.6): the row's content stacked, the group's shape, its name in up to two lines, the faces
 * and the last activity (in Compacto only the dot and "na fila"), and the balance with its word. Its ⋮ and its long
 * press hold the same actions as the row's. The name always takes two lines, so the cards of a line line up.
 */
@Composable
internal fun GroupGridCard(
    group: GroupRow,
    size: GroupGridSize,
    isOnline: Boolean,
    onEvent: (GroupsUiEvent) -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = cardMetricsOf(size)
    val money = rememberMoneyFormatter(group.currency)
    val dates = rememberDateTimeTextFormatter()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onEvent(GroupsUiEvent.OnGroupClicked(group.id)) },
                        onLongClickLabel = stringResource(R.string.groups_actions_title, group.name),
                        onLongClick = onLongPress,
                    ).padding(metrics.padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GroupBadge(
                    initials = group.initials,
                    color = group.color,
                    size = metrics.badge,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = metrics.name,
                        lineHeight = metrics.nameLineHeight,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.heightIn(min = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (size != GroupGridSize.COMPACT) MemberFacesRow(group = group)
                    if (group.isPending) PendingIndicator()
                    val detail = if (size == GroupGridSize.COMPACT) {
                        if (group.isPending) stringResource(R.string.groups_meta_queued) else null
                    } else {
                        detailTextOf(
                            group = group,
                            amountText = money::toAmountText,
                            dates = dates,
                        )
                    }
                    detail?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (group.isSettled) {
                    SettledLabel()
                } else {
                    Column {
                        Text(
                            text = money.toSignedText(group.netMinor),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = metrics.amount),
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
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
            ) {
                GroupRowMenuButton(
                    group = group,
                    isOnline = isOnline,
                    onEvent = onEvent,
                )
            }
        }
    }
}
