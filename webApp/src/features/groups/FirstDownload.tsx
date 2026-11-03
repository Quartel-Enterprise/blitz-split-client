import type { JsDownloadProgress } from 'blitzsplit-shared'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

/** A gray bar where something still on its way will show, while the first download fills the browser. */
export function Skeleton({ className }: { className?: string }) {
  return <span aria-hidden className={cn('block animate-pulse rounded-md bg-muted', className)} />
}

/** The first download's bar (5.1d): it fills as the groups come, and pulses until the server said how many are coming. */
export function DownloadProgressBar({ progress, className }: { progress: JsDownloadProgress; className?: string }) {
  const fraction = progress.fraction
  return (
    <span
      role="progressbar"
      aria-label={t('home_syncProgress')}
      aria-valuemin={fraction == null ? undefined : 0}
      aria-valuemax={fraction == null ? undefined : 100}
      aria-valuenow={fraction == null ? undefined : Math.round(fraction * 100)}
      className={cn('block h-1 overflow-hidden rounded-full bg-muted', className)}
    >
      {fraction == null ? (
        <span className="block h-full w-full animate-pulse rounded-full bg-primary/60" />
      ) : (
        <span className="block h-full rounded-full bg-primary transition-[width]" style={{ width: `${fraction * 100}%` }} />
      )}
    </span>
  )
}

/** "Sincronizando… 3 de 7 grupos" once the first download knows how many groups are coming, else "Sincronizando…". */
export function syncingLabel(progress: JsDownloadProgress | null | undefined): string {
  const groupCount = progress?.groupCount
  if (progress == null || groupCount == null) return t('groups_syncSyncing')
  return t(groupCount === 1 ? 'groups_syncSyncingGroupsOne' : 'groups_syncSyncingGroupsOther', {
    arrived: progress.arrivedGroupCount,
    count: groupCount,
  })
}

/**
 * "Trazendo seus grupos" in Saldo geral's place while the first download brings the groups down one by one, with
 * "3 de 7 grupos" and its bar once the server said how many are coming (5.1d).
 */
export function GroupsFirstDownloadCard({ progress }: { progress: JsDownloadProgress }) {
  const groupCount = progress.groupCount
  const hint =
    groupCount == null
      ? t('groups_firstDownloadHint')
      : t(groupCount === 1 ? 'groups_firstDownloadCountOne' : 'groups_firstDownloadCountOther', {
          arrived: progress.arrivedGroupCount,
          count: groupCount,
        })
  return (
    <section className="flex flex-col gap-3 rounded-xl border bg-card p-4 md:p-5">
      <h2 className="text-base font-semibold">{t('groups_firstDownloadTitle')}</h2>
      <DownloadProgressBar progress={progress} />
      <p className="text-sm text-muted-foreground">{hint}</p>
    </section>
  )
}

/** Gray rows under the groups that came, holding the place of a few still coming. */
export function GroupSkeletonRows({ count }: { count: number }) {
  return (
    <ul aria-hidden className="flex flex-col divide-y rounded-xl border bg-card">
      {Array.from({ length: count }, (_, index) => (
        <li key={index} className="flex min-h-[60px] items-center gap-3 px-3 py-2.5">
          <Skeleton className="size-8 rounded-lg" />
          <span className="flex flex-1 flex-col gap-1.5">
            <Skeleton className="h-3 w-2/5" />
            <Skeleton className="h-2.5 w-1/4" />
          </span>
          <Skeleton className="h-3 w-16" />
        </li>
      ))}
    </ul>
  )
}
