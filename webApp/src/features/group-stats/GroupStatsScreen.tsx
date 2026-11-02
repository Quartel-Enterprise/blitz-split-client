import { ArrowLeft, BarChart3, CalendarDays, Check, ChevronDown, ChevronRight, LineChart, ReceiptText } from 'lucide-react'
import { GroupStatsPresenter, type JsGroupStatsState, type JsRoute, type JsStatsPeriodOption } from 'blitzsplit-shared'
import { useState, type ReactNode } from 'react'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { controlClass } from '@/components/fields'
import { nameOrFormer } from '@/features/balances/BalanceCard'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { formatDayTime, fromDateTimeInputs, toDateInputValue } from '@/lib/dates'
import { formatMoney, formatPercent } from '@/lib/money'
import { cn } from '@/lib/utils'
import { CategoriesCard } from './CategoriesCard'
import { StatsChart } from './StatsChart'
import {
  formatRange,
  grainNoteText,
  paidByText,
  peakLabel,
  periodEmptyTitle,
  periodHint,
  periodLabel,
} from './statsFormat'

const grainLabels: Record<string, MessageKey> = { day: 'stats_grainDay', week: 'stats_grainWeek', month: 'stats_grainMonth', year: 'stats_grainYear' }

const cardClass = 'flex min-w-0 flex-col gap-3.5 rounded-xl border bg-card p-4 md:p-5'

/**
 * "Estatísticas do grupo" (6.6): the group's spending in a period from the bills on this browser, offline too. Four
 * summary cards, then "Ao longo do tempo" and "Maior despesa" beside "Por pessoa" from the tablet up, one column on
 * the phone. The period is a menu under its field, a drawer on the phone; "Escolher datas…" asks for the days.
 */
export function GroupStatsScreen({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new GroupStatsPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsGroupStatsState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const [isPickingDates, setPickingDates] = useState(false)
  const [isPeriodDrawerOpen, setPeriodDrawerOpen] = useState(false)
  if (!presenter || !state?.isLoaded) return null

  const money = (minor: number) => formatMoney(minor, state.currency, state.fractionDigits)
  const range = formatRange(state.periodRangeStyle, state.periodStartMillis, state.periodEndMillis)
  const pickPeriod = (option: JsStatsPeriodOption) => {
    setPeriodDrawerOpen(false)
    if (option.period === 'custom') setPickingDates(true)
    else presenter.pickPeriod(option.period)
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="sticky top-0 z-10 flex h-14 items-center gap-1 border-b bg-background/95 px-2 backdrop-blur sm:px-4">
        <Button variant="ghost" size="icon" aria-label={t('stats_back', { group: state.groupName })} onClick={() => presenter.back()}>
          <ArrowLeft />
        </Button>
        <span className="flex min-w-0 flex-1 flex-col md:hidden">
          <span className="truncate font-semibold">{t('stats_title')}</span>
          <span className="truncate text-xs text-muted-foreground">{state.groupName}</span>
        </span>
        <nav aria-label={t('groupDetail_breadcrumb')} className="mr-auto hidden items-center gap-1.5 text-sm md:flex">
          <span className="text-muted-foreground">{t('groups_title')}</span>
          <ChevronRight aria-hidden className="size-3.5 text-muted-foreground" />
          <button
            type="button"
            onClick={() => presenter.back()}
            className="truncate rounded-sm text-muted-foreground outline-none hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring"
          >
            {state.groupName}
          </button>
          <ChevronRight aria-hidden className="size-3.5 text-muted-foreground" />
          <span aria-current="page" className="font-medium">
            {t('stats_title')}
          </span>
        </nav>
      </header>

      <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-4 p-4 sm:gap-5 md:px-8 md:py-7 lg:px-10 lg:py-8">
        <div className="flex items-stretch gap-4 md:items-end">
          <div className="hidden min-w-0 flex-1 flex-col gap-1 md:flex">
            <h1 className="text-2xl leading-[30px] font-semibold tracking-tight">{t('stats_title')}</h1>
            <span className="text-sm text-muted-foreground">{state.showsPeriodPicker ? `${state.groupName} · ${range}` : state.groupName}</span>
          </div>
          {state.showsPeriodPicker &&
            (isDesktop ? (
              <PeriodMenu state={state} onPick={pickPeriod} />
            ) : (
              <button type="button" aria-haspopup="dialog" onClick={() => setPeriodDrawerOpen(true)} className={cn(controlClass, 'justify-between')}>
                <PeriodTriggerLabel state={state} />
              </button>
            ))}
        </div>

        {state.showsStats && <SummaryCards state={state} money={money} />}

        {state.showsStats && (
          <div className="grid items-start gap-4 md:grid-cols-2 md:gap-5 xl:grid-cols-3">
            <CategoriesCard state={state} money={money} className={cardClass} />
            <div className="flex min-w-0 flex-col gap-4 md:gap-5">
              <OverTimeCard state={state} presenter={presenter} money={money} />
              {state.biggestExpense && (
                <section aria-labelledby="statsBiggest" className={cardClass}>
                  <h2 id="statsBiggest" className="text-[15px] font-semibold">
                    {t('stats_biggest')}
                  </h2>
                  <button
                    type="button"
                    onClick={() => presenter.openBiggestExpense()}
                    className="-m-2 flex items-center gap-3 rounded-lg p-2 text-left outline-none hover:bg-accent/60 focus-visible:ring-[3px] focus-visible:ring-ring"
                  >
                    <span aria-hidden className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-muted text-primary">
                      <ReceiptText className="size-4" />
                    </span>
                    <span className="flex min-w-0 flex-1 flex-col">
                      <span className="truncate font-medium">{state.biggestExpense.description}</span>
                      <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
                        {state.biggestExpense.isPending && <span aria-hidden className="size-1.5 rounded-full bg-orange-500" />}
                        {paidByText(formatDayTime(state.biggestExpense.spentAt, 'other'), state.biggestExpense.payers)}
                      </span>
                    </span>
                    <span className="font-semibold tabular-nums">{money(state.biggestExpense.amountMinor)}</span>
                  </button>
                </section>
              )}
            </div>
            <PeopleCard state={state} presenter={presenter} money={money} />
          </div>
        )}

        {state.isGroupEmpty && (
          <EmptyCard title={t('stats_emptyTitle')} body={t('stats_emptyBody')}>
            <Button onClick={() => presenter.splitBill()}>{t('groupDetail_splitBill')}</Button>
          </EmptyCard>
        )}
        {state.isPeriodEmpty && state.allStartMillis != null && state.allEndMillis != null && (
          <EmptyCard
            title={periodEmptyTitle(state)}
            body={t('stats_periodEmptyBody', { range: formatRange(state.allRangeStyle, state.allStartMillis, state.allEndMillis) })}
          >
            <Button variant="outline" onClick={() => presenter.showWholePeriod()}>
              {t(state.isTrip ? 'stats_seeWholeTrip' : 'stats_seeWholePeriod')}
            </Button>
          </EmptyCard>
        )}
      </main>

      {!isDesktop && (
        <Drawer open={isPeriodDrawerOpen} onOpenChange={setPeriodDrawerOpen}>
          <DrawerContent>
            <DrawerHeader>
              <DrawerTitle>{t('stats_period')}</DrawerTitle>
              <DrawerDescription>{t('stats_periodDrawerBody')}</DrawerDescription>
            </DrawerHeader>
            <div role="radiogroup" aria-label={t('stats_period')} className="mx-4 flex flex-col overflow-hidden rounded-xl border">
              {state.periodOptions.map((option) => (
                <button
                  key={option.period}
                  type="button"
                  role="radio"
                  aria-checked={option.period === state.period}
                  onClick={() => pickPeriod(option)}
                  className="flex min-h-14 items-center justify-between gap-3 border-b px-4 py-2 text-left last:border-b-0"
                >
                  <PeriodOptionText option={option} isTrip={state.isTrip} />
                  {option.period === state.period && <Check aria-hidden className="size-[18px] text-primary" />}
                </button>
              ))}
            </div>
            <DrawerFooter>
              <Button variant="outline" onClick={() => setPeriodDrawerOpen(false)}>
                {t('common_close')}
              </Button>
            </DrawerFooter>
          </DrawerContent>
        </Drawer>
      )}
      {isPickingDates && (
        <PickDatesDialog
          isDesktop={isDesktop}
          initialStart={state.periodStartMillis}
          initialEnd={state.periodEndMillis}
          onPick={(start, end) => {
            setPickingDates(false)
            presenter.pickDates(start, end)
          }}
          onClose={() => setPickingDates(false)}
        />
      )}
    </div>
  )
}

function PeriodTriggerLabel({ state }: { state: JsGroupStatsState }) {
  return (
    <>
      <span className="flex items-center gap-2">
        <CalendarDays aria-hidden className="size-[15px] text-muted-foreground" />
        {periodLabel(state.period, state.isTrip)}
      </span>
      <ChevronDown aria-hidden className="size-3.5 text-muted-foreground" />
    </>
  )
}

function PeriodOptionText({ option, isTrip }: { option: JsStatsPeriodOption; isTrip: boolean }) {
  return (
    <span className="flex flex-col">
      <span className="font-medium">{periodLabel(option.period, isTrip)}</span>
      <span className="text-xs text-muted-foreground">{periodHint(option)}</span>
    </span>
  )
}

/** The period's field and the list under it, "Escolher datas…" after a line. */
function PeriodMenu({ state, onPick }: { state: JsGroupStatsState; onPick: (option: JsStatsPeriodOption) => void }) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button type="button" aria-label={t('stats_period')} className={cn(controlClass, 'w-[220px] justify-between')}>
          <PeriodTriggerLabel state={state} />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-[260px]">
        {state.periodOptions.map((option) => (
          <div key={option.period}>
            {option.period === 'custom' && <DropdownMenuSeparator />}
            <DropdownMenuItem onSelect={() => onPick(option)} className={cn('justify-between', option.period === state.period && 'bg-accent')}>
              <PeriodOptionText option={option} isTrip={state.isTrip} />
              {option.period === state.period && <Check aria-hidden className="size-4" />}
            </DropdownMenuItem>
          </div>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

function SummaryCards({ state, money }: { state: JsGroupStatsState; money: (minor: number) => string }) {
  const people = t(state.memberCount === 1 ? 'stats_peopleOne' : 'stats_peopleOther', { count: state.memberCount })
  const cards: { label: string; value: string; hint: string; isPending?: boolean }[] = [
    { label: t('stats_totalSpent'), value: money(state.totalMinor), hint: periodLabel(state.period, state.isTrip) },
    {
      label: t('stats_expenses'),
      value: String(state.expenseCount),
      hint:
        state.pendingCount > 0
          ? t(state.pendingCount === 1 ? 'stats_queuedOne' : 'stats_queuedOther', { count: state.pendingCount })
          : t('stats_settlementsOut'),
      isPending: state.pendingCount > 0,
    },
    { label: t('stats_average'), value: money(state.averagePerPersonMinor), hint: people },
  ]
  if (state.myShareMinor != null) {
    cards.push({
      label: t('stats_myShare'),
      value: money(state.myShareMinor),
      hint: t('stats_myShareHint', { percent: formatPercent(state.mySharePercent), paid: money(state.myPaidMinor ?? 0) }),
    })
  }
  return (
    <div className="grid grid-cols-2 gap-2.5 md:gap-4 lg:grid-cols-4">
      {cards.map((card) => (
        <section key={card.label} className="flex min-w-0 flex-col gap-1 rounded-xl border bg-card px-3.5 py-3 md:px-5 md:py-4">
          <span className="text-[13px] font-medium text-muted-foreground">{card.label}</span>
          <strong className="text-xl leading-tight font-bold tracking-tight whitespace-nowrap tabular-nums md:text-2xl">{card.value}</strong>
          <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
            {card.isPending && <span aria-hidden className="size-1.5 shrink-0 rounded-full bg-orange-500" />}
            <span className="truncate">{card.hint}</span>
          </span>
        </section>
      ))}
    </div>
  )
}

/** "Por pessoa": Consumo or Pagou, a bar per person scaled by the largest, and what each one means. */
function PeopleCard({ state, presenter, money }: { state: JsGroupStatsState; presenter: GroupStatsPresenter; money: (minor: number) => string }) {
  const isPaid = state.peopleView === 'paid'
  return (
    <section aria-labelledby="statsPeople" className={cn(cardClass, 'order-first md:order-none')}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 id="statsPeople" className="text-[15px] font-semibold">
          {t('stats_byPerson')}
        </h2>
        <div role="tablist" aria-label={t('stats_showByPerson')} className="inline-flex h-8 rounded-lg bg-muted p-[3px]">
          {[
            { label: t('stats_consumption'), selected: !isPaid, onClick: () => presenter.showConsumption() },
            { label: t('stats_paid'), selected: isPaid, onClick: () => presenter.showPaid() },
          ].map((tab) => (
            <button
              key={tab.label}
              type="button"
              role="tab"
              aria-selected={tab.selected}
              onClick={tab.onClick}
              className={cn(
                'rounded-md px-3 text-[13px] font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
                tab.selected ? 'bg-background text-foreground shadow-sm dark:bg-input/30' : 'text-muted-foreground hover:text-foreground',
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>
      <div className="flex flex-col gap-2.5">
        {state.people.map((person) => {
          const name = person.isMe ? t('stats_you', { name: nameOrFormer(person.name) }) : nameOrFormer(person.name)
          return (
            <div key={person.memberId} className="flex items-center gap-2.5">
              <Avatar initial={person.initials} color={person.color} className="size-7 text-[11px]" />
              <span className="w-[108px] shrink-0 truncate font-medium">{name}</span>
              <span role="img" aria-label={`${name}: ${money(person.amountMinor)}`} className="flex h-2 min-w-0 flex-1 rounded-full bg-muted">
                <span className="rounded-full bg-primary" style={{ width: `${person.barFraction * 100}%` }} />
              </span>
              <span className={cn('w-[92px] shrink-0 text-right font-semibold tabular-nums', person.amountMinor === 0 && 'text-muted-foreground')}>
                {money(person.amountMinor)}
              </span>
            </div>
          )
        })}
      </div>
      <p className="text-xs leading-normal text-muted-foreground">{t(isPaid ? 'stats_notePaid' : 'stats_noteConsumption')}</p>
    </section>
  )
}

/** "Ao longo do tempo": Barras or Linha, the grain, the chart, the most expensive one and why some grains are off. */
function OverTimeCard({ state, presenter, money }: { state: JsGroupStatsState; presenter: GroupStatsPresenter; money: (minor: number) => string }) {
  const peak = state.bars.find((bar) => bar.isPeak)
  const note = grainNoteText(state)
  return (
    <section aria-labelledby="statsTime" className={cardClass}>
      <div className="flex items-center justify-between gap-2">
        <h2 id="statsTime" className="text-[15px] font-semibold">
          {t('stats_overTime')}
        </h2>
        <div role="group" aria-label={t('stats_chartType')} className="inline-flex">
          {[
            { code: 'bars', label: t('stats_bars'), icon: <BarChart3 /> },
            { code: 'line', label: t('stats_line'), icon: <LineChart /> },
          ].map((type, index) => (
            <button
              key={type.code}
              type="button"
              aria-pressed={state.chartType === type.code}
              aria-label={type.label}
              title={type.label}
              onClick={() => presenter.pickChartType(type.code)}
              className={cn(
                'flex size-8 items-center justify-center border outline-none focus-visible:ring-[3px] focus-visible:ring-ring [&_svg]:size-4',
                index === 0 ? 'rounded-l-md' : '-ml-px rounded-r-md',
                state.chartType === type.code ? 'bg-accent text-foreground' : 'bg-card text-muted-foreground',
              )}
            >
              {type.icon}
            </button>
          ))}
        </div>
      </div>
      <div role="radiogroup" aria-label={t('stats_groupBy')} className="grid h-8 grid-cols-4 rounded-lg bg-muted p-[3px]">
        {state.grains.map((option) => (
          <button
            key={option.grain}
            type="button"
            role="radio"
            aria-checked={state.grain === option.grain}
            aria-disabled={!option.isEnabled}
            onClick={() => {
              if (option.isEnabled) presenter.pickGrain(option.grain)
            }}
            className={cn(
              'min-w-0 rounded-md text-[13px] font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
              state.grain === option.grain
                ? 'bg-background text-foreground shadow-sm dark:bg-input/30'
                : option.isEnabled
                  ? 'text-muted-foreground hover:text-foreground'
                  : 'cursor-default text-muted-foreground opacity-40',
            )}
          >
            {t(grainLabels[option.grain] ?? 'stats_grainDay')}
          </button>
        ))}
      </div>
      <StatsChart state={state} />
      <div className="flex flex-col gap-1">
        {peak && (
          <span className="text-[13px] text-muted-foreground">
            {peakLabel(peak, state.grain)} <strong className="font-semibold text-foreground tabular-nums">{money(peak.amountMinor)}</strong>
          </span>
        )}
        {note && <span className="text-xs leading-normal text-muted-foreground">{note}</span>}
      </div>
    </section>
  )
}

function EmptyCard({ title, body, children }: { title: string; body: string; children: ReactNode }) {
  return (
    <section className="flex flex-col items-center gap-2 rounded-xl border border-dashed px-6 py-10 text-center">
      <span aria-hidden className="flex size-11 items-center justify-center rounded-full bg-muted text-muted-foreground">
        <BarChart3 className="size-5" />
      </span>
      <h2 className="text-base font-semibold">{title}</h2>
      <p className="max-w-[420px] text-sm leading-normal text-muted-foreground">{body}</p>
      <div className="mt-2">{children}</div>
    </section>
  )
}

/** "Escolher datas": the first and the last day, never after today; a dialog, a drawer on the phone. */
function PickDatesDialog({
  isDesktop,
  initialStart,
  initialEnd,
  onPick,
  onClose,
}: {
  isDesktop: boolean
  initialStart: number
  initialEnd: number
  onPick: (startMillis: number, endMillis: number) => void
  onClose: () => void
}) {
  const [start, setStart] = useState(toDateInputValue(initialStart))
  const [end, setEnd] = useState(toDateInputValue(initialEnd))
  const today = toDateInputValue(Date.now())
  // Why: noon of the day typed, so the day stays the same in the browser's time zone.
  const startMillis = fromDateTimeInputs(start, '12:00')
  const endMillis = fromDateTimeInputs(end, '12:00')
  const canApply = startMillis != null && endMillis != null
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
      className="sm:max-w-[400px]"
      pane={{
        title: t('stats_pickDatesTitle'),
        description: t('stats_pickDatesBody'),
        body: (
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-2">
              <Label htmlFor="statsFrom">{t('stats_from')}</Label>
              <Input id="statsFrom" type="date" max={today} value={start} onChange={(event) => setStart(event.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="statsTo">{t('stats_to')}</Label>
              <Input id="statsTo" type="date" max={today} value={end} onChange={(event) => setEnd(event.target.value)} />
            </div>
          </div>
        ),
        footer: (
          <>
            <Button variant="outline" onClick={onClose}>
              {t('common_cancel')}
            </Button>
            <Button disabled={!canApply} onClick={() => canApply && onPick(startMillis, endMillis)}>
              {t('stats_apply')}
            </Button>
          </>
        ),
      }}
    />
  )
}
