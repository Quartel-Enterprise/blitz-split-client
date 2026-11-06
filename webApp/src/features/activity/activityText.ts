import type { JsActivityChange, JsActivityDebtChange, JsActivityDeletion, JsActivityItem, JsActivitySimplification, JsActivityValue } from 'blitzsplit-shared'
import { nameOrFormer, paymentMethodLabel } from '@/features/balances/BalanceCard'
import { groupTypeLabel } from '@/features/create-group/GroupTypePicker'
import { locale, t, verbForYou, type MessageKey } from '@/i18n'
import { avatarColorLabel } from '@/lib/avatar'
import { categoryName } from '@/lib/categories'
import { uiTag } from '@/lib/currency'
import { formatLongDay, formatShortDate, formatTime, relativeDayOf } from '@/lib/dates'
import { formatMoney, formatNames } from '@/lib/money'

/** "Bruno editou Jantar no Ostradamus": who in bold, what they did, and what it was about in bold (none for a payment). */
export interface Headline {
  who: string
  verb: string
  subject: string | null
}

// Why: by ActivityKind's code, what each kind did; a group edit of only the default split says so.
const verbs: Record<string, MessageKey> = {
  bill_created: 'activity_billCreated',
  bill_edited: 'activity_billEdited',
  bill_deleted: 'activity_billDeleted',
  payment_recorded: 'activity_paymentRecorded',
  payment_undone: 'activity_paymentUndone',
  group_created: 'activity_groupCreated',
  group_edited: 'activity_groupEdited',
  group_deleted: 'activity_groupDeleted',
  invite_link_changed: 'activity_inviteLinkChanged',
  member_added: 'activity_memberAdded',
  member_joined: 'activity_memberJoined',
  member_left: 'activity_memberLeft',
  member_removed: 'activity_memberRemoved',
  member_released: 'activity_memberReleased',
  member_renamed: 'activity_memberRenamed',
  charge_sent: 'activity_chargeSent',
  comment_added: 'activity_commentAdded',
}

// Why: these kinds are about the group itself, named by the group rather than by the item's subject.
const groupKinds = new Set(['group_edited', 'invite_link_changed', 'member_joined', 'member_left', 'simplify_debts_changed'])

// Why: "ligou Simplificar dívidas em" or "desligou…" (13.7), by the event's code.
const SIMPLIFY_KIND = 'simplify_debts_changed'

const fieldLabels: Record<string, MessageKey> = {
  amount: 'activity_fieldAmount',
  description: 'activity_fieldDescription',
  spent_at: 'activity_fieldSpentAt',
  payers: 'activity_fieldPayers',
  split: 'activity_fieldSplit',
  name: 'activity_fieldName',
  type: 'activity_fieldType',
  color: 'activity_fieldColor',
  shape: 'activity_fieldShape',
  photo_url: 'activity_fieldPhoto',
  currency: 'activity_fieldCurrency',
  default_split: 'activity_fieldDefaultSplit',
  category: 'activity_fieldCategory',
}

// Why: the group icon's GroupShape, by its code; only Android draws it, but every feed names it.
const groupShapes: Record<string, MessageKey> = {
  cookie: 'activity_shapeCookie',
  petal: 'activity_shapePetal',
  square: 'activity_shapeSquare',
  circle: 'activity_shapeCircle',
}

// Why: a bill's SplitMode and a group's DefaultSplitMode, by their codes.
const splitModes: Record<string, MessageKey> = {
  equal: 'defaultSplit_modeEqual',
  amount: 'split_modeAmount',
  percent: 'defaultSplit_modePercent',
  shares: 'defaultSplit_modeShares',
  income: 'defaultSplit_modeIncome',
}

export const activityTypes: { code: string; label: MessageKey }[] = [
  { code: 'bills', label: 'activity_typeBills' },
  { code: 'payments', label: 'activity_typePayments' },
  { code: 'comments', label: 'activity_typeComments' },
  { code: 'people-and-groups', label: 'activity_typePeople' },
  { code: 'charges', label: 'activity_typeCharges' },
]

export function typeLabel(code: string): string {
  const type = activityTypes.find((entry) => entry.code === code)
  return type ? t(type.label) : code
}

function splitModeLabel(code: string | null | undefined): string {
  return code ? t(splitModes[code] ?? 'defaultSplit_modeEqual') : ''
}

/** "Você" for this device's user, "Alguém" when the server kept no name. */
export function actorName(item: JsActivityItem): string {
  if (item.actorIsMe) return t('activity_you')
  return item.actorName ?? t('activity_someone')
}

export function headline(item: JsActivityItem): Headline {
  const isDefaultSplitOnly = item.kind === 'group_edited' && item.changes.length === 1 && item.changes[0].field === 'default_split'
  // Why: a comment deleted since reads "Ana apagou um comentário em Jantar", with no text under it.
  const verbKey = isDefaultSplitOnly
    ? 'activity_groupDefaultSplit'
    : item.isCommentDeleted
      ? 'activity_commentDeleted'
      : item.kind === SIMPLIFY_KIND
        ? item.isSimplifyTurnedOn ? 'activity_simplifyOn' : 'activity_simplifyOff'
        : (verbs[item.kind] ?? 'activity_billEdited')
  // Why: Spanish says "Ana dividió" but "Tú dividiste".
  const verb = item.actorIsMe ? verbForYou(t(verbKey)) : t(verbKey)
  const isPayment = item.kind === 'payment_recorded' || item.kind === 'payment_undone'
  const subject = isPayment ? null : groupKinds.has(item.kind) ? item.groupName : (item.subject ?? item.groupName)
  return { who: actorName(item), verb, subject: subject ?? null }
}

/** The headline as one sentence, for a row's accessible name and the detail's region. */
export function headlineText(item: JsActivityItem): string {
  const line = headline(item)
  return [line.who, line.verb, line.subject].filter(Boolean).join(' ')
}

function amountOf(item: JsActivityItem): string {
  if (item.amountMinor == null || !item.currency) return ''
  return formatMoney(item.amountMinor, item.currency, item.fractionDigits)
}

function personName(name: string | null | undefined, isMe: boolean): string {
  if (isMe) return t('splitBill_you')
  return name ?? t('balance_formerMember')
}

/** One side of a change, as its field writes it: an amount, a day, a split, the people who paid… */
export function valueText(field: string, value: JsActivityValue): string {
  if (value.minor != null && value.currency) return formatMoney(value.minor, value.currency, value.fractionDigits)
  if (value.millis != null) return `${formatShortDate(value.millis)}, ${formatTime(value.millis)}`
  if (field === 'split' || field === 'default_split') return splitModeLabel(value.code) || '—'
  if (field === 'type' && value.code) return groupTypeLabel(value.code)
  if (field === 'color' && value.code) return avatarColorLabel(value.code)
  if (field === 'shape' && value.code && groupShapes[value.code]) return t(groupShapes[value.code])
  // Why: the shared code says "previous" and "new" for a photo, never its URL, and nothing for none.
  if (field === 'photo_url') return t(value.code === 'previous' ? 'activity_photoPrevious' : value.code === 'new' ? 'activity_photoNew' : 'activity_photoNone')
  if (field === 'category') return value.category ? categoryName(value.category) : '—'
  if (field === 'payers') {
    const names = [...value.names, ...(value.hasFormerMember ? [t('balance_formerMember')] : [])]
    return names.length > 0 ? formatNames(names) : '—'
  }
  return value.text ?? value.code ?? '—'
}

export function fieldLabel(field: string): string {
  return t(fieldLabels[field] ?? 'activity_fieldName')
}

/** "Valor R$ 612,00 → R$ 642,00 · mais 2 mudanças". */
function changesSummary(item: JsActivityItem): string | null {
  const first: JsActivityChange | undefined = item.changes[0]
  if (!first) return null
  const line = `${fieldLabel(first.field)} ${valueText(first.field, first.before)} → ${valueText(first.field, first.after)}`
  const more = item.moreChangesCount
  if (more === 0) return line
  return `${line} · ${t(more === 1 ? 'activity_moreChangesOne' : 'activity_moreChangesOther', { count: more })}`
}

/** An amount of a Simplify debts pair, unsigned, in that pair's own currency; empty when not even the group's is known. */
function debtMoney(minor: number, pair: JsActivityDebtChange): string {
  return pair.currency ? formatMoney(Math.abs(minor), pair.currency, pair.fractionDigits) : ''
}

/**
 * "7 pagamentos viraram 5. Agora Diego te paga R$ 64, e Ana não te deve mais." (13.7): the first pair that came and
 * the first that went for this browser's user, the group's currency first. Nothing while it waits to go up, which the
 * meta already says.
 */
function simplificationDetail(simplification: JsActivitySimplification | null | undefined): string {
  if (!simplification) return ''
  const counts = t('activity_simplifyCounts', { before: simplification.paymentsBefore, after: simplification.paymentsAfter })
  const added = simplification.newPair
  const gone = simplification.gonePair
  const now = added
    ? t(added.afterMinor > 0 ? 'activity_simplifyNowPaysYou' : 'activity_simplifyNowYouPay', { name: nameOrFormer(added.name), amount: debtMoney(added.afterMinor, added) })
    : null
  const goneOwedMe = gone != null && gone.beforeMinor > 0
  const goneName = { name: nameOrFormer(gone?.name) }
  if (now && gone) return `${counts} ${now}, ${t(goneOwedMe ? 'activity_simplifyAndNoLongerOwesYou' : 'activity_simplifyAndYouNoLongerOwe', goneName)}.`
  if (now) return `${counts} ${now}.`
  if (gone) return `${counts} ${t(goneOwedMe ? 'activity_simplifyNoLongerOwesYou' : 'activity_simplifyYouNoLongerOwe', goneName)}.`
  return counts
}

/** One side of a "Ver mudanças" line of 13.7b: "te devia R$ 64", "você deve R$ 20", "não te devia", "não deve mais". */
function debtSideText(change: JsActivityDebtChange, minor: number, isAfter: boolean): string {
  if (minor === 0) return t(isAfter ? 'activity_debtOwesNothing' : 'activity_debtDidntOweYou')
  const amount = debtMoney(minor, change)
  if (minor > 0) return t(isAfter ? 'activity_debtOwesYou' : 'activity_debtOwedYou', { amount })
  return t(isAfter ? 'activity_debtYouOwe' : 'activity_debtYouOwed', { amount })
}

/** A line of "Ver mudanças" or the desktop's table: what changed (a field, or a person for 13.7b), before and after. */
export interface ChangeRow {
  key: string
  label: string
  before: string
  after: string
}

/** A bill's or a group's edit by field, or what "Simplificar dívidas" changed for this browser's user by person. */
export function changeRows(item: JsActivityItem): ChangeRow[] {
  if (item.simplification) {
    return item.simplification.myChanges.map((change: JsActivityDebtChange, index) => ({
      key: `${index}`,
      label: nameOrFormer(change.name),
      before: debtSideText(change, change.beforeMinor, false),
      after: debtSideText(change, change.afterMinor, true),
    }))
  }
  return item.changes.map((change) => ({
    key: change.field,
    label: fieldLabel(change.field),
    before: valueText(change.field, change.before),
    after: valueText(change.field, change.after),
  }))
}

/** "Mudanças em Jantar", or "O que mudou para você em Viagem Floripa" for 13.7b. */
export function changesRegionLabel(item: JsActivityItem): string {
  if (item.kind === SIMPLIFY_KIND) return t('activity_simplifyChangesLabel', { group: item.groupName ?? '' })
  return t('activity_changesIn', { subject: item.subject ?? '' })
}

/** The line under the headline: the amount and split, a payment's two people, an edit's first change… */
export function detailLine(item: JsActivityItem): string {
  const amount = amountOf(item)
  switch (item.kind) {
    case 'bill_created':
      return [amount, splitModeLabel(item.code)].filter(Boolean).join(' · ')
    case 'bill_edited':
    case 'group_edited':
    case 'member_renamed':
      return changesSummary(item) ?? amount
    case 'bill_deleted':
      return t('activity_detailBillDeleted', { amount })
    case 'payment_recorded':
    case 'payment_undone': {
      const people = `${personName(item.fromName, item.fromIsMe)} → ${personName(item.toName, item.toIsMe)}`
      return [people, amount, item.code ? paymentMethodLabel(item.code) : ''].filter(Boolean).join(' · ')
    }
    case 'group_created':
      return item.code ? groupTypeLabel(item.code) : (item.groupType ? groupTypeLabel(item.groupType) : '')
    case 'group_deleted':
      return t('activity_detailGroupDeleted')
    case 'invite_link_changed':
      return t('activity_detailInviteLink')
    case 'member_added':
      return t('activity_detailMemberAdded')
    case 'member_joined':
      return t('activity_detailMemberJoined')
    case 'member_left':
      return t('activity_detailMemberLeft')
    case 'member_removed':
      return t('activity_detailMemberRemoved')
    case 'member_released':
      return t('activity_detailMemberReleased')
    case 'charge_sent':
      return t('activity_detailChargeSent', { amount })
    case 'comment_added':
      return item.body && !item.isCommentDeleted ? `“${item.body}”` : ''
    case SIMPLIFY_KIND:
      return simplificationDetail(item.simplification)
    default:
      return amount
  }
}

const isPaymentKind = (kind: string) => kind === 'payment_recorded' || kind === 'payment_undone'

/** "Abrir conta", "Ver pagamento", "Abrir grupo"…, or "Conta apagada" / "Grupo apagado" once it is gone. */
export function openLabel(item: JsActivityItem): string {
  if (item.deletion) return t(item.deletion.isGroup ? 'activity_groupGone' : 'activity_billGone')
  if (item.kind === SIMPLIFY_KIND) return t('activity_openGroupBalances')
  switch (item.target) {
    case 'bill':
      return t('activity_openBill')
    case 'members':
      return t('activity_openMembers')
    case 'balances':
      return t('activity_openBalances')
    default:
      return t(isPaymentKind(item.kind) ? 'activity_openPayment' : 'activity_openGroup')
  }
}

// Why: Portuguese says "no sábado" and "no domingo" but "na sexta"; English "on" and Spanish "el" for every day.
function onDay(millis: number): string {
  const relative = relativeDayOf(millis)
  if (relative === 'today') return t('activity_onToday')
  if (relative === 'yesterday') return t('activity_onYesterday')
  const weekday = new Date(millis).getDay()
  const longDay = formatLongDay(millis)
  // Why: mid-sentence, Portuguese and Spanish write the weekday in lower case; English keeps its capital.
  const day = locale === 'en' ? longDay : longDay.charAt(0).toLocaleLowerCase(uiTag) + longDay.slice(1)
  return t(weekday === 0 || weekday === 6 ? 'activity_onDayMasculine' : 'activity_onDayFeminine', { day })
}

/**
 * "Bruno apagou Pastel na praia na sexta-feira, 30 de outubro. O registro fica na Atividade, mas a conta não abre
 * mais.", without the name or the day when the device no longer knows them.
 */
export function deletionText(deletion: JsActivityDeletion): string {
  const subject = deletion.subject ?? t(deletion.isGroup ? 'activity_thisGroup' : 'activity_thisBill')
  const who = deletion.byMe
    ? t('activity_deletedByMe', { subject })
    : deletion.byName
      ? t('activity_deletedBy', { name: deletion.byName, subject })
      : t(deletion.isGroup ? 'activity_groupDeletedPassive' : 'activity_billDeletedPassive', { subject })
  const when = deletion.atMillis != null ? ` ${onDay(deletion.atMillis)}` : ''
  return `${who}${when}. ${t(deletion.isGroup ? 'activity_deletedGroupTail' : 'activity_deletedBillTail')}`
}
