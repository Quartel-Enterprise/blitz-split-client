import { ArrowLeft, CloudOff } from 'lucide-react'
import { ReleaseNotesPresenter, type JsReleaseNotes, type JsReleaseNotesState } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { PageProblem } from '@/components/common'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { LoadingRegion, SkeletonCard, SkeletonLine } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import { t } from '@/i18n'
import { uiTag } from '@/lib/currency'

// Why: the day is a calendar date, so it is read and written in UTC and never shifts with the reader's time zone.
const releaseDateFormat = new Intl.DateTimeFormat(uiTag, { dateStyle: 'long', timeZone: 'UTC' })

/** "November 14, 2026" in English, "14 de novembro de 2026" in Portuguese, from the presenter's `YYYY-MM-DD`. */
function formatReleaseDate(date: string): string {
  const [year, month, day] = date.split('-').map(Number)
  return releaseDateFormat.format(Date.UTC(year, month - 1, day))
}

/**
 * "Release notes" under the profile's "Privacy and about": "Show beta versions" and a card per version with what it
 * changed on the web, newest first. The shared code already picked the versions; this only draws them.
 */
export function ReleaseNotesScreen() {
  const presenter = usePresenter(() => new ReleaseNotesPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsReleaseNotesState) => void) => p.observeState(onChange))
  if (!presenter || !state) return null

  return (
    <div className="min-h-dvh bg-muted/40">
      <header className="sticky top-0 z-10 flex h-14 items-center gap-2 border-b bg-background/95 px-2 backdrop-blur sm:px-4">
        <Button variant="ghost" size="icon" aria-label={t('common_back')} onClick={() => presenter.back()}>
          <ArrowLeft />
        </Button>
        <h1 className="mr-auto text-lg font-semibold">{t('releaseNotes_title')}</h1>
      </header>
      {/* Why: a reading width, since the notes are sentences. */}
      <main className="mx-auto flex w-full max-w-2xl flex-col gap-4 p-4 sm:p-8">
        {state.showsBetaSwitch && (
          <label htmlFor="show-beta-versions" className="flex min-h-12 items-center gap-3 rounded-xl border bg-card px-4 py-2">
            <span className="min-w-0 flex-1 text-sm font-medium">{t('releaseNotes_showBeta')}</span>
            <Switch id="show-beta-versions" checked={state.showsBeta} onCheckedChange={(checked) => presenter.toggleBeta(checked)} />
          </label>
        )}
        {state.showsSkeleton && <ReleasesSkeleton />}
        {state.showsLoadFailed && (
          <PageProblem icon={<CloudOff className="size-10" />} title={t('releaseNotes_loadFailedTitle')} message={t('releaseNotes_loadFailedBody')}>
            <Button size="lg" onClick={() => presenter.retry()}>
              {t('releaseNotes_retry')}
            </Button>
          </PageProblem>
        )}
        {state.showsEmpty && (
          <p className="pt-8 text-center text-sm text-muted-foreground">{t(state.hasOnlyBetaReleases ? 'releaseNotes_emptyOnlyBeta' : 'releaseNotes_empty')}</p>
        )}
        {state.releases.map((release) => (
          <ReleaseCard key={release.version} release={release} />
        ))}
      </main>
    </div>
  )
}

function ReleaseCard({ release }: { release: JsReleaseNotes }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex flex-wrap items-center gap-2">
          <h2>{release.name}</h2>
          {release.betaNumber != null && <Badge variant="outline">{t('releaseNotes_beta', { number: release.betaNumber })}</Badge>}
          {release.isInstalled && <Badge variant="secondary">{t('releaseNotes_thisVersion')}</Badge>}
        </CardTitle>
        <CardDescription>
          <time dateTime={release.date}>{formatReleaseDate(release.date)}</time>
        </CardDescription>
      </CardHeader>
      <CardContent>
        <ul className="flex list-disc flex-col gap-1.5 pl-5 text-sm [overflow-wrap:anywhere]">
          {release.notes.map((note, index) => (
            <li key={index}>{note}</li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}

function ReleasesSkeleton() {
  return (
    <LoadingRegion className="flex flex-col gap-4">
      {[3, 2, 4].map((lines, index) => (
        <SkeletonCard key={index}>
          <SkeletonLine className="h-4 w-1/4" />
          <SkeletonLine className="h-2.5 w-1/3" />
          {Array.from({ length: lines }, (_, line) => (
            <SkeletonLine key={line} className={line === lines - 1 ? 'w-2/3' : undefined} />
          ))}
        </SkeletonCard>
      ))}
    </LoadingRegion>
  )
}
