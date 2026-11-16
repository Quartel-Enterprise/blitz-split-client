import { ChevronRight } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { JsGroupStatsState, JsStatsCategory } from 'blitzsplit-shared'
import { t } from '@/i18n'
import { categoryStyle } from '@/lib/avatar'
import { CategoryBadge, categoryName } from '@/lib/categories'
import { cn } from '@/lib/utils'
import { StatsRing, StatsRingCenter } from './StatsRing'

/**
 * "Por categoria" (6.6): the ring of slices in each main category's fixed color, the largest in its center and the
 * legend with the percentages, then each main category with its bar. The slice the mouse is over (or its legend line,
 * hovered or focused) pops out, the others fade, and the center names it with its share; a click or a tap on a slice,
 * a legend line or a row opens the category's bills ("Expenses by category"). The ring sweeps in again when the
 * period changes.
 */
export function CategoriesCard({
  state,
  money,
  className,
  onOpen,
}: {
  state: JsGroupStatsState
  money: (minor: number) => string
  className: string
  // Why: the main category clicked and what of the card it was: "ring", "legend" or "row".
  onOpen: (categoryId: string, target: string) => void
}) {
  const rows = state.categories
  const ringKey = `${state.periodStartMillis}-${state.periodEndMillis}-${rows.map((row) => `${row.category.id}:${row.percent}`).join(',')}`
  const [selected, setSelected] = useState<number | null>(null)

  useEffect(() => setSelected(null), [ringKey])

  if (rows.length === 0) return null
  const top = (selected !== null ? rows[selected] : undefined) ?? rows[0]
  const description = t('groupStats_categoryPie', { parts: rows.map((row) => `${categoryName(row.category)} ${row.percent}%`).join(', ') })
  return (
    <section aria-labelledby="statsCategories" className={className}>
      <h2 id="statsCategories" className="text-[15px] font-semibold">
        {t('groupStats_byCategory')}
      </h2>
      <div className="flex items-center gap-5">
        <StatsRing
          slices={rows.map((row) => ({ id: row.category.id, percent: row.percent, color: row.category.color }))}
          ringKey={ringKey}
          label={description}
          selected={selected}
          size={132}
          onPick={setSelected}
          onTap={(index) => onOpen(rows[index].category.id, 'ring')}
        >
          <StatsRingCenter percent={top.percent} name={categoryName(top.category)} />
        </StatsRing>
        <ul className="flex min-w-0 flex-col gap-0.5 text-[13px]">
          {rows.map((row, index) => (
            <li key={row.category.id} className="flex min-w-0">
              <button
                type="button"
                aria-label={t('groupStats_categoryOpen', { name: categoryName(row.category), percent: row.percent })}
                onClick={() => onOpen(row.category.id, 'legend')}
                onFocus={() => setSelected(index)}
                onBlur={() => setSelected(null)}
                onPointerEnter={(event) => {
                  if (event.pointerType === 'mouse') setSelected(index)
                }}
                onPointerLeave={(event) => {
                  if (event.pointerType === 'mouse') setSelected(null)
                }}
                className={cn(
                  'flex min-w-0 items-center gap-2 rounded-md px-1 py-0.5 text-left outline-none transition-opacity focus-visible:ring-[3px] focus-visible:ring-ring',
                  selected !== null && selected !== index && 'opacity-40',
                )}
              >
                <span style={categoryStyle(row.category.color)} className="avatar-mark size-3 shrink-0 rounded-[4px]" />
                <span className="truncate">{categoryName(row.category)}</span>
                <span className="text-muted-foreground tabular-nums">{row.percent}%</span>
              </button>
            </li>
          ))}
        </ul>
      </div>
      <ul className="flex flex-col gap-3">
        {rows.map((row) => (
          <CategoryLine key={row.category.id} row={row} money={money} onOpen={() => onOpen(row.category.id, 'row')} />
        ))}
      </ul>
    </section>
  )
}

// Why: a main category's row, a button that opens its bills, its chevron saying so.
function CategoryLine({ row, money, onOpen }: { row: JsStatsCategory; money: (minor: number) => string; onOpen: () => void }) {
  const name = categoryName(row.category)
  return (
    <li className="flex">
      <button
        type="button"
        onClick={onOpen}
        className="-m-2 flex min-w-0 flex-1 items-start gap-3 rounded-lg p-2 text-left outline-none hover:bg-accent/60 focus-visible:ring-[3px] focus-visible:ring-ring"
      >
        <CategoryBadge category={row.category} />
        <span className="flex min-w-0 flex-1 flex-col gap-1.5">
          <span className="flex items-baseline gap-2">
            <span className="min-w-0 flex-1 truncate font-medium">{name}</span>
            <span className="font-semibold tabular-nums">{money(row.amountMinor)}</span>
          </span>
          <span role="img" aria-label={t('groupStats_categoryShare', { name, percent: row.percent })} className="flex h-2 rounded-full bg-muted">
            {/* Why: the bar takes its category's color, as its slice and its legend dot, never the accent. */}
            <span className="avatar-mark rounded-full" style={{ ...categoryStyle(row.category.color), width: `${row.barFraction * 100}%` }} />
          </span>
        </span>
        <ChevronRight aria-hidden className="size-4 shrink-0 self-center text-muted-foreground" />
      </button>
    </li>
  )
}
