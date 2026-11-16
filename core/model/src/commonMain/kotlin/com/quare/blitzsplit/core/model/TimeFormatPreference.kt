package com.quare.blitzsplit.core.model

/*
 * Why: Profile › Preferences › "Time format", how every time of day is drawn on this device: the device's own clock
 * (SYSTEM, the default), always 24 hours ("14:30") or always 12 hours with AM and PM ("2:30 PM"). Kept per device and
 * never synced, like the theme.
 */
enum class TimeFormatPreference(
    val code: String,
) {
    SYSTEM("system"),
    TWENTY_FOUR_HOUR("24h"),
    TWELVE_HOUR("12h"),
    ;

    companion object {
        fun fromCodeOrNull(code: String): TimeFormatPreference? = entries.firstOrNull { it.code == code }
    }
}
