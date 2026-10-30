import { ArrowLeft, ChevronRight, Ellipsis, LogOut, Pencil, Plus, ReceiptText, Trash2, UserPlus } from 'lucide-react'
import { GroupDetailPresenter, type JsExpenseDay, type JsExpenseRow, type JsGroupDetailState, type JsRoute } from 'blitzsplit-shared'
import { useEffect } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar, GroupTabs } from '@/components/common'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { t } from '@/i18n'
import { formatDayLabel, formatTime } from '@/lib/dates'
import { formatMoney, formatSignedMoney } from '@/lib/money'
import { cn } from '@/lib/utils'

// Why: the canvas's columns, the bill, its total and what it does to your balance; the phone folds the total into the meta.
const rowColumns = 'grid grid-cols-[minmax(0,1fr)_auto] md:grid-cols-[minmax(0,1fr)_120px_150px] lg:grid-cols-[minmax(0,1fr)_160px_180px]'

/**
 * "Despesas do grupo": the group's bills by day, newest first, with "Rachar conta" a primary button beside the
 * group's name (at the bottom on the phone), and the group's tabs, Despesas selected. A bill opens "Conta" over it,
 * and a bill someone else deleted while it was open says so here.
 */
export function GroupDetailScreen({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new GroupDetailPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsGroupDetailState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), (action) => {
    if (action === 'coming-soon') toast(t('groups_comingSoon'))
  })
  // Why: state, not an action, so it waits for this screen while the bill's panel still covers it.
  const showsBillDeletedNotice = state?.showsBillDeletedNotice === true
  useEffect(() => {
    if (!showsBillDeletedNotice) return
    toast(t('groupDetail_billDeleted'))
    presenter?.billDeletedNoticeShown()
  }, [presenter, showsBillDeletedNotice])
  if (!presenter || !state?.isLoaded) return null

  const money = (minor: number) => formatMoney(minor, state.currency, state.fractionDigits)
  const members = t(state.memberCount === 1 ? 'members_countOne' : 'members_countOther', { count: state.memberCount })
  const meta = state.isEmpty ? t('groupDetail_metaEmpty', { members }) : t('groupDetail_metaTotal', { members, total: money(state.totalSpentMinor) })
  const notJoined =
    state.notJoinedCount > 0 ? t(state.notJoinedCount === 1 ? 'groupDetail_notJoinedOne' : 'groupDetail_notJoinedOther', { count: state.notJoinedCount }) : null

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="sticky top-0 z-10 flex h-14 items-center gap-1 border-b bg-background/95 px-2 backdrop-blur sm:px-4">
        <Button variant="ghost" size="icon" aria-label={t('common_back')} onClick={() => presenter.back()}>
          <ArrowLeft />
        </Button>
        {/* Why: the page names the group in its heading from the tablet up; the phone's bar names it instead. */}
        <span className="mr-auto truncate font-semibold md:hidden">{state.groupName}</span>
        <nav aria-label={t('groupDetail_breadcrumb')} className="mr-auto hidden items-center gap-1.5 text-sm md:flex">
          <button type="button" onClick={() => presenter.back()} className="rounded-sm text-muted-foreground outline-none hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring">
            {t('groups_title')}
          </button>
          <ChevronRight aria-hidden className="size-3.5 text-muted-foreground" />
          <span aria-current="page" className="truncate font-medium">
            {state.groupName}
          </span>
        </nav>
        <Button variant="ghost" size="icon" aria-label={t('groupDetail_invitePeople')} onClick={() => presenter.invite()} className="md:hidden">
          <UserPlus />
        </Button>
        {/* Why: the ⋯ menu keeps only what changes the group itself (6.1i). */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" aria-label={t('groupDetail_moreOptions')}>
              <Ellipsis />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-52">
            <DropdownMenuItem onSelect={() => presenter.editGroup()}>
              <Pencil />
              {t('groupDetail_editGroup')}
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onSelect={() => presenter.leaveGroup()}>
              <LogOut />
              {t('groupDetail_leaveGroup')}
            </DropdownMenuItem>
            <DropdownMenuItem variant="destructive" onSelect={() => presenter.deleteGroup()}>
              <Trash2 />
              {t('groupDetail_deleteGroup')}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </header>

      <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-4 p-4 sm:gap-5 md:px-8 md:py-7 lg:px-10 lg:py-8">
        <div className="flex items-start gap-4">
          <GroupAvatar initials={state.groupInitials} color={state.groupColor} className="size-12 rounded-xl text-base md:size-14 md:text-lg" />
          <div className="flex min-w-0 flex-1 flex-col gap-1.5">
            <h1 className="sr-only text-2xl leading-[30px] font-semibold tracking-tight md:not-sr-only md:truncate">{state.groupName}</h1>
            <span className="text-sm text-muted-foreground">{meta}</span>
            <button
              type="button"
              onClick={() => presenter.members()}
              aria-label={[members, notJoined, t('groupDetail_seeMembers')].filter(Boolean).join(' · ')}
              className="flex items-center gap-2 self-start rounded-md text-[13px] text-muted-foreground outline-none hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring"
            >
              <span aria-hidden className="flex">
                {state.faces.map((face, index) => (
                  <Avatar key={index} initial={face.initials} color={face.color} className={cn('size-7 border-2 border-background text-[11px]', index > 0 && '-ml-1.5')} />
                ))}
                {state.extraMemberCount > 0 && (
                  <span className="-ml-1.5 flex h-7 min-w-7 items-center justify-center rounded-full border-2 border-background bg-muted px-1.5 text-[11px] font-semibold">
                    +{state.extraMemberCount}
                  </span>
                )}
              </span>
              {notJoined}
            </button>
          </div>
          <div className="hidden items-center gap-2 md:flex">
            <Button variant="outline" onClick={() => presenter.invite()}>
              <UserPlus />
              {t('groupDetail_invite')}
            </Button>
            {state.showsSplitBillButton && (
              <Button onClick={() => presenter.splitBill()}>
                <Plus />
                {t('groupDetail_splitBill')}
              </Button>
            )}
          </div>
        </div>

        <GroupTabs
          selected="expenses"
          onSelect={(tab) => {
            if (tab === 'balances') presenter.balances()
            else if (tab === 'members') presenter.members()
          }}
        />

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
      </main>

      {state.showsSplitBillButton && (
        <div className="sticky bottom-0 border-t bg-card px-4 py-3 md:hidden">
          <Button onClick={() => presenter.splitBill()} className="h-11 w-full text-[15px]">
            <Plus />
            {t('groupDetail_splitBill')}
          </Button>
        </div>
      )}
    </div>
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
      {day.expenses.map((expense) => (
        <ExpenseRowItem key={expense.id} presenter={presenter} expense={expense} state={state} />
      ))}
    </div>
  )
}

function ExpenseRowItem({ presenter, expense, state }: { presenter: GroupDetailPresenter; expense: JsExpenseRow; state: JsGroupDetailState }) {
  const total = formatMoney(expense.totalMinor, state.currency, state.fractionDigits)
  const time = formatTime(expense.spentAt)
  const paid = expense.payerIsMe
    ? t('groupDetail_metaPaidByMe', { time })
    : expense.payerName != null
      ? t('groupDetail_metaPaidBy', { time, name: expense.payerName })
      : time
  const balance = expense.myBalanceMinor
  return (
    <div
      role="row"
      tabIndex={0}
      onClick={() => presenter.openExpense(expense.id)}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault()
          presenter.openExpense(expense.id)
        }
      }}
      className={cn(
        rowColumns,
        'min-h-[60px] cursor-pointer items-center border-b outline-none last:border-b-0 hover:bg-accent/60 focus-visible:ring-[3px] focus-visible:ring-ring focus-visible:ring-inset md:min-h-14',
      )}
    >
      <span role="cell" className="flex min-w-0 items-center gap-3 px-4 py-2">
        <span aria-hidden className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-muted text-primary">
          <ReceiptText className="size-4" />
        </span>
        <span className="flex min-w-0 flex-col">
          <span className="truncate font-medium">{expense.description}</span>
          <span className="flex min-w-0 items-center gap-1.5 text-xs whitespace-nowrap text-muted-foreground">
            {expense.isPending && (
              <span className="inline-flex h-[18px] shrink-0 items-center gap-1 rounded-md border border-amber-200 bg-amber-50 px-1.5 text-[11px] font-medium text-amber-800 dark:border-amber-900 dark:bg-amber-950/60 dark:text-amber-300">
                <span aria-hidden className="size-1.5 rounded-full bg-orange-500" />
                {t('groupDetail_pending')}
              </span>
            )}
            <span className="truncate">
              {paid}
              <span className="md:hidden"> · {total}</span>
            </span>
          </span>
        </span>
      </span>
      <span role="cell" className="hidden px-4 text-right tabular-nums md:block">
        {total}
      </span>
      <span role="cell" className="px-4 text-right">
        {balance != null && (
          <span
            className={cn(
              'font-semibold whitespace-nowrap tabular-nums',
              balance > 0 ? 'text-primary' : balance < 0 ? 'text-destructive' : 'text-muted-foreground',
            )}
          >
            {formatSignedMoney(balance, state.currency, state.fractionDigits)}
          </span>
        )}
      </span>
    </div>
  )
}
