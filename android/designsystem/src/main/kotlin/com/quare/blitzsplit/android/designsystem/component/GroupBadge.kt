package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.android.designsystem.theme.toContentColor
import com.quare.blitzsplit.core.model.AvatarColor

// Why: a group's avatar, its initials ("VF") on one of the palette's colors, in the group's rounded square.
@Composable
fun GroupBadge(
    initials: String,
    color: AvatarColor,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = modifier
            .size(size)
            .background(color.toContainerColor(isDark), groupShapeOf(size)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = color.toContentColor(isDark),
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.32f).sp,
        )
    }
}

/*
 * Why: unlike a person's circle, so a group never reads as someone. The corners are 16 dp from the default size up
 * and a third of the side below it, so the small badges of the lists stay squares instead of turning into circles.
 */
fun groupShapeOf(size: Dp): Shape = RoundedCornerShape(min(16.dp, size / 3))
