package com.quare.blitzsplit.android.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.android.designsystem.theme.toContentColor
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupShape

/*
 * Why: a group's avatar, its initials ("VF") on one of the palette's colors in the group's rounded square, or its photo
 * in that square. Every group is that square, whatever shape an older build stored. The initials stay under the
 * photo, so they show while it loads or when it fails to.
 */
@Composable
fun GroupBadge(
    initials: String,
    color: AvatarColor,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    photoUrl: String? = null,
) {
    val isDark = LocalIsDarkTheme.current
    val outline = groupShapeOf(size)
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

/*
 * Why: the group photo viewer's square (16 dp corners on its 200 dp side) at every size, so a group reads as the same
 * square in a list row, the group's header and the photo opened big (beta feedback of 2026-11-12, Pierre's call).
 */
const val GROUP_CORNER_RATIO = 0.08f

// Why: a group's rounded square, its corners GROUP_CORNER_RATIO of its side.
fun groupShapeOf(size: Dp): Shape = RoundedCornerShape(size * GROUP_CORNER_RATIO)

// Why: Atividade still names a "Forma" an older Android build changed.
@get:StringRes
val GroupShape.labelRes: Int
    get() = when (this) {
        GroupShape.COOKIE -> R.string.designsystem_group_shape_cookie
        GroupShape.PETAL -> R.string.designsystem_group_shape_petal
        GroupShape.SQUARE -> R.string.designsystem_group_shape_square
        GroupShape.CIRCLE -> R.string.designsystem_group_shape_circle
    }
