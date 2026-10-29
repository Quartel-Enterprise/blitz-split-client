package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.android.designsystem.theme.toContentColor
import com.quare.blitzsplit.core.model.AvatarColor

// Why: the initial stays under the photo, so it shows while the photo loads or when it fails to.
@Composable
fun AvatarBadge(
    initial: String,
    color: AvatarColor,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    photoUrl: String? = null,
) {
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = modifier
            .size(size)
            .background(color.toContainerColor(isDark), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = color.toContentColor(isDark),
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.42f).sp,
        )
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().clip(CircleShape),
            )
        }
    }
}
