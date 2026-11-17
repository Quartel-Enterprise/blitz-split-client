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
        <h1 className="me-auto text-lg font-semibold">{t('releaseNotes_title')}</h1>
        {/* Why: a real link, so the browser opens the tab from the click itself; the presenter only counts it. */}
        <Button variant="ghost" size="icon" asChild>
          <a href={state.allReleasesUrl} target="_blank" rel="noopener noreferrer" aria-label={t('releaseNotes_openOnGitHub')} title={t('releaseNotes_openOnGitHub')} onClick={() => presenter.allReleasesOpened()}>
            <GitHubLogo />
          </a>
        </Button>
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
          <ReleaseCard key={release.version} release={release} onOpen={() => presenter.releaseOpened(release.version)} />
        ))}
      </main>
    </div>
  )
}

/**
 * A version's card: its title is a link to its release on GitHub, stretched over the whole card so a click anywhere
 * opens it in another tab, the browser's own way.
 */
function ReleaseCard({ release, onOpen }: { release: JsReleaseNotes; onOpen: () => void }) {
  return (
    <Card className="relative transition-colors hover:bg-accent/40 has-[a:focus-visible]:ring-2 has-[a:focus-visible]:ring-ring">
      <CardHeader>
        <CardTitle className="flex flex-wrap items-center gap-2">
          <h2>
            <a href={release.releaseUrl} target="_blank" rel="noopener noreferrer" aria-label={release.version} className="after:absolute after:inset-0 after:rounded-xl focus-visible:outline-none" onClick={onOpen}>
              {release.name}
            </a>
          </h2>
          {release.betaNumber != null && <Badge variant="outline">{t('releaseNotes_beta', { number: release.betaNumber })}</Badge>}
          {release.isInstalled && <Badge variant="secondary">{t('releaseNotes_thisVersion')}</Badge>}
        </CardTitle>
        <CardDescription>
          <time dateTime={release.date}>{formatReleaseDate(release.date)}</time>
        </CardDescription>
      </CardHeader>
      <CardContent>
        <ul className="flex list-disc flex-col gap-1.5 ps-5 text-sm [overflow-wrap:anywhere]">
          {release.notes.map((note, index) => (
            <li key={index}>{note}</li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}

/** GitHub's mark, which lucide no longer draws. */
function GitHubLogo() {
  return (
    <svg aria-hidden viewBox="0 0 24 24" fill="currentColor" className="size-5">
      <path d="M12 2A10 10 0 0 0 2 12c0 4.42 2.87 8.17 6.84 9.5.5.08.66-.23.66-.5v-1.69c-2.77.6-3.36-1.34-3.36-1.34-.46-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.6.07-.6 1 .07 1.53 1.03 1.53 1.03.87 1.52 2.34 1.07 2.91.83.09-.65.35-1.09.63-1.34-2.22-.25-4.55-1.11-4.55-4.92 0-1.11.38-2 1.03-2.71-.1-.25-.45-1.29.1-2.64 0 0 .84-.27 2.75 1.02.79-.22 1.65-.33 2.5-.33.85 0 1.71.11 2.5.33 1.91-1.29 2.75-1.02 2.75-1.02.55 1.35.2 2.39.1 2.64.65.71 1.03 1.6 1.03 2.71 0 3.82-2.34 4.66-4.57 4.91.36.31.69.92.69 1.85V21c0 .27.16.59.67.5C19.14 20.16 22 16.42 22 12A10 10 0 0 0 12 2z" />
    </svg>
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
