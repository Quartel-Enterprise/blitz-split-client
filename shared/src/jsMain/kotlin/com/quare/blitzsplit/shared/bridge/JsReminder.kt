@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.groups.domain.model.ReminderCalendarEvent
import com.quare.blitzsplit.core.groups.domain.model.ReminderKind
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ReminderBanner
import kotlin.uuid.Uuid

// Why: the group's reminder as plain types: kind is "weekly" or "monthly", day the ISO weekday or the day of the month.
class JsReminder(
    val enabled: Boolean,
    val kind: String,
    val day: Int,
    // Why: minutes after midnight, read on the member's own clock.
    val minute: Int,
)

// Why: "Time to settle up" for a debtor, until dismissed.
class JsReminderBanner(
    val occurrenceAt: Double,
    val reminder: JsReminder,
)

// Why: the repeating event the page offers to the calendar, as an .ics the page words in the person's language.
class JsReminderCalendarEvent(
    val groupName: String,
    private val event: ReminderCalendarEvent,
) {
    fun toIcs(
        title: String,
        description: String,
    ): String = event.toIcs(
        title = title,
        description = description,
        uid = "${Uuid.random()}@blitzsplit.com",
    )
}

internal fun jsReminderOf(reminder: GroupReminder): JsReminder = JsReminder(
    enabled = reminder.isEnabled,
    kind = reminder.kind.code,
    day = reminder.day,
    minute = reminder.minute,
)

internal fun jsReminderBannerOf(banner: ReminderBanner): JsReminderBanner = JsReminderBanner(
    occurrenceAt = banner.occurrenceAt.toDouble(),
    reminder = jsReminderOf(banner.reminder),
)

// Why: an unknown kind from the page is ignored, as the other pickers do.
internal fun reminderKindOf(code: String): ReminderKind? = ReminderKind.fromCodeOrNull(code)
