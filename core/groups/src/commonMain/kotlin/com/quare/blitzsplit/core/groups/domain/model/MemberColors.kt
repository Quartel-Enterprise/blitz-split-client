package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.model.AvatarColor

object MemberColors {
    // Why: someone who hasn't joined gets the color of the palette fewest members have, so faces differ.
    fun findLeastUsed(colors: List<AvatarColor>): AvatarColor {
        val counts = colors.groupingBy { it }.eachCount()
        return AvatarColor.entries.minBy { counts[it] ?: 0 }
    }

    /*
     * Why: someone added from Contacts keeps their own color, as the other groups show them, unless someone in the group
     * already has it; then the least used, as for a name.
     */
    fun findFor(
        preferred: AvatarColor,
        colors: List<AvatarColor>,
    ): AvatarColor = preferred.takeUnless { it in colors } ?: findLeastUsed(colors)
}
