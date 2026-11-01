import type { JsGroupStatsState, JsStatsBar, JsStatsPayer, JsStatsPeriodOption } from 'blitzsplit-shared'
import { t, type MessageKey } from '@/i18n'
import { uiTag } from '@/lib/currency'
import { formatShortDate } from '@/lib/dates'
import { nameOrFormer } from '@/features/balances/BalanceCard'
import { formatMoney } from '@/lib/money'
import { payerNamesText } from '@/lib/payers'

const dayMonth = new Intl.DateTimeFormat(uiTag, { day: 'numeric', month: 'short' })
const dayMonthYear = new Intl.DateTimeFormat(uiTag, { day: 'numeric', month: 'short', year: 'numeric' })
const monthYear = new Intl.DateTimeFormat(uiTag, { month: 'short', year: 'numeric' })
const longMonthYear = new Intl.DateTimeFormat(uiTag, { month: 'long', year: 'numeric' })
const longMonth = new Intl.DateTimeFormat(uiTag, { month: 'long' })
const shortMonth = new Intl.DateTimeFormat(uiTag, { month: 'short' })

const isThisYear = (millis: number) => new Date(millis).getFullYear() === new Date().getFullYear()

/** "24 – 29 de out.", "jan. – out. de 2026" or "setembro de 2026", as the shared code says the period reads. */
export function formatRange(style: string, startMillis: number, endMillis: number): string {
  if (style === 'month') return longMonthYear.format(startMillis)
  if (style === 'days') {
    const format = isThisYear(startMillis) && isThisYear(endMillis) ? dayMonth : dayMonthYear
    return startMillis === endMillis ? format.format(startMillis) : format.formatRange(startMillis, endMillis)
  }
  return monthYear.formatRange(startMillis, endMillis)
}

const periodLabels: Record<string, MessageKey> = {
  all: 'stats_periodAll',
  'this-year': 'stats_periodThisYear',
  'this-month': 'stats_periodThisMonth',
  'last-month': 'stats_periodLastMonth',
  custom: 'stats_periodCustom',
}

/** "Toda a viagem" in a trip, "Todo o período" in any other group, and the other periods by name. */
export function periodLabel(period: string, isTrip: boolean): string {
  if (period === 'all' && isTrip) return t('stats_periodAllTrip')
  return t(periodLabels[period] ?? 'stats_periodAll')
}

/** "nenhuma despesa", "1 despesa" or "7 despesas". */
export function expenseCountText(count: number): string {
  if (count === 0) return t('stats_expensesNone')
  return t(count === 1 ? 'stats_expensesOne' : 'stats_expensesOther', { count })
}

/** Under a period of the menu: "24 – 29 de out. · 7 despesas", or "De um dia até outro" before any dates. */
export function periodHint(option: JsStatsPeriodOption): string {
  if (option.startMillis == null || option.endMillis == null) return t('stats_periodCustomHint')
  return t('stats_periodHint', {
    range: formatRange(option.rangeStyle, option.startMillis, option.endMillis),
    count: expenseCountText(option.expenseCount),
  })
}

/** The label of a bar under the chart: the day ("hoje" for today), the week, the month or the year. */
export function barLabel(bar: JsStatsBar, grain: string, isSparse: boolean): string {
  if (grain === 'day') return bar.isToday ? t('stats_today') : String(new Date(bar.startMillis).getDate())
  if (grain === 'week') return isSparse ? shortMonth.format(bar.startMillis) : dayMonth.formatRange(bar.startMillis, bar.endMillis)
  if (grain === 'month') return shortMonth.format(bar.startMillis)
  return String(new Date(bar.startMillis).getFullYear())
}

/** What a bar names in a sentence: "28 de out.", "26 de out. – 1 de nov.", "julho" or "2025". */
function barName(bar: JsStatsBar, grain: string): string {
  if (grain === 'day') return formatShortDate(bar.startMillis)
  if (grain === 'week') return dayMonth.formatRange(bar.startMillis, bar.endMillis)
  if (grain === 'month') return longMonth.format(bar.startMillis)
  return String(new Date(bar.startMillis).getFullYear())
}

const peakLabels: Record<string, MessageKey> = { day: 'stats_peakDay', week: 'stats_peakWeek', month: 'stats_peakMonth', year: 'stats_peakYear' }

/** "Dia mais caro: 28 de out.,", before the amount. */
export function peakLabel(bar: JsStatsBar, grain: string): string {
  return t(peakLabels[grain] ?? 'stats_peakDay', { when: barName(bar, grain) })
}

const ariaLabels: Record<string, MessageKey> = { day: 'stats_ariaDay', week: 'stats_ariaWeek', month: 'stats_ariaMonth', year: 'stats_ariaYear' }

/** The chart's whole text for screen readers: "Gastos por dia: 24 de out. R$ 561,00; 25 de out. nada; …". */
export function chartAria(state: JsGroupStatsState): string {
  const values = state.bars
    .map((bar) => `${barName(bar, state.grain)} ${bar.amountMinor > 0 ? formatMoney(bar.amountMinor, state.currency, state.fractionDigits) : t('stats_ariaNothing')}`)
    .join('; ')
  return t(ariaLabels[state.grain] ?? 'stats_ariaDay', { values })
}

const grainNotes: Record<string, MessageKey> = {
  DAY_NEEDS_SHORT_PERIOD: 'stats_noteDay',
  WEEK_MONTH_AND_YEAR_NEED_LONGER_PERIOD: 'stats_noteWeekMonthYear',
  MONTH_AND_YEAR_NEED_LONGER_PERIOD: 'stats_noteMonthYear',
  WEEK_NEEDS_LONGER_PERIOD: 'stats_noteWeek',
  YEAR_NEEDS_LONGER_PERIOD: 'stats_noteYear',
  YEAR_SWITCHES_PERIOD: 'stats_noteYearSwitchesAll',
}

/** Why some grains are off: "Mês e Ano aparecem em períodos mais longos que um mês.", one sentence per reason. */
export function grainNoteText(state: JsGroupStatsState): string {
  return state.grainNotes
    .map((note) => (note === 'YEAR_SWITCHES_PERIOD' && state.isTrip ? t('stats_noteYearSwitchesTrip') : t(grainNotes[note] ?? 'stats_noteDay')))
    .join(' ')
}

/** "Nenhuma despesa em setembro" (or "em 2026"), "nesse período" for picked dates or the whole period. */
export function periodEmptyTitle(state: JsGroupStatsState): string {
  if (state.period === 'this-month' || state.period === 'last-month') return t('stats_periodEmptyTitle', { period: longMonth.format(state.periodStartMillis) })
  if (state.period === 'this-year') return t('stats_periodEmptyTitle', { period: String(new Date(state.periodStartMillis).getFullYear()) })
  return t('stats_periodEmptyTitleRange')
}

/** "28 de out., 15:00 · Bruno pagou", "… · Você pagou" or "… · Você e Ana pagaram", for the biggest bill. */
export function paidByText(when: string, payers: readonly JsStatsPayer[]): string {
  if (payers.length > 1) {
    return t('groupDetail_metaPaidBySeveral', { time: when, names: payerNamesText(payers.map((it) => (it.isMe ? t('splitBill_you') : nameOrFormer(it.name)))) })
  }
  const payer = payers[0]
  if (!payer || payer.isMe) return t('groupDetail_metaPaidByMe', { time: when })
  return t('groupDetail_metaPaidBy', { time: when, name: nameOrFormer(payer.name) })
}
