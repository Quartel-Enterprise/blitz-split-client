package com.quare.blitzsplit.feature.groups.presentation.model

/*
 * Why: how long ago a group last changed, counted in the device's days: "hoje", "ontem", "3 dias", "1 semana" (up to
 * four weeks), and the date ("3 out") after that. Each UI picks the words.
 */
enum class ActivityAge {
    TODAY,
    YESTERDAY,
    DAYS,
    WEEKS,
    DATE,
}
