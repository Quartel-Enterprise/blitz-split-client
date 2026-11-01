import type { JsActivityChange, JsActivityDeletion, JsActivityItem, JsActivityValue } from 'blitzsplit-shared'
import { paymentMethodLabel } from '@/features/balances/BalanceCard'
import { groupTypeLabel } from '@/features/create-group/GroupTypePicker'
import { locale, t, type MessageKey } from '@/i18n'
import { avatarColorLabel } from '@/lib/avatar'
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
}

// Why: these kinds are about the group itself, named by the group rather than by the item's subject.
const groupKinds = new Set(['group_edited', 'invite_link_changed', 'member_joined', 'member_left'])

const fieldLabels: Record<string, MessageKey> = {
  amount: 'activity_fieldAmount',
  description: 'activity_fieldDescription',
  spent_at: 'activity_fieldSpentAt',
  payers: 'activity_fieldPayers',
  split: 'activity_fieldSplit',
  name: 'activity_fieldName',
  type: 'activity_fieldType',
  color: 'activity_fieldColor',
  currency: 'activity_fieldCurrency',
  default_split: 'activity_fieldDefaultSplit',
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
  const verb = t(isDefaultSplitOnly ? 'activity_groupDefaultSplit' : (verbs[item.kind] ?? 'activity_billEdited'))
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
  if (isMe) return t('activity_you')
  return name ?? t('balance_formerMember')
}

/** One side of a change, as its field writes it: an amount, a day, a split, the people who paid… */
export function valueText(field: string, value: JsActivityValue): string {
  if (value.minor != null && value.currency) return formatMoney(value.minor, value.currency, value.fractionDigits)
  if (value.millis != null) return `${formatShortDate(value.millis)}, ${formatTime(value.millis)}`
  if (field === 'split' || field === 'default_split') return splitModeLabel(value.code) || '—'
  if (field === 'type' && value.code) return groupTypeLabel(value.code)
  if (field === 'color' && value.code) return avatarColorLabel(value.code)
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
    default:
      return amount
  }
}

const isPaymentKind = (kind: string) => kind === 'payment_recorded' || kind === 'payment_undone'

/** "Abrir conta", "Ver pagamento", "Abrir grupo"…, or "Conta apagada" / "Grupo apagado" once it is gone. */
export function openLabel(item: JsActivityItem): string {
  if (item.deletion) return t(item.deletion.isGroup ? 'activity_groupGone' : 'activity_billGone')
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

// Why: Portuguese says "no sábado" and "no domingo" but "na sexta"; English "on" for every day.
function onDay(millis: number): string {
  const relative = relativeDayOf(millis)
  if (relative === 'today') return t('activity_onToday')
  if (relative === 'yesterday') return t('activity_onYesterday')
  const weekday = new Date(millis).getDay()
  const longDay = formatLongDay(millis)
  // Why: mid-sentence, Portuguese writes the weekday in lower case; English keeps its capital.
  const day = locale === 'pt' ? longDay.charAt(0).toLocaleLowerCase('pt-BR') + longDay.slice(1) : longDay
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
