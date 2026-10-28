package com.quare.blitzsplit.core.model

// Why: kept per device and never synced, so each device keeps the theme picked on it.
enum class ThemePreference(
    val code: String,
) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun fromCodeOrNull(code: String): ThemePreference? = entries.firstOrNull { it.code == code }
    }
}
