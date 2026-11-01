package com.quare.blitzsplit.android.feature.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.relativeDateTimeTextOf
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityItem
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityUiEvent
import kotlinx.datetime.TimeZone

/*
 * Why: the item picked beside the list (a tablet lying down, an unfolded book): who did what, in which group and
 * when, what changed or its detail, who deleted it, the button that opens it and "Mais em" the bill's other events.
 */
@Composable
internal fun ActivityDetailPane(
    item: ActivityItem?,
    related: List<ActivityItem>,
    dates: DateTimeTextFormatter,
    nowMillis: Long,
    onEvent: (ActivityUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (item == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.activity_detail_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActorAvatarIcon(item = item)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = sentenceTextOf(item),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GroupDotIcon(color = item.groupColor)
                    Text(
                        text = whenTextOf(item, dates, nowMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (item.changes.isNotEmpty()) {
            Text(
                text = stringResource(R.string.activity_detail_what_changed),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                ChangeList(
                    item = item,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            detailTextOf(item)?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        item.deletion?.let { deletion ->
            Text(
                text = deletedNoticeTextOf(deletion, dates),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalButton(
            onClick = { onEvent(ActivityUiEvent.OnOpenTargetClicked(item.id)) },
            enabled = !item.isTargetDeleted,
        ) {
            Text(openLabelOf(item))
        }
        if (related.isNotEmpty()) {
            Text(
                text = stringResource(R.string.activity_detail_more_in, item.subject.orEmpty()),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                related.forEachIndexed { index, other ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = groupedRowShapeOf(index, related.size),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        onClick = { onEvent(ActivityUiEvent.OnItemClicked(other.id)) },
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 12.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = sentenceTextOf(other),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = whenTextOf(other, dates, nowMillis),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// Why: "Viagem Floripa · Hoje, 1 de nov. · 21:40", or "Aguardando envio" for a change not up yet.
@Composable
private fun whenTextOf(
    item: ActivityItem,
    dates: DateTimeTextFormatter,
    nowMillis: Long,
): String {
    val time = if (item.isPending) {
        stringResource(R.string.activity_pending)
    } else {
        relativeDateTimeTextOf(
            epochMillis = item.occurredAt,
            day = SpentDay.relativeDayOf(item.occurredAt, nowMillis, TimeZone.currentSystemDefault()),
            dates = dates,
        )
    }
    return item.groupName?.let { stringResource(R.string.activity_group_and_time, it, time) } ?: time
}
