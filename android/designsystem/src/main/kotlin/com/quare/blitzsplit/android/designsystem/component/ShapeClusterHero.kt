package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

// Why: the small badge of expressive shapes that heads the welcome's pitch on a phone lying down.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShapeClusterHero(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(
                width = 150.dp,
                height = 56.dp,
            ).clearAndSetSemantics { },
    ) {
        Box(
            Modifier
                .offset(46.dp, 4.dp)
                .size(46.dp)
                .rotate(14f)
                .background(colors.errorContainer, MaterialShapes.Cookie4Sided.toShape()),
        )
        Box(
            Modifier
                .offset(84.dp, 4.dp)
                .size(48.dp)
                .background(colors.tertiaryContainer, MaterialShapes.Clover4Leaf.toShape()),
        )
        Box(
            Modifier
                .size(56.dp)
                .background(colors.primary, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = logoIcon(),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = colors.onPrimary,
            )
        }
    }
}

// Why: the logo cookie that heads the welcome on a phone upright and the tablet pitch panel.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LogoBadgeIcon(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(72.dp)
            .clearAndSetSemantics { }
            .background(colors.primary, MaterialShapes.Cookie9Sided.toShape()),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = logoIcon(),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = colors.onPrimary,
        )
    }
}

// Why: the shapes that bleed out of the tablet pitch panel's top right corner.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PanelCornerShapesImage(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(260.dp)
            .clearAndSetSemantics { },
    ) {
        Box(
            Modifier
                .offset(0.dp, 60.dp)
                .size(150.dp)
                .background(colors.primary.copy(alpha = 0.18f), MaterialShapes.Cookie9Sided.toShape()),
        )
        Box(
            Modifier
                .offset(110.dp, 20.dp)
                .size(120.dp)
                .rotate(14f)
                .background(colors.errorContainer, MaterialShapes.Cookie4Sided.toShape()),
        )
        Box(
            Modifier
                .offset(140.dp, 130.dp)
                .size(110.dp)
                .background(colors.tertiaryContainer, MaterialShapes.Clover4Leaf.toShape()),
        )
    }
}
