package com.quare.blitzsplit.feature.groups.presentation.mapper

import kotlinx.datetime.TimeZone

// Why: the device's clock and time zone, which the last activity is counted in.
internal class ActivityClock(
    val nowMillis: Long,
    val timeZone: TimeZone,
)
