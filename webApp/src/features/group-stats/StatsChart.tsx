import type { JsGroupStatsState } from 'blitzsplit-shared'
import { cn } from '@/lib/utils'
import { barLabel, chartAria } from './statsFormat'

const chartHeight = 128

/**
 * "Ao longo do tempo" as bars or a line, drawn with plain elements in the design's primary color, scaled by the
 * most expensive bar, with a label under the bars the shared code marks and the whole series for screen readers.
 */
export function StatsChart({ state }: { state: JsGroupStatsState }) {
  const bars = state.bars
  const count = bars.length
  const largest = Math.max(1, ...bars.map((bar) => bar.amountMinor))
  const centerOf = (index: number) => ((index + 0.5) * 100) / count
  const topOf = (amount: number) => chartHeight - 4 - (amount * (chartHeight - 12)) / largest
  const points = bars.map((bar, index) => `${(centerOf(index) * 10).toFixed(1)},${topOf(bar.amountMinor).toFixed(1)}`)
  const gap = count > 20 ? 'mx-px' : count > 8 ? 'mx-[3px]' : 'mx-[5px]'
  return (
    <div className="flex flex-col gap-1.5">
      <div role="img" aria-label={chartAria(state)} className="relative" style={{ height: chartHeight }}>
        {state.chartType === 'line' ? (
          <>
            <svg width="100%" height={chartHeight} viewBox={`0 0 1000 ${chartHeight}`} preserveAspectRatio="none" aria-hidden className="absolute inset-0 overflow-visible">
              <polygon
                points={`${points.join(' ')} ${(centerOf(count - 1) * 10).toFixed(1)},${chartHeight} ${(centerOf(0) * 10).toFixed(1)},${chartHeight}`}
                className="fill-primary/15"
              />
              <polyline points={points.join(' ')} fill="none" strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" vectorEffect="non-scaling-stroke" className="stroke-primary" />
            </svg>
            {bars.map((bar, index) =>
              bar.showsDot ? (
                <span
                  key={bar.startMillis}
                  aria-hidden
                  className="absolute size-2.5 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-card bg-primary"
                  style={{ left: `${centerOf(index)}%`, top: topOf(bar.amountMinor) }}
                />
              ) : null,
            )}
          </>
        ) : (
          <div aria-hidden className="absolute inset-0 grid items-end" style={{ gridTemplateColumns: `repeat(${count}, minmax(0, 1fr))` }}>
            {bars.map((bar) => (
              <span
                key={bar.startMillis}
                className={cn(gap, bar.amountMinor > 0 ? 'bg-primary' : 'bg-muted', count > 20 ? 'rounded-t-[2px]' : 'rounded-t-[4px] rounded-b-[2px]')}
                style={{ height: bar.amountMinor > 0 ? Math.max(4, Math.round((bar.amountMinor * chartHeight) / largest)) : 4 }}
              />
            ))}
          </div>
        )}
      </div>
      <div aria-hidden className="relative h-4 text-xs text-muted-foreground">
        {bars.map((bar, index) =>
          bar.showsLabel ? (
            <span
              key={bar.startMillis}
              className={cn('absolute top-0 -translate-x-1/2 whitespace-nowrap', !state.hasSparseLabels && index === count - 1 && 'font-semibold text-foreground')}
              style={{ left: `${centerOf(index)}%` }}
            >
              {barLabel(bar, state.grain, state.hasSparseLabels)}
            </span>
          ) : null,
        )}
      </div>
    </div>
  )
}
