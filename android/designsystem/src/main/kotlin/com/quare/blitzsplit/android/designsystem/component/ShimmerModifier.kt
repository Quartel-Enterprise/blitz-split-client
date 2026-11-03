package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.dp
import kotlin.math.max

private val defaultShimmerShape = RoundedCornerShape(8.dp)

/*
 * Why: the placeholder of something still loading, in the shape it will take. It draws the sweep of the ShimmerBox
 * around it, placed by the block's position in the window, so the highlight crosses every block of the screen as one
 * band from left to right.
 */
fun Modifier.shimmer(shape: Shape = defaultShimmerShape): Modifier = this then ShimmerElement(shape)

private class ShimmerElement(
    private val shape: Shape,
) : ModifierNodeElement<ShimmerNode>() {
    override fun create(): ShimmerNode = ShimmerNode(shape)

    override fun update(node: ShimmerNode) {
        node.updateShape(shape)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "shimmer"
        properties["shape"] = shape
    }

    override fun equals(other: Any?): Boolean = other is ShimmerElement && other.shape == shape

    override fun hashCode(): Int = shape.hashCode()
}

private class ShimmerNode(
    private var shape: Shape,
) : Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    private var xInRoot = 0f

    private var rootWidth = 0f

    fun updateShape(shape: Shape) {
        this.shape = shape
        invalidateDraw()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        xInRoot = coordinates.positionInRoot().x
        rootWidth = coordinates
            .findRootCoordinates()
            .size.width
            .toFloat()
    }

    override fun ContentDrawScope.draw() {
        val sweep = currentValueOf(LocalShimmerSweep)
        val outline = shape.createOutline(size, layoutDirection, this)
        val progress = sweep.progress?.value
        if (progress == null) {
            drawOutline(outline, sweep.baseColor)
        } else {
            val span = max(rootWidth, size.width)
            val band = span * BAND_FRACTION
            val center = -band + progress * (span + 2 * band) - xInRoot
            val brush = Brush.linearGradient(
                colors = listOf(sweep.baseColor, sweep.highlightColor, sweep.baseColor),
                start = Offset(center - band, 0f),
                end = Offset(center + band, 0f),
            )
            drawOutline(outline, brush)
        }
        drawContent()
    }

    private companion object {
        const val BAND_FRACTION = 0.35f
    }
}
