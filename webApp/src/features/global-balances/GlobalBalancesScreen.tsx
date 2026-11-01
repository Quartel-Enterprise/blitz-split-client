import { ChevronDown, CircleCheck, Check, WifiOff } from 'lucide-react'
import { GlobalBalancesPresenter, type JsBalanceTotal, type JsGlobalBalancesState, type JsPeopleSection, type JsPersonRow, type JsRoute } from 'blitzsplit-shared'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { HomeShell, type HomeTab } from '@/components/HomeShell'
import { PersonGroupsList } from '@/components/PersonGroupsList'
import { Button } from '@/components/ui/button'
import { balanceTone, nameOrFormer, useRecordedPaymentNotice } from '@/features/balances/BalanceCard'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { formatMoney, formatSignedMoney } from '@/lib/money'
import { cn } from '@/lib/utils'
import { ChargeDialog, copyChargeMessage, shareChargeMessage } from './ChargeDialog'

// Grupos goes back to the groups; Contatos and Atividade are not built yet and answer "Coming soon".
const tabEvents: Partial<Record<HomeTab, (presenter: GlobalBalancesPresenter) => void>> = {
  groups: (p) => p.openGroups(),
  contacts: (p) => p.openContacts(),
  activity: (p) => p.openActivity(),
}

const filters = ['all', 'you-owe', 'owes-you'] as const

const filterLabels: Record<string, MessageKey> = {
  all: 'globalBalances_all',
  'you-owe': 'globalBalances_youOwe',
  'owes-you': 'globalBalances_owesYou',
}

const emptyWords: Record<string, { title: MessageKey; body: MessageKey; bodyOne: MessageKey | null; action: MessageKey }> = {
  settled: { title: 'globalBalances_settledTitle', body: 'globalBalances_settledBody', bodyOne: null, action: 'globalBalances_settledAction' },
  'nothing-to-pay': {
    title: 'globalBalances_nothingToPayTitle',
    body: 'globalBalances_nothingToPayBody',
    bodyOne: 'globalBalances_nothingToPayBodyOne',
    action: 'globalBalances_nothingToPayAction',
  },
  'nothing-to-receive': {
    title: 'globalBalances_nothingToReceiveTitle',
    body: 'globalBalances_nothingToReceiveBody',
    bodyOne: 'globalBalances_nothingToReceiveBodyOne',
    action: 'globalBalances_nothingToReceiveAction',
  },
}

/**
 * The home's Saldos tab: each person once, netted across every group, in the home's frame. The three totals are the
 * filter. On the desktop the people list sits beside the one picked, whose groups and actions fill the pane; on a
 * tablet and a phone "Por grupo" opens in the row. There is no "Rachar conta": the actions are on each person.
 */
export function GlobalBalancesScreen({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new GlobalBalancesPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsGlobalBalancesState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), (action) =>
    toast(t(action === 'coming-soon' ? 'groups_comingSoon' : 'globalBalances_changeError')),
  )
  useActions(presenter, (p, onShare: (message: string) => void) => p.observeShare(onShare), shareChargeMessage)
  useActions(presenter, (p, onCopy: (message: string) => void) => p.observeCopy(onCopy), copyChargeMessage)
  // Why: Registrar pagamento, which Acertar or Recebi opened over Saldos, just closed.
  useRecordedPaymentNotice(presenter, state?.recordedPayment)
  // Why: the canvas' desktop (from 1024 px) splits the people and the one picked; narrower, one column.
  const isWide = useMediaQuery('(min-width: 1024px)')
  if (!presenter || !state) return null

  return (
    <HomeShell
      selected="balances"
      profile={state}
      onTab={(tab) => tabEvents[tab]?.(presenter)}
      onProfile={() => presenter.openProfile()}
      overlays={state.charge && <ChargeDialog presenter={presenter} state={state} charge={state.charge} />}
    >
      <main className={cn('mx-auto flex w-full flex-1 flex-col gap-5 p-4 sm:p-8 lg:px-10', isWide ? 'max-w-6xl' : 'max-w-3xl')}>
        <h1 className="text-2xl font-semibold tracking-tight">{t('globalBalances_title')}</h1>
        {state.showsOfflineNotice && (
          <span className="inline-flex items-center gap-1.5 self-start rounded-md border px-2 py-1 text-xs text-muted-foreground">
            <WifiOff aria-hidden className="size-3.5" />
            {t('globalBalances_offline')}
          </span>
        )}
        {state.isLoaded && (
          <>
            <FilterCards presenter={presenter} state={state} />
            {isWide ? (
              <BalancesListDetail presenter={presenter} state={state} />
            ) : state.emptyState ? (
              <EmptyBalances presenter={presenter} emptyState={state.emptyState} total={state.emptyStateTotal} count={state.emptyStatePeopleCount} />
            ) : (
              <section aria-label={t('globalBalances_title')} className="overflow-hidden rounded-xl border bg-card">
                {state.sections.map((section, index) => (
                  <PeopleSection key={String(section.isYouOwe)} presenter={presenter} section={section} isFirst={index === 0} />
                ))}
              </section>
            )}
          </>
        )}
      </main>
    </HomeShell>
  )
}

/** The desktop's list and detail: the people on the left, the one picked on the right, or the empty state there. */
function BalancesListDetail({ presenter, state }: { presenter: GlobalBalancesPresenter; state: JsGlobalBalancesState }) {
  const selected = state.sections.flatMap((section) => section.rows).find((row) => row.id === state.selectedPersonId)
  return (
    <div className="grid grid-cols-[minmax(0,1fr)_minmax(0,1.25fr)] items-start gap-5">
      {state.emptyState ? (
        <span />
      ) : (
        <section aria-label={t('globalBalances_title')} className="overflow-hidden rounded-xl border bg-card">
          {state.sections.map((section, index) => (
            <PeopleSection key={String(section.isYouOwe)} presenter={presenter} section={section} isFirst={index === 0} selectedId={selected?.id ?? null} />
          ))}
        </section>
      )}
      {state.emptyState ? (
        <EmptyBalances presenter={presenter} emptyState={state.emptyState} total={state.emptyStateTotal} count={state.emptyStatePeopleCount} />
      ) : (
        selected && <PersonDetail presenter={presenter} row={selected} />
      )}
    </div>
  )
}

/** "Saldo geral", "Você deve" and "Te devem": each total is a toggle that filters the people under it. */
function FilterCards({ presenter, state }: { presenter: GlobalBalancesPresenter; state: JsGlobalBalancesState }) {
  return (
    <div role="group" aria-label={t('globalBalances_filter')} className="grid grid-cols-1 gap-3 sm:grid-cols-3">
      {filters.map((filter) => {
        const isOn = state.filter === filter
        return (
          <button
            key={filter}
            aria-pressed={isOn}
            onClick={() => presenter.changeFilter(filter)}
            className={cn(
              'flex flex-col gap-1 rounded-xl border bg-card p-4 text-left outline-none hover:bg-accent/50 focus-visible:ring-[3px] focus-visible:ring-ring/50',
              isOn && 'border-primary ring-1 ring-primary',
            )}
          >
            <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
              {t(filterLabels[filter])}
              {isOn && <Check aria-hidden className="size-3.5 text-primary" />}
            </span>
            {state.totals.map((total) => (
              <span key={total.currency} className={cn('text-xl font-semibold tabular-nums', amountTone(filter, total))}>
                {totalText(filter, total)}
              </span>
            ))}
            <span className="text-xs text-muted-foreground">{subText(filter, state)}</span>
          </button>
        )
      })}
    </div>
  )
}

function amountOf(filter: string, total: JsBalanceTotal): number {
  return filter === 'all' ? total.netMinor : filter === 'you-owe' ? -total.toPayMinor : total.toReceiveMinor
}

function amountTone(filter: string, total: JsBalanceTotal): string {
  return balanceTone(amountOf(filter, total))
}

// Why: "Saldo geral" is the signed net ("+R$ 186,40"); the other two are amounts ("R$ 126,00").
function totalText(filter: string, total: JsBalanceTotal): string {
  if (filter === 'all') return formatSignedMoney(total.netMinor, total.currency, total.fractionDigits)
  return formatMoney(Math.abs(amountOf(filter, total)), total.currency, total.fractionDigits)
}

function peopleText(count: number): string {
  if (count === 0) return t('globalBalances_nobody')
  return count === 1 ? t('globalBalances_onePerson') : t('globalBalances_people', { count })
}

function subText(filter: string, state: JsGlobalBalancesState): string {
  const toPay = state.toPayPeopleCount
  const toReceive = state.toReceivePeopleCount
  if (filter === 'you-owe') return peopleText(toPay)
  if (filter === 'owes-you') return peopleText(toReceive)
  if (toPay + toReceive === 0) return t('globalBalances_allSubSettled')
  return t(state.emptyState ? 'globalBalances_allSubEmpty' : 'globalBalances_allSub')
}

// Why: selectedId is the desktop's person in the pane, whose rows pick instead of opening in place; undefined elsewhere.
function PeopleSection({
  presenter,
  section,
  isFirst,
  selectedId,
}: {
  presenter: GlobalBalancesPresenter
  section: JsPeopleSection
  isFirst: boolean
  selectedId?: string | null
}) {
  const title = t(section.isYouOwe ? 'globalBalances_youOwe' : 'globalBalances_owesYou')
  return (
    <>
      <h2 className={cn('flex h-9 items-center justify-between border-b bg-muted px-4 text-xs font-medium text-muted-foreground', !isFirst && 'border-t')}>
        <span>{title}</span>
        <span className="tabular-nums">
          {section.totals.map((total) => formatMoney(total.amountMinor, total.currency, total.fractionDigits)).join(' + ')}
        </span>
      </h2>
      {section.rows.map((row) =>
        selectedId === undefined ? (
          <PersonRowItem key={row.id} presenter={presenter} row={row} />
        ) : (
          <PersonPickRow key={row.id} presenter={presenter} row={row} isSelected={row.id === selectedId} />
        ),
      )}
    </>
  )
}

/** Someone once: the net and whose way it goes, "Por grupo" opening in the row, then Recebi and Cobrar or Acertar. */
function PersonRowItem({ presenter, row }: { presenter: GlobalBalancesPresenter; row: JsPersonRow }) {
  const name = nameOrFormer(row.name)
  return (
    <div className="flex flex-col gap-2 border-b px-4 py-3 last:border-b-0">
      <PersonSummary row={row} name={name} />
      {row.isExpanded && (
        <PersonGroupsList
          groups={row.groups}
          netMinor={row.netMinor}
          currency={row.currency}
          fractionDigits={row.fractionDigits}
          label={`${name} · ${t('globalBalances_byGroup')}`}
          className="ml-12"
        />
      )}
      <div className="ml-12 flex items-center gap-2">
        <Button
          variant="ghost"
          size="sm"
          aria-expanded={row.isExpanded}
          aria-label={t(row.isExpanded ? 'globalBalances_hideByGroup' : 'globalBalances_showByGroup', { name })}
          onClick={() => presenter.togglePerson(row.id)}
          className="-ml-3 text-muted-foreground"
        >
          {t('globalBalances_byGroup')}
          <ChevronDown aria-hidden className={cn('transition-transform', row.isExpanded && 'rotate-180')} />
        </Button>
        <span className="flex-1" />
        <PersonActions presenter={presenter} row={row} name={name} />
      </div>
    </div>
  )
}

/** The face, the name with its groups, and the net with whose way it goes, alike in the row and the desktop's pick. */
function PersonSummary({ row, name }: { row: JsPersonRow; name: string }) {
  const groups = row.groups.length > 1 ? t('globalBalances_groups', { count: row.groups.length }) : (row.groups[0]?.name ?? '')
  const meta = row.isGuest ? t('globalBalances_guest', { groups }) : groups
  return (
    <div className="flex items-center gap-3">
      <PersonFace row={row} className="size-9 text-sm" />
      <span className="flex min-w-0 flex-1 flex-col text-left">
        <span className="truncate font-medium">{name}</span>
        <span className="truncate text-xs text-muted-foreground">{meta}</span>
      </span>
      <span className="flex flex-col items-end">
        <span className={cn('font-semibold tabular-nums', balanceTone(row.netMinor))}>
          {formatSignedMoney(row.netMinor, row.currency, row.fractionDigits)}
        </span>
        <span className="text-xs text-muted-foreground">{t(row.owesYou ? 'globalBalances_rowOwesYou' : 'globalBalances_rowYouOwe')}</span>
      </span>
    </div>
  )
}

// Why: a blank face for someone no longer in the group, as the group's Saldos draws them.
function PersonFace({ row, className }: { row: JsPersonRow; className: string }) {
  if (!row.color) return <span aria-hidden className={cn('shrink-0 rounded-full bg-muted', className)} />
  return <Avatar initial={row.initials} color={row.color} className={className} />
}

/** Recebi and Cobrar for someone who owes you, else Acertar: in the row and in the desktop's pane. */
function PersonActions({ presenter, row, name, size = 'sm' }: { presenter: GlobalBalancesPresenter; row: JsPersonRow; name: string; size?: 'sm' | 'default' }) {
  if (!row.owesYou) {
    return (
      <Button size={size} aria-label={t('globalBalances_settleWith', { name })} onClick={() => presenter.settle(row.id)}>
        {t('globalBalances_settle')}
      </Button>
    )
  }
  return (
    <>
      <Button variant="ghost" size={size} aria-label={t('globalBalances_receivedFrom', { name })} onClick={() => presenter.received(row.id)}>
        {t('globalBalances_received')}
      </Button>
      <Button variant="secondary" size={size} aria-label={t('globalBalances_chargePerson', { name })} onClick={() => presenter.charge(row.id)}>
        {t('globalBalances_charge')}
      </Button>
    </>
  )
}

/** A person on the desktop's list: a click shows them in the pane beside it, the one shown highlighted. */
function PersonPickRow({ presenter, row, isSelected }: { presenter: GlobalBalancesPresenter; row: JsPersonRow; isSelected: boolean }) {
  return (
    <button
      type="button"
      aria-current={isSelected}
      onClick={() => presenter.selectPerson(row.id)}
      className={cn(
        'block w-full border-b px-4 py-2.5 outline-none last:border-b-0 hover:bg-accent/50 focus-visible:ring-[3px] focus-visible:ring-inset focus-visible:ring-ring/50',
        isSelected && 'bg-muted',
      )}
    >
      <PersonSummary row={row} name={nameOrFormer(row.name)} />
    </button>
  )
}

/**
 * The desktop's pane: the person picked, every group they are in, what they owe or are owed with the actions, each
 * group's part as a table, and why one amount settles every group.
 */
function PersonDetail({ presenter, row }: { presenter: GlobalBalancesPresenter; row: JsPersonRow }) {
  const name = nameOrFormer(row.name)
  const groups = row.groups.map((group) => group.name).join(', ')
  const signed = (minor: number) => formatSignedMoney(minor, row.currency, row.fractionDigits)
  const amount = formatMoney(Math.abs(row.netMinor), row.currency, row.fractionDigits)
  const note = !row.isNetOfSeveralGroups
    ? t('globalBalances_detailNoteSingle', { name })
    : t(row.owesYou ? 'globalBalances_detailNoteOwed' : 'globalBalances_detailNoteSettle', { amount })
  return (
    <section aria-label={t('globalBalances_detailPane', { name })} className="flex min-w-0 flex-col gap-5 rounded-xl border bg-card p-6">
      <header className="flex items-center gap-3">
        <PersonFace row={row} className="size-12 text-base" />
        <span className="flex min-w-0 flex-1 flex-col">
          <h2 className="truncate text-xl font-semibold">{name}</h2>
          <span className="text-[13px] text-muted-foreground">{row.isGuest ? t('globalBalances_detailGuest', { groups }) : groups}</span>
        </span>
        <span className="flex items-center gap-2">
          <PersonActions presenter={presenter} row={row} name={name} size="default" />
        </span>
      </header>
      <div className="flex flex-col gap-1">
        <span className="text-[13px] text-muted-foreground">{t(row.owesYou ? 'globalBalances_detailOwesYou' : 'globalBalances_detailYouOwe', { name })}</span>
        <span className={cn('text-3xl font-semibold tabular-nums tracking-tight', balanceTone(row.netMinor))}>{signed(row.netMinor)}</span>
      </div>
      <table className="w-full overflow-hidden rounded-lg border text-sm">
        <caption className="sr-only">{`${name} · ${t('globalBalances_byGroup')}`}</caption>
        <thead>
          <tr className="h-10 border-b text-left text-[13px] text-muted-foreground">
            <th scope="col" className="px-4 font-medium">{t('globalBalances_detailGroup')}</th>
            <th scope="col" className="w-36 px-4 font-medium">{t('globalBalances_detailStatus')}</th>
            <th scope="col" className="w-32 px-4 text-right font-medium">{t('globalBalances_detailAmount')}</th>
          </tr>
        </thead>
        <tbody>
          {row.groups.map((group) => (
            <tr key={group.groupId} className="h-12 border-b">
              <td className="px-4">
                <span className="flex min-w-0 items-center gap-2.5">
                  <GroupAvatar initials={group.initials} color={group.color} className="size-6 shrink-0 rounded-md text-[10px]" />
                  <span className="truncate">{group.name}</span>
                </span>
              </td>
              <td className="px-4 text-muted-foreground">{t(group.amountMinor < 0 ? 'globalBalances_rowYouOwe' : 'globalBalances_rowOwesYou')}</td>
              <td className={cn('px-4 text-right tabular-nums', balanceTone(group.amountMinor))}>{signed(group.amountMinor)}</td>
            </tr>
          ))}
          <tr className="h-11 bg-muted font-semibold">
            <th scope="row" colSpan={2} className="px-4 text-left">
              {t(row.isNetOfSeveralGroups ? 'globalBalances_net' : 'globalBalances_total')}
            </th>
            <td className={cn('px-4 text-right tabular-nums', balanceTone(row.netMinor))}>{signed(row.netMinor)}</td>
          </tr>
        </tbody>
      </table>
      <p className="text-[13px] leading-relaxed text-muted-foreground">{note}</p>
    </section>
  )
}

/** "Tudo acertado" leads to Grupos, "Você não deve nada" to Te devem, and nobody owing you to Você deve. */
function EmptyBalances({ presenter, emptyState, total, count }: { presenter: GlobalBalancesPresenter; emptyState: string; total: JsBalanceTotal | undefined; count: number }) {
  const words = emptyWords[emptyState] ?? emptyWords.settled
  const isPay = emptyState === 'nothing-to-pay'
  const amount = total ? formatMoney(isPay ? total.toReceiveMinor : total.toPayMinor, total.currency, total.fractionDigits) : ''
  const body = words.bodyOne && count === 1 ? t(words.bodyOne, { amount }) : t(words.body, { amount, count })
  return (
    <section className="flex flex-1 flex-col items-center justify-center gap-3 rounded-xl border border-dashed p-8 text-center">
      <span aria-hidden className="flex size-12 items-center justify-center rounded-xl bg-muted text-muted-foreground">
        <CircleCheck className="size-6" />
      </span>
      <h2 className="mt-1 text-lg font-semibold">{t(words.title)}</h2>
      <p className="max-w-sm text-sm text-muted-foreground">{body}</p>
      <Button variant="outline" onClick={() => presenter.emptyAction()}>
        {t(words.action)}
      </Button>
    </section>
  )
}
