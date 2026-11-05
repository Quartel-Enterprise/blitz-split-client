import { ArrowLeft, ArrowUpRight, ChevronRight, CloudOff, Download, Info, KeyRound, Loader2, LogOut, Plus, RotateCw, Tag } from 'lucide-react'
import { ProfilePresenter, type JsPaymentKeyItem, type JsProfileAction, type JsProfileState } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { AccountButtons, providerLabel, segmentClass } from '@/components/common'
import { pageContentClass } from '@/components/HomeShell'
import { PageShell } from '@/components/HomeSidebar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { LoadingRegion, Skeleton, SkeletonLine } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import { NotificationSettingsCard } from '@/features/notification-settings/NotificationSettingsCard'
import { RecoveryCodeDialog } from '@/features/recovery-code/RecoveryCodeDialog'
import { t, type MessageKey } from '@/i18n'
import { downloadExport } from '@/lib/export'
import type { PaymentKeyFormat, PaymentKeyType } from '@/lib/paymentKey'
import type { AccountProvider } from '@/lib/platform'
import { cn } from '@/lib/utils'
import { ProfileAppRow } from '@/features/app-download/AppDownload'
import { EditProfileCard } from './EditProfileCard'
import { ProfileDialogs } from './ProfileDialogs'

const themes: { code: string; label: MessageKey }[] = [
  { code: 'system', label: 'profile_themeSystem' },
  { code: 'light', label: 'profile_themeLight' },
  { code: 'dark', label: 'profile_themeDark' },
]

/**
 * The profile page, in the home's frame with its footer selected. Name, photo and color are edited right here, in
 * "Your profile". On desktop (1024+) the account sits in the left column under it, with signing out right below it
 * (beta feedback 16); on tablet and phone everything is one column, in the same order, the phone with back on top and
 * the home's navigation at the bottom.
 */
export function ProfileScreen() {
  const presenter = usePresenter(() => new ProfilePresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsProfileState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: JsProfileAction) => void) => p.observeActions(onAction), (action) => {
    if (action.type === 'copy') {
      void navigator.clipboard.writeText(action.value).then(() => toast(t('recoveryCode_copied')))
    } else if (action.type === 'open') {
      window.open(action.value, '_blank', 'noopener,noreferrer')
    } else if (action.type === 'export') {
      downloadExport(action.value)
    }
  })
  if (!presenter || !state) return null

  const visibility = (
    <Button variant="ghost" size="icon" aria-label={t('profile_visibility')} onClick={() => presenter.visibilityInfo()}>
      <Info />
    </Button>
  )
  return (
    <PageShell phoneBars="nav">
      <div className="flex-1 bg-muted/40">
        <header className="sticky top-0 z-10 flex h-14 items-center gap-2 border-b bg-background/95 px-2 backdrop-blur md:hidden">
          <Button variant="ghost" size="icon" aria-label={t('common_back')} onClick={() => presenter.back()}>
            <ArrowLeft />
          </Button>
          <h1 className="mr-auto text-lg font-semibold">{t('profile_title')}</h1>
          {visibility}
        </header>
        <main className={cn(pageContentClass, 'gap-4')}>
          {/* Why: the sidebar's selected footer already says where this is; the heading says what it is for. */}
          <div className="hidden items-start gap-3 md:flex">
            <div className="mr-auto flex flex-col gap-1">
              <h1 className="text-2xl leading-[30px] font-semibold tracking-tight">{t('profile_title')}</h1>
              <p className="text-sm text-muted-foreground">{t('profile_subtitle')}</p>
            </div>
            {visibility}
          </div>
          {state.showsOfflineNotice && (
            <div role="status" className="flex items-start gap-3 rounded-lg border bg-card p-3 text-sm">
              <CloudOff className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
              <p>
                <strong className="font-semibold">{t('profile_offlineTitle')}</strong> {t('profile_offlineBody')}
              </p>
            </div>
          )}
          {/* One column below 1024 (profile, keys, account…); the columns only group the cards from there up */}
          <div className="flex flex-col gap-4 lg:grid lg:grid-cols-2 lg:items-start">
            <div className="contents lg:flex lg:flex-col lg:gap-4">
              <div className="order-1">
                <EditProfileCard isPending={state.showsProfilePending} />
              </div>
              {/* Why: right under the profile, in the one column too (same order, after it in the page). */}
              {state.appInviteStatus && state.appInviteLink && (
                <ProfileAppRow
                  status={state.appInviteStatus}
                  link={state.appInviteLink}
                  onOpenCode={() => presenter.openAppCode()}
                  onOpenLink={() => presenter.trackAppLink()}
                  onDismiss={() => presenter.dismissAppInvite()}
                  className="order-1"
                />
              )}
              <AccountCard presenter={presenter} state={state} className="order-3" />
              {/* Why: "Sair" or "Apagar perfil e sair" depends on the account, never guessed before it was read. */}
              {state.isLoaded ? (
                <Button
                  variant="outline"
                  className="order-4 text-destructive hover:text-destructive"
                  disabled={!state.canSignOut}
                  onClick={() => presenter.signOut()}
                >
                  <LogOut /> {t(state.hasAccount ? 'profile_signOut' : 'profile_eraseAndSignOut')}
                </Button>
              ) : (
                <LoadingRegion className="order-4">
                  <Skeleton className="h-9 w-full" />
                </LoadingRegion>
              )}
            </div>
            <div className="contents lg:flex lg:flex-col lg:gap-4">
              <PaymentKeysCard presenter={presenter} state={state} className="order-2" />
              <AppearanceCard presenter={presenter} state={state} className="order-5" />
              <NotificationSettingsCard className="order-6" />
              <Card className="order-6">
                <CardHeader>
                  <CardTitle>{t('profile_categoryMap')}</CardTitle>
                  <CardDescription>{t('profile_categoryMapDetail')}</CardDescription>
                </CardHeader>
                <CardContent>
                  <Button variant="outline" onClick={() => presenter.categoryMap()}>
                    <Tag /> {t('profile_categoryMapOpen')}
                  </Button>
                </CardContent>
              </Card>
              <AboutCard presenter={presenter} className="order-7" />
              {state.showsDeleteAccount && <DeleteAccountCard presenter={presenter} className="order-8" />}
            </div>
          </div>
        </main>
        <ProfileDialogs presenter={presenter} state={state} />
        {state.showsRecoveryCodePrompt && <RecoveryCodeDialog />}
      </div>
    </PageShell>
  )
}

function Row({ title, detail, children }: { title: string; detail?: string | null; children?: ReactNode }) {
  return (
    <div className="flex min-h-12 items-center gap-3 py-2">
      <div className="flex min-w-0 flex-1 flex-col">
        <span className="text-sm font-medium">{title}</span>
        {detail && <span className="text-xs text-muted-foreground">{detail}</span>}
      </div>
      {children}
    </div>
  )
}

interface CardProps {
  presenter: ProfilePresenter
  state: JsProfileState
  className?: string
}

const keyFormatSuffixes: Partial<Record<PaymentKeyFormat, MessageKey>> = {
  SEGWIT: 'profile_keyRowFormatSEGWIT',
  TAPROOT: 'profile_keyRowFormatTAPROOT',
  LNURL: 'profile_keyRowFormatLNURL',
}

/** "Keys to receive payments": each key masked, its default marked, and "Add" asking Pix or Bitcoin first. */
function PaymentKeysCard({ presenter, state, className }: CardProps) {
  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle>{t('profile_keysTitle')}</CardTitle>
        <CardDescription>{t('profile_keysSubtitle')}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col divide-y">
        {!state.paymentKeys && <RowsSkeleton count={2} />}
        {state.paymentKeys?.map((key) => (
          <button
            key={key.id}
            onClick={() => presenter.clickPaymentKey(key.id)}
            className="flex min-h-14 items-center gap-3 py-2 text-left outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
          >
            <span className="flex min-w-0 flex-1 flex-col gap-0.5">
              <span className="text-sm font-medium">{t(`profile_keyRow${key.type as PaymentKeyType}`)}</span>
              <span className="truncate font-mono text-[13px] text-muted-foreground">{keyRowValue(key)}</span>
            </span>
            {key.isDefault && <Badge variant="secondary">{t('profile_keysDefault')}</Badge>}
            <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
          </button>
        ))}
        {/* Why: a key added before the account's own came down could take the default the server already gave another. */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild disabled={!state.canAddPaymentKey}>
            <button className="flex min-h-12 items-center gap-3 text-left text-sm font-medium text-primary outline-none focus-visible:ring-[3px] focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50">
              <Plus className="size-4 shrink-0" />
              {t('profile_keysAdd')}
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start">
            <DropdownMenuItem onSelect={() => presenter.addPaymentKey('PIX')}>{t('profile_keysAddPix')}</DropdownMenuItem>
            <DropdownMenuItem onSelect={() => presenter.addPaymentKey('BITCOIN')}>{t('profile_keysAddBitcoin')}</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </CardContent>
    </Card>
  )
}

// Why: "bc1p5d7r…sxg3297 · Taproot", since on-chain and Lightning keys come in two formats each.
function keyRowValue(key: JsPaymentKeyItem): string {
  const suffix = keyFormatSuffixes[key.format as PaymentKeyFormat]
  return suffix ? t(suffix, { value: key.maskedValue }) : key.maskedValue
}

function AccountCard({ presenter, state, className }: CardProps) {
  const provider = state.accountProvider as AccountProvider | null
  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle>{t('profile_account')}</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col divide-y">
        {!state.isLoaded ? (
          // Why: never "Sem conta" with Conectar before the account was read.
          <RowsSkeleton count={2} />
        ) : provider ? (
          <Row title={providerLabel(provider, true)} detail={state.maskedEmail && t('profile_accountConnectedDetail', { email: state.maskedEmail })}>
            <Badge variant="secondary">{t('profile_accountConnected')}</Badge>
            <Button size="sm" variant="outline" onClick={() => presenter.account()}>
              {t('profile_accountDisconnect')}
            </Button>
          </Row>
        ) : (
          <div className="flex flex-col gap-3 pb-3">
            <Row title={t('profile_accountNone')} detail={t('profile_accountNoneDetail')}>
              {!state.isConnectExpanded && (
                <Button size="sm" variant="outline" disabled={!state.canConnect} onClick={() => presenter.connect()}>
                  {t('profile_connect')}
                </Button>
              )}
            </Row>
            {state.showsConnectButtons && (
              <AccountButtons
                short
                isEnabled={state.areAccountButtonsEnabled}
                connectingProvider={state.connectingProvider}
                onClick={(account) => presenter.clickProvider(account)}
              />
            )}
            {state.hasSignInFailed && <p className="text-sm text-destructive">{t('profile_connectFailed')}</p>}
          </div>
        )}
        {state.showsRecoveryCodeRow && (
          <button onClick={() => presenter.recoveryCode()} className="flex items-center gap-3 text-left outline-none focus-visible:ring-[3px] focus-visible:ring-ring">
            <KeyRound className="size-4 shrink-0 text-muted-foreground" />
            <Row title={t('profile_recoveryCode')} detail={t('profile_recoveryCodeDetail')} />
          </button>
        )}
        {state.syncStatus && (
          <Row title={t('profile_sync')} detail={syncDetail(state)}>
            {state.syncStatus === 'SYNCING' && <Loader2 className="size-4 animate-spin text-muted-foreground" />}
            {state.canRetrySync && (
              <Button size="sm" variant="ghost" onClick={() => presenter.retrySync()}>
                <RotateCw /> {t('profile_syncRetry')}
              </Button>
            )}
          </Row>
        )}
        {state.isSignOutScheduled && (
          <Row title={t('profile_signOutScheduled')} detail={t('profile_signOutScheduledDetail')}>
            <Button size="sm" variant="ghost" onClick={() => presenter.cancelScheduledSignOut()}>
              {t('common_cancel')}
            </Button>
          </Row>
        )}
      </CardContent>
    </Card>
  )
}

/** A card's rows still being read: a title and its detail each, after 150 ms. */
function RowsSkeleton({ count }: { count: number }) {
  return (
    <LoadingRegion className="flex flex-col divide-y">
      {Array.from({ length: count }, (_, index) => (
        <span key={index} className="flex min-h-14 items-center gap-3 py-2">
          <span className="flex flex-1 flex-col gap-2">
            <SkeletonLine className={index === 0 ? 'w-1/3' : 'w-2/5'} />
            <SkeletonLine className="h-2.5 w-1/2" />
          </span>
          <Skeleton className="h-8 w-20" />
        </span>
      ))}
    </LoadingRegion>
  )
}

function syncDetail(state: JsProfileState): string {
  switch (state.syncStatus) {
    case 'SYNCING':
      return t('profile_syncSyncing')
    case 'FAILED':
      return t('profile_syncFailed')
    default:
      if (!state.hasPendingChanges) return t('profile_syncWaitingEmpty')
      return t(state.pendingChangeCount === 1 ? 'profile_syncWaitingOne' : 'profile_syncWaitingOther', { count: state.pendingChangeCount })
  }
}

function AppearanceCard({ presenter, state, className }: CardProps) {
  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle id="appearance">{t('profile_appearance')}</CardTitle>
      </CardHeader>
      <CardContent>
        {/* Why: no theme is marked before the saved one was read. */}
        {!state.isLoaded ? (
          <LoadingRegion>
            <Skeleton className="h-10 w-full rounded-lg" />
          </LoadingRegion>
        ) : (
          <div role="radiogroup" aria-labelledby="appearance" className="grid grid-cols-3 gap-1 rounded-lg bg-muted p-1">
            {themes.map((theme) => {
              const isSelected = theme.code === state.theme
              return (
                <button
                  key={theme.code}
                  role="radio"
                  aria-checked={isSelected}
                  onClick={() => presenter.selectTheme(theme.code)}
                  className={cn(
                    'h-8 rounded-md text-sm font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
                    segmentClass(isSelected),
                  )}
                >
                  {t(theme.label)}
                </button>
              )
            })}
          </div>
        )}
        {state.isLoaded && (
          <label htmlFor="bitcoin-in-sats" className="mt-4 flex items-center gap-3">
            <span className="flex min-w-0 flex-1 flex-col gap-0.5">
              <span className="text-sm font-medium">{t('profile_bitcoinInSats')}</span>
              <span className="text-xs text-muted-foreground">{t('profile_bitcoinInSatsDetail')}</span>
            </span>
            <Switch id="bitcoin-in-sats" checked={state.showsBitcoinInSats} onCheckedChange={(checked) => presenter.toggleBitcoinInSats(checked)} />
          </label>
        )}
      </CardContent>
    </Card>
  )
}

function AboutCard({ presenter, className }: { presenter: ProfilePresenter; className?: string }) {
  const links: { label: MessageKey; open: () => void }[] = [
    { label: 'profile_visibility', open: () => presenter.visibilityInfo() },
    { label: 'profile_privacyPolicy', open: () => presenter.privacyPolicy() },
    { label: 'profile_terms', open: () => presenter.terms() },
  ]
  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle>{t('profile_about')}</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col divide-y">
        {/* Built from this browser's copy, so it works offline too */}
        <Row title={t('profile_export')} detail={t('profile_exportDetail')}>
          <Button size="sm" variant="outline" onClick={() => presenter.export()}>
            <Download /> {t('profile_exportButton')}
          </Button>
        </Row>
        {links.map((link, index) => (
          <button
            key={link.label}
            onClick={link.open}
            className="flex min-h-12 items-center justify-between gap-3 text-left text-sm font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
          >
            {t(link.label)}
            {index > 0 && <ArrowUpRight className="size-4 text-muted-foreground" />}
          </button>
        ))}
        <Row title={t('profile_version')}>
          <span className="text-sm text-muted-foreground">{__APP_VERSION__}</span>
        </Row>
      </CardContent>
    </Card>
  )
}

/** "Delete account and data", at the very end and apart from "Sign out": only an account has one to delete. */
function DeleteAccountCard({ presenter, className }: { presenter: ProfilePresenter; className?: string }) {
  return (
    <Card className={cn('border-destructive', className)}>
      <CardContent className="flex flex-wrap items-center gap-x-4 gap-y-3">
        <div className="flex min-w-0 flex-[1_1_240px] flex-col gap-1">
          <h2 className="text-base font-semibold text-destructive">{t('profile_deleteAccount')}</h2>
          <p className="text-[13px] text-muted-foreground">{t('profile_deleteAccountDetail')}</p>
        </div>
        <Button
          variant="outline"
          className="w-full border-destructive text-destructive hover:text-destructive sm:w-auto"
          onClick={() => presenter.deleteAccount()}
        >
          {t('profile_deleteAccountButton')}
        </Button>
      </CardContent>
    </Card>
  )
}
