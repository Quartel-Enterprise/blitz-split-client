import { ChevronRight, CircleCheck, HandCoins, Pencil, ReceiptText, Share2, ShieldCheck, Trash2 } from 'lucide-react'
import {
  GroupDetailPresenter,
  type JsExpenseRow,
  type JsGroupDetailState,
  type JsPaymentRow,
  type JsRoute,
} from 'blitzsplit-shared'
import { useContext, useEffect } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import type { GroupTab } from '@/components/common'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { PageKeyContext, type HomeTab } from '@/components/HomeShell'
import { Button } from '@/components/ui/button'
import { LoadingRegion, SkeletonRows } from '@/components/ui/skeleton'
import { nameOrFormer, paymentMethodLabel, useRecordedPaymentNotice } from '@/features/balances/BalanceCard'
import { BalancesTab } from '@/features/balances/BalancesTab'
import { BillDeleteDialog } from '@/features/bill-detail/BillDeleteDialog'
import { MembersDialogs, MembersTab, useMembers } from '@/features/members/MembersTab'
import { useSimplifyConflictNotice } from '@/features/simplify-debts/simplify'
import { t } from '@/i18n'
import { formatTime } from '@/lib/dates'
import { formatMoney } from '@/lib/money'
import { CategoryTeachDialog, useCategoryTeachNotice } from './CategoryTeach'
import { toastGroupAction } from './GroupExitDialog'
import { GroupPage, GroupPageSkeleton } from './GroupPage'
import { pickedTabOf, rememberPickedTab } from './pickedTabs'
import { DayRow, DayRowGroup, DayRowsTable, ExpenseLine } from './ExpenseRows'

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
    <DayRowsTable label={t('group_tabExpenses')}>
      {state.days.map((day) => (
        <DayRowGroup key={day.dayStartMillis} dayStartMillis={day.dayStartMillis} relativeDay={day.relativeDay}>
          {day.entries.map((entry) =>
            entry.expense ? (
              <ExpenseRowItem key={entry.id} presenter={presenter} expense={entry.expense} state={state} />
            ) : entry.payment ? (
              <PaymentRowItem key={entry.id} presenter={presenter} payment={entry.payment} state={state} />
            ) : null,
          )}
        </DayRowGroup>
      ))}
    </DayRowsTable>
  )
}

/** A bill's line, its ⋮ offering Conta's Editar, Compartilhar and Apagar without opening it. */
function ExpenseRowItem({ presenter, expense, state }: { presenter: GroupDetailPresenter; expense: JsExpenseRow; state: JsGroupDetailState }) {
  return (
    <ExpenseLine
      expense={expense}
      currency={state.header.currency}
      fractionDigits={state.header.fractionDigits}
      onOpen={() => presenter.openExpense(expense.id)}
      options={[
        { icon: <Pencil />, label: t('groupDetail_entryEdit'), onSelect: () => presenter.editExpense(expense.id) },
        { icon: <Share2 />, label: t('groupDetail_entryShare'), onSelect: () => presenter.shareExpense(expense.id) },
        { icon: <Trash2 />, label: t('groupDetail_entryDelete'), isDestructive: true, onSelect: () => presenter.askToDeleteExpense(expense.id) },
      ]}
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
      id={payment.id}
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
