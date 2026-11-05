import { ArrowRight, Loader2 } from 'lucide-react'
import type { JsSimplifyAmount, JsSimplifyConfirmation, JsSimplifyName, JsSimplifyPersonChange, JsSimplifyQueuedPayment } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { Avatar, PendingDot, pendingSurfaceClass } from '@/components/common'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { nameOrFormer } from '@/features/balances/BalanceCard'
import { t, type MessageKey } from '@/i18n'
import { formatMoney, formatNames } from '@/lib/money'
import { paymentCount, simplifyName } from './simplify'
import { cn } from '@/lib/utils'

type Money = (minor: number) => string

/** What the confirmation asks of Editar grupo's ViewModel. */
export interface SimplifyConfirmEvents {
  confirmSimplifyDebts(): void
  dismissSimplifyDebts(): void
}

/**
 * "Simplificar dívidas?" (13.3, 13.3c, 13.3d) or "Desligar Simplificar dívidas?" (13.3b), which the switch of Editar
 * grupo opens: an AlertDialog over the dialog on desktop and tablet, a drawer with stacked buttons on the phone.
 * Confirming applies it at once, without Salvar; closing it any way changes nothing.
 */
export function SimplifyDebtsConfirmDialog({
  confirmation,
  currency,
  fractionDigits,
  isBusy,
  isDesktop,
  events,
}: {
  confirmation: JsSimplifyConfirmation
  currency: string
  fractionDigits: number
  isBusy: boolean
  isDesktop: boolean
  events: SimplifyConfirmEvents
}) {
  const money: Money = (minor) => formatMoney(minor, currency, fractionDigits)
  const title = t(confirmation.isTurningOn ? 'simplifyConfirm_titleOn' : 'simplifyConfirm_titleOff')
  const body = confirmation.isAlreadyMinimal
    ? t('simplifyConfirm_bodyMinimal', { group: confirmation.groupName })
    : t(confirmation.isTurningOn ? 'simplifyConfirm_bodyOn' : 'simplifyConfirm_bodyOff')
  const onOpenChange = (open: boolean) => {
    if (!open) events.dismissSimplifyDebts()
  }
  const content = (
    <>
      <Counts before={confirmation.paymentsBefore} after={confirmation.paymentsAfter} />
      {confirmation.people.length > 0 && <WhatChanges people={confirmation.people} money={money} />}
      {confirmation.queuedPayments.length > 0 && (
        <p className={cn('flex gap-2 rounded-lg border px-3 py-2.5 text-[13px] leading-relaxed', pendingSurfaceClass)}>
          <PendingDot className="mt-1.5" />
          <span>{queuedText(confirmation.queuedPayments, money)}</span>
        </p>
      )}
      <p className="text-[13px] leading-relaxed text-muted-foreground">{footText(confirmation)}</p>
    </>
  )
  const cancel = (
    <Button variant="outline" disabled={isBusy} onClick={() => events.dismissSimplifyDebts()} className={isDesktop ? undefined : 'h-11'}>
      {t(confirmation.isTurningOn ? 'simplifyConfirm_notNow' : 'common_cancel')}
    </Button>
  )
  const confirm = (
    <Button disabled={isBusy} onClick={() => events.confirmSimplifyDebts()} className={isDesktop ? undefined : 'h-11'}>
      {isBusy && <Loader2 className="animate-spin" />}
      {t(confirmLabel(confirmation))}
    </Button>
  )
  return isDesktop ? (
    <AlertDialog open onOpenChange={onOpenChange}>
      <AlertDialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-[520px]">
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{body}</AlertDialogDescription>
        </AlertDialogHeader>
        {content}
        <AlertDialogFooter>
          {cancel}
          {/* Not an AlertDialogAction, which would close it through onOpenChange as a dismissal */}
          {confirm}
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  ) : (
    <Drawer open onOpenChange={onOpenChange}>
      <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
        <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          <DrawerTitle>{title}</DrawerTitle>
          <DrawerDescription>{body}</DrawerDescription>
        </DrawerHeader>
        <div className="flex flex-col gap-4 overflow-y-auto px-4">{content}</div>
        <DrawerFooter>
          {confirm}
          {cancel}
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

function confirmLabel(confirmation: JsSimplifyConfirmation): MessageKey {
  if (!confirmation.isTurningOn) return 'simplifyConfirm_turnOff'
  return confirmation.isAlreadyMinimal ? 'simplifyConfirm_turnOnAnyway' : 'simplifyConfirm_simplify'
}

/** "7 pagamentos → 5 pagamentos", read as "7 pagamentos viram 5 pagamentos". */
function Counts({ before, after }: { before: number; after: number }) {
  const label = t(before === 1 ? 'simplifyConfirm_countsOne' : 'simplifyConfirm_countsOther', { before: paymentCount(before), after: paymentCount(after) })
  const side = (count: number, tone: string) => (
    <span className="flex items-baseline gap-1.5">
      <strong className={`text-[28px] leading-8 font-bold tabular-nums ${tone}`}>{count}</strong>
      <span className="text-[13px] text-muted-foreground">{t(count === 1 ? 'simplifyConfirm_paymentsWordOne' : 'simplifyConfirm_paymentsWordOther')}</span>
    </span>
  )
  return (
    <div role="img" aria-label={label} className="flex items-center justify-center gap-4 rounded-lg bg-muted px-4 py-3.5">
      {side(before, 'text-muted-foreground')}
      <ArrowRight aria-hidden className="size-5 text-muted-foreground" />
      {side(after, 'text-foreground')}
    </div>
  )
}

/** "O que muda": each person, this browser's user first, with what they stop and start paying or receiving. */
function WhatChanges({ people, money }: { people: JsSimplifyPersonChange[]; money: Money }) {
  return (
    <section aria-labelledby="simplify-what-changes" className="flex flex-col gap-2">
      <h3 id="simplify-what-changes" className="text-sm font-semibold">
        {t('simplifyConfirm_whatChanges')}
      </h3>
      <ul className="flex flex-col divide-y overflow-hidden rounded-lg border">
        {people.map((person) => (
          <li key={person.memberId} className="flex items-start gap-2.5 px-3 py-2">
            <Avatar initial={person.initials} color={person.color} photoUrl={person.photoUrl} className="size-7 text-[11px]" />
            <span className="min-w-0 flex-1 text-[13px] leading-[1.45]">
              <PersonName person={person} /> <span className="text-muted-foreground">{changeText(person, money)}</span>
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}

function PersonName({ person }: { person: JsSimplifyPersonChange }): ReactNode {
  const name = person.person.isMe ? t('simplify_youCapital') : nameOrFormer(person.person.name)
  if (!person.isGuest) return <strong className="font-semibold">{name}.</strong>
  return (
    <>
      <strong className="font-semibold">{name}</strong>
      <span className="text-muted-foreground">{t('simplifyConfirm_guestTag')}.</span>
    </>
  )
}

/** "Não paga mais R$ 118 a Bruno. Paga R$ 29,50 a você. Não recebe mais de Carla. Recebe de Carla, Diego e Elisa." */
function changeText(person: JsSimplifyPersonChange, money: Money): string {
  const isMe = person.person.isMe
  const amounts = (list: JsSimplifyAmount[]) =>
    formatNames(list.map((amount) => t('simplifyConfirm_amountTo', { amount: money(amount.amountMinor), name: simplifyName(amount.to) })))
  const names = (list: JsSimplifyName[]) => formatNames(list.map(simplifyName))
  const sentences: string[] = []
  if (person.stopsPaying.length > 0) sentences.push(t(isMe ? 'simplifyConfirm_stopsPayingMe' : 'simplifyConfirm_stopsPaying', { amounts: amounts(person.stopsPaying) }))
  if (person.pays.length > 0) sentences.push(t(isMe ? 'simplifyConfirm_paysMe' : 'simplifyConfirm_pays', { amounts: amounts(person.pays) }))
  if (person.stopsReceivingFrom.length > 0) {
    sentences.push(t(isMe ? 'simplifyConfirm_stopsReceivingMe' : 'simplifyConfirm_stopsReceiving', { names: names(person.stopsReceivingFrom) }))
  }
  if (person.receivesFrom.length > 0) sentences.push(t(isMe ? 'simplifyConfirm_receivesMe' : 'simplifyConfirm_receives', { names: names(person.receivesFrom) }))
  if (person.isGuest) sentences.push(t('simplifyConfirm_guestNote'))
  return sentences.join(' ')
}

/** "Você tem 1 pagamento na fila: Elisa te pagou R$ 50. A simplificação conta com ele…" (13.3d) */
function queuedText(queued: JsSimplifyQueuedPayment[], money: Money): string {
  if (queued.length > 1) return t('simplifyConfirm_queuedOther', { count: queued.length })
  const { from, to, amountMinor } = queued[0]
  const amount = money(amountMinor)
  const payment = to.isMe
    ? t('simplifyConfirm_queuedPaidYou', { from: simplifyName(from), amount })
    : from.isMe
      ? t('simplifyConfirm_queuedYouPaid', { to: simplifyName(to), amount })
      : t('simplifyConfirm_queuedPaid', { from: simplifyName(from), to: simplifyName(to), amount })
  return t('simplifyConfirm_queuedOne', { payment })
}

function footText(confirmation: JsSimplifyConfirmation): string {
  if (confirmation.isAlreadyMinimal) return t('simplifyConfirm_footMinimal')
  if (confirmation.isTurningOn) return t('simplifyConfirm_footOn')
  const others = confirmation.othersChangingCount
  if (others === 0) return t('simplifyConfirm_footOffNone')
  return others === 1 ? t('simplifyConfirm_footOffOne') : t('simplifyConfirm_footOffOther', { count: others })
}
