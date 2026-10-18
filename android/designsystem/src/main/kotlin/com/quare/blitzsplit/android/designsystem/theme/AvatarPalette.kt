package com.quare.blitzsplit.android.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: the same five avatar colors on every platform (see the canvas rules), each with the
 * content color that reads on it.
 */
fun AvatarColor.toContainerColor(isDark: Boolean): Color = if (isDark) darkContainer else lightContainer

fun AvatarColor.toContentColor(isDark: Boolean): Color = if (isDark) darkContent else lightContent

private val AvatarColor.lightContainer: Color
    get() = when (this) {
        AvatarColor.BLUE -> Color(0xFFC9E6FF)
        AvatarColor.PEACH -> Color(0xFFFFDBCE)
        AvatarColor.GREEN -> Color(0xFFBDEFD3)
        AvatarColor.LILAC -> Color(0xFFE8DDFF)
        AvatarColor.SAND -> Color(0xFFFFE08B)
    }

private val AvatarColor.lightContent: Color
    get() = when (this) {
        AvatarColor.BLUE -> Color(0xFF001E2F)
        AvatarColor.PEACH -> Color(0xFF370E00)
        AvatarColor.GREEN -> Color(0xFF00210F)
        AvatarColor.LILAC -> Color(0xFF21005D)
        AvatarColor.SAND -> Color(0xFF241A00)
    }

private val AvatarColor.darkContainer: Color
    get() = when (this) {
        AvatarColor.BLUE -> Color(0xFF004B6F)
        AvatarColor.PEACH -> Color(0xFF71361C)
        AvatarColor.GREEN -> Color(0xFF00513A)
        AvatarColor.LILAC -> Color(0xFF4F378B)
        AvatarColor.SAND -> Color(0xFF574500)
    }

private val AvatarColor.darkContent: Color
    get() = when (this) {
        AvatarColor.BLUE -> Color(0xFFC9E6FF)
        AvatarColor.PEACH -> Color(0xFFFFDBCE)
        AvatarColor.GREEN -> Color(0xFFBDEFD3)
        AvatarColor.LILAC -> Color(0xFFE8DDFF)
        AvatarColor.SAND -> Color(0xFFFFE08B)
    }
