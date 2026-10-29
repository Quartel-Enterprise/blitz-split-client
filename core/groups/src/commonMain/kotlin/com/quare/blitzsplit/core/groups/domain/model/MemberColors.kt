package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.model.AvatarColor

object MemberColors {
    // Why: someone who hasn't joined gets the color of the palette fewest members have, so faces differ.
    fun findLeastUsed(colors: List<AvatarColor>): AvatarColor {
        val counts = colors.groupingBy { it }.eachCount()
        return AvatarColor.entries.minBy { counts[it] ?: 0 }
    }
}
