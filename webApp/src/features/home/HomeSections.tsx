import { ChevronRight, Pin, ReceiptText } from 'lucide-react'
import type { HomePresenter, JsHomeBill, JsHomeGroup, JsHomePerson, JsHomeState, JsHomeTotal } from 'blitzsplit-shared'
import { useEffect, useId, useLayoutEffect, useRef, useState, type FocusEvent, type MouseEvent, type PointerEvent, type RefObject } from 'react'
import { Avatar } from '@/components/common'
import { balanceTone, nameOrFormer } from '@/features/balances/BalanceCard'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { GroupBalance, lastActivityText, membersText } from '@/features/groups/GroupRowParts'
import { paidText as payersText } from '@/features/groups/GroupsSearchDialog'
import { t, type MessageKey } from '@/i18n'
import { formatDayLabel } from '@/lib/dates'
import { formatMoney, formatSignedMoney } from '@/lib/money'
import { cn } from '@/lib/utils'

interface SectionProps {
  presenter: HomePresenter
  state: JsHomeState
}

const summaryHints: Record<string, MessageKey> = {
  TO_RECEIVE: 'home_summaryToReceiveHint',
  YOU_OWE: 'home_summaryToPayHint',
}

/** A gray bar where something still on its way will show, while the first download fills the browser. */
function Skeleton({ className }: { className?: string }) {
  return <span aria-hidden className={cn('block animate-pulse rounded-md bg-muted', className)} />
}

/**
 * "Saldo geral" (the net, one line per extra currency), then "Te devem" and "Você deve", which open Saldos on that
 * filter ("Você deve" straight to settling up while only one person is in it), disabled as "Nada a receber" and
 * "Nada a pagar" at zero. Desktop: three cards with the net wider; tablet three equal; phone the net over the two.
 */
export function HomeSummaryCards({ presenter, state }: SectionProps) {
  const summary = state.summary
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
          {t(state.isFirstDownload ? 'home_summaryFirstDownloadHint' : isSettled ? 'home_summarySettledHint' : (summaryHints[summary.kind] ?? 'home_summarySettledHint'))}
        </span>
      </section>
      <SplitCard
        label={t(summary.canOpenToReceive ? 'home_toReceive' : 'home_nothingToReceive')}
        total={toReceive}
        amountMinor={toReceive?.toReceiveMinor ?? 0}
        caption={summary.canOpenToReceive ? people(toReceive?.toReceiveCount ?? 0) : t('home_nobodyOwesYou')}
        action={t('groups_summaryToReceiveAction')}
        tone="text-primary"
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
 * "Saldos com pessoas": a face per person you have an open balance with, whom you owe first, in a row that scrolls
 * sideways; " · 9" once it does. A face opens settling up with them for the net. A guest (a name nobody joined as)
 * wears the crossed-out phone, and hovering it, focusing it or a long press shows why they get no notice. Gone,
 * leaving no gap, once everyone is settled; faces of gray while the first download hasn't brought anyone yet.
 */
export function PeopleCard({ presenter, state }: SectionProps) {
  const cardRef = useRef<HTMLElement>(null)
  const rowRef = useRef<HTMLDivElement>(null)
  const tipId = useId()
  const [tip, setTip] = useState<GuestTip | null>(null)
  const isScrolling = useOverflows(rowRef, state.people.length)
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
  if (!state.showsPeople && !state.isFirstDownload) return null

  const people = Array.from(state.people)
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
        title={isScrolling ? t('home_peopleCount', { count: people.length }) : t('home_people')}
        action={t('home_seeBalances')}
        onAction={() => presenter.seeBalances()}
      />
      <div ref={rowRef} className="-mx-1 flex gap-1.5 overflow-x-auto px-1 pb-1 md:gap-2" onScroll={() => tip && setTip(null)}>
        {people.length === 0
          ? [0, 1, 2, 3, 4].map((index) => (
              <span key={index} aria-hidden className="flex w-[72px] shrink-0 flex-col items-center gap-2 py-1.5 md:w-[88px]">
                <Skeleton className="size-12 rounded-full md:size-14" />
                <Skeleton className="h-3 w-12" />
                <Skeleton className="h-3 w-14" />
              </span>
            ))
          : people.map((person) => (
              <PersonFace
                key={`${person.key}/${person.currency}`}
                person={person}
                tipId={tip?.key === person.key ? tipId : undefined}
                onOpen={() => presenter.openPerson(person.key, person.currency)}
                onTip={(face, isTouch) => (face ? showTip(person, face, isTouch) : setTip(null))}
              />
            ))}
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

/** One face of the row: avatar (with the guest's badge), name, the net with its sign and its word. */
function PersonFace({
  person,
  tipId,
  onOpen,
  onTip,
}: {
  person: JsHomePerson
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
      className="flex w-[72px] shrink-0 flex-col items-center gap-0.5 rounded-lg py-1.5 outline-none select-none hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring md:w-[88px]"
    >
      <span className="relative flex">
        {person.color ? (
          <Avatar initial={person.initials} color={person.color} className="size-12 text-base md:size-14 md:text-lg" />
        ) : (
          <span aria-hidden className="size-12 rounded-full bg-muted md:size-14" />
        )}
        {person.isGuest && (
          <span aria-hidden className="absolute -right-0.5 -bottom-0.5 flex size-5 items-center justify-center rounded-full border-2 border-card bg-muted text-muted-foreground">
            <NoAppIcon className="size-[11px]" strokeWidth={2.6} />
          </span>
        )}
      </span>
      <span className="max-w-full truncate text-[13px] leading-4 font-medium">{name}</span>
      <span className={cn('mt-1 text-xs leading-4 font-semibold whitespace-nowrap tabular-nums', balanceTone(person.netMinor))}>
        {formatSignedMoney(person.netMinor, person.currency, person.fractionDigits)}
      </span>
      <span className="text-[11px] leading-[14px] text-muted-foreground">{t(youOwe ? 'groups_balanceYouOwe' : 'home_owesYouWord')}</span>
    </button>
  )
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

/** "+R$ 249,60 a receber" or "−R$ 29,20 você deve": what the bill does to your balance. */
function shareText(bill: JsHomeBill): string {
  const amount = formatSignedMoney(bill.myBalanceMinor, bill.currency, bill.fractionDigits)
  if (bill.myBalanceMinor > 0) return `${amount} ${t('groups_balanceToReceive')}`
  if (bill.myBalanceMinor < 0) return `${amount} ${t('groups_balanceYouOwe')}`
  return amount
}

/** "Você pagou", "Ana pagou", "Ana e Bruno pagaram" or "3 pessoas pagaram", "na fila · " first while it waits. */
function paidText(bill: JsHomeBill): string {
  const paid = payersText(Array.from(bill.payers)) ?? ''
  return bill.isPending ? [t('groups_queued'), paid].filter(Boolean).join(' · ') : paid
}

/** The orange dot of a bill or a group waiting for the server. */
function PendingDot() {
  return <span aria-hidden className="size-[7px] shrink-0 rounded-full bg-orange-500" />
}

function BillIcon({ className }: { className?: string }) {
  return (
    <span aria-hidden className={cn('flex shrink-0 items-center justify-center rounded-lg bg-muted', className)}>
      <ReceiptText className="size-4" />
    </span>
  )
}

/**
 * "Últimas despesas": the five latest bills you paid or take part in, from every group, the most recent first. A
 * table on the desktop (Despesa, Grupo, Quando, Total, Sua parte), a list below it with the group and the day in the
 * meta. A bill opens its Conta over its group; "Ver tudo" is Atividade, not built yet.
 */
export function RecentBills({ presenter, state }: SectionProps) {
  const rows = Array.from(state.billDays).flatMap((day) => Array.from(day.bills).map((bill) => ({ bill, day: formatDayLabel(day.dayStartMillis, day.relativeDay) })))
  if (rows.length === 0 && !state.isFirstDownload) return null
  const open = (bill: JsHomeBill) => presenter.openBill(bill.groupId, bill.id)
  const shareClass = (bill: JsHomeBill) => cn('font-semibold whitespace-nowrap tabular-nums', balanceTone(bill.myBalanceMinor))
  return (
    <section aria-labelledby="home-bills-title" className="min-w-0 overflow-hidden rounded-lg border bg-card">
      <SectionHeader id="home-bills-title" title={t('home_recentBills')} action={t('home_seeAllBills')} onAction={() => presenter.seeAllBills()} className="px-4 pt-3.5 pb-2.5" />
      {rows.length > 0 && (
        <>
          <table className="hidden w-full text-sm lg:table">
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
                <th scope="col" className="px-4 py-2 text-right font-medium">
                  {t('home_columnShare')}
                </th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {rows.map(({ bill, day }) => (
                <tr key={bill.id} onClick={() => open(bill)} className="cursor-pointer hover:bg-accent">
                  <td className="px-4 py-2.5">
                    <span className="flex min-w-0 items-center gap-2.5">
                      <BillIcon className="size-8" />
                      <span className="flex min-w-0 flex-col">
                        <button
                          onClick={(event) => {
                            event.stopPropagation()
                            open(bill)
                          }}
                          className="truncate rounded-sm text-left font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
                        >
                          {bill.description}
                        </button>
                        <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
                          {bill.isPending && <PendingDot />}
                          {paidText(bill)}
                        </span>
                      </span>
                    </span>
                  </td>
                  <td className="px-3 py-2.5 whitespace-nowrap">{bill.groupName}</td>
                  <td className="px-3 py-2.5 whitespace-nowrap text-muted-foreground">{day}</td>
                  <td className="px-3 py-2.5 text-right whitespace-nowrap tabular-nums">{formatMoney(bill.totalMinor, bill.currency, bill.fractionDigits)}</td>
                  <td className="px-4 py-2.5 text-right">
                    <span className={shareClass(bill)}>{shareText(bill)}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <ul className="divide-y border-t lg:hidden">
            {rows.map(({ bill, day }) => (
              <li key={bill.id}>
                <button
                  onClick={() => open(bill)}
                  className="flex w-full items-center gap-3 px-4 py-2.5 text-left text-sm outline-none hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring focus-visible:ring-inset"
                >
                  <BillIcon className="size-9" />
                  <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                    <span className="truncate font-medium">{bill.description}</span>
                    <span className="flex min-w-0 items-center gap-1.5 text-xs text-muted-foreground">
                      {bill.isPending && <PendingDot />}
                      <span className="truncate">{[bill.groupName, paidText(bill), day].join(' · ')}</span>
                    </span>
                  </span>
                  <span className="flex shrink-0 flex-col items-end gap-0.5">
                    <span className="tabular-nums">{formatMoney(bill.totalMinor, bill.currency, bill.fractionDigits)}</span>
                    <span className={cn('text-xs', shareClass(bill))}>{shareText(bill)}</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </>
      )}
      {rows.length === 0 && (
        <div aria-hidden className="divide-y border-t">
          {[0, 1, 2].map((index) => (
            <div key={index} className="flex items-center gap-3 px-4 py-3">
              <Skeleton className="size-8 rounded-lg" />
              <span className="flex flex-1 flex-col gap-2">
                <Skeleton className="h-3 w-2/5" />
                <Skeleton className="h-2.5 w-1/4" />
              </span>
              <Skeleton className="h-3 w-18" />
            </div>
          ))}
        </div>
      )}
    </section>
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

/** A group's row: its shape, name with the pin, the meta with the orange dot, and your balance in it with its word. */
function GroupRowButton({ group, onOpen }: { group: JsHomeGroup; onOpen: () => void }) {
  const row = group.row
  return (
    <button
      onClick={onOpen}
      className="flex w-full items-center gap-3 px-4 py-3 text-left text-sm outline-none hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring focus-visible:ring-inset"
    >
      <GroupAvatar initials={row.initials} color={row.color} className="size-9 rounded-lg text-xs" />
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="flex min-w-0 items-center gap-1.5 font-medium">
          <span className="truncate">{row.name}</span>
          {group.isPinned && <Pin role="img" aria-label={t('home_pinned')} className="size-[13px] shrink-0 text-muted-foreground" />}
        </span>
        <span className="flex min-w-0 items-center gap-1.5 text-xs text-muted-foreground">
          {row.isPending && <PendingDot />}
          <span className="truncate">{groupMetaText(group)}</span>
        </span>
      </span>
      <GroupBalance group={row} />
    </button>
  )
}
