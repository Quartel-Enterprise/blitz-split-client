import { Bell, Copy, Info, MapPin, X, type LucideIcon } from 'lucide-react'
import { AppDownloadPresenter, type HomePresenter, type JsAppDownloadAction, type JsAppDownloadState, type JsAppInvite, type JsRoute } from 'blitzsplit-shared'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { providerLabel } from '@/components/common'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { QrCode } from '@/features/invite/InviteDialog'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { formatShortDate } from '@/lib/dates'
import type { AccountProvider } from '@/lib/platform'
import { cn } from '@/lib/utils'

// Why: the link the QR code opens, as it reads, without the scheme.
const shownLink = (link: string) => link.replace(/^https:\/\//, '')

const perks: { icon: LucideIcon; chip: MessageKey; title: MessageKey; body: MessageKey }[] = [
  { icon: MapPin, chip: 'appInvite_perkNowChip', title: 'appDownload_perkNowTitle', body: 'appDownload_perkNowBody' },
  { icon: Bell, chip: 'appInvite_perkNoticesChip', title: 'appDownload_perkNoticesTitle', body: 'appDownload_perkNoticesBody' },
]

const scanLabels: Record<string, MessageKey> = {
  BOTH: 'appInvite_scanBoth',
  GOOGLE_PLAY: 'appInvite_scanGooglePlay',
  APP_STORE: 'appInvite_scanAppStore',
}

const storeLabels: Record<string, MessageKey> = {
  BOTH: 'appDownload_storesBoth',
  GOOGLE_PLAY: 'appDownload_storesGooglePlay',
  APP_STORE: 'appDownload_storesAppStore',
}

/**
 * "Take Blitz Split on your phone" (14.1), Início's card on a computer and a tablet, after the first bill and one
 * card at a time. ✕ and "I downloaded it" end it for good, here and in Profile; its QR code opens the code big (14.2).
 */
export function AppInviteCard({
  presenter,
  invite,
  needsAccount,
  accountProvider,
}: {
  presenter: HomePresenter
  invite: JsAppInvite
  needsAccount: boolean
  accountProvider: string | null | undefined
}) {
  const provider = needsAccount ? null : (accountProvider as AccountProvider | null)
  const link = shownLink(invite.link)
  return (
    <section
      aria-labelledby="home-app-invite-title"
      className="relative flex flex-wrap items-center gap-4 rounded-lg border bg-card py-4 pr-13 pl-4 max-md:hidden lg:gap-5"
    >
      <Button
        variant="ghost"
        size="icon-sm"
        aria-label={t('appInvite_dismiss')}
        title={t('appInvite_dismiss')}
        className="absolute top-2 right-2 text-muted-foreground"
        onClick={() => presenter.dismissAppInvite()}
      >
        <X />
      </Button>
      <button
        aria-label={t('appInvite_zoomQr')}
        onClick={() => presenter.openAppCode()}
        className="shrink-0 cursor-zoom-in rounded-lg border bg-white p-1 outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
      >
        <QrCode size={invite.qrSize} modules={invite.qrModules} label={t('appInvite_qrDescription')} className="size-[88px] lg:size-24" />
      </button>
      <div className="flex min-w-[240px] flex-1 flex-col gap-1.5">
        <h2 id="home-app-invite-title" className="text-base font-semibold">
          {t(needsAccount ? 'appInvite_titleNoAccount' : 'appInvite_titleWithAccount')}
        </h2>
        <p className="text-sm text-muted-foreground">
          {provider ? t('appInvite_bodyWithAccount', { provider: providerLabel(provider, true) }) : t('appInvite_bodyNoAccount')}
        </p>
        <ul aria-label={t('appInvite_perksLabel')} className="mt-1 flex flex-wrap gap-1.5">
          {perks.map(({ icon: Icon, chip }) => (
            <li key={chip} className="flex items-center gap-1.5 rounded-md bg-muted px-2 py-1 text-xs font-medium">
              <Icon aria-hidden className="size-3.5 text-primary" />
              {t(chip)}
            </li>
          ))}
        </ul>
        <span className="text-xs text-muted-foreground">
          {t(scanLabels[invite.stores] ?? 'appInvite_scanBoth')}{' '}
          <a
            href={invite.link}
            target="_blank"
            rel="noopener"
            className="font-mono text-primary underline underline-offset-2"
            onClick={() => presenter.trackAppLink()}
          >
            {link}
          </a>
        </span>
      </div>
      <div className="flex w-full justify-end gap-2 lg:w-40 lg:flex-col">
        {needsAccount && <Button onClick={() => presenter.connectFromAppInvite()}>{t('home_connectAccount')}</Button>}
        <Button variant="outline" onClick={() => presenter.markAppDownloaded()}>
          {t('appInvite_downloaded')}
        </Button>
      </div>
    </section>
  )
}

/** "We saw you on your phone." (14.1c), once, when a sync found the account on a phone app while the card showed. */
export function toastUsedOnPhone() {
  toast(t('appInvite_usedOnPhoneTitle'), { description: t('appInvite_usedOnPhoneBody') })
}

/**
 * "Blitz Split on your phone" (14.2), a dialog over Início or Perfil: the QR code big, what only the phone gives, and
 * "I downloaded it" while the invite stands. With the app already in use on a phone it says since when (14.2b).
 */
export function AppDownloadDialog({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new AppDownloadPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsAppDownloadState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  useActions(presenter, (p, onAction: (action: JsAppDownloadAction) => void) => p.observeActions(onAction), (action) => {
    if (action.type === 'copy') {
      navigator.clipboard
        .writeText(action.link)
        .then(() => toast(t('appDownload_linkCopied')))
        .catch(() => toast(t('invite_copyFailed')))
    }
  })
  if (!presenter || !state?.isLoaded) return null

  const provider = state.accountProvider as AccountProvider | null
  const body = (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col items-center gap-5 sm:flex-row sm:items-start">
        <div className="flex shrink-0 flex-col items-center gap-2">
          <span className="rounded-xl border bg-white p-2">
            <QrCode size={state.qrSize} modules={state.qrModules} label={t('appInvite_qrDescription')} className="size-40 md:size-44" />
          </span>
          <span className="flex items-center gap-1">
            <a
              href={state.link}
              target="_blank"
              rel="noopener"
              className="font-mono text-xs text-primary underline underline-offset-2"
              onClick={() => presenter.trackLink()}
            >
              {shownLink(state.link)}
            </a>
            <Button variant="ghost" size="icon-sm" aria-label={t('appDownload_copyLink')} title={t('appDownload_copyLink')} onClick={() => presenter.copyLink()}>
              <Copy />
            </Button>
          </span>
        </div>
        <ul aria-label={t('appInvite_perksLabel')} className="flex min-w-0 flex-1 flex-col gap-3.5">
          {perks.map(({ icon: Icon, title, body: text }) => (
            <li key={title} className="flex gap-3">
              <span aria-hidden className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-muted text-primary">
                <Icon className="size-4.5" />
              </span>
              <span className="flex flex-col gap-0.5">
                <strong className="text-sm font-semibold">{t(title)}</strong>
                <span className="text-sm text-muted-foreground">{t(text)}</span>
              </span>
            </li>
          ))}
        </ul>
      </div>
      {state.phoneUseSinceMillis != null && (
        <p role="note" className="flex items-start gap-2 rounded-lg border bg-muted/50 p-3 text-sm">
          <Info aria-hidden className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
          <span>{t('appDownload_inUse', { date: formatShortDate(state.phoneUseSinceMillis) })}</span>
        </p>
      )}
      <p className="text-sm text-muted-foreground">
        {provider ? t('appInvite_bodyWithAccount', { provider: providerLabel(provider, true) }) : t('appDownload_noAccountNote')}
      </p>
    </div>
  )
  const footer = (
    <>
      <Button variant="outline" onClick={() => presenter.close()}>
        {t('appDownload_close')}
      </Button>
      {state.showsDownloadedButton && <Button onClick={() => presenter.markDownloaded()}>{t('appInvite_downloaded')}</Button>}
    </>
  )
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      className="sm:max-w-[600px]"
      onOpenChange={(open) => {
        if (!open) presenter.close()
      }}
      pane={{
        title: t('appDownload_title'),
        description: `${t('appDownload_scan')} ${t(storeLabels[state.stores] ?? 'appDownload_storesBoth')}`,
        body,
        footer,
      }}
    />
  )
}

/**
 * Safari on an iPhone or iPad shows the App Store's own banner (the apple-itunes-app tag of index.html) once the app
 * is on the App Store, so ours stays hidden there; Chrome, Firefox and the others on iOS never show Apple's.
 */
function showsSmartAppBanner(stores: string): boolean {
  if (stores === 'GOOGLE_PLAY') return false
  const agent = navigator.userAgent
  return /iPhone|iPad|iPod/.test(agent) && /Safari/.test(agent) && !/CriOS|FxiOS|EdgiOS|OPiOS|GSA/.test(agent)
}

/**
 * Our banner on Início in a phone's browser (14.3): no card nor QR code, "Open in the app" goes to the app's link,
 * which opens the app when installed or its store. ✕ hides it for good on this device (a device setting), also with
 * an account, since the banner belongs to the phone's browser.
 */
export function AppBanner({ invite, onOpen, onDismiss }: { invite: JsAppInvite; onOpen: () => void; onDismiss: () => void }) {
  if (showsSmartAppBanner(invite.stores)) return null
  return (
    <aside aria-label={t('appBanner_title')} className="flex min-h-16 flex-wrap items-center gap-2 rounded-lg border bg-muted py-2 pr-3 pl-1 md:hidden">
      <Button variant="ghost" size="icon" aria-label={t('appInvite_dismiss')} className="shrink-0 text-muted-foreground" onClick={onDismiss}>
        <X />
      </Button>
      <span aria-hidden className="flex size-10 shrink-0 items-center justify-center rounded-[10px] bg-primary text-primary-foreground">
        <span className="size-6 bg-current mask-[url(/icon.svg)] mask-contain mask-center mask-no-repeat" />
      </span>
      {/* Why: the text keeps a readable width, so on a narrow phone the button wraps under it. */}
      <span className="flex min-w-24 flex-1 flex-col">
        <strong className="text-sm font-semibold">{t('appBanner_title')}</strong>
        <span className="text-xs text-muted-foreground">{t('appBanner_body')}</span>
      </span>
      <Button asChild size="sm" className="ml-auto shrink-0">
        <a href={invite.link} onClick={onOpen}>
          {t('appBanner_open')}
        </a>
      </Button>
    </aside>
  )
}

/**
 * Perfil › "App on your phone" (14.4), right under the profile: highlighted with ✕ while the invite stands, plain for
 * good after it (14.4b), "You already use the app on your phone" once a sync found it (14.4c). It never goes, as the
 * way to download later. A phone's browser shows no code: the store's link, or "Open in the app".
 */
export function ProfileAppRow({
  status,
  link,
  onOpenCode,
  onOpenLink,
  onDismiss,
  className,
}: {
  status: string
  link: string
  onOpenCode: () => void
  onOpenLink: () => void
  onDismiss: () => void
  className?: string
}) {
  const isPhone = !useMediaQuery('(min-width: 768px)')
  const isActive = status === 'ACTIVE'
  const isInUse = status === 'IN_USE'
  const body = isActive ? 'profile_appBodyActive' : isInUse ? 'profile_appBodyInUse' : isPhone ? 'profile_appBodyPhone' : 'profile_appBody'
  const action = isPhone ? (isInUse ? 'profile_appOpen' : 'profile_appDownload') : 'profile_appSeeCode'
  return (
    <section
      aria-labelledby="profile-app-title"
      className={cn('relative flex flex-wrap items-center gap-3.5 rounded-xl border bg-card p-5', isActive && 'pr-13', className)}
    >
      {isActive && (
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={t('appInvite_dismiss')}
          title={t('appInvite_dismiss')}
          className="absolute top-2 right-2 text-muted-foreground"
          onClick={onDismiss}
        >
          <X />
        </Button>
      )}
      <span
        aria-hidden
        className={cn(
          'flex size-11 shrink-0 items-center justify-center rounded-[11px]',
          isActive ? 'bg-primary text-primary-foreground' : 'bg-muted text-foreground',
        )}
      >
        <span className="size-6 bg-current mask-[url(/icon.svg)] mask-contain mask-center mask-no-repeat" />
      </span>
      <span className="flex min-w-[180px] flex-1 flex-col gap-0.5">
        <h2 id="profile-app-title" className="text-base font-semibold">
          {t(isActive ? 'profile_appTitleActive' : 'profile_appTitle')}
        </h2>
        <span className="text-sm text-muted-foreground">{t(body)}</span>
      </span>
      {isPhone ? (
        <Button asChild variant={isActive ? 'default' : 'outline'} className="h-11 w-full">
          <a href={link} target="_blank" rel="noopener" onClick={onOpenLink}>
            {t(action)}
          </a>
        </Button>
      ) : (
        <Button size="sm" variant={isActive ? 'default' : 'outline'} onClick={onOpenCode}>
          {t(action)}
        </Button>
      )}
    </section>
  )
}
