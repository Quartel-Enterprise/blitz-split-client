import { ArrowLeft, ReceiptText, SearchX } from 'lucide-react'
import { AllExpensesPresenter, type JsAllExpensesState } from 'blitzsplit-shared'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { pageContentClass } from '@/components/HomeShell'
import { PageShell } from '@/components/HomeSidebar'
import { Button } from '@/components/ui/button'
import { LoadingRegion, SkeletonRows } from '@/components/ui/skeleton'
import { BillDeleteDialog } from '@/features/bill-detail/BillDeleteDialog'
import { toastGroupAction } from '@/features/group-detail/GroupExitDialog'
import { HomeBillListRow } from '@/features/home/HomeSections'
import { useEndReached } from '@/hooks/useEndReached'
import { AllExpensesFilterBar } from './AllExpensesFilterBar'
import { t } from '@/i18n'
import { formatDayLabel } from '@/lib/dates'
import { cn } from '@/lib/utils'

/**
 * "All expenses", which Home's "See all" opens over it: every bill you paid or take part in, across groups, by day, as
 * Home's list draws them, each with its ⋮ (a right click too). A page in the home's frame on every width, Home's tab
 * selected, one column (a long list a page at a time reads better as a page than in a dialog); the phone has its own
 * bar with back. The next page is asked for as the list's end scrolls into view.
 */
export function AllExpensesScreen() {
  const presenter = usePresenter(() => new AllExpensesPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsAllExpensesState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), toastGroupAction)
  const itemCount = state ? Array.from(state.days).reduce((count, day) => count + day.bills.length, 0) : 0
  const end = useEndReached(!!state?.hasMore && !state.showsMoreSkeleton, itemCount, () => presenter?.loadMore())
  if (!presenter || !state) return null

  return (
    <PageShell selected="home" phoneBars="nav">
      <header className="sticky top-0 z-10 flex h-14 items-center gap-2 border-b bg-background/95 px-2 backdrop-blur md:hidden">
        <Button variant="ghost" size="icon" aria-label={t('common_back')} onClick={() => presenter.back()}>
          <ArrowLeft />
        </Button>
        <h1 className="flex-1 text-lg font-semibold">{t('allExpenses_title')}</h1>
      </header>
      <main className={cn(pageContentClass, 'max-w-3xl gap-3 sm:gap-4')}>
        <div className="hidden items-center gap-2 md:flex">
          <Button variant="ghost" size="icon" aria-label={t('common_back')} onClick={() => presenter.back()}>
            <ArrowLeft />
          </Button>
          <h1 className="text-2xl font-semibold tracking-tight">{t('allExpenses_title')}</h1>
        </div>
        {!state.showsSkeleton && <AllExpensesFilterBar presenter={presenter} state={state} />}
        {state.showsSkeleton && (
          <LoadingRegion isDelayed={false}>
            <SkeletonRows count={6} leading="square" hasTrailing className="rounded-lg border bg-card" />
          </LoadingRegion>
        )}
        {state.showsEmpty && (
          <section className="flex flex-col items-center gap-2 rounded-xl border border-dashed px-6 py-10 text-center">
            <span aria-hidden className="flex size-11 items-center justify-center rounded-full bg-muted text-muted-foreground">
              <ReceiptText className="size-5" />
            </span>
            <h2 className="text-base font-semibold">{t('allExpenses_emptyTitle')}</h2>
            <p className="max-w-[420px] text-sm text-muted-foreground">{t('allExpenses_emptyBody')}</p>
          </section>
        )}
        {state.showsNoResults && (
          <section className="flex flex-col items-center gap-2 rounded-xl border border-dashed px-6 py-10 text-center">
            <span aria-hidden className="flex size-11 items-center justify-center rounded-full bg-muted text-muted-foreground">
              <SearchX className="size-5" />
            </span>
            <h2 className="text-base font-semibold">{t('allExpenses_noResultsTitle')}</h2>
            <p className="max-w-[420px] text-sm text-muted-foreground">{t('allExpenses_noResultsBody')}</p>
            <Button variant="outline" className="mt-2" onClick={() => presenter.clearFilters()}>
              {t('allExpenses_clear')}
            </Button>
          </section>
        )}
        {!state.showsSkeleton &&
          Array.from(state.days).map((day) => {
            // Why: sorted by amount the bills read as one list, with no day over them.
            const label = state.showsDayHeadings ? formatDayLabel(day.dayStartMillis, day.relativeDay) : t('allExpenses_title')
            return (
              <section key={day.dayStartMillis} aria-label={label} className="flex flex-col gap-1.5">
                {state.showsDayHeadings && <h2 className="px-1 text-[13px] font-medium text-muted-foreground">{label}</h2>}
                <ul className="divide-y overflow-hidden rounded-lg border bg-card">
                  {Array.from(day.bills).map((bill) => (
                    <HomeBillListRow key={bill.id} bill={bill} menu={presenter.billMenu} onOpen={() => presenter.openBill(bill.groupId, bill.id)} />
                  ))}
                </ul>
              </section>
            )
          })}
        {state.showsMoreSkeleton && (
          <LoadingRegion isDelayed={false}>
            <SkeletonRows count={2} leading="square" hasTrailing className="rounded-lg border bg-card" />
          </LoadingRegion>
        )}
        <div ref={end} aria-hidden className="h-px" />
      </main>
      {state.billToDelete && (
        <BillDeleteDialog
          deletion={state.billToDelete}
          isDeleting={state.isDeletingBill}
          onConfirm={() => presenter.billMenu.confirmDelete()}
          onDismiss={() => presenter.billMenu.dismissDelete()}
        />
      )}
    </PageShell>
  )
}
