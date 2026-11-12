import { LoadingRegion, Skeleton, SkeletonLine, SkeletonRows } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'

/**
 * A summary card still being read ("Saldo geral", "A receber", "A pagar", "Você deve"…): its label, the amount and the
 * caption under it, in the card's frame.
 */
export function SummaryCardSkeleton({ isLarge = false, className }: { isLarge?: boolean; className?: string }) {
  return (
    <div className={cn('flex min-w-0 flex-col gap-2.5 rounded-xl border bg-card px-4 py-3.5 sm:px-5 sm:py-4', className)}>
      <SkeletonLine className="w-24" />
      <Skeleton className={cn('rounded-md', isLarge ? 'h-8 w-40' : 'h-7 w-28')} />
      <SkeletonLine className="h-2.5 w-20" />
    </div>
  )
}

/** The three summary cards of Grupos, Início and Saldos: the net wider on a phone, three equal from the tablet up. */
export function SummaryCardsSkeleton({ className }: { className?: string }) {
  return (
    <div className={cn('grid grid-cols-2 gap-3 md:grid-cols-3', className)}>
      <SummaryCardSkeleton isLarge className="col-span-2 md:col-span-1" />
      <SummaryCardSkeleton />
      <SummaryCardSkeleton />
    </div>
  )
}

/** A card of rows under its title, as Início's sections, Saldos' people and Contatos' list draw them. */
export function ListCardSkeleton({ rows = 4, leading = 'circle', className }: { rows?: number; leading?: 'circle' | 'square' | 'group' | 'none'; className?: string }) {
  return (
    <div className={cn('overflow-hidden rounded-xl border bg-card', className)}>
      <div className="px-4 pt-4 pb-3">
        <SkeletonLine className="h-4 w-32" />
      </div>
      <SkeletonRows count={rows} leading={leading} className="border-t" />
    </div>
  )
}

/** A page of the home's tabs still being read: the summary cards over a list, after 150 ms. */
export function SummaryListPageSkeleton({ rows = 5, leading = 'circle', isWaitingForInternet = false }: { rows?: number; leading?: 'circle' | 'square' | 'group'; isWaitingForInternet?: boolean }) {
  return (
    <LoadingRegion isWaitingForInternet={isWaitingForInternet} className="flex flex-col gap-5">
      <SummaryCardsSkeleton />
      <SkeletonRows count={rows} leading={leading} className="rounded-xl border bg-card" />
    </LoadingRegion>
  )
}

/**
 * The app before the browser's database opened, when nothing says yet whether the home or the welcome comes: a
 * centered mark and two lines, after 150 ms, so the first paint is never blank.
 */
export function AppSkeleton() {
  return (
    <div className="flex min-h-dvh items-center justify-center bg-background p-4">
      <LoadingRegion className="flex w-full max-w-xs flex-col items-center gap-4">
        <Skeleton className="size-14 rounded-2xl" />
        <SkeletonLine className="h-4 w-40" />
        <SkeletonLine className="w-56" />
      </LoadingRegion>
    </div>
  )
}

/** An invite's QR code and its link under it, with the buttons' places, while the group's code is read. */
export function QrCardSkeleton({ hasChips = false }: { hasChips?: boolean }) {
  return (
    <>
      {hasChips && (
        <span className="flex gap-2">
          {[0, 1, 2].map((index) => (
            <Skeleton key={index} className="h-8 w-24 rounded-full" />
          ))}
        </span>
      )}
      <Skeleton className="mx-auto size-56 rounded-xl" />
      <Skeleton className="h-10 w-full" />
      <span className="flex gap-2">
        <Skeleton className="h-9 flex-1" />
        <Skeleton className="h-9 flex-1" />
      </span>
    </>
  )
}
