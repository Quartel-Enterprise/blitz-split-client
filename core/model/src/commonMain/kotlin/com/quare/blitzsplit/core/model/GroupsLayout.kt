package com.quare.blitzsplit.core.model

// Why: the grid keeps its size while the list is picked, so going back to the grid finds it as it was.
data class GroupsLayout(
    val display: GroupsDisplay,
    val gridSize: GroupGridSize,
) {
    companion object {
        // Why: a fresh install, or codes from a newer version: the list, and the standard grid once picked.
        val initial: GroupsLayout = GroupsLayout(
            display = GroupsDisplay.LIST,
            gridSize = GroupGridSize.STANDARD,
        )
    }
}
