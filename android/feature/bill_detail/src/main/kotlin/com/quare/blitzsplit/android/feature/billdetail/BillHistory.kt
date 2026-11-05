package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.PendingIndicator
import com.quare.blitzsplit.android.designsystem.component.ShimmerListSkeleton
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.component.opensProfile
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryChange
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryEntry
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryKind
import software.quare.loadable.valueOrNull

// Why: the rail runs from under one avatar to the next entry's, down the avatar's middle.
private val railTop = 38.dp
private const val SKELETON_ENTRIES = 3
private val railWidth = 2.dp
private val actorAvatarSize = 32.dp

/*
 * Why: "Histórico" (7.3f), by day, the most recent first: who did what, what changed, and "Desfazer" on the latest
 * edit while the bill is still as it left it. Shimmering while read, hidden while there is nothing in it.
 */
@Composable
internal fun BillHistorySection(
    state: BillDetailUiState,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    val history = state.history.valueOrNull()
    if (history?.isEmpty() == true) return
    val locale = LocalLocale.current.platformLocale
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BillSectionTitle(text = stringResource(R.string.bill_detail_history))
        // Why: online, the server's history is on its way; its lines shimmer meanwhile.
        if (history == null) {
            ShimmerListSkeleton(
                count = SKELETON_ENTRIES,
                leadingSize = actorAvatarSize,
                trailingWidth = null,
                containerShapeOf = { index -> groupedRowShapeOf(index, SKELETON_ENTRIES) },
                spacing = 2.dp,
            )
            return@Column
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = groupedRowShapeOf(
                index = 0,
                count = 1,
            ),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 4.dp,
                ),
            ) {
                history.forEach { day ->
                    Text(
                        text = historyDayTitleOf(day, dates).uppercase(locale),
                        modifier = Modifier
                            .padding(
                                top = 12.dp,
                                bottom = 8.dp,
                            ).semantics { heading() },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    day.entries.forEachIndexed { index, entry ->
                        HistoryEntryRow(
                            entry = entry,
                            showsRail = index < day.entries.lastIndex,
                            state = state,
                            money = money,
                            dates = dates,
                            onEvent = onEvent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryRow(
    entry: BillHistoryEntry,
    showsRail: Boolean,
    state: BillDetailUiState,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    val railColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (!showsRail) return@drawBehind
                val half = actorAvatarSize.toPx() / 2
                val x = if (layoutDirection == LayoutDirection.Ltr) half else size.width - half
                drawLine(
                    color = railColor,
                    start = Offset(x, railTop.toPx()),
                    end = Offset(x, size.height - 2.dp.toPx()),
                    strokeWidth = railWidth.toPx(),
                    cap = StrokeCap.Round,
                )
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ActorAvatarIcon(
            entry = entry,
            onClick = { entry.actorMemberId?.let { onEvent(BillDetailUiEvent.OnPersonClicked(it)) } },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    top = 5.dp,
                    bottom = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EntryHeadlineRow(
                    entry = entry,
                    dates = dates,
                )
                historyDiffsOf(
                    entry = entry,
                    categories = state.categories,
                    money = money,
                    dates = dates,
                ).forEach { diff -> DiffLine(diff) }
                historyNoteOf(entry, money)?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (entry.canUndo) {
                TextButton(
                    onClick = { onEvent(BillDetailUiEvent.OnUndoClicked) },
                    modifier = Modifier.offset(x = (-12).dp),
                    enabled = !state.isUndoing,
                ) {
                    Text(stringResource(R.string.bill_detail_undo))
                }
            }
        }
    }
}

// Why: "Ana trocou a categoria · 9:12", the amber dot before the time while it waits to go up.
@Composable
private fun EntryHeadlineRow(
    entry: BillHistoryEntry,
    dates: DateTimeTextFormatter,
) {
    val who = personNameOf(entry.actorName, entry.actorIsMe)
    val what = historyVerbOf(entry)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(who) }
                append(" ")
                append(what)
            },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (entry.isPending) PendingIndicator()
            Text(
                text = dates.toTimeText(entry.occurredAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Why: "Carla  R$ 112,00 → R$ 98,00", the old value struck through, the new one marked; read as one sentence.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiffLine(diff: HistoryDiff) {
    val spoken = stringResource(R.string.bill_detail_change_spoken, diff.before, diff.after)
    FlowRow(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = diff.field?.let { "$it, $spoken" } ?: spoken
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        diff.field?.let { field ->
            Text(
                text = field,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = diff.before,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(6.dp))
                .padding(
                    horizontal = 6.dp,
                    vertical = 1.dp,
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = TextDecoration.LineThrough,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = diff.after,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                .padding(
                    horizontal = 6.dp,
                    vertical = 1.dp,
                ),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

// Why: who did it, with what they did at its corner, as Atividade draws its rows.
@Composable
private fun ActorAvatarIcon(
    entry: BillHistoryEntry,
    onClick: () -> Unit,
) {
    val rowColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val name = personNameOf(entry.actorName, entry.actorIsMe)
    Box {
        // Why: the actor's face opens their profile while they are in the group, named for TalkBack.
        AvatarBadge(
            initial = entry.actorInitials,
            color = entry.actorColor,
            photoUrl = entry.actorPhotoUrl,
            size = actorAvatarSize,
            modifier = Modifier
                .clip(CircleShape)
                .semantics { if (entry.actorMemberId != null) contentDescription = name }
                .opensProfile(onClick.takeIf { entry.actorMemberId != null }),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(
                    x = 4.dp,
                    y = 4.dp,
                ).size(18.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(2.dp, rowColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconOf(entry),
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

private fun iconOf(entry: BillHistoryEntry): ImageVector = when (entry.kind) {
    BillHistoryKind.CREATED -> Icons.Outlined.Add

    BillHistoryKind.EDITED -> if (entry.changes.firstOrNull() is BillHistoryChange.Category) {
        Icons.Outlined.Sell
    } else {
        Icons.Outlined.Edit
    }

    BillHistoryKind.ATTACHMENTS_ADDED,
    BillHistoryKind.ATTACHMENT_REMOVED,
    -> if (entry.isReceipt) Icons.AutoMirrored.Outlined.ReceiptLong else Icons.Outlined.Image

    BillHistoryKind.LINK_CHANGED -> if (entry.linkVisibility == null) Icons.Outlined.LinkOff else Icons.Outlined.Link
}
