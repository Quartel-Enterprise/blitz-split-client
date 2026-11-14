import { useLayoutEffect, useRef, useState } from 'react'
import type { JsStatsBar } from 'blitzsplit-shared'
import { cn } from '@/lib/utils'

// Why: the least room between two labels under the chart, in pixels.
const labelGap = 6

type Placed = { x: number; isShown: boolean }

/**
 * The labels the shared code marks, each centered under its bar and kept inside the chart. One that would touch the
 * label after it is left out, from the end, so the last (the newest, in bold) always shows and no two ever overlap,
 * whatever the width and the language (beta feedback of 2026-11-14). Measured again whenever the chart's or a label's width changes.
 */
export function StatsChartLabels({
  bars,
  boldsLast,
  labelOf,
}: {
  bars: JsStatsBar[]
  boldsLast: boolean
  labelOf: (bar: JsStatsBar) => string
}) {
  const container = useRef<HTMLDivElement>(null)
  const spans = useRef<(HTMLSpanElement | null)[]>([])
  const [placed, setPlaced] = useState<Placed[] | null>(null)
  const labeled = bars.flatMap((bar, index) => (bar.showsLabel ? [{ bar, index, text: labelOf(bar) }] : []))
  const textsKey = `${bars.length}|${labeled.map((label) => `${label.index}:${label.text}`).join('|')}`

  useLayoutEffect(() => {
    const element = container.current
    if (!element) return
    const place = () => {
      const width = element.clientWidth
      const xs = labeled.map((label, position) => {
        const labelWidth = spans.current[position]?.offsetWidth ?? 0
        const center = ((label.index + 0.5) * width) / Math.max(1, bars.length)
        return { x: Math.min(Math.max(0, center - labelWidth / 2), Math.max(0, width - labelWidth)), labelWidth }
      })
      let nextStart = Number.POSITIVE_INFINITY
      const next: Placed[] = xs.map(({ x }) => ({ x, isShown: false }))
      for (let position = xs.length - 1; position >= 0; position--) {
        const { x, labelWidth } = xs[position]
        if (x + labelWidth + labelGap <= nextStart) {
          next[position].isShown = true
          nextStart = x
        }
      }
      setPlaced(next)
    }
    place()
    // Why: the chart's width and each label's own (a web font arriving after the first measure widens them).
    const observer = new ResizeObserver(place)
    observer.observe(element)
    spans.current.slice(0, labeled.length).forEach((span) => span && observer.observe(span))
    return () => observer.disconnect()
    // eslint-disable-next-line react-hooks/exhaustive-deps -- textsKey stands for the labels and their texts
  }, [textsKey])

  return (
    <div ref={container} aria-hidden className="relative h-4 text-xs text-muted-foreground">
      {labeled.map(({ index, text }, position) => (
        <span
          key={index}
          ref={(span) => {
            spans.current[position] = span
          }}
          className={cn(
            'absolute top-0 whitespace-nowrap',
            boldsLast && index === bars.length - 1 && 'font-semibold text-foreground',
            placed?.[position]?.isShown ? 'visible' : 'invisible',
          )}
          style={{ left: placed?.[position]?.x ?? 0 }}
        >
          {text}
        </span>
      ))}
    </div>
  )
}
