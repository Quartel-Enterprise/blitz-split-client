package com.quare.blitzsplit.android.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor

/*
 * Why: the twelve category colors, the same on every platform (see the canvas rules), each with the content color that
 * reads on it, in the same container/content pairing the five avatar colors use.
 */
fun CategoryColor.toContainerColor(isDark: Boolean): Color = if (isDark) darkContainer else lightContainer

fun CategoryColor.toContentColor(isDark: Boolean): Color = if (isDark) darkContent else lightContent

/*
 * Why: a mark that carries the category (a slice, a legend dot, a bar) takes this tone, not the container's: the
 * container is a background and reads at under 2:1 on a card, while this one reads at 4:1 light and 7:1 dark (WCAG's
 * 3:1 for a meaningful mark), and it never equals one of the theme's own container roles.
 */
fun CategoryColor.toDataColor(isDark: Boolean): Color = if (isDark) darkData else lightData

private val CategoryColor.lightContainer: Color
    get() = when (this) {
        CategoryColor.BLUE -> Color(0xFFD7E2FF)
        CategoryColor.ORANGE -> Color(0xFFFFDCC0)
        CategoryColor.GREEN -> Color(0xFFAAF5A4)
        CategoryColor.PURPLE -> Color(0xFFF0DBFF)
        CategoryColor.YELLOW -> Color(0xFFF6E468)
        CategoryColor.RED -> Color(0xFFFFDADA)
        CategoryColor.TEAL -> Color(0xFFA5F0E6)
        CategoryColor.PINK -> Color(0xFFFFD8EC)
        CategoryColor.INDIGO -> Color(0xFFE2DFFF)
        CategoryColor.LIME -> Color(0xFFDDF0A0)
        CategoryColor.BROWN -> Color(0xFFF1DFD1)
        CategoryColor.GRAY -> Color(0xFFE0E3E8)
    }

private val CategoryColor.lightContent: Color
    get() = when (this) {
        CategoryColor.BLUE -> Color(0xFF001B3F)
        CategoryColor.ORANGE -> Color(0xFF2D1600)
        CategoryColor.GREEN -> Color(0xFF002204)
        CategoryColor.PURPLE -> Color(0xFF2C0051)
        CategoryColor.YELLOW -> Color(0xFF201C00)
        CategoryColor.RED -> Color(0xFF40000B)
        CategoryColor.TEAL -> Color(0xFF00201C)
        CategoryColor.PINK -> Color(0xFF3E001D)
        CategoryColor.INDIGO -> Color(0xFF0E0664)
        CategoryColor.LIME -> Color(0xFF141E00)
        CategoryColor.BROWN -> Color(0xFF2A1709)
        CategoryColor.GRAY -> Color(0xFF191C20)
    }

private val CategoryColor.darkContainer: Color
    get() = when (this) {
        CategoryColor.BLUE -> Color(0xFF00458F)
        CategoryColor.ORANGE -> Color(0xFF6B3B00)
        CategoryColor.GREEN -> Color(0xFF055315)
        CategoryColor.PURPLE -> Color(0xFF5B3383)
        CategoryColor.YELLOW -> Color(0xFF504700)
        CategoryColor.RED -> Color(0xFF8A182B)
        CategoryColor.TEAL -> Color(0xFF00504A)
        CategoryColor.PINK -> Color(0xFF7B2951)
        CategoryColor.INDIGO -> Color(0xFF3D3C8F)
        CategoryColor.LIME -> Color(0xFF3B4D00)
        CategoryColor.BROWN -> Color(0xFF5B4130)
        CategoryColor.GRAY -> Color(0xFF44474C)
    }

private val CategoryColor.darkContent: Color
    get() = lightContainer

private val CategoryColor.lightData: Color
    get() = when (this) {
        CategoryColor.BLUE -> Color(0xFF2A75DC)
        CategoryColor.ORANGE -> Color(0xFFAE650C)
        CategoryColor.GREEN -> Color(0xFF418642)
        CategoryColor.PURPLE -> Color(0xFF8E65B8)
        CategoryColor.YELLOW -> Color(0xFF857800)
        CategoryColor.RED -> Color(0xFFCC4A57)
        CategoryColor.TEAL -> Color(0xFF00867C)
        CategoryColor.PINK -> Color(0xFFB55882)
        CategoryColor.INDIGO -> Color(0xFF6E6EC3)
        CategoryColor.LIME -> Color(0xFF678020)
        CategoryColor.BROWN -> Color(0xFF986E53)
        CategoryColor.GRAY -> Color(0xFF74777C)
    }

private val CategoryColor.darkData: Color
    get() = when (this) {
        CategoryColor.BLUE -> Color(0xFF94B9FF)
        CategoryColor.ORANGE -> Color(0xFFFDA54D)
        CategoryColor.GREEN -> Color(0xFF82CA7D)
        CategoryColor.PURPLE -> Color(0xFFD3A6FF)
        CategoryColor.YELLOW -> Color(0xFFCBBA42)
        CategoryColor.RED -> Color(0xFFFF9EA2)
        CategoryColor.TEAL -> Color(0xFF3ECDC0)
        CategoryColor.PINK -> Color(0xFFFF9AC4)
        CategoryColor.INDIGO -> Color(0xFFB2B1FF)
        CategoryColor.LIME -> Color(0xFFA8C35C)
        CategoryColor.BROWN -> Color(0xFFE0AF90)
        CategoryColor.GRAY -> Color(0xFFB7B9BE)
    }
