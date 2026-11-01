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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.ConnectedButtonRow
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeopleView
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriod
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPersonRow
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val sectionPadding = Modifier.padding(
    start = 4.dp,
    end = 4.dp,
    top = 20.dp,
    bottom = 8.dp,
)

// Why: the period chip and its menu; "Escolher datas…" asks for the days instead of picking at once.
@Composable
internal fun StatsPeriodChip(
    state: GroupStatsUiState,
    dates: StatsTextFormatter,
    onEvent: (GroupStatsUiEvent) -> Unit,
    onPickDates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Surface(
                onClick = { isOpen = true },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(periodLabelOf(state.period, state.isTrip), style = MaterialTheme.typography.labelLarge)
                    Icon(
                        Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            DropdownMenu(
                expanded = isOpen,
                onDismissRequest = { isOpen = false },
            ) {
                state.periodOptions.forEach { option ->
                    if (option.period == StatsPeriod.CUSTOM) HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(periodLabelOf(option.period, state.isTrip))
                                Text(
                                    text = periodHintOf(option, dates),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            isOpen = false
                            if (option.period == StatsPeriod.CUSTOM) {
                                onPickDates()
                            } else {
                                onEvent(GroupStatsUiEvent.OnPeriodPicked(option.period))
                            }
                        },
                        leadingIcon = {
                            Box(modifier = Modifier.size(24.dp)) {
                                if (option.period == state.period) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
        Text(
            text = dates.toRangeText(state.periodRangeStyle, state.periodStartMillis, state.periodEndMillis),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Why: the period's total, how many bills (the queue's with the orange dot) and the average, then this person's part.
@Composable
internal fun StatsSummaryHero(
    state: GroupStatsUiState,
    money: MoneyFormatter,
    modifier: Modifier = Modifier,
) {
    val summaryLabel = stringResource(R.string.group_stats_summary)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(28.dp))
            .padding(20.dp)
            .semantics { contentDescription = summaryLabel },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val content = MaterialTheme.colorScheme.onPrimaryContainer
        Text(
            stringResource(R.string.group_stats_total_spent),
            style = MaterialTheme.typography.titleSmall,
            color = content,
        )
        Text(
            text = money.toText(state.totalMinor),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = content,
        )
        val count = if (state.pendingCount > 0) {
            pluralStringResource(
                R.plurals.group_stats_expenses_queued,
                state.expenseCount,
                state.expenseCount,
                state.pendingCount,
            )
        } else {
            pluralStringResource(R.plurals.group_stats_expenses, state.expenseCount, state.expenseCount)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (state.pendingCount > 0) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                )
            }
            Text(
                text = stringResource(R.string.group_stats_meta, count, money.toText(state.averagePerPersonMinor)),
                style = MaterialTheme.typography.bodyMedium,
                color = content,
            )
        }
        val myShare = state.myShareMinor
        if (myShare != null) {
            HorizontalDivider(
                modifier = Modifier.padding(
                    top = 12.dp,
                    bottom = 8.dp,
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatsHeroFigureColumn(
                    label = stringResource(R.string.group_stats_my_share),
                    value = money.toText(myShare),
                    hint = stringResource(R.string.group_stats_of_total, state.mySharePercent),
                    modifier = Modifier.weight(1f),
                )
                StatsHeroFigureColumn(
                    label = stringResource(R.string.group_stats_you_paid),
                    value = money.toText(state.myPaidMinor ?: 0L),
                    hint = expenseCountTextOf(state.myPaidCount),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatsHeroFigureColumn(
    label: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    val content = MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = content,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = content,
        )
        Text(
            hint,
            style = MaterialTheme.typography.labelSmall,
            color = content,
        )
    }
}

@Composable
internal fun StatsSectionHeading(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

// Why: "Por pessoa": Consumo or Pagou, a bar per person scaled by the largest, and what each one means.
@Composable
internal fun StatsPeopleSection(
    state: GroupStatsUiState,
    money: MoneyFormatter,
    onEvent: (GroupStatsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val views = listOf(StatsPeopleView.CONSUMPTION, StatsPeopleView.PAID)
    Column(modifier = modifier) {
        StatsSectionHeading(stringResource(R.string.group_stats_by_person), modifier = sectionPadding)
        ConnectedButtonRow(
            labels = listOf(
                stringResource(R.string.group_stats_consumption),
                stringResource(R.string.group_stats_paid),
            ),
            selectedIndex = views.indexOf(state.peopleView),
            groupLabel = stringResource(R.string.group_stats_show_by_person),
            onClick = { index -> onEvent(GroupStatsUiEvent.OnPeopleViewPicked(views[index])) },
            height = 40.dp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            state.people.forEachIndexed { index, person ->
                StatsPersonRowItem(
                    person = person,
                    money = money,
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        groupedRowShapeOf(index, state.people.size),
                    ),
                )
            }
        }
        val showsPaid = state.peopleView == StatsPeopleView.PAID
        Text(
            text = stringResource(
                if (showsPaid) R.string.group_stats_note_paid else R.string.group_stats_note_consumption,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = 4.dp,
                end = 4.dp,
                top = 12.dp,
            ),
        )
    }
}

@Composable
private fun StatsPersonRowItem(
    person: StatsPersonRow,
    money: MoneyFormatter,
    modifier: Modifier = Modifier,
) {
    val formerMember = stringResource(DesignR.string.designsystem_former_member)
    val name =
        person.name?.let { if (person.isMe) stringResource(R.string.group_stats_you, it) else it } ?: formerMember
    val amount = money.toText(person.amountMinor)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 10.dp,
            ).semantics(mergeDescendants = true) { contentDescription = "$name: $amount" },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBadge(
            initial = person.initials,
            color = person.avatarColor,
            size = 36.dp,
        )
        val hasAmount = person.amountMinor > 0
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (hasAmount) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            StatsAmountBarIndicator(fraction = person.barFraction.toFloat())
        }
    }
}

@Composable
private fun StatsAmountBarIndicator(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}
