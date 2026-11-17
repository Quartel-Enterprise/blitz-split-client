package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.GroupReminder

/*
 * Why: "Hora de acertar as contas" at the top of the group, only for someone who owes, from the moment the group's
 * reminder arrives or passes until they dismiss it or settle: it comes back with the next occurrence.
 */
data class ReminderBanner(
    // Why: the occurrence it is for (epoch millis), which "Dispensar" remembers.
    val occurrenceAt: Long,
    // Why: the schedule, for the banner's words ("Every Monday at 10:00", "Every 25th at 12:00").
    val reminder: GroupReminder,
)
