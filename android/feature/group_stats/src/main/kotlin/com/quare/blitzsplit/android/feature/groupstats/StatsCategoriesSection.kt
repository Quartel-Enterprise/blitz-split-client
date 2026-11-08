package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.CategoryBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.categoryName
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsCategoryRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsCategoryTarget
import kotlin.math.atan2
import kotlin.math.hypot

private const val FULL_TURN = 360f
private const val START_ANGLE = -90f
private const val SLICE_GAP_DEGREES = 2f
private const val PERCENT = 100f
private const val RING_FRACTION = 0.16f
private const val DIMMED_ALPHA = 0.4f
private const val SWEEP_MILLIS = 600
private const val HALF = 2f
private const val HOLE_FRACTION = 0.4f
private val popOut = 6.dp

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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
 * Why: a ring of slices with a thin gap between them, sweeping in when the period changes. The slice the mouse is over
 * (or its legend line) pops out, the others fade, and the middle names it with its share; without one, the middle
 * names the largest category. A tap or a click on a slice opens its category's bills.
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
    val description = stringResource(R.string.group_stats_category_pie, parts.joinToString(", "))
    val colors = rows.map { it.category.color.toContainerColor(isDark) }
    val pops = rows.indices.map { index ->
        animateFloatAsState(if (pick.selected == index) 1f else 0f, label = "slice").value
    }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(periodKey) {
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(SWEEP_MILLIS))
    }
    val shown = rows.getOrNull(pick.selected ?: -1) ?: rows.first()
    Box(
        modifier = Modifier
            .size(size)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(rows) {
                    detectPickTaps { offset, _ ->
                        // Why: this.size is the pointer area's, in pixels; a bare size is the ring's Dp parameter.
                        findSliceAt(offset, this.size.width.toFloat(), rows)?.let {
                            pick.onOpen(it, StatsCategoryTarget.RING)
                        }
                    }
                }.onMouseHover(
                    key = rows,
                    onMove = { position -> pick.onPick(findSliceAt(position, this.size.width.toFloat(), rows)) },
                    onExit = { pick.onPick(null) },
                ),
        ) {
            val margin = popOut.toPx()
            val stroke = (this.size.minDimension - margin * HALF) * RING_FRACTION
            var angle = START_ANGLE
            val gap = if (rows.size > 1) SLICE_GAP_DEGREES else 0f
            rows.forEachIndexed { index, row ->
                val slice = row.percent / PERCENT * FULL_TURN * sweep.value
                if (slice > gap) {
                    // Why: the picked slice grows outward only, its inner edge staying on the ring.
                    val extra = margin * pops[index]
                    val inset = margin + stroke / HALF - extra / HALF
                    val color = colors[index]
                    drawArc(
                        color = if (pick.selected == null || pick.selected == index) {
                            color
                        } else {
                            color.copy(alpha = DIMMED_ALPHA)
                        },
                        startAngle = angle,
                        sweepAngle = slice - gap,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(this.size.width - inset * HALF, this.size.height - inset * HALF),
                        style = Stroke(width = stroke + extra),
                    )
                }
                angle += slice
            }
        }
        Crossfade(
            targetState = shown,
            label = "center",
        ) { row ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.group_stats_category_percent, row.percent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = categoryName(row.category),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
}

/*
 * Why: the slice under a point of the ring, by its angle from the top clockwise; null deep in the hole (a touch just
 * inside the ring still picks its slice) or outside it.
 */
private fun findSliceAt(
    point: Offset,
    side: Float,
    rows: List<StatsCategoryRow>,
): Int? {
    val radius = side / HALF
    val dx = point.x - radius
    val dy = point.y - radius
    val distance = hypot(dx, dy)
    if (distance > radius || distance < radius * HOLE_FRACTION) return null
    val degrees = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
    val fromTop = (degrees - START_ANGLE + FULL_TURN) % FULL_TURN
    var end = 0f
    rows.forEachIndexed { index, row ->
        end += row.percent / PERCENT * FULL_TURN
        if (fromTop < end) return index
    }
    return rows.lastIndex
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
            ).alpha(if (pick.selected == null || isSelected) 1f else DIMMED_ALPHA)
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
                .background(row.category.color.toContainerColor(isDark), RoundedCornerShape(4.dp)),
        )
        Text(
            text = categoryName(row.category),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.group_stats_category_percent, row.percent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            StatsAmountBarIndicator(fraction = row.barFraction.toFloat())
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
}
