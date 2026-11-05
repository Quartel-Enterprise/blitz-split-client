package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.theme.pendingColor

// Why: the amber dot of what is saved here and not on the server yet, one place for every screen that shows the queue.
@Composable
fun PendingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    color: Color = pendingColor(),
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape),
    )
}
