import { useEffect, useRef, useState, type KeyboardEvent, type PointerEvent } from 'react'
import type { JsGroupStatsState } from 'blitzsplit-shared'
import { t } from '@/i18n'
import { formatCompactMoney, formatMoney } from '@/lib/money'
import { cn } from '@/lib/utils'
import { barLabel, barName, calloutCount, chartAria } from './statsFormat'
import { useReplay } from './useReplay'

const chartHeight = 128
// Why: room above the top value line for half of its label.
const topInset = 7
// Why: how far a finger moves sideways before it runs along the bars instead of tapping one.
const dragSlop = 8

/**
 * "Ao longo do tempo" as bars or a line, drawn with plain elements in the design's primary color and scaled by the top
 * value line: dashed value lines with their short amounts in a gutter on the end side, the dashed average, a label
 * under the bars the shared code marks, and the whole series for screen readers. The mouse over a bar, a tap or a drag
 * on it, or the arrow keys pick it, showing its period, amount and bills in a callout over it; a tap keeps it until a
 * second one.
 */
export function StatsChart({ state }: { state: JsGroupStatsState }) {
  const bars = state.bars
  const count = bars.length
  const top = Math.max(1, state.chartTopMinor)
  const money = (minor: number) => formatMoney(minor, state.currency, state.fractionDigits)
  const ticks = Array.from(state.chartTicksMinor).filter((tick) => tick > 0)
  const tickTexts = ticks.map((tick) => formatCompactMoney(tick, state.currency, state.fractionDigits))
  const widestTick = tickTexts.reduce((widest, text) => (text.length > widest.length ? text : widest), '')
  const centerOf = (index: number) => ((index + 0.5) * 100) / count
  const heightOf = (amount: number) => (amount * (chartHeight - topInset)) / top
  const yOf = (amount: number) => chartHeight - heightOf(amount)
  const gap = count > 20 ? 'mx-px' : count > 8 ? 'mx-[3px]' : 'mx-[5px]'
  const seriesKey = `${state.grain}-${state.periodStartMillis}-${state.periodEndMillis}-${state.chartType}-${count}`
  const [selected, setSelected] = useState<number | null>(null)
  const isGrown = useReplay(seriesKey)
  // Why: where a finger went down and whether it has started running along the bars.
  const touch = useRef<{ x: number; isDragging: boolean } | null>(null)

  // Why: a new grain, period or chart type lets go of the pick.
  useEffect(() => setSelected(null), [seriesKey])

  const grow = (amount: number) => (isGrown ? amount : 0)
  const points = bars.map((bar, index) => `${(centerOf(index) * 10).toFixed(1)},${yOf(bar.amountMinor).toFixed(1)}`)
  const picked = selected !== null ? bars[selected] : undefined

  const indexAt = (event: PointerEvent<HTMLDivElement>) => {
    const rect = event.currentTarget.getBoundingClientRect()
    const index = Math.floor(((event.clientX - rect.left) / rect.width) * count)
    return Math.min(count - 1, Math.max(0, index))
  }

  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (count === 0) return
    const moves: Record<string, () => number | null> = {
      ArrowRight: () => Math.min(count - 1, (selected ?? -1) + 1),
      ArrowLeft: () => Math.max(0, (selected ?? count) - 1),
      Home: () => 0,
      End: () => count - 1,
      Escape: () => null,
    }
    const move = moves[event.key]
    if (!move) return
    event.preventDefault()
    setSelected(move())
  }

  return (
    <div className="grid grid-cols-[minmax(0,1fr)_auto] gap-y-1.5">
      <div
        role="group"
        aria-label={chartAria(state)}
        aria-describedby="statsChartKeys"
        tabIndex={0}
        onKeyDown={onKeyDown}
        onPointerDown={(event) => {
          if (event.pointerType !== 'mouse') touch.current = { x: event.clientX, isDragging: false }
        }}
        onPointerMove={(event) => {
          if (event.pointerType === 'mouse') {
            setSelected(indexAt(event))
            return
          }
          const current = touch.current
          if (!current) return
          if (!current.isDragging && Math.abs(event.clientX - current.x) < dragSlop) return
          current.isDragging = true
          setSelected(indexAt(event))
        }}
        onPointerUp={(event) => {
          // Why: a finger picks on lifting, so a swipe that scrolls the page (a cancel) picks nothing.
          const current = touch.current
          touch.current = null
          if (!current || current.isDragging) return
          const index = indexAt(event)
          setSelected((picked) => (picked === index ? null : index))
        }}
        onPointerCancel={() => {
          touch.current = null
        }}
        onPointerLeave={(event) => {
          if (event.pointerType === 'mouse') setSelected(null)
        }}
        className="relative touch-pan-y rounded-sm outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
        style={{ height: chartHeight }}
      >
        <span aria-hidden className="absolute inset-x-0 bottom-0 border-t border-border" />
        {ticks.map((tick) => (
          <span key={tick} aria-hidden className="absolute inset-x-0 border-t border-dashed border-border" style={{ top: yOf(tick) }} />
        ))}
        {state.chartType === 'line' ? (
          <div
            aria-hidden
            className="absolute inset-0 transition-[clip-path] duration-700 ease-out motion-reduce:transition-none"
            style={{ clipPath: isGrown ? 'inset(-12px -12px -12px -12px)' : 'inset(-12px 100% -12px -12px)' }}
          >
            <svg width="100%" height={chartHeight} viewBox={`0 0 1000 ${chartHeight}`} preserveAspectRatio="none" className="absolute inset-0 overflow-visible">
              <polygon
                points={`${points.join(' ')} ${(centerOf(count - 1) * 10).toFixed(1)},${chartHeight} ${(centerOf(0) * 10).toFixed(1)},${chartHeight}`}
                className="fill-primary/15"
              />
              <polyline points={points.join(' ')} fill="none" strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" vectorEffect="non-scaling-stroke" className="stroke-primary" />
            </svg>
            {selected !== null && (
              <span className="absolute inset-y-0 w-px -translate-x-1/2 bg-muted-foreground/50" style={{ left: `${centerOf(selected)}%` }} />
            )}
            {bars.map((bar, index) =>
              bar.showsDot || index === selected ? (
                <span
                  key={index}
                  className={cn(
                    'absolute -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-card bg-primary',
                    index === selected ? 'size-3.5' : 'size-2.5',
                  )}
                  style={{ left: `${centerOf(index)}%`, top: yOf(bar.amountMinor) }}
                />
              ) : null,
            )}
          </div>
        ) : (
          <div aria-hidden className="absolute inset-0 grid items-end" style={{ gridTemplateColumns: `repeat(${count}, minmax(0, 1fr))` }}>
            {bars.map((bar, index) => (
              <span
                key={index}
                className={cn(
                  gap,
                  'transition-[height,opacity] duration-500 ease-out motion-reduce:transition-none',
                  bar.amountMinor > 0 ? 'bg-primary' : 'bg-muted',
                  count > 20 ? 'rounded-t-[2px]' : 'rounded-t-[4px] rounded-b-[2px]',
                  selected !== null && index !== selected && 'opacity-35',
                )}
                style={{ height: bar.amountMinor > 0 ? Math.max(4, Math.round(grow(heightOf(bar.amountMinor)))) : 4 }}
              />
            ))}
          </div>
        )}
        {state.chartAverageMinor != null && (
          <span
            aria-hidden
            className="pointer-events-none absolute inset-x-0 border-t-[1.5px] border-dashed border-muted-foreground"
            style={{ top: yOf(state.chartAverageMinor) }}
          />
        )}
        {picked && selected !== null && (
          <div
            aria-hidden
            className="pointer-events-none absolute z-10 flex flex-col whitespace-nowrap rounded-lg border bg-popover px-2.5 py-1.5 text-popover-foreground shadow-md"
            style={{
              left: `${centerOf(selected)}%`,
              top: yOf(picked.amountMinor) - 6,
              transform: `translate(${selected < count / 5 ? '-15%' : selected >= (count * 4) / 5 ? '-85%' : '-50%'}, -100%)`,
            }}
          >
            <span className="text-[11px] text-muted-foreground">{barName(picked, state.grain)}</span>
            <span className="text-sm font-semibold tabular-nums">{money(picked.amountMinor)}</span>
            <span className="text-[11px] text-muted-foreground">{calloutCount(picked.expenseCount)}</span>
          </div>
        )}
      </div>
      <div aria-hidden className="relative pl-2 text-[11px] text-muted-foreground" style={{ height: chartHeight }}>
        <span className="invisible block h-0 overflow-hidden whitespace-nowrap">{widestTick}</span>
        {ticks.map((tick, index) => (
          <span key={tick} className="absolute right-0 -translate-y-1/2 whitespace-nowrap tabular-nums" style={{ top: yOf(tick) }}>
            {tickTexts[index]}
          </span>
        ))}
      </div>
      <div aria-hidden className="relative h-4 text-xs text-muted-foreground">
        {bars.map((bar, index) =>
          bar.showsLabel ? (
            <span
              key={index}
              className={cn('absolute top-0 -translate-x-1/2 whitespace-nowrap', !state.hasSparseLabels && index === count - 1 && 'font-semibold text-foreground')}
              style={{ left: `${centerOf(index)}%` }}
            >
              {barLabel(bar, state.grain, state.hasSparseLabels)}
            </span>
          ) : null,
        )}
      </div>
      <span />
      <span id="statsChartKeys" className="sr-only">
        {t('stats_chartKeys')}
      </span>
      <span aria-live="polite" className="sr-only">
        {picked ? `${barName(picked, state.grain)}: ${money(picked.amountMinor)}, ${calloutCount(picked.expenseCount)}` : ''}
      </span>
    </div>
  )
}
