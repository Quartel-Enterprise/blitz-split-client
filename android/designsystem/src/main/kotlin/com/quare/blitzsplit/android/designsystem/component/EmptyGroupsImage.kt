package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

// Why: the three overlapping shapes of the empty groups card, drawn at any width of the canvas.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmptyGroupsImage(
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(
                width = 160.dp * scale,
                height = 120.dp * scale,
            ).clearAndSetSemantics { },
    ) {
        Box(
            Modifier
                .offset(8.dp * scale, 18.dp * scale)
                .size(84.dp * scale)
                .background(colors.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
        )
        Box(
            Modifier
                .offset(62.dp * scale, 6.dp * scale)
                .size(72.dp * scale)
                .rotate(12f)
                .background(colors.errorContainer, MaterialShapes.Cookie4Sided.toShape()),
        )
        Box(
            Modifier
                .offset(88.dp * scale, 56.dp * scale)
                .size(60.dp * scale)
                .background(colors.tertiaryContainer, MaterialShapes.Circle.toShape()),
        )
    }
}
