package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsBar

private val chartHeight = 128.dp
private val minBarHeight = 4.dp
private val dotRadius = 5.dp
private val dotBorder = 2.dp
private val lineWidth = 2.5.dp
private const val AREA_ALPHA = 0.55f
private const val DENSE_BARS = 20
private const val MEDIUM_BARS = 8

/*
 * Why: "Ao longo do tempo" drawn on a Canvas in the theme's primary color, bars or a line scaled by the most expensive
 * one, the labels the shared code marks under it, and the whole series as the chart's text for TalkBack.
 */
@Composable
internal fun StatsChartImage(
    state: GroupStatsUiState,
    description: String,
    labelOf: (StatsBar) -> String,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceContainerHigh
    val area = MaterialTheme.colorScheme.primaryContainer.copy(alpha = AREA_ALPHA)
    val card = MaterialTheme.colorScheme.surfaceContainerLowest
    val bars = state.bars
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .semantics { contentDescription = description },
        ) {
            if (bars.isEmpty()) return@Canvas
            val largest = bars.maxOf { it.amountMinor }.coerceAtLeast(1L).toFloat()
            val slot = size.width / bars.size
            if (state.chartType == StatsChartType.BARS) {
                val gap = when {
                    bars.size > DENSE_BARS -> 1.dp
                    bars.size > MEDIUM_BARS -> 3.dp
                    else -> 5.dp
                }.toPx()
                val radius = if (bars.size > DENSE_BARS) 2.dp.toPx() else 8.dp.toPx()
                bars.forEachIndexed { index, bar ->
                    val height = if (bar.amountMinor > 0) {
                        (bar.amountMinor / largest * size.height).coerceAtLeast(minBarHeight.toPx())
                    } else {
                        minBarHeight.toPx()
                    }
                    drawTopRoundedBar(
                        left = index * slot + gap,
                        width = (slot - gap * 2).coerceAtLeast(1f),
                        height = height,
                        radius = if (bar.amountMinor > 0) radius else 2.dp.toPx(),
                        color = if (bar.amountMinor > 0) primary else empty,
                    )
                }
            } else {
                val points = bars.mapIndexed { index, bar ->
                    Offset(
                        x = (index + 0.5f) * slot,
                        y = size.height - 4.dp.toPx() - bar.amountMinor / largest * (size.height - 12.dp.toPx()),
                    )
                }
                val line = Path().apply {
                    points.forEachIndexed { index, point ->
                        val isFirst = index == 0
                        if (isFirst) {
                            moveTo(point.x, point.y)
                        } else {
                            lineTo(point.x, point.y)
                        }
                    }
                }
                val fill = Path().apply {
                    addPath(line)
                    lineTo(points.last().x, size.height)
                    lineTo(points.first().x, size.height)
                    close()
                }
                drawPath(fill, area)
                drawPath(
                    path = line,
                    color = primary,
                    style = Stroke(
                        width = lineWidth.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
                bars.forEachIndexed { index, bar ->
                    if (bar.showsDot) {
                        drawCircle(
                            card,
                            radius = dotRadius.toPx(),
                            center = points[index],
                        )
                        drawCircle(
                            primary,
                            radius = (dotRadius - dotBorder).toPx(),
                            center = points[index],
                        )
                    }
                }
            }
        }
        StatsChartLabelsRow(
            bars = bars,
            boldsLast = !state.hasSparseLabels,
            labelOf = labelOf,
        )
    }
}

private fun DrawScope.drawTopRoundedBar(
    left: Float,
    width: Float,
    height: Float,
    radius: Float,
    color: Color,
) {
    val top = size.height - height
    val corner = CornerRadius(radius.coerceAtMost(width / 2))
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = top,
                right = left + width,
                bottom = size.height,
                topLeftCornerRadius = corner,
                topRightCornerRadius = corner,
                bottomLeftCornerRadius = CornerRadius(radius / 2),
                bottomRightCornerRadius = CornerRadius(radius / 2),
            ),
        )
    }
    drawPath(path, color)
}

// Why: each label centered under its bar, so a few labels of many bars still line up with them.
@Composable
private fun StatsChartLabelsRow(
    bars: List<StatsBar>,
    boldsLast: Boolean,
    labelOf: (StatsBar) -> String,
) {
    val labeled = bars.withIndex().filter { it.value.showsLabel }
    val colors = MaterialTheme.colorScheme
    Layout(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        content = {
            labeled.forEach { (index, bar) ->
                val isLast = boldsLast && index == bars.lastIndex
                Text(
                    text = labelOf(bar),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isLast) FontWeight.SemiBold else null,
                    color = if (isLast) colors.onSurface else colors.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val slot = constraints.maxWidth.toFloat() / bars.size.coerceAtLeast(1)
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { position, placeable ->
                val center = (labeled[position].index + 0.5f) * slot
                val x = (center - placeable.width / 2f).toInt().coerceIn(
                    0,
                    (constraints.maxWidth - placeable.width).coerceAtLeast(0),
                )
                placeable.place(x, 2.dp.roundToPx())
            }
        }
    }
}
