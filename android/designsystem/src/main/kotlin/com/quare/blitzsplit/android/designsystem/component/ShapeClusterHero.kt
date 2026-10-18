package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Why: the decorative cluster of expressive shapes at the top of the welcome screen. Compact, it is
 * the small badge that heads the pitch lying down and the tablet panel.
 */
@Composable
fun ShapeClusterHero(
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    if (isCompact) CompactShapeClusterHero(modifier) else FullShapeClusterHero(modifier)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FullShapeClusterHero(modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .clearAndSetSemantics { },
    ) {
        Box(
            Modifier
                .offset(20.dp, 26.dp)
                .size(110.dp)
                .background(colors.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
        )
        Box(
            Modifier
                .offset(122.dp, 6.dp)
                .size(88.dp)
                .rotate(14f)
                .background(colors.errorContainer, MaterialShapes.Cookie4Sided.toShape()),
        )
        Box(
            Modifier
                .offset(172.dp, 78.dp)
                .size(96.dp)
                .background(colors.tertiaryContainer, MaterialShapes.Clover4Leaf.toShape()),
        )
        Box(
            Modifier
                .offset(82.dp, 108.dp)
                .size(64.dp)
                .background(colors.primary, MaterialShapes.Circle.toShape()),
        )
        Text(
            text = "R$",
            modifier = Modifier.offset(46.dp, 60.dp),
            color = colors.onPrimaryContainer,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CompactShapeClusterHero(modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(width = 150.dp, height = 56.dp)
            .clearAndSetSemantics { },
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
            Text(text = "R$", color = colors.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// Why: the "R$" cookie that heads the tablet pitch panel, whose own color would hide the cluster.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CurrencyBadgeIcon(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(72.dp)
            .clearAndSetSemantics { }
            .background(colors.primary, MaterialShapes.Cookie9Sided.toShape()),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "R$", color = colors.onPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
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
