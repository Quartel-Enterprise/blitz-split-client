package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.imageVector
import com.quare.blitzsplit.android.designsystem.theme.LocalIsDarkTheme
import com.quare.blitzsplit.android.designsystem.theme.toContainerColor
import com.quare.blitzsplit.android.designsystem.theme.toContentColor
import com.quare.blitzsplit.core.expenses.domain.model.Category

// Why: a category's icon on its main category's color, the same in a bill's row, the picker and the pie's legend.
@Composable
fun CategoryBadge(
    category: Category,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val isDark = LocalIsDarkTheme.current
    Box(
        modifier = modifier
            .size(size)
            .background(category.color.toContainerColor(isDark), RoundedCornerShape(size * CORNER_FRACTION)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = category.icon.imageVector,
            contentDescription = null,
            tint = category.color.toContentColor(isDark),
            modifier = Modifier.size(size * ICON_FRACTION),
        )
    }
}

private const val CORNER_FRACTION = 0.3f
private const val ICON_FRACTION = 0.55f
