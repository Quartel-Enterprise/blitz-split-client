import { Loader2, WifiOff } from 'lucide-react'
import type { ReactNode } from 'react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { t, type MessageKey } from '@/i18n'
import { avatarStyle } from '@/lib/avatar'
import { regionCurrencySymbol } from '@/lib/currency'
import { accountProviders, type AccountProvider } from '@/lib/platform'
import { cn } from '@/lib/utils'

/**
 * The tile with the region's currency symbol ("R$", "€", "CHF"…), with the product name: Bora Rachar
 * in Portuguese, Blitz Split in every other language.
 */
export function BrandMark({ showsName = true, className }: { showsName?: boolean; className?: string }) {
  return (
    <div className={cn('flex items-center gap-2.5', className)}>
      <span
        aria-hidden
        className={cn(
          'flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary font-bold whitespace-nowrap text-primary-foreground',
          // Three-letter symbols like "CHF" would overflow a tile sized for "R$"
          regionCurrencySymbol.length >= 3 ? 'text-[0.625rem] tracking-tight' : 'text-xs',
        )}
      >
        {regionCurrencySymbol}
      </span>
      {showsName && <span className="font-semibold">{t('appName')}</span>}
    </div>
  )
}

/** "No connection." where signing in or recovering needs the internet. */
export function OfflineAlert({ children }: { children: ReactNode }) {
  return (
    <Alert role="alert">
      <WifiOff />
      <AlertDescription className="text-foreground">
        <p>
          <strong className="font-semibold">{t('common_noConnection')}</strong> {children}
        </p>
      </AlertDescription>
    </Alert>
  )
}

interface AccountButtonsProps {
  isEnabled: boolean
  connectingProvider: string | null | undefined
  onClick: (provider: AccountProvider) => void
  short?: boolean
  className?: string
}

/** Apple first on Apple devices, Google first elsewhere. Both sign in through the browser here. */
export function AccountButtons({ isEnabled, connectingProvider, onClick, short = false, className }: AccountButtonsProps) {
  return (
    <div className={cn(short ? 'grid grid-cols-2 gap-2' : 'flex flex-col gap-2', className)}>
      {accountProviders.map((provider) => (
        <Button key={provider} variant="outline" size="lg" disabled={!isEnabled} onClick={() => onClick(provider)}>
          {connectingProvider === provider ? <Loader2 className="animate-spin" /> : <ProviderLogo provider={provider} />}
          {providerLabel(provider, short)}
        </Button>
      ))}
    </div>
  )
}

/** "Google" / "Apple" when short, "Continue with Google" / "Continue with Apple" otherwise. */
export function providerLabel(provider: AccountProvider, short = false): string {
  if (provider === 'google') return t(short ? 'common_google' : 'common_continueWithGoogle')
  return t(short ? 'common_apple' : 'common_continueWithApple')
}

function ProviderLogo({ provider }: { provider: AccountProvider }) {
  if (provider === 'apple') return <AppleLogo />
  return (
    <span aria-hidden className="font-extrabold">
      G
    </span>
  )
}

function AppleLogo() {
  return (
    <svg aria-hidden viewBox="0 0 24 24" fill="currentColor" className="size-4">
      <path d="M16.37 12.6c-.02-2.2 1.8-3.26 1.88-3.31-1.03-1.5-2.62-1.7-3.18-1.73-1.35-.14-2.64.8-3.33.8-.69 0-1.74-.78-2.86-.76-1.47.02-2.83.86-3.59 2.17-1.53 2.65-.39 6.58 1.1 8.73.73 1.05 1.6 2.23 2.73 2.19 1.1-.04 1.51-.71 2.84-.71 1.32 0 1.7.71 2.86.69 1.18-.02 1.93-1.07 2.65-2.13.84-1.22 1.18-2.4 1.2-2.46-.03-.01-2.3-.88-2.3-3.48zM14.2 6.13c.6-.73 1.01-1.75.9-2.76-.87.04-1.92.58-2.54 1.31-.56.64-1.05 1.68-.92 2.67.97.08 1.96-.49 2.56-1.22z" />
    </svg>
  )
}

/** "or" between the account buttons and the account-free path. */
export function OrDivider() {
  return (
    <div aria-hidden className="flex items-center gap-3 text-xs uppercase tracking-wide text-muted-foreground">
      <span className="h-px flex-1 bg-border" />
      {t('common_or')}
      <span className="h-px flex-1 bg-border" />
    </div>
  )
}

/** The person's avatar: their initial on their avatar color. */
/** The initial stays under the photo, so it shows while the photo loads or when it fails to. */
export function Avatar({ initial, color, photoUrl, className }: { initial: string; color: string; photoUrl?: string | null; className?: string }) {
  return (
    <span aria-hidden style={avatarStyle(color)} className={cn('avatar relative flex shrink-0 items-center justify-center overflow-hidden rounded-full font-semibold', className)}>
      {initial}
      {photoUrl && (
        <img
          // Why: a new photo gets a fresh element, so one that failed before doesn't stay hidden.
          key={photoUrl}
          src={photoUrl}
          alt=""
          referrerPolicy="no-referrer"
          className="absolute inset-0 size-full object-cover"
          onError={(event) => (event.currentTarget.hidden = true)}
        />
      )}
    </span>
  )
}

export type GroupTab = 'expenses' | 'balances' | 'members'

const groupTabs: { tab: GroupTab; label: MessageKey }[] = [
  { tab: 'expenses', label: 'group_tabExpenses' },
  { tab: 'balances', label: 'group_tabBalances' },
  { tab: 'members', label: 'group_tabMembers' },
]

/**
 * A group's sections, Despesas, Saldos and Membros, on the group's page and on Membros. Each tab is the ViewModel's
 * event: Membros and Despesas take each other's place, Saldos answers "Coming soon" and the selection stays.
 */
export function GroupTabs({ selected, onSelect, className }: { selected: GroupTab; onSelect: (tab: GroupTab) => void; className?: string }) {
  return (
    <nav
      aria-label={t('group_tabsLabel')}
      className={cn('grid h-9 grid-cols-3 rounded-lg bg-muted p-[3px] text-muted-foreground sm:inline-grid sm:self-start', className)}
    >
      {groupTabs.map(({ tab, label }) => (
        <button
          key={tab}
          type="button"
          aria-current={tab === selected ? 'page' : undefined}
          onClick={() => {
            if (tab !== selected) onSelect(tab)
          }}
          className={cn(
            'flex items-center justify-center rounded-md px-3 text-sm font-medium whitespace-nowrap outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
            tab === selected ? 'bg-background text-foreground shadow-sm dark:bg-input/30' : 'hover:text-foreground',
          )}
        >
          {t(label)}
        </button>
      ))}
    </nav>
  )
}
