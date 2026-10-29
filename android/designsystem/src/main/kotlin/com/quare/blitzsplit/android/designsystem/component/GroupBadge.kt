package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.android.designsystem.theme.toContentColor
import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: a group's avatar, its initials ("VF") on one of the palette's colors. One shape for every group for now,
 * unlike a person's cookie, so a group never reads as someone.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
            .background(color.toContainerColor(isDark), MaterialShapes.Cookie4Sided.toShape()),
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
