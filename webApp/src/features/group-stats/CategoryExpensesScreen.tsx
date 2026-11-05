import { ArrowLeft, Check, ChevronRight } from 'lucide-react'
import { CategoryExpensesPresenter, type JsCategoryExpensesState, type JsRoute } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { groupPageContentClass, HomeTabCrumb, type HomeTab } from '@/components/HomeShell'
import { PageShell, useOpenHomeTab } from '@/components/HomeSidebar'
import { Button } from '@/components/ui/button'
import { SkeletonRows } from '@/components/ui/skeleton'
import { DayRowGroup, DayRowsTable, ExpenseLine } from '@/features/group-detail/ExpenseRows'
import { GroupPageSkeleton } from '@/features/group-detail/GroupPage'
import { t } from '@/i18n'
import { avatarStyle } from '@/lib/avatar'
import { categoryName } from '@/lib/categories'
import { formatMoney } from '@/lib/money'
import { cn } from '@/lib/utils'
import { expenseCountText, formatRange, periodLabel } from './statsFormat'

/**
 * "Expenses by category", from a category of the statistics' "By category": the group's bills of the period the
 * statistics showed, of the main categories its chips pick (all of them while none is), in the group's Expenses table
 * with no ⋮ (as the home's latest expenses, a click opens the bill, whose options are in Bill). A page of its own over
 * the statistics, as they are over the group; back goes to them.
 */
export function CategoryExpensesScreen({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new CategoryExpensesPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsCategoryExpensesState) => void) => p.observeState(onChange))
  const openHomeTab = useOpenHomeTab()
  if (!presenter || !state) return null
  const homeTab = state.homeTab as HomeTab
  if (!state.isLoaded) {
    return (
      <GroupPageSkeleton groupId={route.groupId ?? ''} homeTab={homeTab} onBack={() => presenter.back()}>
        <SkeletonRows count={5} leading="square" className="rounded-xl border bg-card" />
      </GroupPageSkeleton>
    )
  }

  const period =
    state.periodStartMillis != null && state.periodEndMillis != null
      ? formatRange(state.periodRangeStyle, state.periodStartMillis, state.periodEndMillis)
      : periodLabel('all', state.isTrip)
  const subtitle = `${state.groupName} · ${period}`

  return (
    <PageShell selected={homeTab} groups={{ selectedId: route.groupId ?? '' }} phoneBars="none">
      <div className="flex min-h-dvh flex-col">
        <header className="sticky top-0 z-10 flex h-14 items-center gap-1 border-b bg-background/95 px-2 backdrop-blur md:hidden">
          <Button variant="ghost" size="icon" aria-label={t('categoryExpenses_back')} onClick={() => presenter.back()}>
            <ArrowLeft />
          </Button>
          <span className="flex min-w-0 flex-1 flex-col">
            <span className="truncate font-semibold">{t('categoryExpenses_title')}</span>
            <span className="truncate text-xs text-muted-foreground">{subtitle}</span>
          </span>
        </header>

        <main className={cn(groupPageContentClass, 'gap-4 sm:gap-5')}>
          <nav aria-label={t('groupDetail_breadcrumb')} className="-mb-1 hidden items-center gap-1.5 text-[13px] md:flex">
            <HomeTabCrumb tab={homeTab} onOpen={openHomeTab} />
            <span className="truncate text-muted-foreground">{state.groupName}</span>
            <ChevronRight aria-hidden className="size-3.5 text-muted-foreground" />
            <button
              type="button"
              onClick={() => presenter.back()}
              className="truncate rounded-sm text-muted-foreground outline-none hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring"
            >
              {t('stats_title')}
            </button>
            <ChevronRight aria-hidden className="size-3.5 text-muted-foreground" />
            <span aria-current="page">{t('categoryExpenses_title')}</span>
          </nav>
          <div className="hidden min-w-0 flex-col gap-1 md:flex">
            <h1 className="text-2xl leading-[30px] font-semibold tracking-tight">{t('categoryExpenses_title')}</h1>
            <span className="text-sm text-muted-foreground">{subtitle}</span>
          </div>

          <CategoryChips state={state} presenter={presenter} />
          <p aria-live="polite" className="text-[15px] font-semibold tabular-nums">
            {t('categoryExpenses_summary', {
              total: formatMoney(state.totalMinor, state.currency, state.fractionDigits),
              count: expenseCountText(state.expenseCount),
            })}
          </p>

          {state.isEmpty ? (
            <div className="flex flex-col items-center gap-2 rounded-xl border bg-card px-6 py-12 text-center">
              <p className="font-semibold">{t('categoryExpenses_emptyTitle')}</p>
              <p className="max-w-sm text-sm text-muted-foreground">{t('categoryExpenses_emptyBody')}</p>
            </div>
          ) : (
            <DayRowsTable label={t('categoryExpenses_title')}>
              {state.days.map((day) => (
                <DayRowGroup key={day.dayStartMillis} dayStartMillis={day.dayStartMillis} relativeDay={day.relativeDay}>
                  {day.expenses.map((expense) => (
                    <ExpenseLine
                      key={expense.id}
                      expense={expense}
                      currency={state.currency}
                      fractionDigits={state.fractionDigits}
                      extraMeta={categoryName(expense.category)}
                      options={[]}
                      onOpen={() => presenter.openExpense(expense.id)}
                    />
                  ))}
                </DayRowGroup>
              ))}
            </DayRowsTable>
          )}
        </main>
      </div>
    </PageShell>
  )
}

/** The period's main categories as toggles, the picked ones filled with a check, then "Clear" while one is picked. */
function CategoryChips({ state, presenter }: { state: JsCategoryExpensesState; presenter: CategoryExpensesPresenter }) {
  return (
    <div role="group" aria-label={t('categoryExpenses_filters')} className="flex flex-wrap items-center gap-2">
      {state.chips.map((chip) => (
        <button
          key={chip.category.id}
          type="button"
          aria-pressed={chip.isSelected}
          onClick={() => presenter.toggleCategory(chip.category.id)}
          className={cn(
            'inline-flex h-8 items-center gap-1.5 rounded-full border px-3 text-[13px] font-medium outline-none transition-colors focus-visible:ring-[3px] focus-visible:ring-ring',
            chip.isSelected ? 'border-transparent bg-primary/15 text-primary' : 'bg-card hover:bg-accent/60',
          )}
        >
          {chip.isSelected ? (
            <Check aria-hidden className="size-3.5" />
          ) : (
            <span aria-hidden style={avatarStyle(chip.category.color)} className="avatar size-2.5 rounded-full" />
          )}
          {categoryName(chip.category)}
        </button>
      ))}
      {state.showsClear && (
        <Button variant="ghost" size="sm" onClick={() => presenter.clear()}>
          {t('categoryExpenses_clear')}
        </Button>
      )}
    </div>
  )
}
