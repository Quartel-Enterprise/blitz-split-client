import type { JsBillHistoryChange, JsBillHistoryEntry, JsBillHistoryPart } from 'blitzsplit-shared'
import { t, verbForYou, type MessageKey } from '@/i18n'
import { categoryName } from '@/lib/categories'
import { formatDayTime } from '@/lib/dates'
import { payerNamesText } from '@/lib/payers'

type Money = (minor: number) => string

/** One "antes → depois" under a line of Histórico, with whose part or which field it is when the verb doesn't say. */
export interface HistoryDiff {
  label: string | null
  from: string
  to: string
}

// Why: the split's header names its mode with how many are in it, the same as Conta's "Igual entre 5".
export const splitTitles: Record<string, MessageKey> = {
  equal: 'billDetail_splitEqual',
  amount: 'billDetail_splitAmount',
  percent: 'billDetail_splitPercent',
  shares: 'billDetail_splitShares',
  items: 'billDetail_splitItems',
}

// Why: a SplitMode by its code, as Rachar conta names it ("Igual → Partes").
const splitModes: Record<string, MessageKey> = {
  equal: 'split_modeEqual',
  amount: 'split_modeAmount',
  percent: 'defaultSplit_modePercent',
  shares: 'split_modeShares',
  items: 'split_modeItems',
}

// Why: an edit says what it did by its first change.
const changeVerbs: Record<string, MessageKey> = {
  amount: 'billDetail_historyAmount',
  description: 'billDetail_historyDescription',
  'spent-at': 'billDetail_historySpentAt',
  category: 'billDetail_historyCategory',
  currency: 'billDetail_historyCurrency',
  payers: 'billDetail_historyPayers',
  split: 'billDetail_historySplit',
}

// Why: the changes after the first one name their field, as Atividade does.
const fieldLabels: Record<string, MessageKey> = {
  amount: 'activity_fieldAmount',
  description: 'activity_fieldDescription',
  'spent-at': 'activity_fieldSpentAt',
  category: 'activity_fieldCategory',
  currency: 'activity_fieldCurrency',
  payers: 'activity_fieldPayers',
  split: 'activity_fieldSplit',
}

/** "Você" for this browser's person, "Alguém" when the server kept no name. The subject of the verb: French's "Tu". */
export function historyActor(entry: JsBillHistoryEntry): string {
  if (entry.actorIsMe) return t('activity_you')
  return entry.actorName ?? t('billDetail_commentSomeone')
}

/** "criou a conta", "trocou a categoria", "anexou 2 fotos", "desativou o link"… */
export function historyVerb(entry: JsBillHistoryEntry): string {
  // Why: Spanish says "Ana creó" but "Tú creaste".
  return entry.actorIsMe ? verbForYou(verbOf(entry)) : verbOf(entry)
}

function verbOf(entry: JsBillHistoryEntry): string {
  switch (entry.kind) {
    case 'created':
      return t(entry.isPending ? 'billDetail_historyCreatedOffline' : 'billDetail_historyCreated')
    case 'attachments-added':
      if (entry.isReceipt) return t('billDetail_historyReceiptAdded')
      return entry.attachmentCount > 1 ? t('billDetail_historyPhotoOther', { count: entry.attachmentCount }) : t('billDetail_historyPhotoOne')
    case 'attachment-removed':
      return t(entry.isReceipt ? 'billDetail_historyReceiptRemoved' : 'billDetail_historyPhotoRemoved')
    case 'link-changed':
      if (entry.linkVisibility === 'anyone') return t('billDetail_historyLinkAnyone')
      return t(entry.linkVisibility === 'members' ? 'billDetail_historyLinkMembers' : 'billDetail_historyLinkOff')
    default: {
      const first = entry.changes[0]
      if (first) return t(changeVerbs[first.field] ?? 'billDetail_historyEdited')
      // Why: an edit made offline has no changes to tell until the server compares it.
      return t(entry.isPending ? 'billDetail_historyEditedOffline' : 'billDetail_historyEdited')
    }
  }
}

/** CREATED's note, "Igual entre 5 · R$ 312,00", without the parts the line doesn't know. */
export function historyNote(entry: JsBillHistoryEntry, money: Money): string | null {
  if (entry.kind !== 'created') return null
  const mode = entry.splitMode
  const split = mode ? (entry.sharesCount != null ? t(splitTitles[mode] ?? 'billDetail_splitEqual', { count: entry.sharesCount }) : modeLabel(mode)) : null
  const amount = entry.amountMinor != null ? money(entry.amountMinor) : null
  const note = [split, amount].filter(Boolean).join(' · ')
  return note || null
}

/** Every change of an edit as before → after, the first one under its verb, the others with their field. */
export function historyDiffs(entry: JsBillHistoryEntry, money: Money): HistoryDiff[] {
  if (entry.kind !== 'edited') return []
  return entry.changes.flatMap((change, index) => changeDiffs(change, index === 0, money))
}

function changeDiffs(change: JsBillHistoryChange, isFirst: boolean, money: Money): HistoryDiff[] {
  const label = isFirst ? null : t(fieldLabels[change.field] ?? 'activity_fieldName')
  switch (change.field) {
    case 'amount':
      return [{ label, from: moneyOrDash(change.beforeMinor, money), to: moneyOrDash(change.afterMinor, money) }]
    case 'description':
      return [{ label, from: quoted(change.beforeText), to: quoted(change.afterText) }]
    case 'spent-at':
      return [{ label, from: dateOrDash(change.beforeMillis), to: dateOrDash(change.afterMillis) }]
    case 'currency':
      return [{ label, from: change.beforeText ?? '—', to: change.afterText ?? '—' }]
    case 'category':
      return [{ label, from: change.beforeCategory ? categoryName(change.beforeCategory) : '—', to: change.afterCategory ? categoryName(change.afterCategory) : '—' }]
    case 'payers':
      // Why: the same payers paying other amounts say each amount that moved, as the split's parts do.
      if (change.parts.length > 0) return change.parts.map((part) => partDiff(part, money))
      return [{ label, from: payersText(change.beforeNames), to: payersText(change.afterNames) }]
    case 'split': {
      // Why: "Igual → Partes" only when the mode changed, then one line per part that moved.
      const mode = change.beforeMode !== change.afterMode ? [{ label, from: modeLabel(change.beforeMode), to: modeLabel(change.afterMode) }] : []
      return [...mode, ...change.parts.map((part) => partDiff(part, money))]
    }
    default:
      return []
  }
}

function partDiff(part: JsBillHistoryPart, money: Money): HistoryDiff {
  const name = part.isMe ? t('splitBill_you') : (part.name ?? t('billDetail_formerMember'))
  const side = (minor: number | null | undefined) => (minor != null ? money(minor) : t('billDetail_historyOut'))
  return { label: name, from: side(part.beforeMinor), to: side(part.afterMinor) }
}

function modeLabel(code: string | null | undefined): string {
  return code ? t(splitModes[code] ?? 'split_modeEqual') : '—'
}

function moneyOrDash(minor: number | null | undefined, money: Money): string {
  return minor != null ? money(minor) : '—'
}

function quoted(text: string | null | undefined): string {
  return text ? `“${text}”` : '—'
}

function dateOrDash(millis: number | null | undefined): string {
  return millis != null ? formatDayTime(millis, 'other') : '—'
}

// Why: "Bruno → Ana e Bruno"; someone no longer in the group is "Ex-membro".
function payersText(names: (string | null | undefined)[]): string {
  if (names.length === 0) return '—'
  return payerNamesText(names.map((name) => name ?? t('billDetail_formerMember')))
}
