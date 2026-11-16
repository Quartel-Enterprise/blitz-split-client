package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.CategoryBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toDataColor
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsCategoryRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsCategoryTarget

/*
 * Why: how the ring and its legend share the slice the pointer is over, so either one highlights it in the other, and
 * open the category a tap or a click lands on. It reads the state each time, since the pointer handlers keep the pick
 * of the composition that started them.
 */
private class CategoryPick(
    private val state: State<Int?>,
    private val count: Int,
    // Why: the index, or null to let go.
    val onPick: (index: Int?) -> Unit,
    val onOpen: (index: Int, target: StatsCategoryTarget) -> Unit,
) {
    val selected: Int?
        get() = state.value?.takeIf { it in 0 until count }
}

/*
 * Why: "Por categoria" (6.6): the pie with the largest category in its center and the legend with the percentages,
 * then each main category, each in its fixed color.
 */
@Composable
internal fun StatsCategoriesSection(
    state: GroupStatsUiState,
    money: MoneyFormatter,
    onEvent: (GroupStatsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    pieSize: Dp = 132.dp,
) {
    val rows = state.categories
    if (rows.isEmpty()) return
    val selectedState = remember { mutableStateOf<Int?>(null) }
    // Why: the same keys as iOS and the web: the period and each category's share.
    LaunchedEffect(state.periodStartMillis, state.periodEndMillis, rows.map { it.category.id to it.percent }) {
        selectedState.value = null
    }
    val pick = CategoryPick(
        state = selectedState,
        count = rows.size,
        onPick = { index -> selectedState.value = index },
        onOpen = { index, target ->
            rows.getOrNull(index)?.let { onEvent(GroupStatsUiEvent.OnCategoryClicked(it.category.id, target)) }
        },
    )
    Column(modifier = modifier) {
        StatsSectionHeading(
            text = stringResource(R.string.group_stats_by_category),
            modifier = Modifier.padding(
                start = 4.dp,
                end = 4.dp,
                top = 20.dp,
                bottom = 12.dp,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(20.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryPieDisplay(
                rows = rows,
                size = pieSize,
                pick = pick,
                periodKey = state.periodStartMillis to state.periodEndMillis,
            )
            // Why: the legend keeps what the pie leaves, so on a narrow phone a name ends short of its percentage.
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                rows.forEachIndexed { index, row -> LegendLine(row, index, pick) }
            }
        }
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            rows.forEachIndexed { index, row ->
                val shape = groupedRowShapeOf(index, rows.size)
                CategoryRowItem(
                    row = row,
                    money = money,
                    modifier = Modifier
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest, shape)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.group_stats_category_open),
                        ) { pick.onOpen(index, StatsCategoryTarget.ROW) },
                )
            }
        }
    }
}

/*
 * Why: the ring by category: the slice the mouse is over (or its legend line) pops out, the others fade, and the
 * middle names it with its share; without one, the middle names the largest category. A tap or a click on a slice
 * opens its category's bills.
 */
@Composable
private fun CategoryPieDisplay(
    rows: List<StatsCategoryRow>,
    size: Dp,
    pick: CategoryPick,
    periodKey: Any,
) {
    val isDark = LocalIsDarkTheme.current
    val parts = rows.map {
        stringResource(
            R.string.group_stats_category_pie_part,
            categoryName(it.category),
            it.percent,
        )
    }
    val shown = rows.getOrNull(pick.selected ?: -1) ?: rows.first()
    StatsRingDisplay(
        slices = rows.map { StatsRingSlice(it.percent, it.category.color.toDataColor(isDark)) },
        selected = pick.selected,
        sweepKey = periodKey,
        description = stringResource(R.string.group_stats_category_pie, parts.joinToString(", ")),
        size = size,
        onPick = pick.onPick,
        onTap = { index, _ -> pick.onOpen(index, StatsCategoryTarget.RING) },
    ) {
        StatsRingLabel(
            key = shown,
            percent = shown.percent,
            label = categoryName(shown.category),
        )
    }
}

// Why: a legend line highlights its slice with the mouse over it and opens it with a tap, so even a 2% slice is easy to reach.
@Composable
private fun LegendLine(
    row: StatsCategoryRow,
    index: Int,
    pick: CategoryPick,
) {
    val isDark = LocalIsDarkTheme.current
    val isSelected = pick.selected == index
    Row(
        modifier = Modifier
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.group_stats_category_open),
            ) { pick.onOpen(index, StatsCategoryTarget.LEGEND) }
            .onMouseHover(
                key = index,
                onMove = { pick.onPick(index) },
                onExit = { pick.onPick(null) },
            ).alpha(if (pick.selected == null || isSelected) 1f else RING_DIMMED_ALPHA)
            .padding(
                vertical = 3.dp,
                horizontal = 4.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(row.category.color.toDataColor(isDark), RoundedCornerShape(4.dp)),
        )
        Text(
            text = categoryName(row.category),
            modifier = Modifier.weight(
                weight = 1f,
                fill = false,
            ),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.group_stats_category_percent, row.percent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = false,
            maxLines = 1,
        )
    }
}

// Why: "Casa R$ 8.000,00 · 80%", and its bar.
@Composable
private fun CategoryRowItem(
    row: StatsCategoryRow,
    money: MoneyFormatter,
    modifier: Modifier = Modifier,
) {
    val name = categoryName(row.category)
    val share = stringResource(R.string.group_stats_category_share, name, row.percent)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 14.dp,
            ).semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CategoryBadge(category = row.category)
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = share },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = money.toText(row.amountMinor),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            StatsAmountBarIndicator(
                fraction = row.barFraction.toFloat(),
                color = row.category.color.toDataColor(LocalIsDarkTheme.current),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
}
