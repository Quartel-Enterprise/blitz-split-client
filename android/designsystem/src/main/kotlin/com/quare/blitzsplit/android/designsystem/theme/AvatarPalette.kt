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
        AvatarColor.BLUE -> Color(0xFFD7E2FF)
        AvatarColor.PEACH -> Color(0xFFFFDCC0)
        AvatarColor.GREEN -> Color(0xFFAAF5A4)
        AvatarColor.LILAC -> Color(0xFFF0DBFF)
        AvatarColor.SAND -> Color(0xFFF6E468)
    }

private val AvatarColor.lightContent: Color
    get() = when (this) {
        AvatarColor.BLUE -> Color(0xFF001B3F)
        AvatarColor.PEACH -> Color(0xFF2D1600)
        AvatarColor.GREEN -> Color(0xFF002204)
        AvatarColor.LILAC -> Color(0xFF2C0051)
        AvatarColor.SAND -> Color(0xFF201C00)
    }

private val AvatarColor.darkContainer: Color
    get() = when (this) {
        AvatarColor.BLUE -> Color(0xFF00458F)
        AvatarColor.PEACH -> Color(0xFF6B3B00)
        AvatarColor.GREEN -> Color(0xFF055315)
        AvatarColor.LILAC -> Color(0xFF5B3383)
        AvatarColor.SAND -> Color(0xFF504700)
    }

private val AvatarColor.darkContent: Color
    get() = when (this) {
        AvatarColor.BLUE -> Color(0xFFD7E2FF)
        AvatarColor.PEACH -> Color(0xFFFFDCC0)
        AvatarColor.GREEN -> Color(0xFFAAF5A4)
        AvatarColor.LILAC -> Color(0xFFF0DBFF)
        AvatarColor.SAND -> Color(0xFFF6E468)
    }

private val AvatarColor.lightData: Color
    get() = when (this) {
        AvatarColor.BLUE -> Color(0xFF2A75DC)
        AvatarColor.PEACH -> Color(0xFFAE650C)
        AvatarColor.GREEN -> Color(0xFF418642)
        AvatarColor.LILAC -> Color(0xFF8E65B8)
        AvatarColor.SAND -> Color(0xFF857800)
    }

private val AvatarColor.darkData: Color
    get() = when (this) {
        AvatarColor.BLUE -> Color(0xFF94B9FF)
        AvatarColor.PEACH -> Color(0xFFFDA54D)
        AvatarColor.GREEN -> Color(0xFF82CA7D)
        AvatarColor.LILAC -> Color(0xFFD3A6FF)
        AvatarColor.SAND -> Color(0xFFCBBA42)
    }
