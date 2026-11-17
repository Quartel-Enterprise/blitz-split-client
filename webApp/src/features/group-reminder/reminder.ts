import type { JsReminder, JsReminderCalendarEvent } from 'blitzsplit-shared'
import { t } from '@/i18n'
import { uiTag } from '@/lib/currency'
import { formatTime } from '@/lib/dates'
import { downloadFile } from '@/lib/download'

/** The weekday's name for an ISO weekday (1 Monday to 7 Sunday), in the UI's language. */
export function weekdayName(isoDay: number, style: 'long' | 'short' = 'long'): string {
  // Why: 2024-01-01 was a Monday, so day 1 is the 1st and day 7 the 7th.
  return new Intl.DateTimeFormat(uiTag, { weekday: style }).format(new Date(2024, 0, isoDay))
}

/** A time of day from minutes after midnight, on Profile's time format. */
export function reminderTimeText(minute: number): string {
  return formatTime(new Date(2000, 0, 1, Math.floor(minute / 60), minute % 60).getTime())
}

/** "Every Monday at 10:00" or "Every month on day 25 at 12:00". */
export function reminderSummary(reminder: JsReminder): string {
  const time = reminderTimeText(reminder.minute)
  return reminder.kind === 'monthly'
    ? t('reminder_summaryMonthly', { day: reminder.day, time })
    : t('reminder_summaryWeekly', { day: weekdayName(reminder.day), time })
}

/** "HH:mm" for a time input. */
export function toTimeInputValue(minute: number): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${pad(Math.floor(minute / 60))}:${pad(minute % 60)}`
}

/** Minutes after midnight from a time input's "HH:mm"; null while it is incomplete. */
export function fromTimeInputValue(value: string): number | null {
  const match = /^(\d{2}):(\d{2})/.exec(value)
  return match ? Number(match[1]) * 60 + Number(match[2]) : null
}

/** Saves the group's reminder as a repeating .ics event, which the person's calendar opens. */
export function downloadReminderCalendarEvent(event: JsReminderCalendarEvent) {
  const ics = event.toIcs(t('reminder_eventTitle', { group: event.groupName }), t('reminder_eventBody', { group: event.groupName }))
  downloadFile(ics, t('reminder_eventFileName'), 'text/calendar;charset=utf-8')
}
