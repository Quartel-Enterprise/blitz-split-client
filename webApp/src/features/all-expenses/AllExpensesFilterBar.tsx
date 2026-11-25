import { ArrowDownUp, Check, ChevronDown, Search, X } from 'lucide-react'
import type { AllExpensesPresenter, JsAllExpensesState } from 'blitzsplit-shared'
import { Fragment, useState, type ReactNode } from 'react'
import { PickDatesDialog } from '@/components/PickDatesDialog'
import { Avatar } from '@/components/common'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { formatRange } from '@/features/group-stats/statsFormat'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { CategoryBadge, categoryName } from '@/lib/categories'
import { cn } from '@/lib/utils'

const sortLabels: Record<string, MessageKey> = {
  NEWEST: 'allExpenses_sortNewest',
  OLDEST: 'allExpenses_sortOldest',
  LARGEST: 'allExpenses_sortLargest',
  SMALLEST: 'allExpenses_sortSmallest',
}

const shareLabels: Record<string, MessageKey> = {
  ALL: 'allExpenses_shareAll',
  TO_RECEIVE: 'allExpenses_shareReceive',
  TO_PAY: 'allExpenses_sharePay',
}

// Why: the statistics' periods, in their words.
const periodLabels: Record<string, MessageKey> = {
  ALL: 'stats_periodAll',
  THIS_YEAR: 'stats_periodThisYear',
  THIS_MONTH: 'stats_periodThisMonth',
  LAST_MONTH: 'stats_periodLastMonth',
  CUSTOM: 'stats_periodCustom',
}

/** "Group" with nothing picked, the name of the one picked, "Group · 2" with several. */
function pickedLabel(label: string, names: string[]): string {
  if (names.length === 1) return names[0]
  return names.length > 1 ? `${label} · ${names.length}` : label
}

/**
 * Over "All expenses"' bills (Pierre's call, beta feedback): the search, which finds bills by their description as the
 * home's search does, then the order and the filters (in filterOrder: what the statistics picked comes first), each a
 * menu, a filter's button filled while it narrows the list, and "Clear filters" once one does. The row wraps on a
 * phone.
 */
export function AllExpensesFilterBar({ presenter, state }: { presenter: AllExpensesPresenter; state: JsAllExpensesState }) {
  const groupIds = Array.from(state.groupIds)
  const categoryIds = Array.from(state.categoryIds)
  const groups = Array.from(state.groupOptions)
  const categories = Array.from(state.categoryOptions)
  const payerKeys = Array.from(state.payerKeys)
  const payers = Array.from(state.payerOptions)
  const [query, setQuery] = useDraft(state.query, (next) => presenter.search(next))
  const [isPickingDates, setPickingDates] = useState(false)
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const chip = (filter: string) => {
    switch (filter) {
      case 'SORT':
        return (
          <FilterMenu label={t(sortLabels[state.sort])} icon={<ArrowDownUp />} isActive={false} ariaLabel={t('allExpenses_sort')}>
            {Object.keys(sortLabels).map((sort) => (
              <CheckedItem key={sort} label={t(sortLabels[sort])} isChecked={state.sort === sort} onSelect={() => presenter.pickSort(sort)} />
            ))}
          </FilterMenu>
        )
      case 'GROUP':
        return (
          <FilterMenu
            label={pickedLabel(
              t('allExpenses_group'),
              groups.filter((group) => groupIds.includes(group.id)).map((group) => group.name),
            )}
            isActive={groupIds.length > 0}
          >
            {groups.map((group) => (
              <CheckedItem
                key={group.id}
                label={group.name}
                leading={<GroupAvatar initials={group.initials} color={group.color} photoUrl={group.photoUrl} className="size-5 text-[7px]" />}
                isChecked={groupIds.includes(group.id)}
                keepsOpen
                onSelect={() => presenter.toggleGroup(group.id)}
              />
            ))}
          </FilterMenu>
        )
      case 'CATEGORY':
        return (
          <FilterMenu
            label={pickedLabel(
              t('allExpenses_category'),
              categories.filter((category) => categoryIds.includes(category.id)).map(categoryName),
            )}
            isActive={categoryIds.length > 0}
          >
            {categories.map((category) => (
              <CheckedItem
                key={category.id}
                label={categoryName(category)}
                leading={<CategoryBadge category={category} className="size-5 rounded-md [&_svg]:size-3" />}
                isChecked={categoryIds.includes(category.id)}
                keepsOpen
                onSelect={() => presenter.toggleCategory(category.id)}
              />
            ))}
          </FilterMenu>
        )
      case 'SHARE':
        return (
          <FilterMenu label={state.share === 'ALL' ? t('allExpenses_share') : t(shareLabels[state.share])} isActive={state.share !== 'ALL'}>
            {Object.keys(shareLabels).map((share) => (
              <CheckedItem key={share} label={t(shareLabels[share])} isChecked={state.share === share} onSelect={() => presenter.pickShare(share)} />
            ))}
          </FilterMenu>
        )
      case 'PAYER':
        return (
          <FilterMenu label={payerText(payers.filter((payer) => payerKeys.includes(payer.key)), payerKeys.length)} isActive={payerKeys.length > 0}>
            <CheckedItem label={t('allExpenses_payerAll')} isChecked={payerKeys.length === 0} onSelect={() => presenter.pickAnyPayer()} />
            <DropdownMenuSeparator />
            {payers.map((payer) => (
              <CheckedItem
                key={payer.key}
                label={payer.isMe ? t('allExpenses_payerMe') : payer.name}
                leading={<Avatar initial={payer.initials} color={payer.color} photoUrl={payer.photoUrl} className="size-5 text-[9px]" />}
                isChecked={payerKeys.includes(payer.key)}
                keepsOpen
                onSelect={() => presenter.togglePayer(payer.key)}
              />
            ))}
          </FilterMenu>
        )
      case 'PERIOD':
        return (
          <FilterMenu label={periodText(state)} isActive={state.period !== 'ALL'}>
            {Object.keys(periodLabels).map((period) => (
              <CheckedItem
                key={period}
                label={t(periodLabels[period])}
                isChecked={state.period === period}
                onSelect={() => (period === 'CUSTOM' ? setPickingDates(true) : presenter.pickPeriod(period))}
              />
            ))}
          </FilterMenu>
        )
      default:
        return null
    }
  }
  return (
    <div className="flex flex-col gap-2.5">
      <div className="relative">
        <Search className="pointer-events-none absolute top-1/2 inset-s-3 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          type="search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder={t('allExpenses_search')}
          aria-label={t('allExpenses_search')}
          className="ps-9"
        />
      </div>
      <div className="flex flex-wrap items-center gap-2">
        {Array.from(state.filterOrder).map((filter) => (
          <Fragment key={filter}>{chip(filter)}</Fragment>
        ))}
        {state.isFiltering && (
          <Button variant="ghost" size="sm" onClick={() => presenter.clearFilters()}>
            <X /> {t('allExpenses_clear')}
          </Button>
        )}
      </div>
      {isPickingDates && (
        <PickDatesDialog
          isDesktop={isDesktop}
          description={t('allExpenses_pickDatesBody')}
          initialStart={state.customStartMillis ?? Date.now()}
          initialEnd={state.customEndMillis ?? Date.now()}
          onPick={(start, end) => {
            setPickingDates(false)
            presenter.pickCustomPeriod(start, end)
          }}
          onClose={() => setPickingDates(false)}
        />
      )}
    </div>
  )
}

/** "Who paid" with nobody picked, "You paid" or "Paid by Ana" with one, "Who paid · 2" with several. */
function payerText(picked: { name: string; isMe: boolean }[], count: number): string {
  if (count === 1 && picked.length === 1) {
    return picked[0].isMe ? t('allExpenses_payerYou') : t('allExpenses_payerBy', { name: picked[0].name })
  }
  return count > 1 ? `${t('allExpenses_payer')} · ${count}` : t('allExpenses_payer')
}

/** "Period" while it is the whole history, a custom period's days ("Oct 24 – 26"), any other by its name. */
function periodText(state: JsAllExpensesState): string {
  if (state.period === 'ALL') return t('stats_period')
  if (state.period === 'CUSTOM' && state.customStartMillis != null && state.customEndMillis != null) {
    return formatRange('days', state.customStartMillis, state.customEndMillis)
  }
  return t(periodLabels[state.period])
}

function FilterMenu({ label, icon, isActive, ariaLabel, children }: { label: string; icon?: ReactNode; isActive: boolean; ariaLabel?: string; children: ReactNode }) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant={isActive ? 'secondary' : 'outline'} size="sm" aria-label={ariaLabel ? `${ariaLabel}: ${label}` : undefined} className={cn('max-w-56', isActive && 'border border-primary/40')}>
          {icon}
          <span className="truncate">{label}</span>
          <ChevronDown className="opacity-60" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="max-h-80 w-60 overflow-y-auto">
        {children}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

/** One option; a group, a category or a person keeps the menu open, since several may be picked. */
function CheckedItem({ label, leading, isChecked, keepsOpen, onSelect }: { label: string; leading?: ReactNode; isChecked: boolean; keepsOpen?: boolean; onSelect: () => void }) {
  return (
    <DropdownMenuItem
      role="menuitemcheckbox"
      aria-checked={isChecked}
      onSelect={(event) => {
        if (keepsOpen) event.preventDefault()
        onSelect()
      }}
    >
      {leading}
      <span className="flex-1 truncate">{label}</span>
      {isChecked && <Check className="ms-auto" />}
    </DropdownMenuItem>
  )
}
