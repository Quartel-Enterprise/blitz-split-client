package com.quare.blitzsplit.core.model

// Why: how the home draws its open groups, kept per device and never synced, like the theme.
enum class GroupsDisplay(
    val code: String,
) {
    LIST("list"),
    GRID("grid"),
    ;

    companion object {
        fun fromCodeOrNull(code: String): GroupsDisplay? = entries.firstOrNull { it.code == code }
    }
}
