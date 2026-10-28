import { CloudOff, Plus, ReceiptText, RefreshCw, RotateCw, UserRound, Users, Wallet } from 'lucide-react'
import { GroupsPresenter, type JsGroupsState } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar, BrandMark } from '@/components/common'
import { Button } from '@/components/ui/button'
import { RecoveryCodeDialog } from '@/features/recovery-code/RecoveryCodeDialog'
import { t, type MessageKey } from '@/i18n'
import type { AccountProvider } from '@/lib/platform'
import { cn } from '@/lib/utils'

interface Tab {
  label: MessageKey
  icon: ReactNode
  open?: (presenter: GroupsPresenter) => void
}

// Only Groups is built; the other tabs of the canvas answer "Coming soon".
const tabs: Tab[] = [
  { label: 'groups_tabGroups', icon: <Users className="size-4" /> },
  { label: 'groups_tabBalances', icon: <Wallet className="size-4" />, open: (p) => p.openBalances() },
  { label: 'groups_tabContacts', icon: <UserRound className="size-4" />, open: (p) => p.openContacts() },
  { label: 'groups_tabActivity', icon: <ReceiptText className="size-4" />, open: (p) => p.openActivity() },
]

// The canvas names the account under the profile; "No account" covers both local only and anonymous.
const accountLabels: Record<AccountProvider, MessageKey> = {
  google: 'groups_googleAccount',
  apple: 'groups_appleAccount',
}

// Everything synced has no badge: it only shows when the sync asks for attention.
const syncLabels: Record<string, { label: MessageKey; icon: ReactNode }> = {
  OFFLINE: { label: 'groups_syncOffline', icon: <CloudOff className="size-3.5" /> },
  SYNCING: { label: 'groups_syncSyncing', icon: <RefreshCw className="size-3.5" /> },
  FAILED: { label: 'groups_syncFailed', icon: <RotateCw className="size-3.5" /> },
}

/**
 * The first home, before any group exists. Desktop (1024+) has the full sidebar, tablet (768–1023) an
 * icon rail and the phone a top bar with a bottom navigation, as the canvas draws them.
 */
export function GroupsScreen() {
  const presenter = usePresenter(() => new GroupsPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsGroupsState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onShow: (show: boolean) => void) => p.observeComingSoon(onShow), () => toast(t('groups_comingSoon')))
  if (!presenter || !state) return null

  const sync = state.syncBadge ? (syncLabels[state.syncBadge] ?? syncLabels.SYNCING) : null
  const account = state.accountProvider ? accountLabels[state.accountProvider as AccountProvider] : 'groups_noAccount'
  return (
    <div className="flex min-h-dvh">
      <aside className="sticky top-0 hidden h-dvh w-16 shrink-0 flex-col border-r bg-sidebar p-3 md:flex lg:w-60">
        <BrandMark showsName={false} className="h-10 px-1 lg:hidden" />
        <BrandMark className="hidden h-10 px-1.5 lg:flex" />
        <nav aria-label={t('groups_mainNavigation')} className="mt-4 flex flex-col gap-0.5">
          {tabs.map((tab, index) => (
            <button
              key={tab.label}
              aria-label={t(tab.label)}
              aria-current={index === 0 ? 'page' : undefined}
              onClick={() => tab.open?.(presenter)}
              className={cn(
                'flex h-9 items-center justify-center gap-2.5 rounded-md px-2.5 text-sm font-medium outline-none hover:bg-sidebar-accent focus-visible:ring-[3px] focus-visible:ring-ring lg:justify-start',
                index === 0 ? 'bg-sidebar-accent text-sidebar-accent-foreground' : 'text-muted-foreground',
              )}
            >
              {tab.icon}
              <span className="hidden lg:inline">{t(tab.label)}</span>
            </button>
          ))}
        </nav>
        <button
          onClick={() => presenter.openProfile()}
          className="mt-auto flex h-12 items-center justify-center gap-2.5 rounded-lg px-1.5 text-left outline-none hover:bg-sidebar-accent focus-visible:ring-[3px] focus-visible:ring-ring lg:justify-start"
        >
          <Avatar initial={state.avatarInitial} color={state.avatarColor} photoUrl={state.photoUrl} className="size-7 text-xs" />
          {/* Why: the rail hides the name and account only visually, so they stay the button's accessible name. */}
          <span className="sr-only min-w-0 flex-col lg:not-sr-only lg:flex">
            <span className="truncate text-sm font-medium">{state.displayName || t('groups_profile')}</span>
            <span className="text-xs text-muted-foreground">{t(account)}</span>
          </span>
        </button>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center gap-2.5 border-b px-4 md:hidden">
          <BrandMark className="mr-auto" />
          <button aria-label={t('groups_profile')} onClick={() => presenter.openProfile()} className="rounded-full outline-none focus-visible:ring-[3px] focus-visible:ring-ring">
            <Avatar initial={state.avatarInitial} color={state.avatarColor} photoUrl={state.photoUrl} className="size-9 text-sm" />
          </button>
        </header>

        <main className="flex flex-1 flex-col gap-6 p-4 sm:p-8">
          <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
            <h1 className="text-2xl font-bold tracking-tight">{t('groups_title')}</h1>
            {sync && (
              <SyncBadge canRetry={state.canRetrySync} onRetry={() => presenter.retrySync()}>
                {sync.icon}
                {t(sync.label)}
              </SyncBadge>
            )}
          </div>
          <section className="flex flex-1 flex-col items-center justify-center gap-3 rounded-xl border border-dashed p-8 text-center">
            <span aria-hidden className="flex size-12 items-center justify-center rounded-xl bg-muted text-muted-foreground">
              <Users className="size-6" />
            </span>
            <h2 className="mt-1 text-lg font-semibold">{t('groups_emptyTitle')}</h2>
            <p className="max-w-sm text-sm text-muted-foreground">{t('groups_emptyBody')}</p>
            <div className="flex w-full flex-col gap-2 pt-2 sm:w-auto sm:flex-row">
              <Button onClick={() => presenter.createGroup()}>
                <Plus /> {t('groups_create')}
              </Button>
              <Button variant="outline" onClick={() => presenter.joinWithLink()}>
                {t('groups_joinWithLink')}
              </Button>
            </div>
          </section>
        </main>

        <nav aria-label={t('groups_mainNavigation')} className="sticky bottom-0 grid h-16 grid-cols-4 border-t bg-card md:hidden">
          {tabs.map((tab, index) => (
            <button
              key={tab.label}
              aria-current={index === 0 ? 'page' : undefined}
              onClick={() => tab.open?.(presenter)}
              className={cn('flex flex-col items-center justify-center gap-1 text-[11px] font-medium', index === 0 ? 'text-foreground' : 'text-muted-foreground')}
            >
              {tab.icon}
              {t(tab.label)}
            </button>
          ))}
        </nav>
      </div>

      {state.showsRecoveryCodePrompt && <RecoveryCodeDialog />}
    </div>
  )
}

const syncBadgeClass = 'inline-flex h-6 items-center gap-1.5 rounded-md border px-2 text-xs font-medium text-muted-foreground'

function SyncBadge({ canRetry, onRetry, children }: { canRetry: boolean; onRetry: () => void; children: ReactNode }) {
  if (!canRetry) return <span className={syncBadgeClass}>{children}</span>
  return (
    <button
      onClick={onRetry}
      className={cn(syncBadgeClass, 'outline-none hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring')}
    >
      {children}
    </button>
  )
}
