import { ArrowDownUp, Check, ChevronDown, Search, X } from 'lucide-react'
import type { AllExpensesPresenter, JsAllExpensesState } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
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

const payerLabels: Record<string, MessageKey> = {
  ALL: 'allExpenses_payerAll',
  YOU: 'allExpenses_payerYou',
  OTHERS: 'allExpenses_payerOthers',
}

/** "Group" with nothing picked, the name of the one picked, "Group · 2" with several. */
function pickedLabel(label: string, names: string[]): string {
  if (names.length === 1) return names[0]
  return names.length > 1 ? `${label} · ${names.length}` : label
}

/**
 * Over "All expenses"' bills (Pierre's call, beta feedback): the search, which finds bills by their description as the
 * home's search does, then the order and the filters, each a menu, a filter's button filled while it narrows the list,
 * and "Clear filters" once one does. The row wraps on a phone.
 */
export function AllExpensesFilterBar({ presenter, state }: { presenter: AllExpensesPresenter; state: JsAllExpensesState }) {
  const groupIds = Array.from(state.groupIds)
  const categoryIds = Array.from(state.categoryIds)
  const groups = Array.from(state.groupOptions)
  const categories = Array.from(state.categoryOptions)
  return (
    <div className="flex flex-col gap-2.5">
      <div className="relative">
        <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          type="search"
          value={state.query}
          onChange={(event) => presenter.search(event.target.value)}
          placeholder={t('allExpenses_search')}
          aria-label={t('allExpenses_search')}
          className="pl-9"
        />
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <FilterMenu label={t(sortLabels[state.sort])} icon={<ArrowDownUp />} isActive={false} ariaLabel={t('allExpenses_sort')}>
          {Object.keys(sortLabels).map((sort) => (
            <CheckedItem key={sort} label={t(sortLabels[sort])} isChecked={state.sort === sort} onSelect={() => presenter.pickSort(sort)} />
          ))}
        </FilterMenu>
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
        <FilterMenu label={state.share === 'ALL' ? t('allExpenses_share') : t(shareLabels[state.share])} isActive={state.share !== 'ALL'}>
          {Object.keys(shareLabels).map((share) => (
            <CheckedItem key={share} label={t(shareLabels[share])} isChecked={state.share === share} onSelect={() => presenter.pickShare(share)} />
          ))}
        </FilterMenu>
        <FilterMenu label={state.payer === 'ALL' ? t('allExpenses_payer') : t(payerLabels[state.payer])} isActive={state.payer !== 'ALL'}>
          {Object.keys(payerLabels).map((payer) => (
            <CheckedItem key={payer} label={t(payerLabels[payer])} isChecked={state.payer === payer} onSelect={() => presenter.pickPayer(payer)} />
          ))}
        </FilterMenu>
        {state.isFiltering && (
          <Button variant="ghost" size="sm" onClick={() => presenter.clearFilters()}>
            <X /> {t('allExpenses_clear')}
          </Button>
        )}
      </div>
    </div>
  )
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

/** One option; a group or a category keeps the menu open, since several may be picked. */
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
      {isChecked && <Check className="ml-auto" />}
    </DropdownMenuItem>
  )
}
