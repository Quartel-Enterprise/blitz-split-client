import { Check } from 'lucide-react'
import type { JsGroupRow, JsMemberFace } from 'blitzsplit-shared'
import { pendingSurfaceClass } from '@/components/common'
import { AvatarStack } from '@/components/AvatarStack'
import { balanceTone } from '@/features/balances/BalanceCard'
import { t, type MessageKey } from '@/i18n'
import { formatShortDate } from '@/lib/dates'
import { formatPlainMoney, formatSignedMoney } from '@/lib/money'
import { cn } from '@/lib/utils'

/** "Hoje", "Ontem", "Há 3 dias", "Há 1 semana" or "3 de out.". */
function activityText(group: JsGroupRow): string {
  const count = group.activityCount
  switch (group.activityAge) {
    case 'TODAY':
      return t('groups_activityToday')
    case 'YESTERDAY':
      return t('groups_activityYesterday')
    case 'DAYS':
      return t(count === 1 ? 'groups_activityDaysOne' : 'groups_activityDaysOther', { count })
    case 'WEEKS':
      return t(count === 1 ? 'groups_activityWeeksOne' : 'groups_activityWeeksOther', { count })
    default:
      return formatShortDate(group.lastActivityMillis)
  }
}

/** The Última atividade column: when, then what else the row has to say ("Ontem · você recebe 120,00 e deve 218,00"). */
export function lastActivityText(group: JsGroupRow): string {
  const when = activityText(group)
  switch (group.detail) {
    case 'EVERYONE_SETTLED':
      return t('groups_metaEveryoneSettled', { date: formatShortDate(group.lastActivityMillis) })
    case 'QUEUED':
      return group.pendingBillDescription ? `${when} · ${t('groups_metaQueuedBill', { bill: group.pendingBillDescription })}` : when
    case 'MIXED':
      return `${when} · ${t('groups_metaMixed', { receives: formatPlainMoney(group.receivesMinor, group.fractionDigits, group.currency), owes: formatPlainMoney(group.owesMinor, group.fractionDigits, group.currency) })}`
    case 'OTHERS_STILL_SETTLING':
      return `${when} · ${t('groups_metaOthersSettling')}`
    case 'INVITE_SOMEONE':
      return `${t('groups_onlyYou')} · ${t('groups_metaInvite')}`
    default:
      return when
  }
}

const balanceLabels: Record<string, MessageKey> = {
  TO_RECEIVE: 'groups_balanceToReceive',
  YOU_OWE: 'groups_balanceYouOwe',
  YOU_ARE_SETTLED: 'groups_balanceYouAreSettled',
  NO_EXPENSES: 'groups_noExpenses',
}

/**
 * "+R$ 214,90 a receber", "−R$ 98,00 você deve", "R$ 0,00 sem despesas", or "Quitado". At the end of a row, or at the
 * start of a grid's card with its own amount size (`amountClassName`).
 */
export function GroupBalance({ group, isStacked = false, amountClassName }: { group: JsGroupRow; isStacked?: boolean; amountClassName?: string }) {
  if (group.isSettled) {
    return (
      <span className="inline-flex items-center gap-1 text-sm font-semibold text-muted-foreground">
        <Check aria-hidden className="size-4" /> {t('groups_balanceEveryoneSettled')}
      </span>
    )
  }
  return (
    <span className={cn('flex flex-col', isStacked ? 'items-start' : 'items-end')}>
      <span className={cn('font-semibold whitespace-nowrap tabular-nums', amountClassName, balanceTone(group.netMinor))}>{formatSignedMoney(group.netMinor, group.currency, group.fractionDigits)}</span>
      <span className="text-xs text-muted-foreground">{t(balanceLabels[group.balanceKind] ?? 'groups_noExpenses')}</span>
    </span>
  )
}

/** "na fila": the group, a bill or a payment changed here and not on the server yet. */
export function QueuedBadge() {
  return (
    <span className={cn('inline-flex h-5 shrink-0 items-center rounded-md border px-1.5 text-[11px] font-medium', pendingSurfaceClass)}>
      {t('groups_queued')}
    </span>
  )
}

/** The faces the ViewModel picked and "+N"; screen readers get the member count from the row instead. */
export function Faces({ group }: { group: JsGroupRow }) {
  return <FaceStack faces={Array.from(group.faces)} extraCount={group.extraMemberCount} />
}

/** A group's members, or who paid a bill on Home (`isSmall`, a line tall), as the app's stack of faces. */
export function FaceStack({ faces, extraCount, isSmall = false }: { faces: JsMemberFace[]; extraCount: number; isSmall?: boolean }) {
  return <AvatarStack faces={faces.map((face) => ({ initials: face.initial, color: face.color, photoUrl: face.photoUrl }))} extraCount={extraCount} size={isSmall ? 'xs' : 'sm'} />
}

/** "6 members" or "Just you". */
export function membersText(count: number): string {
  // Why: a group of one is "Just you", so the count is always plural.
  return count === 1 ? t('groups_onlyYou') : t('groups_memberCount', { count })
}
