import { ChevronRight, CircleCheck, EllipsisVertical, HandCoins, Pencil, ReceiptText, Share2, ShieldCheck, Trash2 } from 'lucide-react'
import {
  GroupDetailPresenter,
  type JsExpenseDay,
  type JsExpenseRow,
  type JsGroupDetailState,
  type JsPaymentRow,
  type JsRoute,
} from 'blitzsplit-shared'
import { useContext, useEffect, useState, type ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import type { GroupTab } from '@/components/common'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { PageKeyContext, type HomeTab } from '@/components/HomeShell'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { LoadingRegion, SkeletonRows } from '@/components/ui/skeleton'
import { balanceTone, nameOrFormer, paymentMethodLabel, useRecordedPaymentNotice } from '@/features/balances/BalanceCard'
import { BalancesTab } from '@/features/balances/BalancesTab'
import { BillDeleteDialog } from '@/features/bill-detail/BillDeleteDialog'
import { MembersDialogs, MembersTab, useMembers } from '@/features/members/MembersTab'
import { useSimplifyConflictNotice } from '@/features/simplify-debts/simplify'
import { t } from '@/i18n'
import { formatDayLabel, formatTime } from '@/lib/dates'
import { formatMoney, formatSignedMoney } from '@/lib/money'
import { payerNamesText } from '@/lib/payers'
import { CategoryBadge } from '@/lib/categories'
import { cn } from '@/lib/utils'
import { CategoryTeachDialog, useCategoryTeachNotice } from './CategoryTeach'
import { toastGroupAction } from './GroupExitDialog'
import { GroupPage, GroupPageSkeleton } from './GroupPage'
import { pickedTabOf, rememberPickedTab } from './pickedTabs'
import { PendingDot, pendingSurfaceClass } from '@/components/common'

// Why: the canvas's columns, the bill, its total, what it does to your balance and its ⋮; the phone folds the total into the meta.
const rowColumns =
  'grid grid-cols-[minmax(0,1fr)_auto_44px] md:grid-cols-[minmax(0,1fr)_120px_150px_52px] lg:grid-cols-[minmax(0,1fr)_160px_180px_52px]'

/**
 * The group's page (header, balance card, tabs), drawn once over the tab picked, which only swaps what is under the
 * tabs: "Despesas do grupo" (the bills and the payments that settled its debts by day, newest first), Saldos or
 * Membros. A bill opens "Conta" over it, and its ⋮ (or a right click) offers Conta's options without opening it; a
 * payment asks "Apagar este acerto?", from its ⋮ too. A bill someone else deleted while it was
 * open, and a payment just recorded, say so here.
 */
export function GroupDetailScreen({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new GroupDetailPresenter(route))
  const members = useMembers(route)
  const state = useWatch(presenter, (p, onChange: (state: JsGroupDetailState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), toastGroupAction)
  // Why: back from a page the group opened draws the group anew, on the tab picked before it.
  const pageKey = useContext(PageKeyContext)
  useEffect(() => {
    const picked = pickedTabOf(pageKey)
    if (presenter && picked && picked !== route.groupTab) presenter.restoreTab(picked)
  }, [presenter, pageKey, route.groupTab])
  const pickedTab = state?.tab
  useEffect(() => {
    if (pickedTab) rememberPickedTab(pageKey, pickedTab)
  }, [pageKey, pickedTab])
  // Why: state, not an action, so it waits for this screen while the bill's panel still covers it.
  const showsBillDeletedNotice = state?.showsBillDeletedNotice === true
  useEffect(() => {
    if (!showsBillDeletedNotice) return
    toast(t('groupDetail_billDeleted'))
    presenter?.billDeletedNoticeShown()
  }, [presenter, showsBillDeletedNotice])
  useRecordedPaymentNotice(presenter, state?.recordedPayment)
  useCategoryTeachNotice(presenter, state?.teach)
  useSimplifyConflictNotice(presenter, state?.simplifyConflict)
  if (!presenter || !state) return null
  if (!state.isLoaded) return <GroupPageSkeleton groupId={route.groupId ?? ''} homeTab={state.homeTab as HomeTab} onBack={() => presenter.back()} />

  const tab = state.tab as GroupTab
  const membersPresenter = members.presenter
  const membersState = members.state?.isLoaded ? members.state : null
  return (
    <GroupPage
      groupId={route.groupId ?? ''}
      homeTab={state.homeTab as HomeTab}
      header={state.header}
      tab={tab}
      hasExpenses={state.hasBills}
      // Why: with a bill, your balance in it is known once the account's copy is whole; null till then.
      isBalanceLoading={state.isMyBalanceLoading}
      showsSplitBillButton={state.showsSplitBillButton}
      onAddPerson={tab === 'members' && membersPresenter && membersState ? () => membersPresenter.add() : undefined}
      exit={state.exit}
      events={presenter}
      onSelectTab={(picked) => presenter.selectTab(picked)}
    >
      {tab === 'expenses' && <ExpensesTab presenter={presenter} state={state} />}
      {tab === 'balances' && <BalancesTab presenter={presenter} state={state} />}
      {tab === 'members' &&
        (membersPresenter && membersState ? (
          <MembersTab presenter={membersPresenter} state={membersState} />
        ) : (
          <LoadingRegion>
            <SkeletonRows count={4} className="rounded-xl border bg-card" />
          </LoadingRegion>
        ))}
      {state.paymentToDelete && (
        <ConfirmDialog
          title={t('payment_deleteTitle')}
          description={t('payment_deleteBody')}
          confirmLabel={t('payment_deleteConfirm')}
          onConfirm={() => presenter.confirmPaymentDelete()}
          onDismiss={() => presenter.dismissPaymentDelete()}
        />
      )}
      {state.billToDelete && (
        <BillDeleteDialog
          deletion={state.billToDelete}
          isDeleting={state.isDeletingBill}
          money={(minor) => formatMoney(minor, state.header.currency, state.header.fractionDigits)}
          onConfirm={() => presenter.confirmExpenseDelete()}
          onDismiss={() => presenter.dismissExpenseDelete()}
        />
      )}
      {state.teach?.isPickingWord && <CategoryTeachDialog presenter={presenter} teach={state.teach} />}
      {membersPresenter && membersState && <MembersDialogs presenter={membersPresenter} state={membersState} />}
    </GroupPage>
  )
}

/** Despesas: "Você entrou como" right after joining, then the empty state or the bills and payments by day. */
function ExpensesTab({ presenter, state }: { presenter: GroupDetailPresenter; state: JsGroupDetailState }) {
  return (
    <>
      {state.joinedAs != null && <JoinedNotice presenter={presenter} name={state.joinedAs} showsProtectAccess={state.showsProtectAccess} />}
      {state.isLoadingBills ? (
        // Why: a restored account's bills may still be coming, so never "Nenhuma despesa ainda" before they did.
        <LoadingRegion isDelayed={false} isWaitingForInternet={state.isWaitingForInternet}>
          <SkeletonRows count={5} leading="square" className="rounded-xl border bg-card" />
        </LoadingRegion>
      ) : state.isEmpty ? (
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
    </>
  )
}

/** Right after joining through the link (3.3): "Você entrou como Bruno", and "Proteja seu acesso" while the account has no Google or Apple. */
function JoinedNotice({ presenter, name, showsProtectAccess }: { presenter: GroupDetailPresenter; name: string; showsProtectAccess: boolean }) {
  return (
    <>
      <section role="status" className="flex gap-3 rounded-xl border border-primary/30 bg-primary/10 p-4">
        <CircleCheck className="mt-0.5 size-5 shrink-0 text-primary" />
        <div>
          <p className="font-semibold">{t('groupDetail_joinedTitle', { name })}</p>
          <p className="text-sm text-muted-foreground">{t('groupDetail_joinedMessage')}</p>
        </div>
      </section>
      {showsProtectAccess && (
        <button type="button" onClick={() => presenter.protectAccess()} className="flex items-center gap-3 rounded-xl border bg-card p-4 text-left hover:bg-muted/60">
          <ShieldCheck className="size-5 shrink-0 text-primary" />
          <span className="flex-1">
            <span className="block font-semibold">{t('groupDetail_protectTitle')}</span>
            <span className="block text-sm text-muted-foreground">{t('groupDetail_protectMessage')}</span>
          </span>
          <ChevronRight className="size-5 text-muted-foreground" />
        </button>
      )}
    </>
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
        <span role="columnheader" className="sr-only">
          {t('groupDetail_columnOptions')}
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

/** One option of a line's ⋮: Conta's own for a bill, "Apagar" for a payment. */
interface DayRowOption {
  icon: ReactNode
  label: string
  isDestructive?: boolean
  onSelect: () => void
}

/**
 * A line of the day, a bill or a payment: its icon, title and meta (with "na fila" while it waits for the server), the
 * amount beside it from the tablet up (in the meta on the phone), the last column, and its ⋮, which a right click on
 * the line opens too.
 */
function DayRow({
  icon,
  title,
  meta,
  amount,
  isPending,
  trailing,
  options,
  onOpen,
}: {
  icon: ReactNode
  title: string
  meta: string
  amount: string
  isPending: boolean
  trailing: ReactNode
  options: DayRowOption[]
  onOpen: () => void
}) {
  const [isMenuOpen, setMenuOpen] = useState(false)
  return (
    <div
      role="row"
      tabIndex={0}
      onClick={onOpen}
      onContextMenu={(event) => {
        event.preventDefault()
        setMenuOpen(true)
      }}
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
              <span className={cn('inline-flex h-[18px] shrink-0 items-center gap-1 rounded-md border px-1.5 text-[11px] font-medium', pendingSurfaceClass)}>
                <PendingDot />
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
      <span role="cell" className="pl-4 text-right md:px-4">
        {trailing}
      </span>
      {/* Why: the ⋮ is its own control: its clicks and keys never reach the line, which opens the bill. */}
      <span role="cell" className="flex justify-center pr-1" onClick={(event) => event.stopPropagation()} onKeyDown={(event) => event.stopPropagation()}>
        <DropdownMenu open={isMenuOpen} onOpenChange={setMenuOpen}>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" aria-label={t('groupDetail_entryOptions', { name: title })}>
              <EllipsisVertical />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-52">
            {options.map((option) => (
              <DropdownMenuItem key={option.label} variant={option.isDestructive ? 'destructive' : 'default'} onSelect={option.onSelect}>
                {option.icon}
                {option.label}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
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
          names: payerNamesText(payers.map((it) => (it.isMe ? t('splitBill_you') : nameOrFormer(it.name)))),
        })
      : payer?.isMe
        ? t('groupDetail_metaPaidByMe', { time })
        : payer?.name != null
          ? t('groupDetail_metaPaidBy', { time, name: payer.name })
          : time
  const balance = expense.myBalanceMinor
  return (
    <DayRow
      icon={<CategoryBadge category={expense.category} className="size-full rounded-[inherit]" />}
      title={expense.description}
      meta={paid}
      amount={formatMoney(expense.totalMinor, currency, fractionDigits)}
      isPending={expense.isPending}
      onOpen={() => presenter.openExpense(expense.id)}
      options={[
        { icon: <Pencil />, label: t('groupDetail_entryEdit'), onSelect: () => presenter.editExpense(expense.id) },
        { icon: <Share2 />, label: t('groupDetail_entryShare'), onSelect: () => presenter.shareExpense(expense.id) },
        { icon: <Trash2 />, label: t('groupDetail_entryDelete'), isDestructive: true, onSelect: () => presenter.deleteExpense(expense.id) },
      ]}
      trailing={
        balance != null && (
          <span className={cn('font-semibold whitespace-nowrap tabular-nums', balanceTone(balance))}>{formatSignedMoney(balance, currency, fractionDigits)}</span>
        )
      }
    />
  )
}

/** "Ana te pagou · 12:10 · Acerto · Pix", the amount and the "acerto" tag; tapping it, or its ⋮'s "Apagar", asks to delete it. */
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
      options={[{ icon: <Trash2 />, label: t('groupDetail_entryDelete'), isDestructive: true, onSelect: () => presenter.openPayment(payment.id) }]}
      trailing={
        <span className="inline-flex h-5 items-center rounded-full border px-2 text-[11px] font-medium text-muted-foreground">{t('payment_tag')}</span>
      }
    />
  )
}
