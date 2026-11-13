package com.quare.blitzsplit.android.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: the five avatar colors share the values of the category color of the same hue on every platform (see the
 * canvas rules), so a face and a slice of one hue never drift apart.
 */
fun AvatarColor.toContainerColor(isDark: Boolean): Color = categoryColor.toContainerColor(isDark)

fun AvatarColor.toContentColor(isDark: Boolean): Color = categoryColor.toContentColor(isDark)

// Why: a mark that stands for a person (a bar, a segment of a split) takes this tone, as a category's does.
fun AvatarColor.toDataColor(isDark: Boolean): Color = categoryColor.toDataColor(isDark)

// Why: a mark of something with no color of its own (a group's dot before its color is read) still reads at 3:1.
fun neutralDataColorOf(isDark: Boolean): Color = CategoryColor.GRAY.toDataColor(isDark)

private val AvatarColor.categoryColor: CategoryColor
    get() = when (this) {
        AvatarColor.BLUE -> CategoryColor.BLUE
        AvatarColor.PEACH -> CategoryColor.ORANGE
        AvatarColor.GREEN -> CategoryColor.GREEN
        AvatarColor.LILAC -> CategoryColor.PURPLE
        AvatarColor.SAND -> CategoryColor.YELLOW
    }
