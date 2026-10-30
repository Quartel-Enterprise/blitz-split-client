import { t } from '@/i18n'
import { uiTag } from '@/lib/currency'

// Why: "today", "yesterday" or "other", as the presenters send a day.
export type RelativeDay = 'today' | 'yesterday' | 'other'

const timeFormat = new Intl.DateTimeFormat(uiTag, { hour: 'numeric', minute: '2-digit' })
const dayMonthFormat = new Intl.DateTimeFormat(uiTag, { day: 'numeric', month: 'short' })
const weekdayFormat = new Intl.DateTimeFormat(uiTag, { weekday: 'short', day: 'numeric', month: 'short' })
const weekdayYearFormat = new Intl.DateTimeFormat(uiTag, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })

/** "21:30" in Portuguese, "9:30 PM" in English. */
export function formatTime(millis: number): string {
  return timeFormat.format(millis)
}

// Why: a day of another year says which, so last October's bills don't read as this October's.
function weekdayDate(millis: number): string {
  const isThisYear = new Date(millis).getFullYear() === new Date().getFullYear()
  return capitalize((isThisYear ? weekdayFormat : weekdayYearFormat).format(millis))
}

/** A day's heading on the group's list: "Hoje", "Ontem" or "Sex., 24 de out.". */
export function formatDayLabel(dayStartMillis: number, relativeDay: string): string {
  if (relativeDay === 'today') return t('date_today')
  if (relativeDay === 'yesterday') return t('date_yesterday')
  return weekdayDate(dayStartMillis)
}

/** "Quando": "Hoje, 28 de out. · 21:30", "Ontem, 27 de out. · 21:30" or "Sex., 24 de out. · 21:30". */
export function formatWhen(millis: number, relativeDay: string): string {
  const time = formatTime(millis)
  if (relativeDay === 'today') return `${t('date_todayOn', { date: dayMonthFormat.format(millis) })} · ${time}`
  if (relativeDay === 'yesterday') return `${t('date_yesterdayOn', { date: dayMonthFormat.format(millis) })} · ${time}`
  return `${weekdayDate(millis)} · ${time}`
}

function capitalize(text: string): string {
  return text.charAt(0).toLocaleUpperCase(uiTag) + text.slice(1)
}

const pad = (value: number) => String(value).padStart(2, '0')

/** "2026-10-28", a date input's value for an instant, in the browser's time zone. */
export function toDateInputValue(millis: number): string {
  const date = new Date(millis)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** "21:30", a time input's value for an instant, in the browser's time zone. */
export function toTimeInputValue(millis: number): string {
  const date = new Date(millis)
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** The instant a date input and a time input name together, null while either is incomplete. */
export function fromDateTimeInputs(date: string, time: string): number | null {
  const day = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date)
  const clock = /^(\d{2}):(\d{2})/.exec(time)
  if (!day || !clock) return null
  const millis = new Date(Number(day[1]), Number(day[2]) - 1, Number(day[3]), Number(clock[1]), Number(clock[2])).getTime()
  return Number.isNaN(millis) ? null : millis
}
