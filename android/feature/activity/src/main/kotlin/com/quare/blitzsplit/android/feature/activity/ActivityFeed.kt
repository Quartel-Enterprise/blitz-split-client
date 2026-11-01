package com.quare.blitzsplit.android.feature.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.EmptyStateColumn
import com.quare.blitzsplit.android.designsystem.component.InfoNoteBanner
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.core.activity.domain.model.ActivityType
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityEmptyState
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityStatus
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityUiEvent
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val PLACEHOLDER_COUNT = 5
private const val END_THRESHOLD = 4

@Composable
internal fun typeLabelOf(type: ActivityType): String = stringResource(
    when (type) {
        ActivityType.BILLS -> R.string.activity_type_bills
        ActivityType.PAYMENTS -> R.string.activity_type_payments
        ActivityType.COMMENTS -> R.string.activity_type_comments
        ActivityType.PEOPLE_AND_GROUPS -> R.string.activity_type_people
        ActivityType.CHARGES -> R.string.activity_type_charges
    },
)

/*
 * Why: what sits above the feed, or in the left column of a phone lying down: the connection's chip, the note of a
 * device that never synced, the filters with their chips, and "3 novas" with "Marcar tudo como lido".
 */
@Composable
internal fun ActivityToolsSection(
    state: ActivityUiState,
    onEvent: (ActivityUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.status?.let { status ->
            StatusChip(
                status = status,
                queuedCount = state.queuedCount,
                onRetry = { onEvent(ActivityUiEvent.OnRefresh) },
            )
        }
        if (state.showsNotSyncedNotice) NotSyncedBanner()
        if (state.showsTools) {
            FilterChipRow(
                state = state,
                onEvent = onEvent,
            )
            if (state.canMarkAllRead) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.activity_new_count, state.unreadCount, state.unreadCount),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = { onEvent(ActivityUiEvent.OnMarkAllReadClicked) }) {
                        Text(stringResource(R.string.activity_mark_all_read))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    status: ActivityStatus,
    queuedCount: Int,
    onRetry: () -> Unit,
) {
    val (icon, text) = when (status) {
        ActivityStatus.OFFLINE -> Icons.Outlined.CloudOff to if (queuedCount > 0) {
            pluralStringResource(R.plurals.activity_offline_queued, queuedCount, queuedCount)
        } else {
            stringResource(R.string.activity_offline_chip_none)
        }

        ActivityStatus.SERVER_ERROR -> Icons.Outlined.WarningAmber to stringResource(R.string.activity_server_error)

        ActivityStatus.SYNCING -> Icons.Outlined.Sync to stringResource(R.string.activity_syncing)
    }
    Row(
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = {},
            label = { Text(text) },
            leadingIcon = {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        if (status == ActivityStatus.SERVER_ERROR) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.activity_retry)) }
        }
    }
}

@Composable
private fun NotSyncedBanner() {
    InfoNoteBanner(text = stringResource(R.string.activity_not_synced))
}

/*
 * Why: "Grupo" and "Tipo" open "Filtrar atividade"; once picked they show what is picked with an X that removes it,
 * the person a contact's history opened it on too, and "Limpar" clears them all.
 */
@Composable
private fun FilterChipRow(
    state: ActivityUiState,
    onEvent: (ActivityUiEvent) -> Unit,
) {
    val filter = state.filter
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        state.personName?.let { name ->
            RemovableChip(
                label = name,
                onRemove = { onEvent(ActivityUiEvent.OnPersonChipRemoved) },
            )
        }
        val groupName = state.groupChipName
        if (groupName == null) {
            OpenFilterChip(
                label = stringResource(R.string.activity_filter_group),
                onEvent = onEvent,
            )
        } else {
            RemovableChip(
                label = groupName,
                onRemove = { onEvent(ActivityUiEvent.OnGroupChipRemoved) },
                onClick = { onEvent(ActivityUiEvent.OnFilterClicked) },
            )
        }
        val types = state.typeChips
        if (types.isEmpty()) {
            OpenFilterChip(
                label = stringResource(R.string.activity_filter_type),
                onEvent = onEvent,
            )
        } else {
            RemovableChip(
                label = types.singleOrNull()?.let { typeLabelOf(it) }
                    ?: pluralStringResource(R.plurals.activity_types_count, types.size, types.size),
                onRemove = { types.forEach { onEvent(ActivityUiEvent.OnTypeChipRemoved(it)) } },
                onClick = { onEvent(ActivityUiEvent.OnFilterClicked) },
            )
        }
        if (filter.isActive) {
            TextButton(onClick = { onEvent(ActivityUiEvent.OnClearFiltersClicked) }) {
                Text(stringResource(R.string.activity_filter_clear))
            }
        }
    }
}

@Composable
private fun OpenFilterChip(
    label: String,
    onEvent: (ActivityUiEvent) -> Unit,
) {
    FilterChip(
        selected = false,
        onClick = { onEvent(ActivityUiEvent.OnFilterClicked) },
        label = { Text(label) },
        leadingIcon = {
            Icon(
                Icons.Outlined.FilterAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

@Composable
private fun RemovableChip(
    label: String,
    onRemove: () -> Unit,
    onClick: () -> Unit = onRemove,
) {
    val remove = stringResource(R.string.activity_remove_filter, label)
    InputChip(
        selected = true,
        onClick = onClick,
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingIcon = {
            Icon(
                Icons.Outlined.Close,
                contentDescription = remove,
                modifier = Modifier
                    .size(18.dp)
                    .semantics { contentDescription = remove },
            )
        },
    )
}

/*
 * Why: the feed itself, day by day, with what its end says: "Carregando mais…" while an older page comes, the offline
 * note about the 90 days, the empty states, or the placeholders of a new device's first sync.
 */
internal fun LazyListScope.feedItems(
    state: ActivityUiState,
    dates: DateTimeTextFormatter,
    selectedId: String?,
    onEvent: (ActivityUiEvent) -> Unit,
) {
    if (state.showsPlaceholders) {
        items(PLACEHOLDER_COUNT) { index ->
            PlaceholderRow(
                index = index,
                count = PLACEHOLDER_COUNT,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 1.dp,
                ),
            )
        }
        return
    }
    state.emptyState?.let { emptyState ->
        item(key = "empty") {
            EmptyActivityContent(
                emptyState = emptyState,
                onAction = {
                    onEvent(
                        if (emptyState == ActivityEmptyState.NO_RESULTS) {
                            ActivityUiEvent.OnClearFiltersClicked
                        } else {
                            ActivityUiEvent.OnEmptyActionClicked
                        },
                    )
                },
            )
        }
    }
    state.days.forEach { day ->
        item(key = "day-${day.dayStartMillis}") {
            Text(
                text = dayTitleOf(day.dayStartMillis, day.relativeDay, dates),
                modifier = Modifier
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 16.dp,
                        bottom = 8.dp,
                    ).semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        itemsIndexed(day.items, key = { _, item -> item.id }) { index, item ->
            ActivityRow(
                item = item,
                index = index,
                count = day.items.size,
                isExpanded = item.id == state.expandedItemId,
                isSelected = item.id == selectedId,
                dates = dates,
                onEvent = onEvent,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 1.dp,
                ),
            )
        }
    }
    if (state.isLoadingMore) {
        item(key = "loading-more") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = stringResource(R.string.activity_loading_more),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (state.showsOfflineEnd) {
        item(key = "offline-end") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 16.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.activity_offline_end),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Why: "Nada por aqui ainda" leads to Grupos, "Nada com esses filtros" clears them.
@Composable
private fun EmptyActivityContent(
    emptyState: ActivityEmptyState,
    onAction: () -> Unit,
) {
    val isNoResults = emptyState == ActivityEmptyState.NO_RESULTS
    EmptyStateColumn(
        icon = if (isNoResults) Icons.Outlined.FilterAlt else Icons.Outlined.History,
        title = stringResource(if (isNoResults) R.string.activity_no_results_title else R.string.activity_empty_title),
        body = stringResource(if (isNoResults) R.string.activity_no_results_body else R.string.activity_empty_body),
        action = stringResource(
            if (isNoResults) R.string.activity_no_results_action else R.string.activity_empty_action,
        ),
        onAction = onAction,
    )
}

// Why: the list's end asks for the next page, of the device's items first, then online the server's older ones.
@Composable
internal fun EndReachedEffect(
    listState: LazyListState,
    hasMore: Boolean,
    onEndReached: () -> Unit,
) {
    val currentHasMore by rememberUpdatedState(hasMore)
    val currentOnEndReached by rememberUpdatedState(onEndReached)
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - END_THRESHOLD
        }.distinctUntilChanged()
            .filter { it && currentHasMore }
            .collect { currentOnEndReached() }
    }
}
