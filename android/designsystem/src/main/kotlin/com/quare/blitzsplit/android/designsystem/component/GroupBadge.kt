package com.quare.blitzsplit.android.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.android.designsystem.theme.toContentColor
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupShape

/*
 * Why: a group's avatar, its initials ("VF") on one of the palette's colors in the group's shape, or its photo in
 * that shape. The initials stay under the photo, so they show while it loads or when it fails to.
 */
@Composable
fun GroupBadge(
    initials: String,
    color: AvatarColor,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: GroupShape = GroupShape.fallback,
    photoUrl: String? = null,
) {
    val isDark = LocalIsDarkTheme.current
    val outline = groupShapeOf(shape, size)
    Box(
        modifier = modifier
            .size(size)
            .background(color.toContainerColor(isDark), outline),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = color.toContentColor(isDark),
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.32f).sp,
        )
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().clip(outline),
            )
        }
    }
}

// Why: "Ícone do grupo"'s shapes as the boards draw them, unlike a person's circle but for "Círculo".
fun groupShapeOf(
    shape: GroupShape,
    size: Dp,
): Shape = when (shape) {
    GroupShape.COOKIE -> CookieShape
    GroupShape.PETAL -> PetalShape
    GroupShape.SQUARE -> groupShapeOf(size)
    GroupShape.CIRCLE -> CircleShape
}

/*
 * Why: the rounded square of a group made before shapes. The corners are 16 dp from the default size up and a third
 * of the side below it, so the small badges of the lists stay squares instead of turning into circles.
 */
fun groupShapeOf(size: Dp): Shape = RoundedCornerShape(min(16.dp, size / 3))

// Why: the boards' 50% 12px 50% 12px on their 48 px badge, a leaf with two round corners.
private val PetalShape = RoundedCornerShape(
    topStartPercent = 50,
    topEndPercent = 25,
    bottomEndPercent = 50,
    bottomStartPercent = 25,
)

// Why: the boards' "50% / 60% 60% 40% 40%", rounder on top than at the bottom, which RoundedCornerShape can't draw.
private object CookieShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val top = CornerRadius(size.width * 0.5f, size.height * 0.6f)
        val bottom = CornerRadius(size.width * 0.5f, size.height * 0.4f)
        return Outline.Rounded(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                topLeftCornerRadius = top,
                topRightCornerRadius = top,
                bottomRightCornerRadius = bottom,
                bottomLeftCornerRadius = bottom,
            ),
        )
    }
}

@get:StringRes
val GroupShape.labelRes: Int
    get() = when (this) {
        GroupShape.COOKIE -> R.string.designsystem_group_shape_cookie
        GroupShape.PETAL -> R.string.designsystem_group_shape_petal
        GroupShape.SQUARE -> R.string.designsystem_group_shape_square
        GroupShape.CIRCLE -> R.string.designsystem_group_shape_circle
    }
