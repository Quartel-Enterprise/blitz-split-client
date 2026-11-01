package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.paidTextOrNull
import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState

private const val DISABLED_ALPHA = 0.38f
private val cardShape = RoundedCornerShape(20.dp)
private val sectionPadding = Modifier.padding(
    start = 4.dp,
    end = 4.dp,
    top = 20.dp,
    bottom = 8.dp,
)

// Why: "Ao longo do tempo": Barras or Linha, the grain, the chart, the most expensive one and why some grains are off.
@Composable
internal fun StatsOverTimeSection(
    state: GroupStatsUiState,
    money: MoneyFormatter,
    dates: StatsTextFormatter,
    onEvent: (GroupStatsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = stringResource(R.string.group_stats_today)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(
                start = 4.dp,
                end = 4.dp,
                top = 12.dp,
                bottom = 4.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatsSectionHeading(stringResource(R.string.group_stats_over_time), modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                StatsChartTypeToggle(
                    isSelected = state.chartType == StatsChartType.BARS,
                    label = stringResource(R.string.group_stats_bars),
                    onClick = { onEvent(GroupStatsUiEvent.OnChartTypePicked(StatsChartType.BARS)) },
                ) { Icon(Icons.Outlined.BarChart, contentDescription = null) }
                StatsChartTypeToggle(
                    isSelected = state.chartType == StatsChartType.LINE,
                    label = stringResource(R.string.group_stats_line),
                    onClick = { onEvent(GroupStatsUiEvent.OnChartTypePicked(StatsChartType.LINE)) },
                ) { Icon(Icons.AutoMirrored.Outlined.ShowChart, contentDescription = null) }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, cardShape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatsGrainRow(
                state = state,
                onEvent = onEvent,
            )
            StatsChartImage(
                state = state,
                description = chartDescriptionOf(state, dates, money),
                labelOf = { bar -> dates.toBarLabel(bar, state.grain, state.hasSparseLabels, today) },
            )
            val peak = state.peak
            peakLabelOf(state, dates)?.let { label ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = money.toText(peak?.amountMinor ?: 0L),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            grainNoteTextOf(state)?.let { note ->
                Text(
                    note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatsChartTypeToggle(
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    IconToggleButton(
        checked = isSelected,
        onCheckedChange = { if (!isSelected) onClick() },
        colors = IconButtonDefaults.iconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.semantics {
            contentDescription = label
            role = Role.RadioButton
        },
    ) { icon() }
}

// Why: "Dia", "Semana", "Mês" and "Ano" as the connected group; one the period can't show is dimmed and does nothing.
@Composable
private fun StatsGrainRow(
    state: GroupStatsUiState,
    onEvent: (GroupStatsUiEvent) -> Unit,
) {
    val groupLabel = stringResource(R.string.group_stats_group_by)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .semantics { contentDescription = groupLabel },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val colors = MaterialTheme.colorScheme
        state.grains.forEachIndexed { index, option ->
            val isSelected = option.grain == state.grain
            val outer = 18.dp
            val inner = 6.dp
            val start = if (isSelected || index == 0) outer else inner
            val end = if (isSelected || index == state.grains.lastIndex) outer else inner
            Surface(
                onClick = { onEvent(GroupStatsUiEvent.OnGrainPicked(option.grain)) },
                enabled = option.isEnabled,
                shape = RoundedCornerShape(
                    topStart = start,
                    bottomStart = start,
                    topEnd = end,
                    bottomEnd = end,
                ),
                color = if (isSelected) colors.primary else colors.surfaceContainerHigh,
                contentColor = if (isSelected) colors.onPrimary else colors.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .alpha(if (option.isEnabled) 1f else DISABLED_ALPHA)
                    .semantics {
                        role = Role.RadioButton
                        selected = isSelected
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(grainLabelOf(option.grain)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// Why: "Maior despesa", which opens the bill.
@Composable
internal fun StatsBiggestSection(
    state: GroupStatsUiState,
    money: MoneyFormatter,
    times: DateTimeTextFormatter,
    onEvent: (GroupStatsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expense = state.biggestExpense ?: return
    Column(modifier = modifier) {
        StatsSectionHeading(stringResource(R.string.group_stats_biggest), modifier = sectionPadding)
        Surface(
            onClick = { onEvent(GroupStatsUiEvent.OnBiggestExpenseClicked) },
            shape = cardShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        expense.description,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val whenText = "${times.toDateText(expense.spentAt)}, ${times.toTimeText(expense.spentAt)}"
                    val paid = paidTextOrNull(
                        expense.payers,
                        isMe = { it.isMe },
                        nameOf = { it.name },
                    )
                    Text(
                        text = paid?.let {
                            stringResource(
                                R.string.group_stats_biggest_meta,
                                whenText,
                                it,
                            )
                        } ?: whenText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(money.toText(expense.amountMinor), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
