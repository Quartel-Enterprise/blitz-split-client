import type { JsGroupStatsState, JsStatsCategory } from 'blitzsplit-shared'
import { t } from '@/i18n'
import { avatarStyle } from '@/lib/avatar'
import { CategoryBadge, categoryName } from '@/lib/categories'

const radius = 52
const stroke = 18
const circumference = 2 * Math.PI * radius
const gap = 2

/**
 * "Por categoria" (6.6): the ring of slices in each main category's fixed color, the largest in its center and the
 * legend with the percentages, then each main category with its bar and its subcategories.
 */
export function CategoriesCard({ state, money, className }: { state: JsGroupStatsState; money: (minor: number) => string; className: string }) {
  const rows = state.categories
  if (rows.length === 0) return null
  const top = rows[0]
  const description = t('groupStats_categoryPie', { parts: rows.map((row) => `${categoryName(row.category)} ${row.percent}%`).join(', ') })
  return (
    <section aria-labelledby="statsCategories" className={className}>
      <h2 id="statsCategories" className="text-[15px] font-semibold">
        {t('groupStats_byCategory')}
      </h2>
      <div className="flex items-center gap-5">
        <div role="img" aria-label={description} className="relative size-[132px] shrink-0">
          <svg viewBox="0 0 132 132" className="size-full -rotate-90">
            {slicesOf(rows).map(({ row, offset, length }) => (
              <circle
                key={row.category.id}
                cx="66"
                cy="66"
                r={radius}
                fill="none"
                strokeWidth={stroke}
                className="avatar-stroke"
                style={avatarStyle(row.category.color)}
                strokeDasharray={`${length} ${circumference - length}`}
                strokeDashoffset={-offset}
              />
            ))}
          </svg>
          <span aria-hidden className="absolute inset-0 flex flex-col items-center justify-center px-6 text-center">
            <span className="text-lg font-bold tabular-nums">{top.percent}%</span>
            <span className="w-full truncate text-[11px] text-muted-foreground">{categoryName(top.category)}</span>
          </span>
        </div>
        <ul aria-hidden className="flex min-w-0 flex-col gap-2 text-[13px]">
          {rows.map((row) => (
            <li key={row.category.id} className="flex items-center gap-2">
              <span style={avatarStyle(row.category.color)} className="avatar size-3 shrink-0 rounded-[4px]" />
              <span className="truncate">{categoryName(row.category)}</span>
              <span className="text-muted-foreground tabular-nums">{row.percent}%</span>
            </li>
          ))}
        </ul>
      </div>
      <ul className="flex flex-col gap-3">
        {rows.map((row) => (
          <CategoryLine key={row.category.id} row={row} money={money} />
        ))}
      </ul>
    </section>
  )
}

function CategoryLine({ row, money }: { row: JsStatsCategory; money: (minor: number) => string }) {
  const name = categoryName(row.category)
  return (
    <li className="flex items-start gap-3">
      <CategoryBadge category={row.category} />
      <span className="flex min-w-0 flex-1 flex-col gap-1.5">
        <span className="flex items-baseline gap-2">
          <span className="min-w-0 flex-1 truncate font-medium">{name}</span>
          <span className="font-semibold tabular-nums">{money(row.amountMinor)}</span>
        </span>
        <span role="img" aria-label={t('groupStats_categoryShare', { name, percent: row.percent })} className="flex h-2 rounded-full bg-muted">
          <span className="rounded-full bg-primary" style={{ width: `${row.barFraction * 100}%` }} />
        </span>
        {row.subcategories.length > 0 && (
          <span className="text-xs text-muted-foreground">
            {row.subcategories.map((sub) => `${categoryName(sub.category)} ${money(sub.amountMinor)}`).join(' · ')}
          </span>
        )}
      </span>
    </li>
  )
}

// Why: each slice's length along the ring, with a thin gap after it while there is more than one.
function slicesOf(rows: JsStatsCategory[]): { row: JsStatsCategory; offset: number; length: number }[] {
  let offset = 0
  return rows.map((row) => {
    const full = (row.percent / 100) * circumference
    const length = Math.max(0, rows.length > 1 ? full - gap : full)
    const slice = { row, offset, length }
    offset += full
    return slice
  })
}
