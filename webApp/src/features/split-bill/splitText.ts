import type { JsBillDefaultSplit, JsBillPerson, JsSplitEditor, JsSplitFact } from 'blitzsplit-shared'
import { t, type MessageKey } from '@/i18n'
import { formatNames, formatPercent } from '@/lib/money'

/** An amount in the bill's currency, from its minor unit. */
export type Money = (minor: number) => string

/** "Você" for this browser's person, their name for anyone else. */
export function personName(person: JsBillPerson): string {
  return person.isMe ? t('splitBill_you') : person.name
}

/** "Diego não participa", "Diego e Elisa não participam" or "Você não participa". */
function outOfSplit(people: JsBillPerson[]): string {
  if (people.length === 1) return people[0].isMe ? t('split_outMe') : t('split_outOne', { name: people[0].name })
  return t('split_outOther', { names: formatNames(people.map(personName)) })
}

/** "Elisa está sem valor", "Elisa e Diego estão sem valor" or "Você está sem valor". */
function withoutValue(people: JsBillPerson[]): string {
  if (people.length === 1) return people[0].isMe ? t('split_noValueMe') : t('split_noValueOne', { name: people[0].name })
  return t('split_noValueOther', { names: formatNames(people.map(personName)) })
}

function sentences(...parts: (string | null)[]): string {
  return parts
    .filter((part): part is string => !!part)
    .map((part) => (part.endsWith('.') ? part : `${part}.`))
    .join(' ')
}

/**
 * Where a fact shows: Dividir's status or the line under Rachar conta's "Divisão". Only "each" reads differently
 * between them in Portuguese: "R$ 62,40 para cada um" in the status, "R$ 62,40 cada" in the summary.
 */
export type FactPlace = 'status' | 'summary'

/** The words of one fact the shared code decided a split says about itself. */
export function splitFactText(fact: JsSplitFact, money: Money, place: FactPlace = 'status'): string {
  const amount = money(fact.amountMinor ?? 0)
  const total = money(fact.totalMinor ?? 0)
  const count = fact.count ?? 0
  const percent = formatPercent(fact.percent ?? 0)
  switch (fact.kind) {
    case 'each':
      return t(place === 'summary' ? 'splitBill_each' : 'split_equalEach', { amount })
    case 'nobody':
      return t('split_nobodyTitle')
    case 'nobody-checked':
      return t('split_nobodyChecked', { total })
    case 'nobody-has-parts':
      return t('split_nobodyHasParts', { total })
    case 'among':
      return t(count === 1 ? 'split_amongOne' : 'split_amongOther', { total, count })
    case 'out':
      return outOfSplit(fact.people)
    case 'amount-left':
      return t('split_amountLeft', { amount })
    case 'amount-over':
      return t('split_amountOver', { amount })
    case 'amount-adds-up':
      return t('split_addsUpAmount', { amount: total })
    case 'parts-match-total':
      return t('split_addsUpBody')
    case 'amount-so-far':
      return t('split_amountSoFar', { assigned: amount, total })
    case 'without-value':
      return withoutValue(fact.people)
    case 'percent-left':
      return t('split_percentLeft', { percent })
    case 'percent-over':
      return t('split_percentOver', { percent })
    case 'percent-adds-up':
      return t('split_addsUpPercent', { percent: formatPercent(100) })
    case 'parts':
      return t(count === 1 ? 'split_sharesOne' : 'split_sharesOther', { count, amount })
    case 'group-default':
      return t('split_groupDefault')
    case 'items':
      return t(count === 1 ? 'split_factItemsOne' : 'split_factItemsOther', { count })
    case 'loose-items':
      return t(count === 1 ? 'split_looseItemsOne' : 'split_looseItemsOther', { count })
    case 'person-parts':
      return fact.person?.isMe ? t('split_youHaveShares', { count }) : t('split_hasShares', { name: fact.person?.name ?? '', count })
    default:
      return ''
  }
}

/** Dividir's status: a title, the line under it, whether it is wrong, and Valores' progress (0 to 100). */
export interface SplitStatus {
  title: string
  body: string
  isError: boolean
  progress: number | null
}

/** Dividir's status, in the words of the facts the ViewModel picked for it. */
export function splitStatus(editor: JsSplitEditor, money: Money): SplitStatus {
  const progress =
    editor.mode === 'amount' ? (editor.totalMinor > 0 ? Math.min(100, Math.max(0, (editor.assignedMinor / editor.totalMinor) * 100)) : 0) : null
  return {
    title: splitFactText(editor.statusTitle, money),
    body: sentences(...editor.statusDetails.map((fact) => splitFactText(fact, money))),
    isError: !editor.addsUp,
    progress,
  }
}

/**
 * "R$ 62,40 cada · Diego não participa", the line under Rachar conta's "Divisão", "Padrão do grupo" first while the
 * bill keeps the group's default split.
 */
export function splitSummary(facts: JsSplitFact[], money: Money, exceptionName?: string): string {
  return facts
    .map((fact) => (fact.kind === 'group-default' && exceptionName ? t('splitBill_groupException', { name: exceptionName }) : splitFactText(fact, money, 'summary')))
    .join(' · ')
}

const defaultModes: Record<string, MessageKey> = {
  equal: 'defaultSplit_modeEqual',
  shares: 'defaultSplit_modeShares',
  percent: 'defaultSplit_modePercent',
  income: 'defaultSplit_modeIncome',
}

/** "Igual", "Partes", "Porcentagem" or "Pela renda": a DefaultSplitMode's code by name, "Igual" for one unknown. */
export function defaultSplitModeLabel(mode: string): string {
  return t(defaultModes[mode] ?? 'defaultSplit_modeEqual')
}

/** "Partes, 3 para 1", "Porcentagem, 75% e 25%", "Igual, sem Diego": the group's default split a bill opens with. */
export function defaultSplitText(split: JsBillDefaultSplit): string {
  const mode = defaultSplitModeLabel(split.mode)
  let detail: string | null
  if (split.mode === 'shares') {
    detail =
      split.values.length === 2
        ? t('split_defaultSharesTwo', { first: split.values[0], second: split.values[1] })
        : formatNames(split.values.map(String))
  } else if (split.mode === 'percent' || split.mode === 'income') {
    detail = formatNames(split.values.map(formatPercent))
  } else {
    detail = split.excluded.length > 0 ? t('split_defaultWithout', { names: formatNames(split.excluded.map(personName)) }) : null
  }
  return detail ? t('split_defaultDescription', { mode, detail }) : mode
}
