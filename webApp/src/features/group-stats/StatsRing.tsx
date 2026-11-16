import { useRef, type ReactNode } from 'react'
import { categoryStyle } from '@/lib/avatar'
import { cn } from '@/lib/utils'
import { useReplay } from './useReplay'

const radius = 52
const stroke = 18
const circumference = 2 * Math.PI * radius
const gap = 2

// Why: one slice of a ring: who or what it is, its rounded share (a ring's adding up to 100) and its category color.
export interface StatsRingSlice {
  id: string
  percent: number
  color: string
}

/**
 * The ring of "Por categoria" and "Por pessoa": slices with a thin gap between them, sweeping in again when ringKey
 * changes. The picked slice pops out and the others fade; the mouse picks by hovering, and a click or a tap lands on
 * onTap, which is told whether a mouse made it. Shares that are all 0 leave a bare neutral ring. The center is the
 * caller's, as is the label read for the whole ring.
 */
export function StatsRing({
  slices,
  ringKey,
  label,
  selected,
  size,
  onPick,
  onTap,
  children,
}: {
  slices: StatsRingSlice[]
  ringKey: string
  label: string
  selected: number | null
  size: number
  onPick: (index: number | null) => void
  onTap: (index: number, isMouse: boolean) => void
  children: ReactNode
}) {
  const isSwept = useReplay(ringKey)
  // Why: a click doesn't say what made it, so the pointer that pressed it is kept for onTap.
  const pointerType = useRef('mouse')
  const isEmpty = !slices.some((slice) => slice.percent > 0)
  return (
    <div role="img" aria-label={label} className="relative shrink-0" style={{ width: size, height: size }}>
      <svg
        viewBox="0 0 132 132"
        className="size-full -rotate-90"
        onPointerLeave={(event) => {
          if (event.pointerType === 'mouse') onPick(null)
        }}
        onPointerMove={(event) => {
          // Why: the mouse in the hole lets go, as on Android and Apple; a slice under it is the event's target.
          if (event.pointerType === 'mouse' && event.target === event.currentTarget) onPick(null)
        }}
      >
        {isEmpty ? (
          <circle cx="66" cy="66" r={radius} fill="none" strokeWidth={stroke} className="stroke-muted" />
        ) : (
          arcsOf(slices).map(({ slice, offset, length }, index) => (
            <circle
              key={slice.id}
              cx="66"
              cy="66"
              r={radius}
              fill="none"
              strokeWidth={stroke}
              className={cn(
                'avatar-stroke origin-center cursor-pointer transition-[stroke-dasharray,stroke-dashoffset,scale,opacity] duration-300 ease-out [transform-box:view-box] motion-reduce:transition-none',
                selected === index && 'scale-[1.07]',
                selected !== null && selected !== index && 'opacity-40',
              )}
              style={categoryStyle(slice.color)}
              strokeDasharray={isSwept ? `${length} ${circumference - length}` : `0 ${circumference}`}
              strokeDashoffset={isSwept ? -offset : 0}
              onPointerEnter={(event) => {
                if (event.pointerType === 'mouse') onPick(index)
              }}
              onPointerDown={(event) => {
                pointerType.current = event.pointerType
              }}
              onClick={() => onTap(index, pointerType.current === 'mouse')}
            />
          ))
        )}
      </svg>
      <span aria-hidden className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center px-6 text-center">
        {children}
      </span>
    </div>
  )
}

// Why: the ring's middle, the picked or largest share over its name.
export function StatsRingCenter({ percent, name }: { percent: number; name: string }) {
  return (
    <>
      <span className="text-lg font-bold tabular-nums">{percent}%</span>
      <span className="w-full truncate text-[11px] text-muted-foreground">{name}</span>
    </>
  )
}

// Why: each slice's length along the ring, with a thin gap after it while more than one has a share.
function arcsOf(slices: StatsRingSlice[]): { slice: StatsRingSlice; offset: number; length: number }[] {
  const hasGaps = slices.filter((slice) => slice.percent > 0).length > 1
  let offset = 0
  return slices.map((slice) => {
    const full = (slice.percent / 100) * circumference
    const length = Math.max(0, hasGaps ? full - gap : full)
    const arc = { slice, offset, length }
    offset += full
    return arc
  })
}
