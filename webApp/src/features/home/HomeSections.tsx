import { ChevronRight, Pin } from 'lucide-react'
import { getHomePeopleLayout, type HomePresenter, type JsBillMenu, type JsHomeBill, type JsHomeGroup, type JsHomePerson, type JsHomeState, type JsHomeTotal } from 'blitzsplit-shared'
import { useEffect, useId, useLayoutEffect, useRef, useState, type FocusEvent, type MouseEvent, type PointerEvent, type ReactNode, type RefObject } from 'react'
import { homeBillMenuOptions, RowMenuButton } from '@/components/BillRowMenu'
import { Avatar, PendingDot } from '@/components/common'
import { SummaryCardSkeleton } from '@/components/PageSkeletons'
import { LoadingRegion, Skeleton, SkeletonCircle, SkeletonLine, SkeletonRows } from '@/components/ui/skeleton'
import { balanceTone, nameOrFormer } from '@/features/balances/BalanceCard'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { FaceStack, GroupBalance, lastActivityText, membersText } from '@/features/groups/GroupRowParts'
import { paidText as payersText } from '@/features/groups/SearchDialog'
import { t, type MessageKey } from '@/i18n'
import { formatDayLabel } from '@/lib/dates'
import { formatMoney, formatSignedMoney } from '@/lib/money'
import { CategoryBadge } from '@/lib/categories'
import { cn } from '@/lib/utils'

interface SectionProps {
  presenter: HomePresenter
  state: JsHomeState
}

const summaryHints: Record<string, MessageKey> = {
  TO_RECEIVE: 'home_summaryToReceiveHint',
  YOU_OWE: 'home_summaryToPayHint',
}

/**
 * "Saldo geral" (the net, one line per extra currency), then "Te devem" and "Você deve", which open Saldos on that
 * filter ("Você deve" straight to settling up while only one person is in it), disabled as "Nada a receber" and
 * "Nada a pagar" at zero. Desktop: three cards with the net wider; tablet three equal; phone the net over the two.
 */
export function HomeSummaryCards({ presenter, state }: SectionProps) {
  const summary = state.summary
  if (!summary) return <HomeSummarySkeleton />
  const main = summary.main
  const net = main ? formatSignedMoney(main.netMinor, main.currency, main.fractionDigits) : null
  const otherNets = Array.from(summary.others, (total) => formatSignedMoney(total.netMinor, total.currency, total.fractionDigits))
  const isSettled = summary.kind === 'YOU_ARE_SETTLED'
  // Why: at zero the disabled card still shows the big amount's currency.
  const toReceive = summary.toReceive ?? main ?? undefined
  const toPay = summary.toPay ?? main ?? undefined
  const onlyPayee = state.onlyPersonToPay?.name
  const people = (count: number) => t(count === 1 ? 'home_peopleOne' : 'home_peopleOther', { count })
  return (
    <div className="grid grid-cols-2 gap-2 md:grid-cols-3 md:gap-4 lg:grid-cols-[1.4fr_1fr_1fr]">
      <section aria-labelledby="home-summary-title" className="col-span-2 flex min-w-0 flex-col gap-1 rounded-lg border bg-card px-4 py-3.5 md:col-span-1 md:px-5 md:py-4">
        <h2 id="home-summary-title" className="text-[13px] font-medium text-muted-foreground">
          {t('groups_summaryTitle')}
        </h2>
        {net ? (
          <span className={cn('truncate text-[30px] leading-tight font-bold tracking-tight tabular-nums lg:text-4xl', isSettled ? 'text-muted-foreground' : balanceTone(main?.netMinor ?? 0))}>
            {net}
          </span>
        ) : (
          <Skeleton className="my-1 h-8 w-40" />
        )}
        {otherNets.map((amount) => (
          <span key={amount} className="truncate text-lg font-semibold tabular-nums text-muted-foreground">
            {amount}
          </span>
        ))}
        <span className="text-xs text-muted-foreground">
          {t(state.firstDownload ? 'home_summaryFirstDownloadHint' : isSettled ? 'home_summarySettledHint' : (summaryHints[summary.kind] ?? 'home_summarySettledHint'))}
        </span>
      </section>
      <SplitCard
        label={t(summary.canOpenToReceive ? 'home_toReceive' : 'home_nothingToReceive')}
        total={toReceive}
        amountMinor={toReceive?.toReceiveMinor ?? 0}
        caption={summary.canOpenToReceive ? people(toReceive?.toReceiveCount ?? 0) : t('home_nobodyOwesYou')}
        action={t('groups_summaryToReceiveAction')}
        tone="text-positive"
        isEnabled={summary.canOpenToReceive}
        onClick={() => presenter.openToReceive()}
      />
      <SplitCard
        label={t(summary.canOpenToPay ? 'home_toPay' : 'home_nothingToPay')}
        total={toPay}
        amountMinor={toPay?.toPayMinor ?? 0}
        caption={summary.canOpenToPay ? (onlyPayee ? t('home_onlyTo', { name: onlyPayee }) : people(toPay?.toPayCount ?? 0)) : t('home_youOweNobody')}
        action={onlyPayee ? t('home_settleUpWith', { name: onlyPayee }) : t('groups_summaryToPayAction')}
        tone="text-destructive"
        isEnabled={summary.canOpenToPay}
        onClick={() => presenter.openToPay()}
      />
    </div>
  )
}

/** The three summary cards while the balances are still read, in the cards' own grid. */
function HomeSummarySkeleton() {
  return (
    <LoadingRegion className="grid grid-cols-2 gap-2 md:grid-cols-3 md:gap-4 lg:grid-cols-[1.4fr_1fr_1fr]">
      <SummaryCardSkeleton isLarge className="col-span-2 rounded-lg md:col-span-1" />
      <SummaryCardSkeleton className="rounded-lg" />
      <SummaryCardSkeleton className="rounded-lg" />
    </LoadingRegion>
  )
}

interface SplitCardProps {
  label: string
  total: JsHomeTotal | undefined
  amountMinor: number
  caption: string
  action: string
  tone: string
  isEnabled: boolean
  onClick: () => void
}

function SplitCard({ label, total, amountMinor, caption, action, tone, isEnabled, onClick }: SplitCardProps) {
  const amount = total ? formatMoney(amountMinor, total.currency, total.fractionDigits) : null
  return (
    <button
      onClick={onClick}
      disabled={!isEnabled}
      aria-label={isEnabled && amount ? `${label} ${amount}. ${action}` : label}
      className="flex min-w-0 flex-col gap-1 rounded-lg border bg-card px-3 py-2.5 text-left outline-none hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring disabled:pointer-events-none md:px-4 md:py-3.5"
    >
      <span className="flex items-center justify-between gap-2 text-[13px] font-medium text-muted-foreground">
        {label}
        <ChevronRight aria-hidden className={cn('size-3.5', !isEnabled && 'opacity-40')} />
      </span>
      {amount ? (
        <span className={cn('truncate text-base font-semibold tabular-nums md:text-[22px]', isEnabled ? tone : 'text-muted-foreground')}>{amount}</span>
      ) : (
        <Skeleton className="h-6 w-24" />
      )}
      <span className="truncate text-xs text-muted-foreground">{caption}</span>
    </button>
  )
}

/** A card's title with its link on the right ("Ver saldos", "Ver tudo", "Ver todos"). */
function SectionHeader({ id, title, action, onAction, className }: { id: string; title: string; action: string; onAction: () => void; className?: string }) {
  return (
    <div className={cn('flex items-center justify-between gap-2', className)}>
      <h2 id={id} className="text-[15px] font-semibold">
        {title}
      </h2>
      <button
        onClick={onAction}
        className="flex h-8 items-center rounded-md px-2.5 text-[13px] font-medium text-primary outline-none hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring"
      >
        {action}
      </button>
    </div>
  )
}

/** The tip of a guest's face, where it showed and whether a touch's long press opened it. */
interface GuestTip {
  key: string
  name: string
  left: number
  isTouch: boolean
}

/**
 * "Saldos com pessoas": each person you have an open balance with, whom you owe first. The shared HomePeopleLayout picks
 * by the card's width and the count, as Android and Apple do (beta feedback): rows (face, name, amount and "te deve" on
 * one line) while they take two lines at most, in as many columns as fit; else a face each in a row that scrolls
 * sideways, " · 9" once it does. A face opens settling up with them for the net. A guest (a name nobody joined as)
 * wears the crossed-out phone, and hovering it, focusing it or a long press shows why they get no notice. Gone,
 * leaving no gap, once everyone is settled; faces of gray while the first download hasn't brought anyone yet.
 */
export function PeopleCard({ presenter, state }: SectionProps) {
  const cardRef = useRef<HTMLElement>(null)
  const rowRef = useRef<HTMLDivElement>(null)
  const tipId = useId()
  const [tip, setTip] = useState<GuestTip | null>(null)
  const isScrolling = useOverflows(rowRef, state.people?.length ?? 0)
  // Why: the card mounts once there is someone to show, so the width is followed from then.
  const width = useContentWidth(cardRef, state.showsPeople || state.isLoadingPeople)
  // Why: a long press shows the tip until the next touch anywhere, which a touch screen has instead of hover's end.
  useEffect(() => {
    if (!tip?.isTouch) return
    const hide = () => setTip(null)
    const timer = window.setTimeout(() => document.addEventListener('pointerdown', hide, { once: true }), 0)
    return () => {
      window.clearTimeout(timer)
      document.removeEventListener('pointerdown', hide)
    }
  }, [tip])
  // Why: while the balances are read nothing says yet whether anybody owes anybody, so the faces shimmer.
  if (!state.showsPeople && !state.isLoadingPeople) return null

  const people = Array.from(state.people ?? [])
  const layout = state.isLoadingPeople || people.length === 0 ? null : getHomePeopleLayout(people.length, width)
  const showsRows = layout?.showsRows === true
  const showTip = (person: JsHomePerson, face: HTMLElement, isTouch: boolean) => {
    const card = cardRef.current
    if (!card) return
    const center = face.getBoundingClientRect().left - card.getBoundingClientRect().left + face.offsetWidth / 2
    setTip({ key: person.key, name: nameOrFormer(person.name), left: Math.max(8, Math.min(center - 40, card.clientWidth - 288)), isTouch })
  }
  return (
    <section ref={cardRef} aria-labelledby="home-people-title" className="relative flex flex-col gap-1.5 rounded-lg border bg-card p-3 md:px-4 md:py-3.5">
      <SectionHeader
        id="home-people-title"
        title={isScrolling && !showsRows ? t('home_peopleCount', { count: people.length }) : t('home_people')}
        action={t('home_seeBalances')}
        onAction={() => presenter.seeBalances()}
      />
      <div
        ref={rowRef}
        style={showsRows ? { gridTemplateColumns: `repeat(${layout?.columns ?? 1}, minmax(0, 1fr))` } : undefined}
        className={showsRows ? '-mx-1 grid gap-1 px-1' : '-mx-1 flex gap-1.5 overflow-x-auto px-1 pb-1 md:gap-2'}
        onScroll={() => tip && setTip(null)}
      >
        {state.isLoadingPeople ? (
          <FacesSkeleton />
        ) : (
          people.map((person) => (
            <PersonFace
              key={`${person.key}/${person.currency}`}
              person={person}
              isRow={showsRows}
              tipId={tip?.key === person.key ? tipId : undefined}
              onOpen={() => presenter.openPerson(person.key, person.currency)}
              onTip={(face, isTouch) => (face ? showTip(person, face, isTouch) : setTip(null))}
            />
          ))
        )}
      </div>
      {tip && (
        <div
          id={tipId}
          role="tooltip"
          style={{ left: tip.left }}
          className="absolute top-full z-10 -mt-2 flex w-[280px] gap-2 rounded-lg border bg-popover px-3 py-2.5 text-[13px] leading-[1.45] text-popover-foreground shadow-lg"
        >
          <NoAppIcon className="mt-0.5 size-4 shrink-0 text-primary" />
          <span>
            <strong className="font-semibold">{t('home_guestTipTitle', { name: tip.name })}</strong>
            <br />
            <span className="text-muted-foreground">{t(tip.isTouch ? 'home_guestTipTap' : 'home_guestTipClick')}</span>
          </span>
        </div>
      )}
    </section>
  )
}

/** Five faces' places, each with its name and amount, while the people are read or the first download brings them. */
function FacesSkeleton() {
  return (
    <LoadingRegion className="flex gap-1.5 md:gap-2">
      {[0, 1, 2, 3, 4].map((index) => (
        <span key={index} className="flex w-[72px] shrink-0 flex-col items-center gap-2 py-1.5 md:w-[88px]">
          <SkeletonCircle className="size-12 md:size-14" />
          <SkeletonLine className="w-12" />
          <SkeletonLine className="w-14" />
        </span>
      ))}
    </LoadingRegion>
  )
}

/** The crossed-out phone of a guest, someone nobody joined as, who has no app to get a notice on. */
function NoAppIcon({ className, strokeWidth = 2 }: { className?: string; strokeWidth?: number }) {
  return (
    <svg aria-hidden viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
      <rect x="7" y="2" width="10" height="20" rx="2" />
      <path d="M3 3l18 18" />
    </svg>
  )
}

const longPressMillis = 500

/**
 * One person: avatar (with the guest's badge), name, the net with its sign and its word; stacked as a face of the row,
 * or on one line as a row (`isRow`).
 */
function PersonFace({
  person,
  isRow,
  tipId,
  onOpen,
  onTip,
}: {
  person: JsHomePerson
  isRow: boolean
  tipId: string | undefined
  onOpen: () => void
  onTip: (face: HTMLElement | null, isTouch: boolean) => void
}) {
  const longPress = useRef<{ timer: number; didShow: boolean } | null>(null)
  const name = nameOrFormer(person.name)
  const amount = formatMoney(Math.abs(person.netMinor), person.currency, person.fractionDigits)
  const youOwe = person.netMinor < 0
  const label = [
    t(youOwe ? 'home_youOwePerson' : 'home_personOwesYou', { name, amount }),
    person.isGuest ? t('home_guestSuffix') : '',
    '. ',
    t(youOwe ? 'balance_settle' : 'home_recordPayment'),
  ].join('')
  const cancelLongPress = () => {
    if (longPress.current) window.clearTimeout(longPress.current.timer)
  }
  // Why: only a guest's face has a tip; the others just open on a click or a tap.
  const guestHandlers = person.isGuest
    ? {
        onPointerEnter: (event: PointerEvent<HTMLButtonElement>) => event.pointerType === 'mouse' && onTip(event.currentTarget, false),
        onPointerLeave: (event: PointerEvent<HTMLButtonElement>) => event.pointerType === 'mouse' && onTip(null, false),
        onFocus: (event: FocusEvent<HTMLButtonElement>) => event.currentTarget.matches(':focus-visible') && onTip(event.currentTarget, false),
        onBlur: () => onTip(null, false),
        onPointerDown: (event: PointerEvent<HTMLButtonElement>) => {
          if (event.pointerType === 'mouse') return
          const face = event.currentTarget
          const press = { timer: 0, didShow: false }
          press.timer = window.setTimeout(() => {
            press.didShow = true
            onTip(face, true)
          }, longPressMillis)
          longPress.current = press
        },
        onPointerUp: cancelLongPress,
        onPointerCancel: cancelLongPress,
        // Why: a long press on a touch screen also opens the browser's menu, which would cover the tip.
        onContextMenu: (event: MouseEvent) => event.preventDefault(),
      }
    : {}
  return (
    <button
      aria-label={label}
      aria-describedby={tipId}
      onClick={() => {
        // Why: the tap ending a long press only showed the tip; it doesn't also open the settling up.
        if (longPress.current?.didShow) {
          longPress.current = null
          return
        }
        onOpen()
      }}
      {...guestHandlers}
      className={cn(
        'rounded-lg outline-none select-none hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring',
        isRow ? 'flex min-h-14 min-w-0 items-center gap-3 px-2 py-2 text-left' : 'flex w-[72px] shrink-0 flex-col items-center gap-0.5 py-1.5 md:w-[88px]',
      )}
    >
      <span className="relative flex shrink-0">
        {person.color ? (
          <Avatar initial={person.initials} color={person.color} photoUrl={person.photoUrl} className={isRow ? 'size-10 text-sm' : 'size-12 text-base md:size-14 md:text-lg'} />
        ) : (
          <span aria-hidden className={cn('rounded-full bg-muted', isRow ? 'size-10' : 'size-12 md:size-14')} />
        )}
        {person.isGuest && (
          <span aria-hidden className="absolute -right-0.5 -bottom-0.5 flex size-5 items-center justify-center rounded-full border-2 border-card bg-muted text-muted-foreground">
            <NoAppIcon className="size-[11px]" strokeWidth={2.6} />
          </span>
        )}
      </span>
      {isRow ? (
        <>
          <span className="min-w-0 flex-1 truncate text-sm font-medium">{name}</span>
          <span className="flex shrink-0 items-baseline gap-1.5 whitespace-nowrap">
            <span className={cn('text-sm font-semibold tabular-nums', balanceTone(person.netMinor))}>
              {formatSignedMoney(person.netMinor, person.currency, person.fractionDigits)}
            </span>
            <span className="text-xs text-muted-foreground">{t(youOwe ? 'groups_balanceYouOwe' : 'home_owesYouWord')}</span>
          </span>
        </>
      ) : (
        <>
          <span className="max-w-full truncate text-[13px] leading-4 font-medium">{name}</span>
          <span className={cn('mt-1 text-xs leading-4 font-semibold whitespace-nowrap tabular-nums', balanceTone(person.netMinor))}>
            {formatSignedMoney(person.netMinor, person.currency, person.fractionDigits)}
          </span>
          <span className="text-[11px] leading-[14px] text-muted-foreground">{t(youOwe ? 'groups_balanceYouOwe' : 'home_owesYouWord')}</span>
        </>
      )}
    </button>
  )
}

/** The element's width inside its padding, in CSS px, followed as the window or the layout changes. */
function useContentWidth(ref: RefObject<HTMLElement | null>, isMounted: boolean): number {
  const [width, setWidth] = useState(0)
  useLayoutEffect(() => {
    const element = ref.current
    if (!element) return
    // Why: measured before the first paint too, so a wide card doesn't flash the faces before its rows.
    const style = getComputedStyle(element)
    setWidth(element.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight))
    const observer = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width))
    observer.observe(element)
    return () => observer.disconnect()
  }, [ref, isMounted])
  return width
}

/** Whether the row is wider than its box, so it scrolls; checked again as the window or the row changes. */
function useOverflows(ref: RefObject<HTMLElement | null>, itemCount: number): boolean {
  const [overflows, setOverflows] = useState(false)
  useLayoutEffect(() => {
    const element = ref.current
    if (!element) return
    const measure = () => setOverflows(element.scrollWidth > element.clientWidth + 1)
    measure()
    const observer = new ResizeObserver(measure)
    observer.observe(element)
    return () => observer.disconnect()
  }, [ref, itemCount])
  return overflows
}

/** "+R$ 249,60" and "a receber", or "−R$ 29,20" and "você deve": what the bill does to your balance. */
function shareParts(bill: JsHomeBill): { amount: string; word?: string } {
  const amount = formatSignedMoney(bill.myBalanceMinor, bill.currency, bill.fractionDigits)
  if (bill.myBalanceMinor > 0) return { amount, word: t('groups_balanceToReceive') }
  if (bill.myBalanceMinor < 0) return { amount, word: t('groups_balanceYouOwe') }
  return { amount }
}

/**
 * "+R$ 249,60" or "−R$ 29,20": in a list the sign and the color say it, as Groups' list does (beta feedback); a screen
 * reader still hears "a receber" or "você deve". The bill itself keeps the word.
 */
function ShareAmount({ bill, className }: { bill: JsHomeBill; className?: string }) {
  const { amount, word } = shareParts(bill)
  return (
    <span className={cn('font-semibold whitespace-nowrap tabular-nums', balanceTone(bill.myBalanceMinor), className)}>
      {amount}
      {word && <span className="sr-only"> {word}</span>}
    </span>
  )
}

/** "Você pagou", "Ana pagou", "Ana e Bruno pagaram" or "3 pessoas pagaram", "na fila · " first while it waits. */
function paidText(bill: JsHomeBill): string {
  const paid = payersText(Array.from(bill.payers)) ?? ''
  return bill.isPending ? [t('groups_queued'), paid].filter(Boolean).join(' · ') : paid
}

/**
 * Under a bill, the group's square (left out in the table, whose Grupo column has it) and who paid as faces and "+N",
 * the amber dot and "na fila" while it waits (beta feedback, as the legacy app drew them: "Viagem Floripa · Você pagou"
 * was cut off on a phone). Screen readers get that sentence instead.
 */
function BillOrigin({ bill, showsGroup, children }: { bill: JsHomeBill; showsGroup: boolean; children?: ReactNode }) {
  const label = showsGroup ? [bill.groupName, paidText(bill)].filter(Boolean).join(' · ') : paidText(bill)
  return (
    <span className="flex min-w-0 items-center gap-1.5 text-xs text-muted-foreground">
      <span className="sr-only">{label}</span>
      {showsGroup && <GroupAvatar initials={bill.groupInitials} color={bill.groupColor} photoUrl={bill.groupPhotoUrl} className="size-5 text-[7px]" />}
      <FaceStack faces={Array.from(bill.payerFaces)} extraCount={bill.extraPayerCount} isSmall />
      {bill.isPending && (
        <span aria-hidden className="flex shrink-0 items-center gap-1">
          <PendingDot className="size-[7px]" />
          {t('groups_queued')}
        </span>
      )}
      {children}
    </span>
  )
}

/**
 * "Últimas despesas": the five latest bills you paid or take part in, from every group, the most recent first. A
 * table on the desktop (Despesa, Grupo, Quando, Total, Sua parte), a list below it with the group's square, the faces
 * of who paid and the day under the description. The card picks by its own width, not the window's: beside "Seus grupos" a narrower desktop window (half a
 * laptop's screen) leaves the table no room for "Sua parte", so the card stays a list until the table fits. There the
 * description takes what the other columns leave, the group's name is capped, tighter the narrower the card, and Sua
 * parte's word sits under its amount; a hundred million of another year in the narrowest card scrolls the table
 * sideways rather than cutting it. A bill opens its Conta over its group, its ⋮ (or a right click) the group's options; "Ver tudo" opens "Todas as despesas".
 */
export function RecentBills({ presenter, state }: SectionProps) {
  const rows = Array.from(state.billDays).flatMap((day) => Array.from(day.bills).map((bill) => ({ bill, day: formatDayLabel(day.dayStartMillis, day.relativeDay) })))
  if (rows.length === 0 && !state.firstDownload) return null
  const open = (bill: JsHomeBill) => presenter.openBill(bill.groupId, bill.id)
  return (
    <section aria-labelledby="home-bills-title" className="@container min-w-0 overflow-hidden rounded-lg border bg-card">
      <SectionHeader id="home-bills-title" title={t('home_recentBills')} action={t('home_seeAllBills')} onAction={() => presenter.seeAllBills()} className="px-4 pt-3.5 pb-2.5" />
      {rows.length > 0 && (
        <>
          <div className="hidden overflow-x-auto lg:@min-[40rem]:block">
            <table className="w-full text-sm">
              <thead className="border-y text-xs text-muted-foreground">
                <tr className="text-left">
                  <th scope="col" className="px-4 py-2 font-medium">
                    {t('groupDetail_columnExpense')}
                  </th>
                  <th scope="col" className="px-3 py-2 font-medium">
                    {t('groups_columnGroup')}
                  </th>
                  <th scope="col" className="px-3 py-2 font-medium">
                    {t('home_columnWhen')}
                  </th>
                  <th scope="col" className="px-3 py-2 text-right font-medium">
                    {t('groupDetail_columnTotal')}
                  </th>
                  <th scope="col" className="py-2 pl-3 text-right font-medium">
                    {t('home_columnShare')}
                  </th>
                  <th scope="col" className="w-12 py-2 pr-1">
                    <span className="sr-only">{t('groupDetail_columnOptions')}</span>
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {rows.map(({ bill, day }) => (
                  <HomeBillTableRow key={bill.id} bill={bill} day={day} menu={presenter.billMenu} onOpen={() => open(bill)} />
                ))}
              </tbody>
            </table>
          </div>
          <ul className="divide-y border-t lg:@min-[40rem]:hidden">
            {rows.map(({ bill, day }) => (
              <HomeBillListRow key={bill.id} bill={bill} menu={presenter.billMenu} onOpen={() => open(bill)}>
                <span className="truncate">{day}</span>
              </HomeBillListRow>
            ))}
          </ul>
        </>
      )}
      {rows.length === 0 && (
        <LoadingRegion isDelayed={false}>
          <SkeletonRows count={3} leading="square" className="border-t" />
        </LoadingRegion>
      )}
    </section>
  )
}

/**
 * A bill as a row of the desktop table of "Latest expenses", ending in its ⋮ (Pierre's call, beta feedback: the group's
 * Expenses' options), which a right click on the row opens too.
 */
function HomeBillTableRow({ bill, day, menu, onOpen }: { bill: JsHomeBill; day: string; menu: JsBillMenu; onOpen: () => void }) {
  const [isMenuOpen, setMenuOpen] = useState(false)
  return (
    <tr
      onClick={onOpen}
      onContextMenu={(event) => {
        event.preventDefault()
        setMenuOpen(true)
      }}
      className="cursor-pointer hover:bg-accent"
    >
      <td className="w-full max-w-0 px-4 py-2.5">
        <span className="flex min-w-0 items-center gap-2.5">
          <CategoryBadge category={bill.category} className="size-9 rounded-lg" />
          <span className="flex min-w-0 flex-col">
            <button
              onClick={(event) => {
                event.stopPropagation()
                onOpen()
              }}
              title={bill.description}
              className="truncate rounded-sm text-left font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
            >
              {bill.description}
            </button>
            <BillOrigin bill={bill} showsGroup={false} />
          </span>
        </span>
      </td>
      <td className="px-3 py-2.5">
        <span className="flex items-center gap-2">
          <GroupAvatar initials={bill.groupInitials} color={bill.groupColor} photoUrl={bill.groupPhotoUrl} className="size-5 text-[7px]" />
          <span title={bill.groupName} className="block max-w-24 truncate @3xl:max-w-32 @4xl:max-w-48 @5xl:max-w-64">
            {bill.groupName}
          </span>
        </span>
      </td>
      <td className="px-3 py-2.5 whitespace-nowrap text-muted-foreground">{day}</td>
      <td className="px-3 py-2.5 text-right whitespace-nowrap tabular-nums">{formatMoney(bill.totalMinor, bill.currency, bill.fractionDigits)}</td>
      <td className="py-2.5 pl-3 text-right">
        <ShareAmount bill={bill} />
      </td>
      <td className="py-1 pr-1" onClick={(event) => event.stopPropagation()}>
        <RowMenuButton title={bill.description} options={homeBillMenuOptions(menu, bill)} open={isMenuOpen} onOpenChange={setMenuOpen} />
      </td>
    </tr>
  )
}

/**
 * A bill as a list row (Home's "Latest expenses" below the desktop, and "All expenses"): its category, the group's
 * square and the payers' faces under the description, what follows them (the day, on Home), its total and your share,
 * then its ⋮, which a right click on the row opens too.
 */
export function HomeBillListRow({ bill, menu, onOpen, children }: { bill: JsHomeBill; menu: JsBillMenu; onOpen: () => void; children?: ReactNode }) {
  const [isMenuOpen, setMenuOpen] = useState(false)
  return (
    <li
      className="flex items-center hover:bg-accent"
      onContextMenu={(event) => {
        event.preventDefault()
        setMenuOpen(true)
      }}
    >
      <button
        onClick={onOpen}
        className="flex min-w-0 flex-1 items-center gap-3 py-2.5 pr-1 pl-4 text-left text-sm outline-none focus-visible:ring-[3px] focus-visible:ring-ring focus-visible:ring-inset"
      >
        <CategoryBadge category={bill.category} className="size-9 rounded-lg" />
        <span className="flex min-w-0 flex-1 flex-col gap-0.5 overflow-hidden">
          <span className="truncate font-medium">{bill.description}</span>
          <BillOrigin bill={bill} showsGroup>
            {children}
          </BillOrigin>
        </span>
        <span className="flex shrink-0 flex-col items-end gap-0.5">
          <span className="tabular-nums">{formatMoney(bill.totalMinor, bill.currency, bill.fractionDigits)}</span>
          <ShareAmount bill={bill} className="text-xs" />
        </span>
      </button>
      <span className="shrink-0 pr-1">
        <RowMenuButton title={bill.description} options={homeBillMenuOptions(menu, bill)} open={isMenuOpen} onOpenChange={setMenuOpen} />
      </span>
    </li>
  )
}

/** "na fila · Mercado" while a bill waits, else the last activity, with the member count when that's all it says. */
function groupMetaText(group: JsHomeGroup): string {
  const row = group.row
  if (row.detail === 'ACTIVITY') return `${lastActivityText(row)} · ${membersText(row.memberCount)}`
  if (row.detail === 'QUEUED' && row.pendingBillDescription) return t('groups_metaQueuedBill', { bill: row.pendingBillDescription })
  return lastActivityText(row)
}

/**
 * "Seus grupos": up to three, the pinned first ("Fixar na Início" in the group's ⋯ menu), then the latest activity;
 * settled groups stay out. Each opens its group; "Ver todos" is the Grupos tab.
 */
export function YourGroups({ presenter, state }: SectionProps) {
  if (state.groups.length === 0) return null
  return (
    <section aria-labelledby="home-groups-title" className="min-w-0 overflow-hidden rounded-lg border bg-card">
      <SectionHeader id="home-groups-title" title={t('home_yourGroups')} action={t('home_seeAllGroups')} onAction={() => presenter.seeAllGroups()} className="px-4 pt-3.5 pb-2.5" />
      <ul className="divide-y border-t">
        {Array.from(state.groups).map((group) => (
          <li key={group.row.id}>
            <GroupRowButton group={group} onOpen={() => presenter.openGroup(group.row.id)} />
          </li>
        ))}
      </ul>
    </section>
  )
}

/** A group's row: its shape, name with the pin, the meta with the amber dot, and your balance in it with its word. */
function GroupRowButton({ group, onOpen }: { group: JsHomeGroup; onOpen: () => void }) {
  const row = group.row
  return (
    <button
      data-group-link={row.id}
      onClick={onOpen}
      className="flex w-full items-center gap-3 px-4 py-3 text-left text-sm outline-none hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring focus-visible:ring-inset"
    >
      <GroupAvatar data-group-avatar initials={row.initials} color={row.color} photoUrl={row.photoUrl} className="size-9 text-xs" />
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="flex min-w-0 items-center gap-1.5 font-medium">
          <span data-group-name className="truncate">
            {row.name}
          </span>
          {group.isPinned && <Pin role="img" aria-label={t('home_pinned')} className="size-[13px] shrink-0 text-muted-foreground" />}
        </span>
        <span className="flex min-w-0 items-center gap-1.5 text-xs text-muted-foreground">
          {row.isPending && <PendingDot className="size-[7px]" />}
          <span className="truncate">{groupMetaText(group)}</span>
        </span>
      </span>
      <GroupBalance group={row} />
    </button>
  )
}
