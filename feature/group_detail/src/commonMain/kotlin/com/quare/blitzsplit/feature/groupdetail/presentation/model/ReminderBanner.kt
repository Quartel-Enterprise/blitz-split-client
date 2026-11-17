package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.CurrencyBalances
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import kotlinx.datetime.TimeZone

/*
 * Why: "Hora de acertar as contas" at the top of the group, only for someone who owes, from the moment the group's
 * reminder arrives or passes until they dismiss it or settle: it comes back with the next occurrence.
 */
data class ReminderBanner(
    // Why: the occurrence it is for (epoch millis), which "Dispensar" remembers.
    val occurrenceAt: Long,
    // Why: the schedule, for the banner's words ("Every Monday at 10:00", "Every 25th at 12:00").
    val reminder: GroupReminder,
) {
    companion object {
        /*
         * Why: only someone who owes (in any currency of the group) sees it, once the schedule's latest occurrence on
         * their clock is after the one they dismissed; settling takes it away on its own, and a debt that comes later
         * brings back the occurrence that already passed, as a push of the day would have.
         */
        fun of(
            group: Group,
            byCurrency: List<CurrencyBalances>,
            meId: String?,
            nowMillis: Long,
            timeZone: TimeZone,
        ): ReminderBanner? {
            val occurrence = group.reminder.findLastOccurrenceAtOrBefore(nowMillis, timeZone) ?: return null
            val owes = meId != null && byCurrency.any { each -> each.balances.debts.any { it.fromMemberId == meId } }
            return ReminderBanner(occurrence, group.reminder).takeIf { owes && occurrence > group.reminderDismissedAt }
        }
    }
}
