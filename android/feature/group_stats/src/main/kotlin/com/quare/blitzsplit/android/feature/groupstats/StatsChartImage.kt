package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsBar

private val minBarHeight = 4.dp
private val dotRadius = 5.dp
private val selectedDotRadius = 7.dp
private val dotBorder = 2.dp
private val lineWidth = 2.5.dp
private val gridWidth = 1.dp
private val averageWidth = 1.5.dp
private val gutterGap = 8.dp
private val calloutGap = 6.dp
private val labelGap = 6.dp // Why: the least room between two labels under the chart.
private const val AREA_ALPHA = 0.55f
private const val DIMMED_ALPHA = 0.35f
private const val GRID_ALPHA = 0.7f
private const val DENSE_BARS = 20
private const val MEDIUM_BARS = 8
private const val GROW_MILLIS = 450

/*
 * Why: "Over time" drawn on a Canvas in the theme's primary color, bars or a line scaled by the chart's top value line,
 * with dashed value lines labeled in a gutter on the end side, the dashed average, the labels the shared code marks
 * under it, and the whole series as the chart's text for TalkBack. A bar or point picked by touch, a drag, the mouse
 * or the arrow keys shows its period, amount and bills in a callout over it; touch keeps it until a second tap, a click
 * keeps what hovering picked. The bars grow from the base, and the line draws in from the start side, when the grain, the
 * period or the chart type changes.
 */
@Composable
internal fun StatsChartImage(
    state: GroupStatsUiState,
    money: MoneyFormatter,
    description: String,
    labelOf: (StatsBar) -> String,
    nameOf: (StatsBar) -> String,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val primary = colors.primary
    val empty = colors.surfaceContainerHigh
    val area = colors.primaryContainer.copy(alpha = AREA_ALPHA)
    val card = colors.surfaceContainerLowest
    val grid = colors.outlineVariant.copy(alpha = GRID_ALPHA)
    val average = colors.onSurfaceVariant
    val bars = state.bars
    val haptics = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    val tickStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val ticks = state.chartTicksMinor.filter { it > 0 }
    val tickTexts = remember(ticks, money, tickStyle, measurer) {
        ticks.map { measurer.measure(money.toCompactText(it), tickStyle) }
    }
    val top = state.chartTopMinor.toFloat()
    // Why: one state for the chart's whole life, so the pointer handlers, kept across compositions, always write to it.
    val selectedState = remember { mutableStateOf<Int?>(null) }
    // Why: the same keys as iOS and the web, so a sync that moves one amount keeps the callout open on every platform.
    LaunchedEffect(state.grain, state.periodStartMillis, state.periodEndMillis, state.chartType, bars.size) {
        selectedState.value = null
    }
    val selected = selectedState.value?.takeIf { it in bars.indices }
    val pick: (Int?, Boolean) -> Unit = { index, buzzes ->
        if (index != selectedState.value && index != null && buzzes) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
        selectedState.value = index
    }
    val growth = remember { Animatable(0f) }
    LaunchedEffect(state.grain, state.periodStartMillis, state.periodEndMillis, state.chartType) {
        growth.snapTo(0f)
        growth.animateTo(1f, tween(GROW_MILLIS))
    }
    val chartDensity = LocalDensity.current
    val gutterWidth = with(chartDensity) {
        val widest = tickTexts.maxOfOrNull { it.size.width } ?: 0
        if (widest == 0) 0.dp else widest.toDp() + gutterGap
    }
    val labelHalf = with(chartDensity) { ((tickTexts.firstOrNull()?.size?.height ?: 0) / 2).toDp() }
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = description }
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown || bars.isEmpty()) return@onKeyEvent false
                        val current = selected
                        when (event.key) {
                            Key.DirectionRight -> pick(((current ?: -1) + 1).coerceAtMost(bars.lastIndex), false)
                            Key.DirectionLeft -> pick(((current ?: bars.size) - 1).coerceAtLeast(0), false)
                            Key.MoveHome -> pick(0, false)
                            Key.MoveEnd -> pick(bars.lastIndex, false)
                            Key.Escape -> pick(null, false)
                            else -> return@onKeyEvent false
                        }
                        true
                    }.pointerInput(bars.size, gutterWidth) {
                        // Why: the width is read on each event, so a fold, a rotation or a resized window picks right.
                        detectPickTaps { offset, isMouse ->
                            val index = findIndexAt(offset.x, size.width - gutterWidth.toPx(), bars.size)
                            if (index != null) {
                                pick(if (!isMouse && index == selectedState.value) null else index, !isMouse)
                            }
                        }
                    }.pointerInput(bars.size, gutterWidth) {
                        detectHorizontalDragGestures(
                            onDragStart = { start ->
                                findIndexAt(start.x, size.width - gutterWidth.toPx(), bars.size)?.let { pick(it, true) }
                            },
                        ) { change, _ ->
                            findIndexAt(
                                change.position.x,
                                size.width - gutterWidth.toPx(),
                                bars.size,
                            )?.let { pick(it, true) }
                        }
                    }.onMouseHover(
                        key = bars.size to gutterWidth,
                        onMove = { position ->
                            findIndexAt(position.x, size.width - gutterWidth.toPx(), bars.size)?.let { pick(it, false) }
                        },
                        onExit = { pick(null, false) },
                    ),
            ) {
                if (bars.isEmpty()) return@Canvas
                val plotWidth = size.width - gutterWidth.toPx()
                val plotTop = labelHalf.toPx()
                val plotHeight = size.height - plotTop
                val slot = plotWidth / bars.size

                fun yOf(amount: Float): Float = size.height - amount / top * plotHeight
                drawGrid(
                    ticks = ticks,
                    texts = tickTexts,
                    plotWidth = plotWidth,
                    yOf = { yOf(it.toFloat()) },
                    color = grid,
                )
                val current = selected
                if (state.chartType == StatsChartType.BARS) {
                    val gap = when {
                        bars.size > DENSE_BARS -> 1.dp
                        bars.size > MEDIUM_BARS -> 3.dp
                        else -> 5.dp
                    }.toPx()
                    val radius = if (bars.size > DENSE_BARS) 2.dp.toPx() else 8.dp.toPx()
                    bars.forEachIndexed { index, bar ->
                        val barHeight = if (bar.amountMinor > 0) {
                            (bar.amountMinor / top * plotHeight * growth.value).coerceAtLeast(minBarHeight.toPx())
                        } else {
                            minBarHeight.toPx()
                        }
                        val color = if (bar.amountMinor > 0) primary else empty
                        drawTopRoundedBar(
                            left = index * slot + gap,
                            width = (slot - gap * 2).coerceAtLeast(1f),
                            height = barHeight,
                            radius = if (bar.amountMinor > 0) radius else 2.dp.toPx(),
                            color = if (current == null ||
                                current == index
                            ) {
                                color
                            } else {
                                color.copy(alpha = DIMMED_ALPHA)
                            },
                        )
                    }
                } else {
                    val points = bars.mapIndexed { index, bar ->
                        Offset(
                            x = (index + 0.5f) * slot,
                            y = yOf(bar.amountMinor.toFloat()),
                        )
                    }
                    current?.let { index ->
                        drawLine(
                            color = colors.onSurfaceVariant,
                            start = Offset(points[index].x, plotTop),
                            end = Offset(points[index].x, size.height),
                            strokeWidth = gridWidth.toPx(),
                        )
                    }
                    // Why: the line draws in from the start side, its dots spilling past the chart's edges once whole.
                    val spill = selectedDotRadius.toPx()
                    clipRect(
                        left = -spill,
                        top = -spill,
                        right = -spill + (plotWidth + spill * 2) * growth.value,
                        bottom = size.height + spill,
                    ) {
                        drawLineSeries(points, area, primary)
                        bars.forEachIndexed { index, bar ->
                            val isSelected = index == current
                            if (bar.showsDot || isSelected) {
                                val outer = if (isSelected) selectedDotRadius else dotRadius
                                drawCircle(
                                    card,
                                    radius = outer.toPx(),
                                    center = points[index],
                                )
                                drawCircle(
                                    primary,
                                    radius = (outer - dotBorder).toPx(),
                                    center = points[index],
                                )
                            }
                        }
                    }
                }
                state.chartAverageMinor?.let { mean ->
                    val y = yOf(mean.toFloat())
                    drawLine(
                        color = average,
                        start = Offset(0f, y),
                        end = Offset(plotWidth, y),
                        strokeWidth = averageWidth.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    )
                }
            }
            selected?.let { index ->
                val bar = bars[index]
                StatsChartCalloutBubble(
                    name = nameOf(bar),
                    amount = money.toText(bar.amountMinor),
                    count = bar.expenseCount,
                    index = index,
                    barCount = bars.size,
                    anchorFraction = bar.amountMinor / top,
                    gutterWidth = gutterWidth,
                    plotTop = labelHalf,
                )
            }
        }
        StatsChartLabelsRow(
            bars = bars,
            boldsLast = !state.hasSparseLabels,
            labelOf = labelOf,
            modifier = Modifier.padding(end = gutterWidth),
        )
    }
}

// Why: the bar under a point of the plot; null while there are no bars or the plot has no width yet.
private fun findIndexAt(
    x: Float,
    plotWidth: Float,
    count: Int,
): Int? {
    if (count <= 0 || plotWidth <= 0f) return null
    return (x / (plotWidth / count)).toInt().coerceIn(0, count - 1)
}

// Why: a faint line at the base and a dashed one at each value, its short amount in the end gutter.
private fun DrawScope.drawGrid(
    ticks: List<Long>,
    texts: List<TextLayoutResult>,
    plotWidth: Float,
    yOf: (Long) -> Float,
    color: Color,
) {
    drawLine(color, Offset(0f, size.height), Offset(plotWidth, size.height), strokeWidth = gridWidth.toPx())
    val dashes = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx()))
    ticks.forEachIndexed { index, tick ->
        val y = yOf(tick)
        drawLine(
            color,
            Offset(0f, y),
            Offset(plotWidth, y),
            strokeWidth = gridWidth.toPx(),
            pathEffect = dashes,
        )
        val text = texts[index]
        drawText(
            textLayoutResult = text,
            topLeft = Offset(size.width - text.size.width, y - text.size.height / 2f),
        )
    }
}

private fun DrawScope.drawLineSeries(
    points: List<Offset>,
    area: Color,
    primary: Color,
) {
    val line = Path().apply {
        points.forEachIndexed { index, point ->
            if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
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

/*
 * Why: the picked bar's "28 Oct", "R$ 1,458" and "3 expenses", centered over it and kept inside the chart's width,
 * above its top; a tall one's callout rises over the grains above, as a tooltip does.
 */
@Composable
private fun StatsChartCalloutBubble(
    name: String,
    amount: String,
    count: Int,
    index: Int,
    barCount: Int,
    anchorFraction: Float,
    gutterWidth: Dp,
    plotTop: Dp,
) {
    val countText = if (count == 0) {
        stringResource(R.string.group_stats_callout_none)
    } else {
        pluralStringResource(R.plurals.group_stats_expenses, count, count)
    }
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            // Why: no Surface, which would take the pointer and make a hovering mouse leave the chart under it.
            val shape = RoundedCornerShape(10.dp)
            val textColor = MaterialTheme.colorScheme.inverseOnSurface
            Column(
                modifier = Modifier
                    .shadow(2.dp, shape)
                    .background(MaterialTheme.colorScheme.inverseSurface, shape)
                    .padding(
                        horizontal = 10.dp,
                        vertical = 6.dp,
                    ),
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                )
                Text(
                    amount,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
                Text(
                    countText,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                )
            }
        },
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
            ),
        )
        val plotWidth = constraints.maxWidth - gutterWidth.roundToPx()
        val slot = plotWidth.toFloat() / barCount
        val center = (index + 0.5f) * slot
        val x = (center - placeable.width / 2f).toInt().coerceIn(
            0,
            (constraints.maxWidth - placeable.width).coerceAtLeast(0),
        )
        val plotHeight = constraints.maxHeight - plotTop.roundToPx()
        val anchor = constraints.maxHeight - anchorFraction * plotHeight
        val y = (anchor - calloutGap.roundToPx() - placeable.height).toInt()
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(x, y, zIndex = 1f)
        }
    }
}

/*
 * Why: each label centered under its bar, so a few labels of many bars still line up with them. A label that would
 * touch the one after it is left out, from the end, so the last (the newest, in bold) always shows and no two ever
 * overlap, whatever the width and the language (beta feedback of 2026-11-14).
 */
@Composable
private fun StatsChartLabelsRow(
    bars: List<StatsBar>,
    boldsLast: Boolean,
    labelOf: (StatsBar) -> String,
    modifier: Modifier = Modifier,
) {
    val labeled = bars.withIndex().filter { it.value.showsLabel }
    val colors = MaterialTheme.colorScheme
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 20.dp),
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
        val placeables = measurables.map {
            it.measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                ),
            )
        }
        val slot = constraints.maxWidth.toFloat() / bars.size.coerceAtLeast(1)
        val xs = placeables.mapIndexed { position, placeable ->
            val center = (labeled[position].index + 0.5f) * slot
            (center - placeable.width / 2f).toInt().coerceIn(
                0,
                (constraints.maxWidth - placeable.width).coerceAtLeast(0),
            )
        }
        val gap = labelGap.roundToPx()
        var nextStart = Int.MAX_VALUE
        val shown = mutableListOf<Int>()
        for (position in placeables.indices.reversed()) {
            if (xs[position] + placeables[position].width + gap <= nextStart) {
                shown += position
                nextStart = xs[position]
            }
        }
        // Why: as tall as its tallest label, so a large font never clips them.
        val top = 2.dp.roundToPx()
        val height = (placeables.maxOfOrNull { it.height } ?: 0) + top
        layout(constraints.maxWidth, constraints.constrainHeight(height)) {
            shown.forEach { position -> placeables[position].place(xs[position], top) }
        }
    }
}
