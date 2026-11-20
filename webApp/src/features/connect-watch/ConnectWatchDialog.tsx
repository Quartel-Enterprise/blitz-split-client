import { useEffect, useRef, useState, type ReactNode } from 'react'
import { CircleAlert, CircleCheck, Loader2, Watch, WifiOff } from 'lucide-react'
import { ConnectWatchPresenter, type JsConnectWatchState } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { cn } from '@/lib/utils'

const ERROR_KEYS: Record<string, MessageKey> = {
  invalid_or_expired: 'connectWatch_errorInvalid',
  too_many_attempts: 'connectWatch_errorTooMany',
  needs_account: 'connectWatch_errorNeedsAccount',
  failed: 'connectWatch_errorFailed',
}

// Why: the code reads as two groups of three ("K7P 4QX"), as the watch shows it.
const CODE_GROUP = 3

/**
 * Profile › "Connect watch" (15.14): the code a watch shows when its phone is out of reach, typed on any device with
 * the account. A dialog on desktop and tablet, a drawer under 768 px. A wrong or expired code (15.14b) and no internet
 * (15.14c) turn Connect off and say why; typing again brings it back. Once connected only Close is left.
 */
export function ConnectWatchDialog() {
  const presenter = usePresenter(() => new ConnectWatchPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsConnectWatchState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  if (!presenter || !state) return null

  const buttonSize = cn(!isDesktop && 'h-11')
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      onOpenChange={(open) => {
        if (!open) presenter.close()
      }}
      className="sm:max-w-[460px]"
      pane={{
        leading: <WatchTile isConnected={state.isConnected} />,
        title: t(state.isConnected ? 'connectWatch_connectedTitle' : 'connectWatch_title'),
        description: t(state.isConnected ? 'connectWatch_connectedBody' : 'connectWatch_body'),
        body: state.isConnected ? null : <ConnectWatchForm presenter={presenter} state={state} />,
        footer: state.isConnected ? (
          <Button onClick={() => presenter.close()} className={buttonSize}>
            {t('common_close')}
          </Button>
        ) : (
          <>
            <Button variant="outline" onClick={() => presenter.close()} className={buttonSize}>
              {t('common_close')}
            </Button>
            <Button disabled={!state.canConnect} onClick={() => presenter.connect()} className={buttonSize}>
              {state.isConnecting && <Loader2 className="animate-spin" aria-hidden />}
              {t('connectWatch_connect')}
            </Button>
          </>
        ),
      }}
    />
  )
}

function WatchTile({ isConnected }: { isConnected: boolean }) {
  const Icon = isConnected ? CircleCheck : Watch
  return (
    <span aria-hidden className="flex size-12 items-center justify-center rounded-xl bg-primary-container text-primary-container-foreground">
      <Icon className="size-6" />
    </span>
  )
}

function ConnectWatchForm({ presenter, state }: { presenter: ConnectWatchPresenter; state: JsConnectWatchState }) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [isFocused, setFocused] = useState(false)
  // Why: the field is disabled while connecting, which drops its focus, so a refused code is typed again at once.
  useEffect(() => {
    if (!state.isConnecting) inputRef.current?.focus()
  }, [state.isConnecting])
  const errorKey = state.error ? ERROR_KEYS[state.error] : undefined
  return (
    <div className="flex flex-col gap-3">
      <label htmlFor="connect-watch-code" className="text-sm font-medium text-muted-foreground">
        {t('connectWatch_codeLabel')}
      </label>
      {/* Why: one field drawn as six boxes, so typing, a paste or the keyboard's suggestion fills them all. */}
      <div className="relative" dir="ltr">
        <input
          ref={inputRef}
          id="connect-watch-code"
          value={state.code}
          onChange={(event) => presenter.changeCode(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') presenter.connect()
          }}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          disabled={state.isConnecting}
          aria-invalid={state.isCodeWrong}
          aria-describedby="connect-watch-notices connect-watch-rules"
          autoComplete="one-time-code"
          autoCapitalize="characters"
          autoCorrect="off"
          spellCheck={false}
          inputMode="text"
          className="absolute inset-0 z-10 w-full cursor-text opacity-0"
        />
        <div aria-hidden className="flex justify-center gap-1.5 sm:gap-2">
          {Array.from({ length: state.codeLength }, (_, index) => (
            <CodeBox
              key={index}
              char={state.code[index]}
              hasGap={index === CODE_GROUP}
              hasError={state.isCodeWrong}
              isCurrent={isFocused && index === Math.min(state.code.length, state.codeLength - 1)}
            />
          ))}
        </div>
      </div>
      <div id="connect-watch-notices" role="status" className="flex flex-col gap-1.5 empty:hidden">
        {errorKey && (
          <Notice icon={<CircleAlert />} className="text-destructive">
            {t(errorKey)}
          </Notice>
        )}
        {state.showsOfflineNotice && (
          <Notice icon={<WifiOff />} className="text-muted-foreground">
            {t('connectWatch_offline')}
          </Notice>
        )}
      </div>
      {/* Why: the server hands the account to whoever holds the watch's secret, so only a code of one's own watch is typed. */}
      <div id="connect-watch-rules" className="flex flex-col gap-1.5 text-sm text-muted-foreground">
        <p>{t('connectWatch_ownWatchOnly')}</p>
        <p>{t('connectWatch_validity')}</p>
      </div>
    </div>
  )
}

function CodeBox({ char, hasGap, hasError, isCurrent }: { char?: string; hasGap: boolean; hasError: boolean; isCurrent: boolean }) {
  return (
    <span
      className={cn(
        'flex h-14 w-11 shrink items-center justify-center rounded-xl border bg-background font-mono text-2xl font-semibold sm:h-15 sm:w-12',
        hasGap && 'ms-2 sm:ms-3',
        hasError ? 'border-2 border-destructive' : isCurrent ? 'border-2 border-ring' : 'border-field-border',
      )}
    >
      {char ?? ''}
    </span>
  )
}

function Notice({ icon, className, children }: { icon: ReactNode; className: string; children: ReactNode }) {
  return (
    <p className={cn('flex items-center gap-1.5 text-sm [&_svg]:size-4 [&_svg]:shrink-0', className)}>
      {icon}
      {children}
    </p>
  )
}
