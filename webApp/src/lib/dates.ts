import { t } from '@/i18n'
import { uiTag } from '@/lib/currency'

// Why: "today", "yesterday" or "other", as the presenters send a day.
export type RelativeDay = 'today' | 'yesterday' | 'other'

// Why: Profile's "Time format", as the presenter sends it: the browser's own clock for the language, or always 24 or 12 hours.
export type TimeFormat = 'system' | '24h' | '12h'

const timeFormats: Record<TimeFormat, Intl.DateTimeFormat> = {
  system: new Intl.DateTimeFormat(uiTag, { hour: 'numeric', minute: '2-digit' }),
  '24h': new Intl.DateTimeFormat(uiTag, { hour: 'numeric', minute: '2-digit', hourCycle: 'h23' }),
  '12h': new Intl.DateTimeFormat(uiTag, { hour: 'numeric', minute: '2-digit', hourCycle: 'h12' }),
}
let timeFormat = timeFormats.system
let currentTimeFormat: TimeFormat = 'system'

/** Set by the app's root, before the screens draw, so every time of day follows Profile's "Time format". */
export function setTimeFormat(code: string) {
  currentTimeFormat = code === '24h' || code === '12h' ? code : 'system'
  timeFormat = timeFormats[currentTimeFormat]
}

/** The time format every time of day follows now: the time inputs draw their own clock on it. */
export function getTimeFormat(): TimeFormat {
  return currentTimeFormat
}

/** The language's words for the morning and the afternoon on a 12-hour clock: "AM" and "PM", "午前" and "午後". */
export function uiDayPeriods(): [string, string] {
  const periodOf = (hour: number) =>
    timeFormats['12h'].formatToParts(new Date(2000, 0, 1, hour)).find((part) => part.type === 'dayPeriod')?.value
  return [periodOf(9) ?? 'AM', periodOf(21) ?? 'PM']
}

/** 14:30 today in `format`, Profile's example of each option ("14:30", "2:30 PM"). */
export function formatSampleTime(format: TimeFormat): string {
  const sample = new Date()
  sample.setHours(14, 30, 0, 0)
  return timeFormats[format].format(sample)
}
const dayMonthFormat = new Intl.DateTimeFormat(uiTag, { day: 'numeric', month: 'short' })
const dayMonthYearFormat = new Intl.DateTimeFormat(uiTag, { day: 'numeric', month: 'short', year: 'numeric' })
const weekdayFormat = new Intl.DateTimeFormat(uiTag, { weekday: 'short', day: 'numeric', month: 'short' })
const weekdayYearFormat = new Intl.DateTimeFormat(uiTag, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })

/** "21:30" in Portuguese, "9:30 PM" in English, or always 24 or 12 hours as Profile's "Time format" says. */
export function formatTime(millis: number): string {
  return timeFormat.format(millis)
}

// Why: a day of another year says which, so last October's bills don't read as this October's.
function weekdayDate(millis: number): string {
  const isThisYear = new Date(millis).getFullYear() === new Date().getFullYear()
  return capitalize((isThisYear ? weekdayFormat : weekdayYearFormat).format(millis))
}

/** "3 de out." or "Oct 3": a day of this year, short. */
export function formatShortDate(millis: number): string {
  const isThisYear = new Date(millis).getFullYear() === new Date().getFullYear()
  return (isThisYear ? dayMonthFormat : dayMonthYearFormat).format(millis)
}

/** A day's heading on the group's list: "Hoje", "Ontem" or "Sex., 24 de out.". */
export function formatDayLabel(dayStartMillis: number, relativeDay: string): string {
  if (relativeDay === 'today') return t('date_today')
  if (relativeDay === 'yesterday') return t('date_yesterday')
  return weekdayDate(dayStartMillis)
}

/** "Quando": "Hoje, 28 de out. · 21:30", "Ontem, 27 de out. · 21:30" or "Sex., 24 de out. · 21:30". */
export function formatWhen(millis: number, relativeDay: string): string {
  const day =
    relativeDay === 'today'
      ? t('date_todayOn', { date: dayMonthFormat.format(millis) })
      : relativeDay === 'yesterday'
        ? t('date_yesterdayOn', { date: dayMonthFormat.format(millis) })
        : weekdayDate(millis)
  return `${day} · ${formatTime(millis)}`
}

/** A bill's day and time on its own page: "Hoje, 14:30", "Ontem, 21:10" or "27 de out., 21:10". */
export function formatDayTime(millis: number, relativeDay: string): string {
  const time = formatTime(millis)
  if (relativeDay === 'today') return `${t('date_today')}, ${time}`
  if (relativeDay === 'yesterday') return `${t('date_yesterday')}, ${time}`
  const isThisYear = new Date(millis).getFullYear() === new Date().getFullYear()
  return `${(isThisYear ? dayMonthFormat : dayMonthYearFormat).format(millis)}, ${time}`
}

const longDayFormat = new Intl.DateTimeFormat(uiTag, { weekday: 'long', day: 'numeric', month: 'long' })
const longDayYearFormat = new Intl.DateTimeFormat(uiTag, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })

/** "Sexta-feira, 30 de outubro" or "Friday, October 30": a day written out, with the year when it isn't this one. */
export function formatLongDay(millis: number): string {
  const isThisYear = new Date(millis).getFullYear() === new Date().getFullYear()
  return capitalize((isThisYear ? longDayFormat : longDayYearFormat).format(millis))
}

/** Atividade's day headings: "Hoje", "Ontem" or "Sexta-feira, 30 de outubro". */
export function formatLongDayLabel(dayStartMillis: number, relativeDay: string): string {
  if (relativeDay === 'today') return t('date_today')
  if (relativeDay === 'yesterday') return t('date_yesterday')
  return formatLongDay(dayStartMillis)
}

/** Whether `millis` falls today, yesterday or on another day, in the browser's time zone. */
export function relativeDayOf(millis: number, nowMillis: number = Date.now()): RelativeDay {
  const day = new Date(millis)
  const today = new Date(nowMillis)
  if (day.toDateString() === today.toDateString()) return 'today'
  today.setDate(today.getDate() - 1)
  return day.toDateString() === today.toDateString() ? 'yesterday' : 'other'
}

const relativeTimeFormat = new Intl.RelativeTimeFormat(uiTag, { numeric: 'auto' })

/**
 * "há 2 minutos" / "2 minutes ago" from `millis` to `nowMillis`, or null under a minute, which each sentence says in
 * its own words ("agora há pouco"). Why null for the future too: the instant comes from another device's clock.
 */
export function formatTimeAgo(millis: number, nowMillis: number): string | null {
  const seconds = Math.floor((nowMillis - millis) / 1000)
  if (seconds < 60) return null
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return relativeTimeFormat.format(-minutes, 'minute')
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return relativeTimeFormat.format(-hours, 'hour')
  return relativeTimeFormat.format(-Math.floor(hours / 24), 'day')
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
