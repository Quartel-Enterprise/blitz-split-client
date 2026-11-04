import { Check, Loader2, WifiOff } from 'lucide-react'
import type { ReactNode } from 'react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { t, type MessageKey } from '@/i18n'
import { avatarColorLabel, avatarStyle } from '@/lib/avatar'
import { accountProviders, type AccountProvider } from '@/lib/platform'
import { cn } from '@/lib/utils'

/**
 * The tile with the app's logo (the glyph of its icon, public/icon.svg), with the product name: Bora Rachar in
 * Portuguese, Blitz Split in every other language.
 */
export function BrandMark({ showsName = true, className }: { showsName?: boolean; className?: string }) {
  // Why: a span, so the logo can sit inside the button that opens Início (a button holds no div).
  return (
    <span className={cn('flex items-center gap-2.5', className)}>
      <span aria-hidden className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground">
        {/* Why: a mask over the favicon's own file, so the glyph is drawn from one copy in the tile's color. */}
        <span className="size-5 bg-current mask-[url(/icon.svg)] mask-contain mask-center mask-no-repeat" />
      </span>
      {showsName && <span className="font-semibold">{t('appName')}</span>}
    </span>
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

export function ProviderLogo({ provider }: { provider: AccountProvider }) {
  if (provider === 'apple') return <AppleLogo />
  return <GoogleLogo />
}

/** Google's four-color "G", as its sign in branding draws it. */
function GoogleLogo() {
  return (
    <svg aria-hidden viewBox="0 0 48 48" className="size-4">
      <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z" />
      <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z" />
      <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z" />
      <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
    </svg>
  )
}

function AppleLogo() {
  return (
    <svg aria-hidden viewBox="0 0 24 24" fill="currentColor" className="size-4">
      <path d="M16.37 12.6c-.02-2.2 1.8-3.26 1.88-3.31-1.03-1.5-2.62-1.7-3.18-1.73-1.35-.14-2.64.8-3.33.8-.69 0-1.74-.78-2.86-.76-1.47.02-2.83.86-3.59 2.17-1.53 2.65-.39 6.58 1.1 8.73.73 1.05 1.6 2.23 2.73 2.19 1.1-.04 1.51-.71 2.84-.71 1.32 0 1.7.71 2.86.69 1.18-.02 1.93-1.07 2.65-2.13.84-1.22 1.18-2.4 1.2-2.46-.03-.01-2.3-.88-2.3-3.48zM14.2 6.13c.6-.73 1.01-1.75.9-2.76-.87.04-1.92.58-2.54 1.31-.56.64-1.05 1.68-.92 2.67.97.08 1.96-.49 2.56-1.22z" />
    </svg>
  )
}

/** "Split a bill"'s icon: a receipt with a plus, the legacy Blitz Split app's, in place of lucide's bare `Plus`. */
export function SplitBillIcon() {
  return (
    <svg aria-hidden width={24} height={24} viewBox="0 0 24 24" fill="currentColor">
      <path
        fillRule="evenodd"
        d="M5.75 12V10.334H18.25V2.834L17 4.084 15.75 2.834 14.5 4.084 13.25 2.834 12 4.084 10.75 2.834 9.5 4.084 8.25 2.834 7 4.084 5.75 2.834 4.5 4.084 3.25 2.834V19.5L4.5 18.25 5.75 19.5 7 18.25 8.25 19.5 9.5 18.25 10.75 19.5 12 18.25H10.75V14.167H14.5V12H5.75ZM5.75 8.667H15.75V7H5.75V8.667ZM13.25 19.5 12 18.25H14.5L13.25 19.5Z"
      />
      <path d="M20.75 15.016V17.063H11.75V15.016H20.75ZM17.922 11.167V21.167H15.588V11.167H17.922Z" />
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

/**
 * A person's face and name, which open their profile while `onOpen` is set (beta feedback 18): the one Contatos opens,
 * or Perfil for this device's user. Someone with no profile to open (no longer in the group) gets them as plain text.
 * `isRound` is a face alone, named by `label` for a screen reader since it shows no name.
 */
export function PersonButton({
  onOpen,
  label,
  isRound = false,
  className,
  children,
}: {
  onOpen?: (() => void) | null
  label?: string
  isRound?: boolean
  className?: string
  children?: ReactNode
}) {
  if (!onOpen) return <span className={className}>{children}</span>
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onOpen}
      className={cn(
        'cursor-pointer text-left outline-none hover:bg-accent/50 focus-visible:ring-[3px] focus-visible:ring-ring/50',
        isRound ? 'rounded-full' : 'rounded-md',
        className,
      )}
    >
      {children}
    </button>
  )
}

/** The avatar colors as a radio group, the one picked with a check. `labelledBy` is the id of its label. */
export function AvatarColorPicker({ colors, selected, onSelect, labelledBy }: { colors: string[]; selected: string; onSelect: (code: string) => void; labelledBy: string }) {
  return (
    <div role="radiogroup" aria-labelledby={labelledBy} className="flex gap-3">
      {colors.map((code) => {
        const isSelected = code === selected
        return (
          <button
            key={code}
            type="button"
            role="radio"
            aria-checked={isSelected}
            aria-label={avatarColorLabel(code)}
            style={avatarStyle(code)}
            onClick={() => onSelect(code)}
            className={cn(
              'avatar flex size-10 items-center justify-center rounded-full outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
              isSelected && 'ring-2 ring-primary ring-offset-2 ring-offset-card',
            )}
          >
            {isSelected && <Check className="size-4" />}
          </button>
        )
      })}
    </div>
  )
}

/**
 * A segment of a segmented control on its `bg-muted` track (Dividir's modes, a group's tabs, Aparência…): the one
 * picked lifts in the page's background in both themes. Why not shadcn's `dark:bg-input/30`: the dark `--input` is
 * the track's own color, so the pick vanished.
 */
export function segmentClass(isSelected: boolean): string {
  return isSelected ? 'bg-background text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'
}

export type GroupTab = 'expenses' | 'balances' | 'members'

const groupTabs: { tab: GroupTab; label: MessageKey }[] = [
  { tab: 'expenses', label: 'group_tabExpenses' },
  { tab: 'balances', label: 'group_tabBalances' },
  { tab: 'members', label: 'group_tabMembers' },
]

/**
 * A group's sections, Despesas, Saldos and Membros, on the group's page and on Membros. Each tab is the ViewModel's
 * event: each takes the other's place, so the group never stacks over itself.
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
            segmentClass(tab === selected),
          )}
        >
          {t(label)}
        </button>
      ))}
    </nav>
  )
}
