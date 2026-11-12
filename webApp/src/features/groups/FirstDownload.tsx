import type { JsDownloadProgress } from 'blitzsplit-shared'
import { LoadingRegion, SkeletonRows } from '@/components/ui/skeleton'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

/**
 * The first download's bar (5.1d): it fills as the groups come, and pulses until the server said how many are coming.
 * Offline it stops where it was, since nothing comes until the browser reconnects.
 */
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
        <span className={cn('block h-full w-full rounded-full', progress.isWaitingForInternet ? 'bg-primary/30' : 'animate-pulse bg-primary/60')} />
      ) : (
        <span className="block h-full rounded-full bg-primary transition-[width]" style={{ width: `${fraction * 100}%` }} />
      )}
    </span>
  )
}

/**
 * "Sincronizando… 3 de 7 grupos" once the first download knows how many groups are coming, else "Sincronizando…";
 * offline "Sem internet. Seus grupos aparecem quando reconectar." instead of the count.
 */
export function syncingLabel(progress: JsDownloadProgress | null | undefined): string {
  if (progress?.isWaitingForInternet) return t('common_waitingForInternetGroups')
  const groupCount = progress?.groupCount
  if (progress == null || groupCount == null) return t('groups_syncSyncing')
  return t(groupCount === 1 ? 'groups_syncSyncingGroupsOne' : 'groups_syncSyncingGroupsOther', {
    arrived: progress.arrivedGroupCount,
    count: groupCount,
  })
}

/**
 * "Trazendo seus grupos" in Saldo geral's place while the first download brings the groups down one by one, with
 * "3 de 7 grupos" and its bar once the server said how many are coming (5.1d); offline the bar stops and the card
 * says the groups come once the browser reconnects.
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
  // Why: offline, "Sem internet. Seus grupos aparecem quando reconectar." takes the title's and the count's place.
  return (
    <section className="flex flex-col gap-3 rounded-xl border bg-card p-4 md:p-5">
      <h2 className="text-base font-semibold">{t(progress.isWaitingForInternet ? 'common_waitingForInternetGroups' : 'groups_firstDownloadTitle')}</h2>
      <DownloadProgressBar progress={progress} />
      {!progress.isWaitingForInternet && <p className="text-sm text-muted-foreground">{hint}</p>}
    </section>
  )
}

/** Shimmering rows under the groups that came, holding the place of a few still coming. */
export function GroupSkeletonRows({ count }: { count: number }) {
  // Why: the server is what brings them, never a fast read, so they show at once.
  return (
    <LoadingRegion isDelayed={false}>
      <SkeletonRows count={count} leading="group" className="rounded-xl border bg-card" rowClassName="px-3" />
    </LoadingRegion>
  )
}
