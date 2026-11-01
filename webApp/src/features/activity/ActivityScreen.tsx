import { CheckCheck, ChevronDown, CircleCheck, Clock, CloudOff, Info, Link2, ListFilter, Loader2, Pencil, Receipt, RefreshCw, RotateCcw, Send, Trash2, TriangleAlert, UserMinus, UserPlus, UserRound } from 'lucide-react'
import { ActivityPresenter, type JsActivityItem, type JsActivityState, type JsRoute } from 'blitzsplit-shared'
import { useEffect, useRef, type ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { HomeShell, type HomeTab } from '@/components/HomeShell'
import { Button } from '@/components/ui/button'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { avatarStyle } from '@/lib/avatar'
import { formatDayTime, formatLongDayLabel, formatTime } from '@/lib/dates'
import { cn } from '@/lib/utils'
import { ActiveFilters, ActivityFilterButton } from './ActivityFilter'
import { deletionText, detailLine, fieldLabel, headline, headlineText, openLabel, valueText } from './activityText'

// Grupos and Saldos take Atividade's place; Contatos is not built yet and answers "Coming soon".
const tabEvents: Partial<Record<HomeTab, (presenter: ActivityPresenter) => void>> = {
  groups: (p) => p.openGroups(),
  balances: (p) => p.openBalances(),
  contacts: (p) => p.openContacts(),
}

/**
 * The home's Atividade tab (page 12): every group's events by day, the unread ones marked, in the home's frame. On
 * the desktop (from 1024 px) the feed sits beside the item picked, as Saldos does; on a tablet and a phone a click
 * opens what the item is about, and a bill's edit shows its changes in the row. "Filtrar" is a Popover from the
 * tablet up and a Drawer on the phone.
 */
export function ActivityScreen({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new ActivityPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsActivityState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), () => toast(t('groups_comingSoon')))
  // Why: the canvas' desktop (from 1024 px) splits the feed and the item picked; narrower, one column.
  const isWide = useMediaQuery('(min-width: 1024px)')
  const isPhone = !useMediaQuery('(min-width: 768px)')
  useEffect(() => {
    presenter?.detailPaneChanged(isWide)
  }, [presenter, isWide])
  useDeletedNotice(presenter, state)
  if (!presenter || !state) return null

  return (
    <HomeShell
      selected="activity"
      profile={state}
      activityUnreadCount={state.unreadCount}
      onTab={(tab) => tabEvents[tab]?.(presenter)}
      onProfile={() => presenter.openProfile()}
    >
      <main className={cn('mx-auto flex w-full flex-1 flex-col gap-3 p-4 sm:gap-4 sm:p-8 lg:px-10', isWide ? 'max-w-6xl' : 'max-w-3xl')}>
        <h1 className="text-2xl font-semibold tracking-tight">{t('activity_title')}</h1>
        {state.isLoaded && (
          <>
            <Toolbar presenter={presenter} state={state} isPhone={isPhone} />
            {state.showsNotSyncedNotice && (
              <div role="note" className="flex gap-2.5 rounded-lg border bg-muted/40 p-3 text-[13px] leading-relaxed">
                <Info aria-hidden className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
                <span>{t('activity_notSynced')}</span>
              </div>
            )}
            {isWide ? (
              <div className="grid grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)] items-start gap-5">
                <Feed presenter={presenter} state={state} isWide />
                <DetailPane presenter={presenter} state={state} />
              </div>
            ) : (
              <Feed presenter={presenter} state={state} isWide={false} />
            )}
          </>
        )}
      </main>
    </HomeShell>
  )
}

/** "Conta apagada" as a toast when a click (one column) lands on a bill or group someone deleted; then it's consumed. */
function useDeletedNotice(presenter: ActivityPresenter | null, state: JsActivityState | null) {
  const notice = state?.deletedNotice ?? null
  // Why: each state is a new object, so the notice is keyed by what it says, to toast it once.
  const key = notice ? `${notice.isGroup}|${notice.subject}|${notice.byName}|${notice.atMillis}` : null
  useEffect(() => {
    if (!presenter || !notice) return
    toast(t(notice.isGroup ? 'activity_groupGone' : 'activity_billGone'), { description: deletionText(notice) })
    presenter.deletedNoticeShown()
  }, [presenter, key])
}

/** The status when it asks for attention, "Filtrar" with the filters in use, and "3 novas" with "Marcar tudo como lido". */
function Toolbar({ presenter, state, isPhone }: { presenter: ActivityPresenter; state: JsActivityState; isPhone: boolean }) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <StatusChip presenter={presenter} state={state} />
      {state.showsTools && (
        <>
          <ActivityFilterButton presenter={presenter} state={state} isPhone={isPhone} />
          <ActiveFilters presenter={presenter} state={state} />
          <span className="flex-1" />
          {state.canMarkAllRead && (
            <span className="flex items-center gap-1">
              <span className="text-[13px] font-medium text-primary tabular-nums">
                {t(state.unreadCount === 1 ? 'activity_newOne' : 'activity_newOther', { count: state.unreadCount })}
              </span>
              <Button variant="ghost" size="sm" onClick={() => presenter.markAllRead()}>
                <CheckCheck /> {t(isPhone ? 'activity_markRead' : 'activity_markAllRead')}
              </Button>
            </span>
          )}
        </>
      )}
    </div>
  )
}

/** "Offline · 2 alterações na fila", "Sem resposta do servidor" with "Tentar de novo", or "Sincronizando…". */
function StatusChip({ presenter, state }: { presenter: ActivityPresenter; state: JsActivityState }) {
  if (!state.status) return null
  const hasQueue = state.status === 'offline' && state.queuedCount > 0
  const label =
    state.status === 'offline'
      ? hasQueue
        ? t(state.queuedCount === 1 ? 'groups_syncOfflineQueuedOne' : 'groups_syncOfflineQueuedOther', { count: state.queuedCount })
        : t('activity_offline')
      : t(state.status === 'server-error' ? 'activity_serverError' : 'activity_syncing')
  const icon =
    state.status === 'offline' ? <CloudOff className="size-3.5" /> : state.status === 'server-error' ? <TriangleAlert className="size-3.5" /> : <RefreshCw className="size-3.5 animate-spin" />
  return (
    <>
      <span
        role="status"
        className={cn(
          'inline-flex h-6 items-center gap-1.5 rounded-md border px-2 text-xs font-medium',
          hasQueue ? 'border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-300' : state.status === 'server-error' ? 'text-foreground' : 'text-muted-foreground',
        )}
      >
        {icon}
        {label}
      </span>
      {state.status === 'server-error' && (
        <Button variant="outline" size="xs" disabled={state.isRefreshing} onClick={() => presenter.refresh()}>
          {t('activity_retry')}
        </Button>
      )}
    </>
  )
}

/** The days with their items, then "Carregando mais…" or, offline, why the older ones wait. */
function Feed({ presenter, state, isWide }: { presenter: ActivityPresenter; state: JsActivityState; isWide: boolean }) {
  const itemCount = state.days.reduce((count, day) => count + day.items.length, 0)
  const end = useEndReached(state.hasMore && !state.isLoadingMore, itemCount, () => presenter.endReached())
  if (state.showsPlaceholders) return <Placeholders />
  if (state.days.length === 0) {
    // Why: on the desktop the empty state takes the detail's place, as the canvas draws it.
    return isWide ? <span /> : state.emptyState ? <EmptyActivity presenter={presenter} emptyState={state.emptyState} /> : null
  }
  const selectedId = isWide ? (state.selectedItem?.id ?? null) : null
  return (
    <div className="flex flex-col gap-3">
      <section aria-label={t('activity_title')} className="overflow-hidden rounded-xl border bg-card">
        <div className="-mb-px">
          {state.days.map((day, index) => (
            <div key={day.dayStartMillis}>
              <h2 className={cn('flex h-9 items-center border-b bg-muted px-4 text-xs font-medium text-muted-foreground', index > 0 && 'border-t')}>
                {formatLongDayLabel(day.dayStartMillis, day.relativeDay)}
              </h2>
              {day.items.map((item) => (
                <FeedRow key={item.id} presenter={presenter} item={item} isWide={isWide} isSelected={item.id === selectedId} />
              ))}
            </div>
          ))}
        </div>
      </section>
      <div ref={end} />
      {state.isLoadingMore && (
        <div role="status" className="flex items-center justify-center gap-2 py-2 text-[13px] text-muted-foreground">
          <Loader2 aria-hidden className="size-4 animate-spin" />
          {t('activity_loadingMore')}
        </div>
      )}
      {state.showsOfflineEnd && <p className="px-4 text-center text-[13px] text-muted-foreground">{t('activity_offlineEnd')}</p>}
    </div>
  )
}

/** Calls `onEnd` when the end of the list scrolls into view, while there is more to show. */
function useEndReached(isEnabled: boolean, itemCount: number, onEnd: () => void) {
  const ref = useRef<HTMLDivElement>(null)
  const onEndRef = useRef(onEnd)
  onEndRef.current = onEnd
  useEffect(() => {
    const element = ref.current
    if (!isEnabled || !element) return
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) onEndRef.current()
    })
    observer.observe(element)
    return () => observer.disconnect()
    // Why: a new page shown re-arms it, so a short page still asks for the next one.
  }, [isEnabled, itemCount])
  return ref
}

/** One item: the face with the kind's badge, "Bruno editou Jantar", its detail, the group and the time, the unread dot. */
function FeedRow({ presenter, item, isWide, isSelected }: { presenter: ActivityPresenter; item: JsActivityItem; isWide: boolean; isSelected: boolean }) {
  const showsChanges = !isWide && item.hasChangesToShow
  return (
    <div className={cn('border-b', isSelected && 'bg-muted')}>
      <button
        type="button"
        aria-current={isSelected || undefined}
        onClick={() => presenter.clickItem(item.id)}
        className={cn(
          'flex w-full gap-3 px-4 text-left outline-none hover:bg-accent/50 focus-visible:ring-[3px] focus-visible:ring-inset focus-visible:ring-ring/50',
          showsChanges ? 'pt-3 pb-1.5' : 'py-3',
        )}
      >
        <ActorFace item={item} className="size-8 text-xs" badgeClassName="size-4 [&>svg]:size-2.5" />
        <span className="flex min-w-0 flex-1 flex-col gap-1">
          <HeadlineLine item={item} className="text-sm leading-5" />
          <span className={cn('text-[13px] leading-[18px] text-muted-foreground', isWide ? 'line-clamp-1' : 'line-clamp-2')}>{detailLine(item)}</span>
          <ItemMeta item={item} />
        </span>
        {item.isUnread && (
          <>
            <span aria-hidden className="mt-1.5 size-2 shrink-0 rounded-full bg-primary" />
            <span className="sr-only">{t('activity_unread')}</span>
          </>
        )}
      </button>
      {showsChanges && <InlineChanges presenter={presenter} item={item} />}
    </div>
  )
}

/** "Ver mudanças" under a bill's edit, which opens its before → after in the row, with "Abrir conta". */
function InlineChanges({ presenter, item }: { presenter: ActivityPresenter; item: JsActivityItem }) {
  return (
    <div className="flex flex-col gap-1 pr-4 pb-2 pl-15">
      {item.isExpanded && (
        <div role="region" aria-label={t('activity_changesIn', { subject: item.subject ?? '' })} className="flex flex-col gap-1.5 rounded-lg border bg-background p-3 text-[13px]">
          {item.changes.map((change) => (
            <span key={change.field} className="flex flex-wrap gap-x-2">
              <span className="min-w-20 text-muted-foreground">{fieldLabel(change.field)}</span>
              <span>
                <s className="text-muted-foreground">{valueText(change.field, change.before)}</s> <span aria-label={t('activity_to')}>→</span>{' '}
                <span className="font-medium">{valueText(change.field, change.after)}</span>
              </span>
            </span>
          ))}
          <Button variant="link" size="sm" className="h-7 self-start px-0" onClick={() => presenter.openTarget(item.id)}>
            {openLabel(item)}
          </Button>
        </div>
      )}
      <Button variant="ghost" size="sm" aria-expanded={item.isExpanded} className="-ml-3 self-start text-muted-foreground" onClick={() => presenter.toggleChanges(item.id)}>
        {t(item.isExpanded ? 'activity_hideChanges' : 'activity_showChanges')}
        <ChevronDown aria-hidden className={cn('transition-transform', item.isExpanded && 'rotate-180')} />
      </Button>
    </div>
  )
}

/** "**Bruno** editou **Jantar no Ostradamus**", the names bolder while the item is unread. */
function HeadlineLine({ item, isBold = item.isUnread, className }: { item: JsActivityItem; isBold?: boolean; className?: string }) {
  const line = headline(item)
  const strong = isBold ? 'font-bold' : 'font-semibold'
  return (
    <span className={className}>
      <strong className={strong}>{line.who}</strong> {line.verb}
      {line.subject && (
        <>
          {' '}
          <strong className={strong}>{line.subject}</strong>
        </>
      )}
    </span>
  )
}

/** The group's shape and name, then the time, or the orange dot with "Aguardando envio" while it waits to go up. */
function ItemMeta({ item }: { item: JsActivityItem }) {
  return (
    <span className="flex min-w-0 items-center gap-1.5 text-xs text-muted-foreground">
      <GroupMark color={item.groupColor} />
      <span className="truncate">{item.groupName}</span>
      <span aria-hidden>·</span>
      {item.isPending ? (
        <>
          <span aria-hidden className="size-1.5 shrink-0 rounded-full bg-amber-600" />
          <span className="shrink-0">{t('activity_pending')}</span>
        </>
      ) : (
        <span className="shrink-0 tabular-nums">{formatTime(item.occurredAt)}</span>
      )}
    </span>
  )
}

// Why: a deleted group is no longer on the device, so its shape goes grey.
function GroupMark({ color }: { color: string | null | undefined }) {
  if (!color) return <span aria-hidden className="size-3 shrink-0 rounded-[3px] bg-muted" />
  return <span aria-hidden style={avatarStyle(color)} className="avatar size-3 shrink-0 rounded-[3px]" />
}

function kindIcon(item: JsActivityItem): ReactNode {
  if (item.deletion) return <Trash2 />
  switch (item.kind) {
    case 'payment_recorded':
      return <CircleCheck />
    case 'payment_undone':
      return <RotateCcw />
    case 'group_created':
    case 'group_edited':
      return <Pencil />
    case 'group_deleted':
      return <Trash2 />
    case 'invite_link_changed':
      return <Link2 />
    case 'member_added':
    case 'member_joined':
      return <UserPlus />
    case 'member_left':
    case 'member_removed':
      return <UserMinus />
    case 'member_released':
    case 'member_renamed':
      return <UserRound />
    case 'charge_sent':
      return <Send />
    default:
      return <Receipt />
  }
}

// Why: as the canvas colors the badge: bills and charges blue, payments green, people and groups grey, deleted red.
function badgeTone(item: JsActivityItem): { className: string; style?: React.CSSProperties } {
  if (item.deletion) return { className: 'bg-destructive text-background' }
  if (item.type === 'payments') return { className: 'avatar', style: avatarStyle('green') }
  if (item.type === 'bills' || item.type === 'charges') return { className: 'avatar', style: avatarStyle('blue') }
  if (item.type === 'comments') return { className: 'bg-primary text-primary-foreground' }
  return { className: 'bg-muted text-foreground' }
}

/** Who did it, with what kind of thing it was as a badge on the corner (a bin once its bill or group is gone). */
function ActorFace({ item, className, badgeClassName }: { item: JsActivityItem; className: string; badgeClassName: string }) {
  const tone = badgeTone(item)
  return (
    <span className="relative shrink-0 self-start">
      <Avatar initial={item.actorInitials} color={item.actorColor} className={className} />
      <span
        aria-hidden
        style={tone.style}
        className={cn('absolute -right-1 -bottom-1 flex items-center justify-center rounded-full border-2 border-card', tone.className, badgeClassName)}
      >
        {kindIcon(item)}
      </span>
    </span>
  )
}

/** A new device waiting for its first page: grey rows under "Sincronizando…". */
function Placeholders() {
  const widths = [
    ['88%', '60%'],
    ['72%', '48%'],
    ['80%', '66%'],
    ['64%', '52%'],
    ['76%', '40%'],
  ]
  return (
    <div role="status" aria-label={t('activity_loading')} className="overflow-hidden rounded-xl border bg-card">
      <span className="block h-9 border-b bg-muted" />
      {widths.map(([first, second], index) => (
        <span key={index} className="flex gap-3 border-b px-4 py-3.5 last:border-b-0">
          <span className="size-8 shrink-0 animate-pulse rounded-full bg-muted" />
          <span className="flex flex-1 flex-col gap-2 pt-1">
            <span className="h-3 animate-pulse rounded bg-muted" style={{ width: first }} />
            <span className="h-2.5 animate-pulse rounded bg-muted" style={{ width: second }} />
          </span>
        </span>
      ))}
    </div>
  )
}

/** "Nada por aqui ainda" leads to Grupos; "Nada com esses filtros" clears them. */
function EmptyActivity({ presenter, emptyState }: { presenter: ActivityPresenter; emptyState: string }) {
  const isNoResults = emptyState === 'no-results'
  return (
    <section className="flex flex-1 flex-col items-center justify-center gap-3 rounded-xl border border-dashed p-8 text-center">
      <span aria-hidden className="flex size-12 items-center justify-center rounded-xl bg-muted text-muted-foreground">
        {isNoResults ? <ListFilter className="size-6" /> : <Clock className="size-6" />}
      </span>
      <h2 className="mt-1 text-lg font-semibold">{t(isNoResults ? 'activity_noResultsTitle' : 'activity_emptyTitle')}</h2>
      <p className="max-w-sm text-sm text-muted-foreground">{t(isNoResults ? 'activity_noResultsBody' : 'activity_emptyBody')}</p>
      <Button variant="outline" onClick={() => (isNoResults ? presenter.clearFilters() : presenter.emptyAction())}>
        {t(isNoResults ? 'activity_noResultsAction' : 'activity_emptyAction')}
      </Button>
    </section>
  )
}

/** The desktop's pane: the item picked, the empty state, or, while the first page comes, where the details will be. */
function DetailPane({ presenter, state }: { presenter: ActivityPresenter; state: JsActivityState }) {
  if (state.emptyState) return <EmptyActivity presenter={presenter} emptyState={state.emptyState} />
  const item = state.selectedItem
  if (!item) {
    return (
      <section aria-label={t('activity_title')} className="flex min-h-40 items-center justify-center rounded-xl border border-dashed p-6">
        <p className="text-sm text-muted-foreground">{t('activity_detailWait')}</p>
      </section>
    )
  }
  return <ItemDetail presenter={presenter} item={item} related={state.relatedItems} />
}

function whenText(item: JsActivityItem): string {
  if (item.isPending) return t('activity_metaPending')
  return `${formatLongDayLabel(item.dayStartMillis, item.relativeDay)}, ${formatTime(item.occurredAt)}`
}

/**
 * Who did what, in which group and when, with what it opens; then what changed (before and after), its detail, or
 * who deleted it, and the other events of the same bill.
 */
function ItemDetail({ presenter, item, related }: { presenter: ActivityPresenter; item: JsActivityItem; related: JsActivityItem[] }) {
  return (
    <section aria-label={headlineText(item)} className="sticky top-8 flex min-w-0 flex-col gap-5 rounded-xl border bg-card p-6">
      <header className="flex items-start gap-3">
        <ActorFace item={item} className="size-12 text-base" badgeClassName="size-[22px] [&>svg]:size-3" />
        <span className="flex min-w-0 flex-1 flex-col gap-1">
          <h2 className="text-lg leading-snug">
            <HeadlineLine item={item} isBold={false} />
          </h2>
          <span className="text-[13px] text-muted-foreground">{[item.groupName, whenText(item)].filter(Boolean).join(' · ')}</span>
        </span>
        {!item.deletion && (
          <Button variant="outline" size="sm" onClick={() => presenter.openTarget(item.id)}>
            {openLabel(item)}
          </Button>
        )}
      </header>
      {item.changes.length > 0 ? (
        <div className="flex flex-col gap-2">
          <h3 className="text-sm font-medium">{t('activity_whatChanged')}</h3>
          <table className="w-full overflow-hidden rounded-lg border text-sm">
            <thead>
              <tr className="h-9 border-b bg-muted text-left text-xs text-muted-foreground">
                <th scope="col" className="w-32 px-3 font-medium">{t('activity_columnField')}</th>
                <th scope="col" className="px-3 font-medium">{t('activity_columnBefore')}</th>
                <th scope="col" className="px-3 font-medium">{t('activity_columnAfter')}</th>
              </tr>
            </thead>
            <tbody>
              {item.changes.map((change) => (
                <tr key={change.field} className="h-10 border-b last:border-b-0">
                  <th scope="row" className="px-3 text-left font-normal text-muted-foreground">{fieldLabel(change.field)}</th>
                  <td className="px-3 text-muted-foreground"><s>{valueText(change.field, change.before)}</s></td>
                  <td className="px-3 font-medium">{valueText(change.field, change.after)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        !item.deletion && <p className="text-sm leading-relaxed">{detailLine(item)}</p>
      )}
      {item.deletion && (
        <div role="note" className="flex gap-2.5 rounded-lg border bg-muted/40 p-3 text-[13px] leading-relaxed">
          <Trash2 aria-hidden className="mt-0.5 size-4 shrink-0 text-destructive" />
          <span>{deletionText(item.deletion)}</span>
        </div>
      )}
      {related.length > 0 && (
        <div className="flex flex-col gap-1">
          <h3 className="pb-1 text-sm font-medium">{t('activity_moreIn', { subject: item.subject ?? '' })}</h3>
          {related.map((other) => (
            <button
              key={other.id}
              type="button"
              onClick={() => presenter.clickItem(other.id)}
              className="flex items-start gap-3 rounded-md p-2 text-left outline-none hover:bg-accent/50 focus-visible:ring-[3px] focus-visible:ring-ring/50"
            >
              <Avatar initial={other.actorInitials} color={other.actorColor} className="size-7 text-[11px]" />
              <span className="flex min-w-0 flex-1 flex-col">
                <HeadlineLine item={other} className="text-[13px]" />
                <span className="truncate text-xs text-muted-foreground">{detailLine(other)}</span>
              </span>
              <span className="shrink-0 text-xs text-muted-foreground tabular-nums">
                {other.isPending ? t('activity_pending') : formatDayTime(other.occurredAt, other.relativeDay)}
              </span>
            </button>
          ))}
        </div>
      )}
    </section>
  )
}
