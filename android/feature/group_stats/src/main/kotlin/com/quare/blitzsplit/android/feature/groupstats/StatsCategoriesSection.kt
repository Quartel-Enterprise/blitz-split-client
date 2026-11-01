package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
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
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsCategoryRow

private const val FULL_TURN = 360f
private const val START_ANGLE = -90f
private const val SLICE_GAP_DEGREES = 2f
private const val PERCENT = 100f
private const val RING_FRACTION = 0.16f

/*
 * Why: "Por categoria" (6.6): the pie with the largest category in its center and the legend with the percentages,
 * then each main category with its subcategories under it, each in its fixed color.
 */
@Composable
internal fun StatsCategoriesSection(
    state: GroupStatsUiState,
    money: MoneyFormatter,
    modifier: Modifier = Modifier,
    pieSize: Dp = 132.dp,
) {
    val rows = state.categories
    if (rows.isEmpty()) return
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
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row -> LegendLine(row) }
            }
        }
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            rows.forEachIndexed { index, row ->
                CategoryRowItem(
                    row = row,
                    money = money,
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        groupedRowShapeOf(index, rows.size),
                    ),
                )
            }
        }
    }
}

// Why: a ring of slices with a thin gap between them, the largest category's name and share in the middle.
@Composable
private fun CategoryPieDisplay(
    rows: List<StatsCategoryRow>,
    size: Dp,
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
    val top = rows.first()
    Box(
        modifier = Modifier
            .size(size)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = this.size.minDimension * RING_FRACTION
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            var angle = START_ANGLE
            val gap = if (rows.size > 1) SLICE_GAP_DEGREES else 0f
            rows.forEachIndexed { index, row ->
                val sweep = row.percent / PERCENT * FULL_TURN
                if (sweep > gap) {
                    drawArc(
                        color = colors[index],
                        startAngle = angle,
                        sweepAngle = sweep - gap,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                }
                angle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.group_stats_category_percent, top.percent),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = categoryName(top.category),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun LegendLine(row: StatsCategoryRow) {
    val isDark = LocalIsDarkTheme.current
    Row(
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

// Why: "Casa R$ 8.000,00 · 80%", its bar, and "Aluguel R$ 7.000,00 · Condomínio R$ 1.000,00" under it.
@Composable
private fun CategoryRowItem(
    row: StatsCategoryRow,
    money: MoneyFormatter,
    modifier: Modifier = Modifier,
) {
    val name = categoryName(row.category)
    val share = stringResource(R.string.group_stats_category_share, name, row.percent)
    val subcategories = row.subcategories.map { "${categoryName(it.category)} ${money.toText(it.amountMinor)}" }
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
            if (subcategories.isNotEmpty()) {
                Text(
                    text = subcategories.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
