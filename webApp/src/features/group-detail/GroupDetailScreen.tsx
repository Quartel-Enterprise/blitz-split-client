import { HandCoins, ReceiptText } from 'lucide-react'
import {
  GroupDetailPresenter,
  type JsExpenseDay,
  type JsExpenseRow,
  type JsGroupDetailState,
  type JsPaymentRow,
  type JsRoute,
} from 'blitzsplit-shared'
import { useEffect, type ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { Button } from '@/components/ui/button'
import { balanceTone, nameOrFormer, paymentMethodLabel, useRecordedPaymentNotice } from '@/features/balances/BalanceCard'
import { t } from '@/i18n'
import { formatDayLabel, formatTime } from '@/lib/dates'
import { formatMoney, formatSignedMoney } from '@/lib/money'
import { payerNamesText } from '@/lib/payers'
import { cn } from '@/lib/utils'
import { GroupPage } from './GroupPage'

// Why: the canvas's columns, the bill, its total and what it does to your balance; the phone folds the total into the meta.
const rowColumns = 'grid grid-cols-[minmax(0,1fr)_auto] md:grid-cols-[minmax(0,1fr)_120px_150px] lg:grid-cols-[minmax(0,1fr)_160px_180px]'

/**
 * "Despesas do grupo": the group's bills and the payments that settled its debts by day, newest first, under the
 * group's page (header, balance card, tabs). A bill opens "Conta" over it; a payment asks "Apagar este acerto?". A bill
 * someone else deleted while it was open, and a payment just recorded, say so here.
 */
export function GroupDetailScreen({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new GroupDetailPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsGroupDetailState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), (action) => {
    if (action === 'coming-soon') toast(t('groups_comingSoon'))
    if (action === 'change-error') toast(t('groupDetail_changeError'))
  })
  // Why: state, not an action, so it waits for this screen while the bill's panel still covers it.
  const showsBillDeletedNotice = state?.showsBillDeletedNotice === true
  useEffect(() => {
    if (!showsBillDeletedNotice) return
    toast(t('groupDetail_billDeleted'))
    presenter?.billDeletedNoticeShown()
  }, [presenter, showsBillDeletedNotice])
  useRecordedPaymentNotice(presenter, state?.recordedPayment)
  if (!presenter || !state?.isLoaded) return null

  return (
    <GroupPage
      header={state.header}
      tab="expenses"
      hasExpenses={!state.isEmpty}
      showsSplitBillButton={state.showsSplitBillButton}
      events={presenter}
      onSelectTab={(tab) => {
        if (tab === 'balances') presenter.balances()
        else if (tab === 'members') presenter.members()
      }}
    >
      {state.isEmpty ? (
        <section className="flex flex-col items-center gap-2 rounded-xl border border-dashed px-6 py-10 text-center">
          <span aria-hidden className="flex size-11 items-center justify-center rounded-full bg-muted text-muted-foreground">
            <ReceiptText className="size-5" />
          </span>
          <h2 className="text-base font-semibold">{t('groupDetail_emptyTitle')}</h2>
          <p className="max-w-[420px] text-sm text-muted-foreground">{t('groupDetail_emptyBody')}</p>
          <div className="mt-2 flex flex-wrap justify-center gap-2">
            <Button onClick={() => presenter.splitBill()}>{t('groupDetail_splitBill')}</Button>
            <Button variant="outline" onClick={() => presenter.invite()}>
              {t('groupDetail_invitePeople')}
            </Button>
          </div>
        </section>
      ) : (
        <ExpensesTable presenter={presenter} state={state} />
      )}
      {state.paymentToDelete && (
        <ConfirmDialog
          title={t('payment_deleteTitle')}
          description={t('payment_deleteBody')}
          confirmLabel={t('payment_deleteConfirm')}
          onConfirm={() => presenter.confirmPaymentDelete()}
          onDismiss={() => presenter.dismissPaymentDelete()}
        />
      )}
    </GroupPage>
  )
}

function ExpensesTable({ presenter, state }: { presenter: GroupDetailPresenter; state: JsGroupDetailState }) {
  return (
    <div role="table" aria-label={t('group_tabExpenses')} className="flex flex-col overflow-hidden rounded-xl border bg-card">
      <div role="row" className={cn(rowColumns, 'hidden h-10 items-center border-b text-[13px] font-medium text-muted-foreground md:grid')}>
        <span role="columnheader" className="px-4">
          {t('groupDetail_columnExpense')}
        </span>
        <span role="columnheader" className="px-4 text-right">
          {t('groupDetail_columnTotal')}
        </span>
        <span role="columnheader" className="px-4 text-right">
          {t('groupDetail_columnBalance')}
        </span>
      </div>
      {state.days.map((day) => (
        <ExpenseDayRows key={day.dayStartMillis} presenter={presenter} day={day} state={state} />
      ))}
    </div>
  )
}

function ExpenseDayRows({ presenter, day, state }: { presenter: GroupDetailPresenter; day: JsExpenseDay; state: JsGroupDetailState }) {
  return (
    <div role="rowgroup" className="border-b last:border-b-0">
      <div role="row" className="flex h-8 items-center border-b bg-sidebar px-4 text-xs font-medium text-muted-foreground">
        <span role="rowheader">{formatDayLabel(day.dayStartMillis, day.relativeDay)}</span>
      </div>
      {day.entries.map((entry) =>
        entry.expense ? (
          <ExpenseRowItem key={entry.id} presenter={presenter} expense={entry.expense} state={state} />
        ) : entry.payment ? (
          <PaymentRowItem key={entry.id} presenter={presenter} payment={entry.payment} state={state} />
        ) : null,
      )}
    </div>
  )
}

/**
 * A line of the day, a bill or a payment: its icon, title and meta (with "na fila" while it waits for the server), the
 * amount beside it from the tablet up (in the meta on the phone), and the last column.
 */
function DayRow({
  icon,
  title,
  meta,
  amount,
  isPending,
  trailing,
  onOpen,
}: {
  icon: ReactNode
  title: string
  meta: string
  amount: string
  isPending: boolean
  trailing: ReactNode
  onOpen: () => void
}) {
  return (
    <div
      role="row"
      tabIndex={0}
      onClick={onOpen}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault()
          onOpen()
        }
      }}
      className={cn(
        rowColumns,
        'min-h-[60px] cursor-pointer items-center border-b outline-none last:border-b-0 hover:bg-accent/60 focus-visible:ring-[3px] focus-visible:ring-ring focus-visible:ring-inset md:min-h-14',
      )}
    >
      <span role="cell" className="flex min-w-0 items-center gap-3 px-4 py-2">
        <span aria-hidden className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-muted text-primary">
          {icon}
        </span>
        <span className="flex min-w-0 flex-col">
          <span className="truncate font-medium">{title}</span>
          <span className="flex min-w-0 items-center gap-1.5 text-xs whitespace-nowrap text-muted-foreground">
            {isPending && (
              <span className="inline-flex h-[18px] shrink-0 items-center gap-1 rounded-md border border-amber-200 bg-amber-50 px-1.5 text-[11px] font-medium text-amber-800 dark:border-amber-900 dark:bg-amber-950/60 dark:text-amber-300">
                <span aria-hidden className="size-1.5 rounded-full bg-orange-500" />
                {t('groupDetail_pending')}
              </span>
            )}
            <span className="truncate">
              {meta}
              <span className="md:hidden"> · {amount}</span>
            </span>
          </span>
        </span>
      </span>
      <span role="cell" className="hidden px-4 text-right tabular-nums md:block">
        {amount}
      </span>
      <span role="cell" className="px-4 text-right">
        {trailing}
      </span>
    </div>
  )
}

function ExpenseRowItem({ presenter, expense, state }: { presenter: GroupDetailPresenter; expense: JsExpenseRow; state: JsGroupDetailState }) {
  const { currency, fractionDigits } = state.header
  const time = formatTime(expense.spentAt)
  const payers = expense.payers
  const payer = payers.length === 1 ? payers[0] : null
  const paid =
    payers.length > 1
      ? t('groupDetail_metaPaidBySeveral', {
          time,
          names: payerNamesText(payers.map((it) => (it.isMe ? t('splitBill_you') : (it.name ?? t('splitBill_formerMember'))))),
        })
      : payer?.isMe
        ? t('groupDetail_metaPaidByMe', { time })
        : payer?.name != null
          ? t('groupDetail_metaPaidBy', { time, name: payer.name })
          : time
  const balance = expense.myBalanceMinor
  return (
    <DayRow
      icon={<ReceiptText className="size-4" />}
      title={expense.description}
      meta={paid}
      amount={formatMoney(expense.totalMinor, currency, fractionDigits)}
      isPending={expense.isPending}
      onOpen={() => presenter.openExpense(expense.id)}
      trailing={
        balance != null && (
          <span className={cn('font-semibold whitespace-nowrap tabular-nums', balanceTone(balance))}>{formatSignedMoney(balance, currency, fractionDigits)}</span>
        )
      }
    />
  )
}

/** "Ana te pagou · 12:10 · Acerto · Pix", the amount and the "acerto" tag; tapping it asks to delete it. */
function PaymentRowItem({ presenter, payment, state }: { presenter: GroupDetailPresenter; payment: JsPaymentRow; state: JsGroupDetailState }) {
  const title = payment.toIsMe
    ? t('payment_paidYou', { name: nameOrFormer(payment.fromName) })
    : payment.fromIsMe
      ? t('payment_youPaid', { name: nameOrFormer(payment.toName) })
      : t('payment_paid', { from: nameOrFormer(payment.fromName), to: nameOrFormer(payment.toName) })
  return (
    <DayRow
      icon={<HandCoins className="size-4" />}
      title={title}
      meta={t('payment_meta', { time: formatTime(payment.paidAt), method: paymentMethodLabel(payment.method) })}
      amount={formatMoney(payment.amountMinor, state.header.currency, state.header.fractionDigits)}
      isPending={payment.isPending}
      onOpen={() => presenter.openPayment(payment.id)}
      trailing={
        <span className="inline-flex h-5 items-center rounded-full border px-2 text-[11px] font-medium text-muted-foreground">{t('payment_tag')}</span>
      }
    />
  )
}
