package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.paidTextOrNull
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.feature.groups.presentation.model.HomeBill
import com.quare.blitzsplit.feature.groups.presentation.model.HomeBillDay
import com.quare.blitzsplit.feature.groups.presentation.model.HomeGroup
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState

private const val SKELETON_BILLS = 3
private val groupCardShape = RoundedCornerShape(24.dp)

// Why: "Seus grupos", up to three cards in a row: the pinned first, then the latest activity.
@Composable
internal fun HomeGroupsSection(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    metrics: HomeMetrics,
) {
    if (state.groups.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HomeSectionHeader(
            title = stringResource(R.string.groups_home_groups_title),
            action = stringResource(R.string.groups_home_see_all_groups),
            onAction = { onEvent(HomeUiEvent.OnSeeAllGroupsClicked) },
            modifier = Modifier.padding(horizontal = metrics.gutter),
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = metrics.gutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.groups.forEach { group ->
                HomeGroupCard(
                    group = group,
                    metrics = metrics,
                    onClick = { onEvent(HomeUiEvent.OnGroupClicked(group.row.id)) },
                )
            }
        }
    }
}

/*
 * Why: a group's card: its shape, the pin "Fixar na Início" left, its name, what Grupos' row says under it (the last
 * activity, "na fila · Mercado") and the balance with its word.
 */
@Composable
private fun HomeGroupCard(
    group: HomeGroup,
    metrics: HomeMetrics,
    onClick: () -> Unit,
) {
    val row = group.row
    val money = rememberMoneyFormatter(row.currency)
    val dates = rememberDateTimeTextFormatter()
    val amount = money.toSignedText(row.netMinor)
    val word = stringResource(balanceLabelOf(row.balanceKind))
    val description = stringResource(
        if (group.isPinned) R.string.groups_home_pinned_group_description else R.string.groups_home_group_description,
        row.name,
        amount,
        word,
    )
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(
                width = metrics.groupCardWidth,
                height = metrics.groupCardHeight,
            ).semantics { contentDescription = description },
        shape = groupCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics {}
                .padding(
                    horizontal = if (metrics.isCompact) 14.dp else 16.dp,
                    vertical = if (metrics.isCompact) 12.dp else 14.dp,
                ),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                GroupBadge(
                    initials = row.initials,
                    color = row.color,
                    size = if (metrics.isCompact) 36.dp else 40.dp,
                )
                if (group.isPinned) {
                    Box(modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.TopEnd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Column {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = if (metrics.isCompact) 14.sp else 15.sp,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (row.isPending) PendingIndicator()
                    Text(
                        text = detailTextOf(
                            group = row,
                            amountText = money::toAmountText,
                            dates = dates,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = if (metrics.isCompact) 16.sp else 17.sp,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = amountColorOf(row.netMinor),
                    maxLines = 1,
                )
                Text(
                    text = word,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/*
 * Why: "Últimas despesas", the five latest bills you paid or take part in, by day, the payments left to Atividade.
 * While the first download fills the device, grey rows hold their place.
 */
@Composable
internal fun HomeBillsSection(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    metrics: HomeMetrics,
) {
    if (state.billDays.isEmpty() && !state.isFirstDownload) return
    Column(modifier = Modifier.padding(horizontal = metrics.gutter)) {
        HomeSectionHeader(
            title = stringResource(R.string.groups_home_bills_title),
            action = stringResource(R.string.groups_home_see_all_bills),
            onAction = { onEvent(HomeUiEvent.OnSeeAllBillsClicked) },
        )
        state.billDays.forEach { day ->
            BillDayColumn(
                day = day,
                metrics = metrics,
                onEvent = onEvent,
            )
        }
        if (state.isFirstDownload) BillSkeletonColumn()
    }
}

@Composable
private fun BillDayColumn(
    day: HomeBillDay,
    metrics: HomeMetrics,
    onEvent: (HomeUiEvent) -> Unit,
) {
    val dates = rememberDateTimeTextFormatter()
    Text(
        text = when (day.relativeDay) {
            RelativeDay.TODAY -> stringResource(R.string.groups_home_today)
            RelativeDay.YESTERDAY -> stringResource(R.string.groups_home_yesterday)
            RelativeDay.OTHER -> dates.toWeekdayDateText(day.dayStartMillis)
        },
        modifier = Modifier
            .padding(
                horizontal = 4.dp,
                vertical = 8.dp,
            ).semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        day.bills.forEachIndexed { index, bill ->
            HomeBillRow(
                bill = bill,
                shape = groupedRowShapeOf(index, day.bills.size),
                isCompact = metrics.isCompact,
                onClick = { onEvent(HomeUiEvent.OnBillClicked(bill.groupId, bill.id)) },
            )
        }
    }
}

/*
 * Why: a bill: "Mercado", "Viagem Floripa · Você pagou" ("na fila · " while it waits to go up), its total and what it
 * does to your balance. Bills have no category yet, so every one wears the same receipt.
 */
@Composable
internal fun HomeBillRow(
    bill: HomeBill,
    shape: Shape,
    isCompact: Boolean,
    onClick: (() -> Unit)?,
) {
    val money = rememberMoneyFormatter(bill.currency)
    val paidBy = paidTextOrNull(
        bill.payers,
        isMe = { it.isMe },
        nameOf = { it.name },
    )
    val origin = paidBy?.let { stringResource(R.string.groups_search_bill_meta, bill.groupName, it) } ?: bill.groupName
    val meta = if (bill.isPending) stringResource(R.string.groups_meta_queued_bill, origin) else origin
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 14.dp else 16.dp,
                vertical = if (isCompact) 10.dp else 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 40.dp else 44.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(if (isCompact) 12.dp else 14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = bill.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (bill.isPending) PendingIndicator()
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = money.toText(bill.totalMinor),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                if (bill.myBalanceMinor != 0L) {
                    Text(
                        text = stringResource(
                            R.string.groups_home_amount_with_word,
                            money.toSignedText(bill.myBalanceMinor),
                            stringResource(
                                if (bill.myBalanceMinor > 0) {
                                    R.string.groups_balance_to_receive
                                } else {
                                    R.string.groups_balance_you_owe
                                },
                            ),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = amountColorOf(bill.myBalanceMinor),
                        maxLines = 1,
                    )
                }
            }
        }
    }
    val rowModifier = Modifier.fillMaxWidth()
    val color = MaterialTheme.colorScheme.surfaceContainerLowest
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = rowModifier,
            shape = shape,
            color = color,
            content = content,
        )
    } else {
        Surface(
            modifier = rowModifier,
            shape = shape,
            color = color,
            content = content,
        )
    }
}

@Composable
private fun BillSkeletonColumn() {
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .clearAndSetSemantics {},
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(SKELETON_BILLS) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        groupedRowShapeOf(index, SKELETON_BILLS),
                    ).padding(
                        horizontal = 16.dp,
                        vertical = 12.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBox(
                    width = 44.dp,
                    height = 44.dp,
                    shape = RoundedCornerShape(14.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkeletonBox(
                        width = null,
                        height = 12.dp,
                        modifier = Modifier.fillMaxWidth(0.6f),
                    )
                    SkeletonBox(
                        width = null,
                        modifier = Modifier.fillMaxWidth(0.4f),
                    )
                }
                SkeletonBox(
                    width = 64.dp,
                    height = 14.dp,
                )
            }
        }
    }
}
