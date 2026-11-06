import { Banknote, BellOff, BellRing, Camera, KeyRound, SquarePlus, Users, X, type LucideIcon } from 'lucide-react'
import { HomePresenter, type JsHomeState } from 'blitzsplit-shared'
import { useEffect, type ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { SplitBillIcon } from '@/components/common'
import { ActivityBell, HomeShell, loadedProfile, pageContentClass, type HomeTab } from '@/components/HomeShell'
import { ListCardSkeleton, SummaryCardsSkeleton } from '@/components/PageSkeletons'
import { Button } from '@/components/ui/button'
import { LoadingRegion, Skeleton, SkeletonCircle, SkeletonLine } from '@/components/ui/skeleton'
import { useRecordedPaymentNotice } from '@/features/balances/BalanceCard'
import { useDeletedGroupNotice } from '@/features/groups/GroupsScreen'
import { toastGroupAction } from '@/features/group-detail/GroupExitDialog'
import { DownloadProgressBar } from '@/features/groups/FirstDownload'
import { showsSyncStatus, SyncStatus } from '@/features/groups/GroupsPage'
import { useHomeSearch } from '@/features/groups/SearchDialog'
import { AppBanner, AppInviteCard, toastUsedOnPhone } from '@/features/app-download/AppDownload'
import { HomeSummaryCards, PeopleCard, RecentBills, YourGroups } from '@/features/home/HomeSections'
import { RecoveryCodeDialog } from '@/features/recovery-code/RecoveryCodeDialog'
import { t, type MessageKey } from '@/i18n'
import { cn } from '@/lib/utils'
import { isWebPushSupported, requestNotificationPermission, watchNotificationPermission } from '@/push/webPush'

// Grupos, Saldos and Contatos take their place right above Início.
const tabEvents: Partial<Record<HomeTab, (presenter: HomePresenter) => void>> = {
  groups: (p) => p.openGroups(),
  balances: (p) => p.openBalances(),
  contacts: (p) => p.openContacts(),
}

/**
 * Início (flow 10), the home's first tab and where the app opens, in the home's frame (HomeShell). Desktop: the title
 * row with the shortcuts beside "Rachar conta", the three summary cards, the faces, then "Últimas despesas" as a table
 * beside "Seus grupos". Tablet and phone: the shortcuts as a row of tiles and one column, as the canvas draws them.
 */
export function HomeScreen() {
  const presenter = usePresenter(() => new HomePresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsHomeState) => void) => p.observeState(onChange))
  const search = useHomeSearch(presenter, state?.canSearch ?? false)
  useActions(
    presenter,
    (p, onAction: (action: string) => void) => p.observeActions(onAction),
    (action) => {
      if (action === 'notifications-on') toast(t('home_notificationsOn'))
      // Why: only where the card was drawn; a phone's browser never shows it (14.3).
      else if (action === 'used-on-phone') {
        if (window.matchMedia('(min-width: 768px)').matches) toastUsedOnPhone()
      }
      else if (action === 'request-notification-permission') {
        void requestNotificationPermission().then((permission) => presenter?.notificationPermissionChecked(permission))
      } else toastGroupAction(action)
    },
  )
  useNotificationPermission(presenter)
  // Why: Registrar pagamento, which "Quem pagou a quem?" or a face opened over Início, just closed.
  useRecordedPaymentNotice(presenter, state?.recordedPayment)
  useDeletedGroupNotice(presenter, state)
  if (!presenter || !state) return null

  const hasSyncStatus = showsSyncStatus(state, state.firstDownload)
  return (
    <HomeShell
      selected="home"
      profile={loadedProfile(state)}
      onTab={(tab) => tabEvents[tab]?.(presenter)}
      onProfile={() => presenter.openProfile()}
      onBrand={() => presenter.openBrand()}
      search={search}
      bell={{ unreadCount: state.activityUnreadCount, onOpen: () => presenter.openActivity() }}
      syncStatus={hasSyncStatus && <SyncStatus state={state} presenter={presenter} downloadProgress={state.firstDownload} />}
      overlays={state.showsRecoveryCodePrompt && <RecoveryCodeDialog />}
    >
      <main className={cn(pageContentClass, 'gap-3.5 md:gap-4 lg:gap-5')}>
        {/* Why: a phone's browser gets the banner (14.3), never the card nor a QR code. */}
        {state.showsAppBanner && state.appInvite && <AppBanner invite={state.appInvite} onOpen={() => presenter.trackAppBannerOpen()} onDismiss={() => presenter.dismissAppBanner()} />}
        <div className="flex min-h-9 flex-col items-stretch gap-3 md:flex-row md:items-center">
          {/* Why: the sync status beside the title from the tablet up, in the phone's top bar below, never above the page. */}
          <div className="flex min-w-0 items-center gap-3 md:mr-auto">
            {/* Why: the phone's bottom bar already names the page; the heading stays for screen readers. */}
            <h1 className="sr-only text-2xl leading-8 font-semibold tracking-tight md:not-sr-only">{t('home_title')}</h1>
            {hasSyncStatus && (
              <div className="hidden min-w-0 items-center gap-2 md:flex">
                <SyncStatus state={state} presenter={presenter} downloadProgress={state.firstDownload} />
              </div>
            )}
          </div>
          {state.showsHome && (
            <div className="flex flex-col items-stretch gap-2 md:flex-row md:items-center">
              <Shortcuts presenter={presenter} state={state} variant="buttons" />
              {state.showsSplitBillButton && (
                <Button className="h-11 md:h-9" onClick={() => presenter.splitBill()}>
                  <SplitBillIcon /> {t('groups_splitBill')}
                </Button>
              )}
            </div>
          )}
          {/* Why: the phone's top bar holds the bell; from the tablet up it ends Início's title row. */}
          <ActivityBell unreadCount={state.activityUnreadCount} onOpen={() => presenter.openActivity()} className="hidden md:flex" />
        </div>
        {state.showsHome && <Shortcuts presenter={presenter} state={state} variant="tiles" />}

        {/* Why: only the first download's bar; the status itself is in the title row or the phone's top bar. */}
        {state.firstDownload && <DownloadProgressBar progress={state.firstDownload} className="max-w-80" />}

        {!state.isLoaded && !state.firstDownload && <HomeSkeleton />}

        {state.showsFirstUse && <FirstUseCard presenter={presenter} />}

        {state.showsHome && (
          <>
            {state.showsRecoveryCodeCard && <RecoveryCodeCard presenter={presenter} />}
            {state.notificationPrompt && <NotificationCard presenter={presenter} isBlocked={state.notificationPrompt === 'BLOCKED'} />}
            {state.showsAppInviteCard && state.appInvite && (
              <AppInviteCard
                presenter={presenter}
                invite={state.appInvite}
                needsAccount={state.appInviteNeedsAccount}
                accountProvider={state.accountProvider}
              />
            )}
            <HomeSummaryCards presenter={presenter} state={state} />
            <PeopleCard presenter={presenter} state={state} />
            <div className="flex flex-col gap-3.5 md:gap-4 lg:grid lg:grid-cols-[minmax(0,1fr)_380px] lg:items-start lg:gap-5">
              <RecentBills presenter={presenter} state={state} />
              <YourGroups presenter={presenter} state={state} />
            </div>
          </>
        )}
      </main>
    </HomeShell>
  )
}

/**
 * Início before the browser's database answered, in the shape of the page: the shortcuts, the summary cards, the
 * faces, then "Últimas despesas" beside "Seus grupos" on the desktop. Nothing says yet whether this is the first use.
 */
function HomeSkeleton() {
  return (
    <LoadingRegion className="flex flex-col gap-3.5 md:gap-4 lg:gap-5">
      <div className="grid grid-cols-3 gap-2 lg:hidden">
        {[0, 1, 2].map((index) => (
          <Skeleton key={index} className="min-h-[72px] rounded-lg md:min-h-16" />
        ))}
      </div>
      <SummaryCardsSkeleton className="gap-2 md:gap-4" />
      <div className="flex flex-col gap-3 rounded-lg border bg-card p-3 md:px-4 md:py-3.5">
        <SkeletonLine className="h-4 w-36" />
        <div className="flex gap-1.5 overflow-hidden md:gap-2">
          {[0, 1, 2, 3, 4].map((index) => (
            <span key={index} className="flex w-[72px] shrink-0 flex-col items-center gap-2 py-1.5 md:w-[88px]">
              <SkeletonCircle className="size-12 md:size-14" />
              <SkeletonLine className="w-12" />
            </span>
          ))}
        </div>
      </div>
      <div className="flex flex-col gap-3.5 md:gap-4 lg:grid lg:grid-cols-[minmax(0,1fr)_380px] lg:items-start lg:gap-5">
        <ListCardSkeleton rows={4} leading="square" className="rounded-lg" />
        <ListCardSkeleton rows={3} leading="square" className="rounded-lg" />
      </div>
    </LoadingRegion>
  )
}

interface Shortcut {
  label: MessageKey
  icon: ReactNode
  isEnabled: boolean
  onClick: () => void
}

/**
 * "Ler cupom fiscal", "Registrar pagamento" and "Novo grupo": outline buttons beside "Rachar conta" on the desktop,
 * a row of tiles under the title on the tablet and the phone. Offline only "Ler cupom fiscal" says "Precisa de
 * internet"; it stays focusable (aria-disabled), so the hint reaches the keyboard and a pointer's hover too.
 */
function Shortcuts({ presenter, state, variant }: { presenter: HomePresenter; state: JsHomeState; variant: 'buttons' | 'tiles' }) {
  const shortcuts: Shortcut[] = [
    { label: 'home_scanReceipt', icon: <Camera />, isEnabled: state.canScanReceipt, onClick: () => presenter.scanReceipt() },
    { label: 'home_recordPayment', icon: <Banknote />, isEnabled: true, onClick: () => presenter.recordPayment() },
    { label: 'groups_newGroup', icon: <SquarePlus />, isEnabled: true, onClick: () => presenter.createGroup() },
  ]
  const hint = (shortcut: Shortcut) => (shortcut.isEnabled ? undefined : t('groupExit_needsInternet'))
  if (variant === 'buttons') {
    return (
      <div className="hidden items-center gap-2 lg:flex">
        {shortcuts.map((shortcut) => (
          <Button
            key={shortcut.label}
            variant="outline"
            aria-disabled={!shortcut.isEnabled}
            title={hint(shortcut)}
            onClick={() => shortcut.isEnabled && shortcut.onClick()}
            className={cn(!shortcut.isEnabled && 'text-muted-foreground opacity-60 hover:bg-background')}
          >
            {shortcut.icon}
            {t(shortcut.label)}
          </Button>
        ))}
      </div>
    )
  }
  return (
    <nav aria-label={t('home_shortcuts')} className="grid grid-cols-3 gap-2 lg:hidden">
      {shortcuts.map((shortcut) => (
        <button
          key={shortcut.label}
          aria-disabled={!shortcut.isEnabled}
          onClick={() => shortcut.isEnabled && shortcut.onClick()}
          className={cn(
            'flex min-h-[72px] flex-col items-center justify-center gap-1 rounded-lg border bg-card px-1.5 py-2 text-center text-[13px] leading-4 font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring md:min-h-16 md:flex-row md:gap-2 [&_svg]:size-[18px] [&_svg]:shrink-0',
            shortcut.isEnabled ? 'hover:bg-accent' : 'text-muted-foreground opacity-70',
          )}
        >
          {shortcut.icon}
          <span className="flex flex-col">
            {t(shortcut.label)}
            {!shortcut.isEnabled && <span className="text-[11px] font-normal">{hint(shortcut)}</span>}
          </span>
        </button>
      ))}
    </nav>
  )
}

/** No group and nobody yet (1.3): only this card, with no shortcuts nor "Rachar conta". */
function FirstUseCard({ presenter }: { presenter: HomePresenter }) {
  return (
    <section className="mt-12 flex w-full max-w-[420px] flex-col items-center gap-2 self-center rounded-xl border bg-card px-6 pt-8 pb-6 text-center md:mt-24">
      <span aria-hidden className="flex size-12 items-center justify-center rounded-xl bg-muted">
        <Users className="size-6" />
      </span>
      <h2 className="mt-1 text-lg font-semibold">{t('groups_emptyTitle')}</h2>
      <p className="text-sm leading-normal text-muted-foreground">{t('home_firstUseBody')}</p>
      <div className="mt-2 flex w-full flex-col gap-2">
        <Button className="h-10" onClick={() => presenter.createGroup()}>
          {t('groups_create')}
        </Button>
        <Button variant="outline" className="h-10" onClick={() => presenter.joinWithLink()}>
          {t('groups_joinWithLink')}
        </Button>
      </div>
    </section>
  )
}

/** "Guarde seu código de recuperação", after the first sync of an account without Google or Apple; ✕ hides it until the next visit. */
function RecoveryCodeCard({ presenter }: { presenter: HomePresenter }) {
  return (
    <HomeActionCard id="home-recovery-title" icon={KeyRound} title={t('home_recoveryTitle')} body={t('home_recoveryBody')}>
      <Button variant="outline" size="sm" onClick={() => presenter.connectAccount()}>
        {t('home_connectAccount')}
      </Button>
      <Button size="sm" onClick={() => presenter.showRecoveryCode()}>
        {t('home_showCode')}
      </Button>
      <Button variant="ghost" size="icon-sm" aria-label={t('home_dismiss')} className="text-muted-foreground" onClick={() => presenter.dismissRecoveryCodeCard()}>
        <X />
      </Button>
    </HomeActionCard>
  )
}

/**
 * The soft ask before the browser's prompt, once there is a group, so "Not now" never spends it. Blocked, it says where
 * to allow them instead, since a page can't open the browser's settings.
 */
function NotificationCard({ presenter, isBlocked }: { presenter: HomePresenter; isBlocked: boolean }) {
  return (
    <HomeActionCard
      id="home-notifications-title"
      icon={isBlocked ? BellOff : BellRing}
      title={t(isBlocked ? 'home_notificationsOffTitle' : 'home_notificationsTitle')}
      body={t(isBlocked ? 'home_notificationsOffBody' : 'home_notificationsBody')}
    >
      <Button variant="outline" size="sm" onClick={() => presenter.dismissNotificationPrompt()}>
        {t('home_notificationsNotNow')}
      </Button>
      {!isBlocked && (
        <Button size="sm" onClick={() => presenter.turnOnNotifications()}>
          {t('home_notificationsTurnOn')}
        </Button>
      )}
    </HomeActionCard>
  )
}

/** A card of Início that asks for something: its icon, title and text, and its answers at the end. */
function HomeActionCard({ id, icon: Icon, title, body, children }: { id: string; icon: LucideIcon; title: string; body: string; children: ReactNode }) {
  return (
    <section role="note" aria-labelledby={id} className="flex flex-wrap items-center gap-3 rounded-lg border bg-card py-3 pr-3 pl-4">
      <Icon aria-hidden className="size-[18px] shrink-0 text-primary" />
      <span className="flex min-w-[200px] flex-1 flex-col gap-0.5">
        <strong id={id} className="text-sm font-semibold">
          {title}
        </strong>
        <span className="text-[13px] text-muted-foreground">{body}</span>
      </span>
      <span className="ml-auto flex items-center gap-2">{children}</span>
    </section>
  )
}

/** The browser's permission, told to Home on open, whenever the tab comes back and whenever the site settings change it. */
function useNotificationPermission(presenter: HomePresenter | null) {
  useEffect(() => {
    if (!presenter || !isWebPushSupported()) return
    return watchNotificationPermission((permission) => presenter.notificationPermissionChecked(permission))
  }, [presenter])
}
