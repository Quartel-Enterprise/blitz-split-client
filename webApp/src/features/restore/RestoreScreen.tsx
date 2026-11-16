import { ArrowLeft, ChevronRight, Loader2, QrCode, RotateCw } from 'lucide-react'
import { RestorePresenter, type JsRestoreState, type JsRestoreWord } from 'blitzsplit-shared'
import { useRef, useState } from 'react'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { AccountButtons, OfflineAlert } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useDraft } from '@/hooks/useDraft'
import { t, type MessageKey } from '@/i18n'
import { cn } from '@/lib/utils'
import { QrScannerDialog } from './QrScannerDialog'

const codeErrors: Record<string, MessageKey> = {
  INVALID_CODE: 'restore_errorInvalidCode',
  TOO_MANY_ATTEMPTS: 'restore_errorTooManyAttempts',
  UNKNOWN: 'restore_errorUnknown',
}

export function RestoreScreen() {
  const presenter = usePresenter(() => new RestorePresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsRestoreState) => void) => p.observeState(onChange))
  const [isScanning, setScanning] = useState(false)
  useActions(presenter, (p, onOpen: (open: boolean) => void) => p.observeOpenQrScanner(onOpen), () => setScanning(true))
  const wordInputs = useRef<(HTMLInputElement | null)[]>([])
  useActions(presenter, (p, onFocus: (index: number) => void) => p.observeFocusWord(onFocus), (index) => wordInputs.current[index]?.focus())
  if (!presenter || !state) return null

  const codeErrorKey = state.error ? codeErrors[state.error] : undefined
  const codeError = codeErrorKey ? t(codeErrorKey) : undefined
  return (
    <main className="flex min-h-dvh items-center justify-center bg-muted/40 p-4 sm:p-8">
      <div className="page-enter flex w-full max-w-4xl flex-col gap-4">
        <Button variant="ghost" size="sm" className="-ml-2 self-start" onClick={() => presenter.back()}>
          <ArrowLeft /> {t('common_back')}
        </Button>
        <div className="grid overflow-hidden rounded-xl border bg-card shadow-sm md:grid-cols-2">
          <section className="flex flex-col gap-6 p-6 sm:p-8 md:border-r">
            <div className="flex flex-col gap-1.5">
              <h1 className="text-2xl font-semibold tracking-tight">{t('restore_title')}</h1>
              <p className="text-sm text-muted-foreground">{t('restore_subtitle')}</p>
            </div>
            <AccountButtons short isEnabled={state.areAccountButtonsEnabled} connectingProvider={state.connectingProvider} onClick={(provider) => presenter.clickProvider(provider)} />
            {state.error === 'SIGN_IN_FAILED' && <p className="text-sm text-destructive">{t('common_signInFailed')}</p>}
            {state.showsOfflineNotice && (
              <OfflineAlert>{t('restore_offlineNotice')}</OfflineAlert>
            )}
          </section>
          <form
            className="flex flex-col gap-6 border-t p-6 sm:p-8 md:border-t-0"
            onSubmit={(event) => {
              event.preventDefault()
              presenter.restore()
            }}
          >
            <div className="flex flex-col gap-2">
              <Label htmlFor="recovery-word-0">{t('restore_codeLabel')}</Label>
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                {state.words.map((word, index) => (
                  <WordInput
                    key={index}
                    index={index}
                    word={word}
                    hasError={codeError !== undefined}
                    inputRef={(input) => {
                      wordInputs.current[index] = input
                    }}
                    isLast={index === state.words.length - 1}
                    onFocus={() => presenter.focusWord(index)}
                    onNext={() => wordInputs.current[index + 1]?.focus()}
                    onChange={(value) => {
                      presenter.changeWord(index, value)
                      return presenter.getWordText(index)
                    }}
                  />
                ))}
              </div>
              {state.suggestions.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {state.suggestions.map((suggestion) => (
                    <Button
                      key={suggestion}
                      type="button"
                      variant="secondary"
                      size="sm"
                      className="rounded-full font-mono"
                      // Keeps the focus in the field, so the ViewModel's next field gets it
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={() => presenter.pickSuggestion(suggestion)}
                    >
                      {suggestion}
                    </Button>
                  ))}
                </div>
              )}
              <span id="recovery-code-help" className={cn('text-xs', codeError ? 'text-destructive' : 'text-muted-foreground')}>
                {codeError ?? t('restore_codeHint')}
              </span>
            </div>
            <button
              type="button"
              onClick={() => presenter.scanQr()}
              className="flex items-center gap-3 rounded-lg border p-3 text-left text-sm outline-none transition-colors hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring"
            >
              <span className="flex size-9 items-center justify-center rounded-md bg-muted">
                <QrCode className="size-5" />
              </span>
              <span className="flex flex-1 flex-col">
                <span className="font-medium">{t('restore_scanQrTitle')}</span>
                <span className="text-xs text-muted-foreground">{t('restore_scanQrBody')}</span>
              </span>
              <ChevronRight className="size-4 text-muted-foreground" />
            </button>
            <div className="mt-auto flex flex-col gap-2">
              {state.showsOfflineNotice ? (
                <>
                  <Button type="submit" size="lg">
                    <RotateCw /> {t('restore_tryAgain')}
                  </Button>
                  <Button type="button" variant="link" onClick={() => presenter.startFresh()}>
                    {t('restore_startFresh')}
                  </Button>
                </>
              ) : (
                <Button type="submit" size="lg" disabled={!state.canRestore}>
                  {state.isRestoring && <Loader2 className="animate-spin" />}
                  {t('restore_recover')}
                </Button>
              )}
            </div>
          </form>
        </div>
      </div>
      <QrScannerDialog
        open={isScanning}
        onOpenChange={setScanning}
        onScanned={(content) => {
          setScanning(false)
          presenter.qrScanned(content)
        }}
      />
    </main>
  )
}

interface WordInputProps {
  index: number
  word: JsRestoreWord
  hasError: boolean
  inputRef: (input: HTMLInputElement | null) => void
  isLast: boolean
  onFocus: () => void
  onNext: () => void
  onChange: (value: string) => string
}

/**
 * One recovery word. The draft shows what the ViewModel kept for the field in the same keystroke, so a space it drops
 * or words it spreads to the other fields don't stay typed here. Enter moves on, as the phones' Next does, and only the
 * last field submits.
 */
function WordInput({ index, word, hasError, inputRef, isLast, onFocus, onNext, onChange }: WordInputProps) {
  const [text, setText] = useDraft(word.text, onChange)
  return (
    <div className="relative">
      <span className="pointer-events-none absolute top-1/2 left-2.5 -translate-y-1/2 text-xs text-muted-foreground tabular-nums">
        {index + 1}
      </span>
      <Input
        id={`recovery-word-${index}`}
        ref={inputRef}
        value={text}
        autoComplete="off"
        autoCapitalize="none"
        autoCorrect="off"
        spellCheck={false}
        aria-label={t('restore_wordLabel', { number: index + 1 })}
        aria-invalid={hasError || word.isDeadEnd}
        aria-describedby="recovery-code-help"
        className="h-10 pl-8 font-mono"
        enterKeyHint={isLast ? 'go' : 'next'}
        onFocus={onFocus}
        onKeyDown={(event) => {
          if (event.key === 'Enter' && !isLast) {
            event.preventDefault()
            onNext()
          }
        }}
        onChange={(event) => setText(event.target.value)}
      />
    </div>
  )
}
