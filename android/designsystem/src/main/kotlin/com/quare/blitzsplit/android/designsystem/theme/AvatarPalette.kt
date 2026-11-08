package com.quare.blitzsplit.android.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: the same five avatar colors on every platform (see the canvas rules), each with the
 * content color that reads on it.
 */
fun AvatarColor.toContainerColor(isDark: Boolean): Color = if (isDark) darkContainer else lightContainer

fun AvatarColor.toContentColor(isDark: Boolean): Color = if (isDark) darkContent else lightContent

// Why: a mark that stands for a person (a bar, a segment of a split) takes this tone, as a category's does.
fun AvatarColor.toDataColor(isDark: Boolean): Color = if (isDark) darkData else lightData

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

private val AvatarColor.lightData: Color
    get() = when (this) {
        AvatarColor.BLUE -> Color(0xFF017EB7)
        AvatarColor.PEACH -> Color(0xFFB95D35)
        AvatarColor.GREEN -> Color(0xFF008863)
        AvatarColor.LILAC -> Color(0xFF8069C0)
        AvatarColor.SAND -> Color(0xFF907400)
    }

private val AvatarColor.darkData: Color
    get() = when (this) {
        AvatarColor.BLUE -> Color(0xFF69C1FE)
        AvatarColor.PEACH -> Color(0xFFFFA17B)
        AvatarColor.GREEN -> Color(0xFF5ACDA1)
        AvatarColor.LILAC -> Color(0xFFC3ACFF)
        AvatarColor.SAND -> Color(0xFFD9B53C)
    }
