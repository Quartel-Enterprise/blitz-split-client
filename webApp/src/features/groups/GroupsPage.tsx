import { Check, ChevronRight, CloudOff, ListFilter, Plus, QrCode, RefreshCw, SquarePlus } from 'lucide-react'
import type { GroupsPresenter, JsBalanceTotal, JsGroupRow, JsGroupsState } from 'blitzsplit-shared'
import { useState, type ReactNode } from 'react'
import { useDraft } from '@/hooks/useDraft'
import { Avatar } from '@/components/common'
import { Button } from '@/components/ui/button'
import { balanceTone } from '@/features/balances/BalanceCard'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { GroupExitDialog } from '@/features/group-detail/GroupExitDialog'
import { GroupRowMenu } from '@/features/groups/GroupRowMenu'
import { t, type MessageKey } from '@/i18n'
import { formatShortDate } from '@/lib/dates'
import { formatMoney, formatPlainMoney, formatSignedMoney } from '@/lib/money'
import { cn } from '@/lib/utils'

/**
 * The groups page: "Saldo geral", "A receber" and "A pagar", the tabs Em aberto / Quitados with "Filtrar grupos", and
 * the groups as a table (desktop and tablet, which drops the activity column) or a list (phone). A row opens the
 * group; its ⋯ menu (or a right click on the table) holds the group's actions. The grid comes with its own flow.
 */
export function GroupsPage({ presenter, state }: { presenter: GroupsPresenter; state: JsGroupsState }) {
  const groups = state.showsSettledGroups ? state.settledGroups : state.openGroups
  const showsNoGroups = state.showsSettledGroups ? state.showsNoSettledGroups : state.showsNoOpenGroups
  /*
   * Why: one row's menu at a time, which its ⋯ or a right click on the table's row opens. The table and the phone's
   * list are both rendered (CSS hides one), so the key says which, or the hidden twin's menu would open too.
   */
  const [openMenu, setOpenMenu] = useState<string | null>(null)
  const menuOf = (group: JsGroupRow, surface: 'table' | 'cards') => {
    const key = `${surface}:${group.id}`
    return (
      <GroupRowMenu
        presenter={presenter}
        group={group}
        isOnline={state.exit.isOnline}
        open={openMenu === key}
        onOpenChange={(open) => setOpenMenu(open ? key : null)}
      />
    )
  }
  const exitGroup = state.exitGroup
  return (
    <>
      <div className="flex flex-wrap items-center gap-3">
        <div className="mr-auto flex flex-wrap items-center gap-2">
          <SyncStatus state={state} presenter={presenter} />
        </div>
        <div className="flex w-full gap-2 md:w-auto">
          <Button variant="outline" className="hidden md:inline-flex" onClick={() => presenter.joinWithLink()}>
            <QrCode /> {t('groups_joinWithLink')}
          </Button>
          <Button variant="outline" className="hidden md:inline-flex" onClick={() => presenter.createGroup()}>
            <SquarePlus /> {t('groups_newGroup')}
          </Button>
          <Button className="h-10 flex-1 md:h-9 md:flex-none" onClick={() => presenter.splitBill()}>
            <Plus /> {t('groups_splitBill')}
          </Button>
          <Button variant="outline" size="icon-lg" className="md:hidden" aria-label={t('groups_newGroup')} onClick={() => presenter.createGroup()}>
            <SquarePlus />
          </Button>
          <Button variant="outline" size="icon-lg" className="md:hidden" aria-label={t('groups_joinWithLink')} onClick={() => presenter.joinWithLink()}>
            <QrCode />
          </Button>
        </div>
      </div>

      <SummaryCards presenter={presenter} state={state} />

      <div className="flex flex-wrap items-center gap-3">
        <div role="tablist" aria-label={t('groups_status')} className="inline-flex h-9 items-center rounded-lg bg-muted p-[3px]">
          <StatusTab isSelected={!state.showsSettledGroups} count={state.openGroups.length} onSelect={() => presenter.showSettled(false)}>
            {t('groups_openTitle')}
          </StatusTab>
          <StatusTab isSelected={state.showsSettledGroups} count={state.settledGroups.length} onSelect={() => presenter.showSettled(true)}>
            {t('groups_settledTitle')}
          </StatusTab>
        </div>
        <GroupsFilter presenter={presenter} query={state.filterQuery} />
      </div>

      {groups.length === 0 ? (
        showsNoGroups ? (
          <p className="py-6 text-center text-sm text-muted-foreground">{t(state.showsSettledGroups ? 'groups_noSettled' : 'groups_noOpen')}</p>
        ) : (
          state.filterQuery.trim() && <p className="py-6 text-center text-sm text-muted-foreground">{t('groups_noMatch', { query: state.filterQuery.trim() })}</p>
        )
      ) : (
        <>
          <GroupsTable
            presenter={presenter}
            groups={groups}
            menuOf={(group) => menuOf(group, 'table')}
            onContextMenu={(groupId) => setOpenMenu(`table:${groupId}`)}
          />
          <GroupsCardList presenter={presenter} groups={groups} menuOf={(group) => menuOf(group, 'cards')} />
        </>
      )}

      {exitGroup && (
        <GroupExitDialog
          exit={state.exit}
          currency={exitGroup.currency}
          fractionDigits={exitGroup.fractionDigits}
          events={{
            confirmGroupExit: () => presenter.confirmGroupExit(),
            dismissGroupExit: () => presenter.dismissGroupExit(),
          }}
        />
      )}
    </>
  )
}

/** The sync status, only when it asks for attention: "Offline · 1 alteração na fila", "Sincronizando…" or "Sem resposta do servidor" with "Tentar de novo". */
export function SyncStatus({ state, presenter }: { state: JsGroupsState; presenter: GroupsPresenter }) {
  if (!state.syncBadge) return null
  const isOffline = state.syncBadge === 'OFFLINE'
  const label = isOffline
    ? state.pendingChangeCount > 0
      ? t(state.pendingChangeCount === 1 ? 'groups_syncOfflineQueuedOne' : 'groups_syncOfflineQueuedOther', { count: state.pendingChangeCount })
      : t('groups_syncOffline')
    : state.syncBadge === 'FAILED'
      ? t('groups_syncNoResponse')
      : t('groups_syncSyncing')
  return (
    <>
      <span
        className={cn(
          'inline-flex h-6 items-center gap-1.5 rounded-md border px-2 text-xs font-medium',
          isOffline && state.pendingChangeCount > 0 ? 'border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-300' : 'text-muted-foreground',
        )}
      >
        {isOffline ? <CloudOff className="size-3.5" /> : state.syncBadge === 'SYNCING' && <RefreshCw className="size-3.5" />}
        {label}
      </span>
      {state.canRetrySync && (
        <button onClick={() => presenter.retrySync()} className="h-6 rounded-md px-2 text-xs font-medium text-primary underline outline-none focus-visible:ring-[3px] focus-visible:ring-ring">
          {t('groups_tryAgain')}
        </button>
      )}
    </>
  )
}

/** Each currency's amount with its sign, or a zero in the first currency when none has any. */
function amountsOf(totals: JsBalanceTotal[], select: (total: JsBalanceTotal) => number): string[] {
  const amounts = totals.filter((total) => select(total) !== 0).map((total) => formatSignedMoney(select(total), total.currency, total.fractionDigits))
  if (amounts.length > 0) return amounts
  const first = totals[0]
  return first ? [formatMoney(0, first.currency, first.fractionDigits)] : []
}

const summaryHints: Record<string, MessageKey> = {
  TO_RECEIVE: 'groups_summaryToReceiveHint',
  YOU_OWE: 'groups_summaryToPayHint',
}

/** "Saldo geral", "A receber" and "A pagar" as buttons, one line per currency; the last two stay disabled while empty. */
function SummaryCards({ presenter, state }: { presenter: GroupsPresenter; state: JsGroupsState }) {
  const summary = state.summary
  const totals = Array.from(summary.totals)
  const inGroups = (count: number) => t(count === 1 ? 'groups_inGroupsOne' : 'groups_inGroupsOther', { count })
  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-3">
      <SummaryCard
        label={t('groups_summaryTitle')}
        action={t('groups_summaryOpenBalances')}
        amounts={amountsOf(totals, (total) => total.netMinor)}
        caption={t(summaryHints[summary.kind] ?? 'groups_summarySettledHint')}
        tone={balanceTone(totals.find((total) => total.netMinor !== 0)?.netMinor ?? 0)}
        isEnabled
        onClick={() => presenter.openBalances()}
        className="col-span-2 md:col-span-1"
      />
      <SummaryCard
        label={t('groups_summaryToReceive')}
        action={t('groups_summaryToReceiveAction')}
        amounts={amountsOf(totals, (total) => total.toReceiveMinor)}
        caption={inGroups(summary.toReceiveGroupCount)}
        tone="text-primary"
        isEnabled={summary.canOpenToReceive}
        onClick={() => presenter.openToReceive()}
      />
      <SummaryCard
        label={t('groups_summaryToPay')}
        action={t('groups_summaryToPayAction')}
        amounts={amountsOf(totals, (total) => -total.toPayMinor)}
        caption={inGroups(summary.toPayGroupCount)}
        tone="text-destructive"
        isEnabled={summary.canOpenToPay}
        onClick={() => presenter.openToPay()}
      />
    </div>
  )
}

interface SummaryCardProps {
  label: string
  action: string
  amounts: string[]
  caption: string
  tone: string
  isEnabled: boolean
  onClick: () => void
  className?: string
}

function SummaryCard({ label, action, amounts, caption, tone, isEnabled, onClick, className }: SummaryCardProps) {
  return (
    <button
      onClick={onClick}
      disabled={!isEnabled}
      aria-label={`${label} ${amounts.join(', ')}. ${action}`}
      className={cn(
        'flex min-w-0 flex-col gap-1 rounded-xl border bg-card px-4 py-3.5 text-left outline-none hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring disabled:pointer-events-none sm:px-5 sm:py-4',
        className,
      )}
    >
      <span className="flex items-center justify-between gap-2 text-[13px] font-medium text-muted-foreground">
        {label}
        {isEnabled && <ChevronRight aria-hidden className="size-4" />}
      </span>
      {amounts.map((amount) => (
        <span key={amount} className={cn('truncate text-2xl font-bold tracking-tight tabular-nums sm:text-[28px]', isEnabled ? tone : 'text-muted-foreground')}>
          {amount}
        </span>
      ))}
      <span className="text-xs text-muted-foreground">{caption}</span>
    </button>
  )
}

function StatusTab({ isSelected, count, onSelect, children }: { isSelected: boolean; count: number; onSelect: () => void; children: ReactNode }) {
  return (
    <button
      role="tab"
      aria-selected={isSelected}
      onClick={onSelect}
      className={cn(
        'flex h-[30px] items-center gap-1.5 rounded-md px-3 text-sm font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
        isSelected ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground',
      )}
    >
      {children} <span className="font-normal text-muted-foreground">{count}</span>
    </button>
  )
}

function GroupsFilter({ presenter, query }: { presenter: GroupsPresenter; query: string }) {
  const [draft, setDraft] = useDraft(query, (next) => presenter.filter(next))
  return (
    <label className="ml-auto hidden h-9 w-60 items-center gap-1.5 rounded-md border px-2.5 text-muted-foreground focus-within:ring-[3px] focus-within:ring-ring/50 md:flex">
      <ListFilter aria-hidden className="size-3.5 shrink-0" />
      <input
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
        placeholder={t('groups_filter')}
        aria-label={t('groups_filter')}
        className="min-w-0 flex-1 bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
      />
    </label>
  )
}

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
function lastActivityText(group: JsGroupRow): string {
  const when = activityText(group)
  switch (group.detail) {
    case 'EVERYONE_SETTLED':
      return t('groups_metaEveryoneSettled', { date: formatShortDate(group.lastActivityMillis) })
    case 'QUEUED':
      return group.pendingBillDescription ? `${when} · ${t('groups_metaQueuedBill', { bill: group.pendingBillDescription })}` : when
    case 'MIXED':
      return `${when} · ${t('groups_metaMixed', { receives: formatPlainMoney(group.receivesMinor, group.fractionDigits), owes: formatPlainMoney(group.owesMinor, group.fractionDigits) })}`
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

/** "+R$ 214,90 a receber", "−R$ 98,00 você deve", "R$ 0,00 sem despesas", or "Quitado". */
function GroupBalance({ group }: { group: JsGroupRow }) {
  if (group.isSettled) {
    return (
      <span className="inline-flex items-center gap-1 text-sm font-semibold text-muted-foreground">
        <Check aria-hidden className="size-4" /> {t('groups_balanceEveryoneSettled')}
      </span>
    )
  }
  return (
    <span className="flex flex-col items-end">
      <span className={cn('font-semibold whitespace-nowrap tabular-nums', balanceTone(group.netMinor))}>{formatSignedMoney(group.netMinor, group.currency, group.fractionDigits)}</span>
      <span className="text-xs text-muted-foreground">{t(balanceLabels[group.balanceKind] ?? 'groups_noExpenses')}</span>
    </span>
  )
}

function QueuedBadge() {
  return (
    <span className="inline-flex h-5 shrink-0 items-center rounded-md border border-amber-200 bg-amber-50 px-1.5 text-[11px] font-medium text-amber-800 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-300">
      {t('groups_queued')}
    </span>
  )
}

function Faces({ group }: { group: JsGroupRow }) {
  return (
    <span className="flex items-center -space-x-1.5">
      {group.faces.map((face, index) => (
        <Avatar key={index} initial={face.initial} color={face.color} className="size-6 text-[10px] ring-2 ring-card" />
      ))}
      {group.extraMemberCount > 0 && (
        <span className="flex h-6 min-w-6 items-center justify-center rounded-full bg-muted px-1.5 text-[10px] font-semibold text-muted-foreground ring-2 ring-card">
          +{group.extraMemberCount}
        </span>
      )}
    </span>
  )
}

function membersText(count: number): string {
  // Why: a group of one is "Just you", so the count is always plural.
  return count === 1 ? t('groups_onlyYou') : t('groups_memberCount', { count })
}

interface GroupRowsProps {
  presenter: GroupsPresenter
  groups: JsGroupRow[]
  menuOf: (group: JsGroupRow) => ReactNode
}

/** Desktop and tablet: Grupo, Membros, Última atividade (desktop only), Seu saldo and the ⋯ of each row. */
function GroupsTable({ presenter, groups, menuOf, onContextMenu }: GroupRowsProps & { onContextMenu: (groupId: string) => void }) {
  return (
    <div className="hidden overflow-hidden rounded-xl border bg-card md:block">
      <table className="w-full table-fixed text-sm">
        <thead className="border-b text-[13px] font-medium text-muted-foreground">
          <tr className="h-10">
            <th scope="col" className="px-3 text-left font-medium">
              {t('groups_columnGroup')}
            </th>
            <th scope="col" className="w-[130px] px-3 text-left font-medium lg:w-[150px]">
              {t('groups_columnMembers')}
            </th>
            <th scope="col" className="hidden px-3 text-left font-medium lg:table-cell">
              {t('groups_columnActivity')}
            </th>
            <th scope="col" className="w-[130px] px-3 text-right font-medium lg:w-[150px]">
              {t('groups_columnBalance')}
            </th>
            <th scope="col" className="w-12">
              <span className="sr-only">{t('groups_columnActions')}</span>
            </th>
          </tr>
        </thead>
        <tbody className="divide-y">
          {groups.map((group) => (
            <tr
              key={group.id}
              onClick={() => presenter.openGroup(group.id)}
              onContextMenu={(event) => {
                event.preventDefault()
                onContextMenu(group.id)
              }}
              className="h-14 cursor-pointer hover:bg-accent"
            >
              <td className="px-3">
                <span className="flex min-w-0 items-center gap-2.5">
                  <GroupAvatar initials={group.initials} color={group.color} className="size-8 text-xs" />
                  <button
                    onClick={(event) => {
                      event.stopPropagation()
                      presenter.openGroup(group.id)
                    }}
                    className="truncate rounded-sm text-left font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
                  >
                    {group.name}
                  </button>
                  {group.isPending && <QueuedBadge />}
                </span>
              </td>
              <td className="px-3">
                <span aria-label={membersText(group.memberCount)}>
                  <span aria-hidden>
                    <Faces group={group} />
                  </span>
                </span>
              </td>
              <td className="hidden truncate px-3 text-[13px] text-muted-foreground lg:table-cell">{lastActivityText(group)}</td>
              <td className="px-3 text-right">
                <GroupBalance group={group} />
              </td>
              <td className="pr-2 text-right">{menuOf(group)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

/** The phone: a list, each row with its member count under the name and its ⋯. */
function GroupsCardList({ presenter, groups, menuOf }: GroupRowsProps) {
  return (
    <ul className="flex flex-col divide-y rounded-xl border bg-card md:hidden">
      {groups.map((group) => (
        <li key={group.id} className="flex items-center pr-1">
          <button
            onClick={() => presenter.openGroup(group.id)}
            className="flex min-h-[60px] min-w-0 flex-1 items-center gap-3 px-3 py-2.5 text-left outline-none hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring"
          >
            <GroupAvatar initials={group.initials} color={group.color} className="size-8 text-xs" />
            <span className="flex min-w-0 flex-1 flex-col">
              <span className={cn('font-medium', group.usesTwoNameLines ? 'line-clamp-2' : 'truncate')}>{group.name}</span>
              <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
                {membersText(group.memberCount)}
                {group.isPending && <QueuedBadge />}
              </span>
            </span>
            <GroupBalance group={group} />
          </button>
          {menuOf(group)}
        </li>
      ))}
    </ul>
  )
}
