package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val outerRadius = 20.dp
private val innerRadius = 6.dp

// Why: a list reads as one card, its first and last rows rounder than the ones between.
fun groupedRowShapeOf(
    index: Int,
    count: Int,
): Shape = RoundedCornerShape(
    topStart = if (index == 0) outerRadius else innerRadius,
    topEnd = if (index == 0) outerRadius else innerRadius,
    bottomStart = if (index == count - 1) outerRadius else innerRadius,
    bottomEnd = if (index == count - 1) outerRadius else innerRadius,
)
