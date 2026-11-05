import { EllipsisVertical } from 'lucide-react'
import type { JsExpenseRow } from 'blitzsplit-shared'
import { createContext, useContext, useState, type ReactNode } from 'react'
import { PendingDot, pendingSurfaceClass } from '@/components/common'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { balanceTone, nameOrFormer } from '@/features/balances/BalanceCard'
import { t } from '@/i18n'
import { formatDayLabel, formatTime } from '@/lib/dates'
import { formatMoney, formatSignedMoney } from '@/lib/money'
import { payerNamesText } from '@/lib/payers'
import { CategoryBadge } from '@/lib/categories'
import { cn } from '@/lib/utils'

// Why: the canvas's columns, the bill, its total, what it does to your balance and its ⋮; the phone folds the total into the meta.
const rowColumns =
  'grid grid-cols-[minmax(0,1fr)_auto_44px] md:grid-cols-[minmax(0,1fr)_120px_150px_52px] lg:grid-cols-[minmax(0,1fr)_160px_180px_52px]'

/** The line whose ⋮ is open, one for the whole table, so opening a line's menu (or right clicking it) closes any other. */
const OpenRowMenuContext = createContext<{ openId: string | null; setOpenId: (id: string | null) => void }>({
  openId: null,
  setOpenId: () => {},
})

/**
 * The table of a list of bills by day (the group's Expenses, a category's from the statistics): its column headers
 * from the tablet up, then each day's rows.
 */
export function DayRowsTable({ label, children }: { label: string; children: ReactNode }) {
  const [openId, setOpenId] = useState<string | null>(null)
  return (
    <OpenRowMenuContext.Provider value={{ openId, setOpenId }}>
      <div role="table" aria-label={label} className="flex flex-col overflow-hidden rounded-xl border bg-card">
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
        {children}
      </div>
    </OpenRowMenuContext.Provider>
  )
}

/** A day of the table: "Today", "Yesterday" or its date over its rows. */
export function DayRowGroup({ dayStartMillis, relativeDay, children }: { dayStartMillis: number; relativeDay: string; children: ReactNode }) {
  return (
    <div role="rowgroup" className="border-b last:border-b-0">
      <div role="row" className="flex h-8 items-center border-b bg-sidebar px-4 text-xs font-medium text-muted-foreground">
        <span role="rowheader">{formatDayLabel(dayStartMillis, relativeDay)}</span>
      </div>
      {children}
    </div>
  )
}

/** One option of a line's ⋮: Conta's own for a bill, "Apagar" for a payment. */
export interface DayRowOption {
  icon: ReactNode
  label: string
  isDestructive?: boolean
  onSelect: () => void
}

/**
 * A line of the day, a bill or a payment: its icon, title and meta (with "na fila" while it waits for the server), the
 * amount beside it from the tablet up (in the meta on the phone), the last column, and its ⋮, which a right click on
 * the line opens too. A list without options (a category's bills) keeps the ⋮'s column empty.
 */
export function DayRow({
  id,
  icon,
  title,
  meta,
  amount,
  isPending,
  trailing,
  options,
  onOpen,
}: {
  // Why: the entry's id, which says whether its ⋮ is the table's open one.
  id: string
  icon: ReactNode
  title: string
  meta: string
  amount: string
  isPending: boolean
  trailing: ReactNode
  options: DayRowOption[]
  onOpen: () => void
}) {
  const menu = useContext(OpenRowMenuContext)
  const isMenuOpen = menu.openId === id
  const setMenuOpen = (open: boolean) => menu.setOpenId(open ? id : null)
  const hasMenu = options.length > 0
  return (
    <div
      role="row"
      tabIndex={0}
      onClick={onOpen}
      onContextMenu={(event) => {
        if (!hasMenu) return
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
        {hasMenu && (
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
        )}
      </span>
    </div>
  )
}

/**
 * A bill's line: its category's badge, "21:30 · Bruno pagou", the total and what it does to your balance. extraMeta
 * follows who paid (a category's list names the bill's subcategory there).
 */
export function ExpenseLine({
  expense,
  currency,
  fractionDigits,
  options,
  extraMeta,
  onOpen,
}: {
  expense: JsExpenseRow
  currency: string
  fractionDigits: number
  options: DayRowOption[]
  extraMeta?: string
  onOpen: () => void
}) {
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
      id={expense.id}
      icon={<CategoryBadge category={expense.category} className="size-full rounded-[inherit]" />}
      title={expense.description}
      meta={extraMeta ? `${paid} · ${extraMeta}` : paid}
      amount={formatMoney(expense.totalMinor, currency, fractionDigits)}
      isPending={expense.isPending}
      onOpen={onOpen}
      options={options}
      trailing={
        balance != null && (
          <span className={cn('font-semibold whitespace-nowrap tabular-nums', balanceTone(balance))}>{formatSignedMoney(balance, currency, fractionDigits)}</span>
        )
      }
    />
  )
}
