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
        CategoryColor.BLUE -> Color(0xFFC9E6FF)
        CategoryColor.ORANGE -> Color(0xFFFFDBCE)
        CategoryColor.GREEN -> Color(0xFFBDEFD3)
        CategoryColor.PURPLE -> Color(0xFFE8DDFF)
        CategoryColor.YELLOW -> Color(0xFFFFE08B)
        CategoryColor.RED -> Color(0xFFFFDAD6)
        CategoryColor.TEAL -> Color(0xFFA5F0E6)
        CategoryColor.PINK -> Color(0xFFFFD8EC)
        CategoryColor.INDIGO -> Color(0xFFDDE1FF)
        CategoryColor.LIME -> Color(0xFFDDF0A0)
        CategoryColor.BROWN -> Color(0xFFF1DFD1)
        CategoryColor.GRAY -> Color(0xFFE0E3E8)
    }

private val CategoryColor.lightContent: Color
    get() = when (this) {
        CategoryColor.BLUE -> Color(0xFF001E2F)
        CategoryColor.ORANGE -> Color(0xFF370E00)
        CategoryColor.GREEN -> Color(0xFF00210F)
        CategoryColor.PURPLE -> Color(0xFF21005D)
        CategoryColor.YELLOW -> Color(0xFF241A00)
        CategoryColor.RED -> Color(0xFF410002)
        CategoryColor.TEAL -> Color(0xFF00201C)
        CategoryColor.PINK -> Color(0xFF3E001D)
        CategoryColor.INDIGO -> Color(0xFF00164E)
        CategoryColor.LIME -> Color(0xFF141E00)
        CategoryColor.BROWN -> Color(0xFF2A1709)
        CategoryColor.GRAY -> Color(0xFF191C20)
    }

private val CategoryColor.darkContainer: Color
    get() = when (this) {
        CategoryColor.BLUE -> Color(0xFF004B6F)
        CategoryColor.ORANGE -> Color(0xFF71361C)
        CategoryColor.GREEN -> Color(0xFF00513A)
        CategoryColor.PURPLE -> Color(0xFF4F378B)
        CategoryColor.YELLOW -> Color(0xFF574500)
        CategoryColor.RED -> Color(0xFF93000A)
        CategoryColor.TEAL -> Color(0xFF00504A)
        CategoryColor.PINK -> Color(0xFF7B2951)
        CategoryColor.INDIGO -> Color(0xFF2F3A8F)
        CategoryColor.LIME -> Color(0xFF3B4D00)
        CategoryColor.BROWN -> Color(0xFF5B4130)
        CategoryColor.GRAY -> Color(0xFF44474C)
    }

private val CategoryColor.darkContent: Color
    get() = lightContainer

private val CategoryColor.lightData: Color
    get() = when (this) {
        CategoryColor.BLUE -> Color(0xFF017EB7)
        CategoryColor.ORANGE -> Color(0xFFB95D35)
        CategoryColor.GREEN -> Color(0xFF008863)
        CategoryColor.PURPLE -> Color(0xFF8069C0)
        CategoryColor.YELLOW -> Color(0xFF907400)
        CategoryColor.RED -> Color(0xFFD93D35)
        CategoryColor.TEAL -> Color(0xFF00867C)
        CategoryColor.PINK -> Color(0xFFB55882)
        CategoryColor.INDIGO -> Color(0xFF6570C7)
        CategoryColor.LIME -> Color(0xFF678020)
        CategoryColor.BROWN -> Color(0xFF986E53)
        CategoryColor.GRAY -> Color(0xFF74777C)
    }

private val CategoryColor.darkData: Color
    get() = when (this) {
        CategoryColor.BLUE -> Color(0xFF69C1FE)
        CategoryColor.ORANGE -> Color(0xFFFFA17B)
        CategoryColor.GREEN -> Color(0xFF5ACDA1)
        CategoryColor.PURPLE -> Color(0xFFC3ACFF)
        CategoryColor.YELLOW -> Color(0xFFD9B53C)
        CategoryColor.RED -> Color(0xFFFF9F94)
        CategoryColor.TEAL -> Color(0xFF3ECDC0)
        CategoryColor.PINK -> Color(0xFFFF9AC4)
        CategoryColor.INDIGO -> Color(0xFFABB3FF)
        CategoryColor.LIME -> Color(0xFFA8C35C)
        CategoryColor.BROWN -> Color(0xFFE0AF90)
        CategoryColor.GRAY -> Color(0xFFB7B9BE)
    }
