import { Loader2, WifiOff } from 'lucide-react'
import type { ReactNode } from 'react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { avatarStyle } from '@/lib/avatar'
import { cn } from '@/lib/utils'

/** The "R$" tile with the product name. */
export function BrandMark({ showsName = true, className }: { showsName?: boolean; className?: string }) {
  return (
    <div className={cn('flex items-center gap-2.5', className)}>
      <span aria-hidden className="flex size-8 items-center justify-center rounded-lg bg-primary text-xs font-bold text-primary-foreground">
        R$
      </span>
      {showsName && <span className="font-semibold">Bora Rachar</span>}
    </div>
  )
}

/** "Sem conexão." where signing in or recovering needs the internet. */
export function OfflineAlert({ children }: { children: ReactNode }) {
  return (
    <Alert role="alert">
      <WifiOff />
      <AlertDescription className="text-foreground">
        <p>
          <strong className="font-semibold">Sem conexão.</strong> {children}
        </p>
      </AlertDescription>
    </Alert>
  )
}

interface AccountButtonsProps {
  isEnabled: boolean
  connectingProvider: string | null | undefined
  onClick: (provider: 'google' | 'apple') => void
  short?: boolean
  className?: string
}

/** Google first on the web, as on Android. Both sign in through the browser here. */
export function AccountButtons({ isEnabled, connectingProvider, onClick, short = false, className }: AccountButtonsProps) {
  return (
    <div className={cn(short ? 'grid grid-cols-2 gap-2' : 'flex flex-col gap-2', className)}>
      <Button variant="outline" size="lg" disabled={!isEnabled} onClick={() => onClick('google')}>
        {connectingProvider === 'google' ? <Loader2 className="animate-spin" /> : <span aria-hidden className="font-extrabold">G</span>}
        {short ? 'Google' : 'Continuar com Google'}
      </Button>
      <Button variant="outline" size="lg" disabled={!isEnabled} onClick={() => onClick('apple')}>
        {connectingProvider === 'apple' ? <Loader2 className="animate-spin" /> : <AppleLogo />}
        {short ? 'Apple' : 'Continuar com a Apple'}
      </Button>
    </div>
  )
}

function AppleLogo() {
  return (
    <svg aria-hidden viewBox="0 0 24 24" fill="currentColor" className="size-4">
      <path d="M16.37 12.6c-.02-2.2 1.8-3.26 1.88-3.31-1.03-1.5-2.62-1.7-3.18-1.73-1.35-.14-2.64.8-3.33.8-.69 0-1.74-.78-2.86-.76-1.47.02-2.83.86-3.59 2.17-1.53 2.65-.39 6.58 1.1 8.73.73 1.05 1.6 2.23 2.73 2.19 1.1-.04 1.51-.71 2.84-.71 1.32 0 1.7.71 2.86.69 1.18-.02 1.93-1.07 2.65-2.13.84-1.22 1.18-2.4 1.2-2.46-.03-.01-2.3-.88-2.3-3.48zM14.2 6.13c.6-.73 1.01-1.75.9-2.76-.87.04-1.92.58-2.54 1.31-.56.64-1.05 1.68-.92 2.67.97.08 1.96-.49 2.56-1.22z" />
    </svg>
  )
}

/** "ou" between the account buttons and the account-free path. */
export function OrDivider() {
  return (
    <div aria-hidden className="flex items-center gap-3 text-xs uppercase tracking-wide text-muted-foreground">
      <span className="h-px flex-1 bg-border" />
      ou
      <span className="h-px flex-1 bg-border" />
    </div>
  )
}

/** The person's avatar: their initial on their avatar color. */
export function Avatar({ initial, color, className }: { initial: string; color: string; className?: string }) {
  return (
    <span aria-hidden style={avatarStyle(color)} className={cn('avatar flex shrink-0 items-center justify-center rounded-full font-semibold', className)}>
      {initial}
    </span>
  )
}
