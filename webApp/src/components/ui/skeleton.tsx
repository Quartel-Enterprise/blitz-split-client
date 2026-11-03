import { useEffect, useState, type ComponentProps, type ReactNode } from 'react'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

// Why: a read of the browser's database usually answers sooner, and a shimmer flashing for a frame reads as a glitch.
const APPEARS_AFTER_MILLIS = 150

/**
 * A placeholder block where something still on its way will show, with a soft band sweeping across it (static under
 * reduced motion, see `.skeleton` in index.css). Hidden from screen readers: the `LoadingRegion` around it speaks.
 */
export function Skeleton({ className, ...props }: ComponentProps<'span'>) {
  return <span data-slot="skeleton" aria-hidden className={cn('skeleton block shrink-0 rounded-md', className)} {...props} />
}

/** False for the first `delayMillis` after the caller mounts, then true: a fast read never shows its placeholder. */
export function useAppearsAfter(delayMillis: number = APPEARS_AFTER_MILLIS): boolean {
  const [appears, setAppears] = useState(delayMillis <= 0)
  useEffect(() => {
    if (delayMillis <= 0) return
    const timer = window.setTimeout(() => setAppears(true), delayMillis)
    return () => window.clearTimeout(timer)
  }, [delayMillis])
  return appears
}

/**
 * A section that has no answer yet: its shimmer blocks fade in after 150 ms (`isDelayed`, on by default; off for
 * what waits on the server, which never answers in a frame), and screen readers hear one "Carregando". While the
 * account's copy is partial and the browser offline, "Sem internet. Seus dados aparecem quando reconectar." under it.
 */
export function LoadingRegion({
  isDelayed = true,
  isWaitingForInternet = false,
  label,
  className,
  children,
}: {
  isDelayed?: boolean
  isWaitingForInternet?: boolean
  // Why: "Carregando" by default; a section may name what loads ("Carregando comentários").
  label?: string
  className?: string
  children: ReactNode
}) {
  const appears = useAppearsAfter(isDelayed ? APPEARS_AFTER_MILLIS : 0)
  if (!appears) return null
  return (
    <div role="status" aria-busy="true" className={cn('animate-in fade-in-0 duration-300 motion-reduce:animate-none', className)}>
      <span className="sr-only">{label ?? t('common_loading')}</span>
      {children}
      {isWaitingForInternet && <WaitingForInternetNotice className="mt-3" />}
    </div>
  )
}

/** "Sem internet. Seus dados aparecem quando reconectar.": the account's copy is partial and the browser offline. */
export function WaitingForInternetNotice({ isGroups = false, className }: { isGroups?: boolean; className?: string }) {
  return <p className={cn('text-sm text-muted-foreground', className)}>{t(isGroups ? 'common_waitingForInternetGroups' : 'common_waitingForInternet')}</p>
}

// Why: rows of the same width read as a pattern, not as content; these vary them the same way on every visit.
const firstLineWidths = ['w-2/5', 'w-1/2', 'w-1/3', 'w-3/5']
const secondLineWidths = ['w-1/4', 'w-1/3', 'w-1/5', 'w-2/7']

/** A line of text that hasn't come yet, as tall as a line of `text-sm` by default. */
export function SkeletonLine({ className }: { className?: string }) {
  return <Skeleton className={cn('h-3 w-full rounded', className)} />
}

/** A face or an avatar that hasn't come yet. */
export function SkeletonCircle({ className }: { className?: string }) {
  return <Skeleton className={cn('size-8 rounded-full', className)} />
}

/**
 * A list row: the avatar (a circle, or the rounded square of a group or a category), its two lines and the amount on
 * the right, as the app's rows draw them. `index` varies the lines' widths.
 */
export function SkeletonRow({
  index = 0,
  leading = 'circle',
  hasTrailing = true,
  className,
}: {
  index?: number
  leading?: 'circle' | 'square' | 'none'
  hasTrailing?: boolean
  className?: string
}) {
  return (
    <div className={cn('flex min-h-[60px] items-center gap-3 px-4 py-2.5', className)}>
      {leading === 'circle' && <SkeletonCircle className="size-9" />}
      {leading === 'square' && <Skeleton className="size-9 rounded-lg" />}
      <span className="flex min-w-0 flex-1 flex-col gap-2">
        <SkeletonLine className={firstLineWidths[index % firstLineWidths.length]} />
        <SkeletonLine className={cn('h-2.5', secondLineWidths[index % secondLineWidths.length])} />
      </span>
      {hasTrailing && <SkeletonLine className="w-16" />}
    </div>
  )
}

/** `count` rows one under the other, divided as the app's lists are; the caller frames them (a card or none). */
export function SkeletonRows({
  count = 4,
  leading,
  hasTrailing,
  className,
  rowClassName,
}: {
  count?: number
  leading?: 'circle' | 'square' | 'none'
  hasTrailing?: boolean
  className?: string
  rowClassName?: string
}) {
  return (
    <div className={cn('flex flex-col divide-y', className)}>
      {Array.from({ length: count }, (_, index) => (
        <SkeletonRow key={index} index={index} leading={leading} hasTrailing={hasTrailing} className={rowClassName} />
      ))}
    </div>
  )
}

/** A card with a title line and `lines` lines under it, or the blocks given as children in its frame. */
export function SkeletonCard({ lines = 2, className, children }: { lines?: number; className?: string; children?: ReactNode }) {
  return (
    <div className={cn('flex flex-col gap-3 rounded-xl border bg-card p-4', className)}>
      {children ?? (
        <>
          <SkeletonLine className="h-4 w-1/3" />
          {Array.from({ length: lines }, (_, index) => (
            <SkeletonLine key={index} className={index === lines - 1 ? 'w-2/3' : undefined} />
          ))}
        </>
      )}
    </div>
  )
}

/** A form's fields that wait for what they edit: a label over a field-shaped block each, then the button's place. */
export function SkeletonForm({ fields = 3, className }: { fields?: number; className?: string }) {
  return (
    <div className={cn('flex flex-col gap-5', className)}>
      {Array.from({ length: fields }, (_, index) => (
        <div key={index} className="flex flex-col gap-2">
          <SkeletonLine className={cn('h-3', firstLineWidths[index % firstLineWidths.length], 'max-w-32')} />
          <Skeleton className="h-9 w-full" />
        </div>
      ))}
      <Skeleton className="mt-1 h-9 w-full rounded-md" />
    </div>
  )
}
