package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.android.designsystem.theme.toContentColor
import com.quare.blitzsplit.core.model.AvatarColor

private const val VIEW_WIDTH = 164f
private const val VIEW_HEIGHT = 132f
private const val FADED_ALPHA = 0.4f

/*
 * Why: the boards' before and after of "Um exemplo" (13.2), in the same 164×132 frame: Carla, Elisa and this device's
 * user, the arrows of who pays whom with the amount, and Elisa faded once she pays nothing.
 */
@Composable
internal fun SimplifyExampleImage(
    isSimplified: Boolean,
    amount: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val isDark = LocalIsDarkTheme.current
    val arrowColor = MaterialTheme.colorScheme.primary
    val labelStyle = TextStyle(
        color = arrowColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Canvas(
        modifier = modifier
            .size(
                width = VIEW_WIDTH.dp,
                height = VIEW_HEIGHT.dp,
            ).semantics { contentDescription = description },
    ) {
        val scale = size.width / VIEW_WIDTH
        val draw = ExampleDrawing(
            scope = this,
            scale = scale,
            measurer = measurer,
        )
        if (isSimplified) {
            draw.arrow(
                color = arrowColor,
                line = listOf(Offset(40f, 46f), Offset(66f, 90f)),
                head = listOf(Offset(57f, 87f), Offset(66f, 91f), Offset(67f, 81f)),
            )
            draw.label(amount, Offset(32f, 72f), labelStyle)
        } else {
            draw.arrow(
                color = arrowColor,
                line = listOf(Offset(52f, 28f), Offset(110f, 28f)),
                head = listOf(Offset(104f, 22f), Offset(111f, 28f), Offset(104f, 34f)),
            )
            draw.label(amount, Offset(82f, 14f), labelStyle)
            draw.arrow(
                color = arrowColor,
                line = listOf(Offset(124f, 46f), Offset(98f, 90f)),
                head = listOf(Offset(97f, 81f), Offset(98f, 91f), Offset(107f, 87f)),
            )
            draw.label(amount, Offset(132f, 72f), labelStyle)
        }
        draw.face("CA", AvatarColor.LILAC, Offset(30f, 28f), isDark, alpha = 1f)
        draw.face("EL", AvatarColor.SAND, Offset(134f, 28f), isDark, alpha = if (isSimplified) FADED_ALPHA else 1f)
        draw.face("PV", AvatarColor.BLUE, Offset(82f, 108f), isDark, alpha = 1f)
    }
}

// Why: the boards' coordinates, in the 164×132 frame, scaled to the canvas.
private class ExampleDrawing(
    private val scope: DrawScope,
    private val scale: Float,
    private val measurer: TextMeasurer,
) {
    fun arrow(
        color: Color,
        line: List<Offset>,
        head: List<Offset>,
    ) {
        val stroke = Stroke(
            width = 2f * scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        scope.drawPath(pathOf(line), color, style = stroke)
        scope.drawPath(pathOf(head), color, style = stroke)
    }

    fun label(
        text: String,
        center: Offset,
        style: TextStyle,
    ) {
        val layout = measurer.measure(text, style)
        scope.drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                x = center.x * scale - layout.size.width / 2f,
                y = center.y * scale - layout.size.height / 2f,
            ),
        )
    }

    fun face(
        initials: String,
        color: AvatarColor,
        center: Offset,
        isDark: Boolean,
        alpha: Float,
    ) {
        scope.drawCircle(
            color = color.toContainerColor(isDark),
            radius = FACE_RADIUS * scale,
            center = center * scale,
            alpha = alpha,
        )
        val layout = measurer.measure(
            text = initials,
            style = TextStyle(
                color = color.toContentColor(isDark).copy(alpha = alpha),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        scope.drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                x = center.x * scale - layout.size.width / 2f,
                y = center.y * scale - layout.size.height / 2f,
            ),
        )
    }

    private fun pathOf(points: List<Offset>): Path = Path().apply {
        points.forEachIndexed { index, point ->
            if (index == 0) moveTo(point.x * scale, point.y * scale) else lineTo(point.x * scale, point.y * scale)
        }
    }

    private companion object {
        const val FACE_RADIUS = 18f
    }
}
