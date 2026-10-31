package com.quare.blitzsplit.feature.groups.presentation.model

// Why: the latest of the group's own change, its bills and its payments.
data class LastActivity(
    // Why: the date the UI writes once it is older than four weeks, and for "todos acertaram em 3 out".
    val epochMillis: Long,
    val age: ActivityAge,
    // Why: the days for DAYS ("3 dias") and the weeks for WEEKS ("1 semana"); 0 otherwise.
    val count: Int,
)
