import { ArrowLeft, ChevronRight, Loader2, QrCode, RotateCw } from 'lucide-react'
import { RestorePresenter, type JsRestoreState } from 'blitzsplit-shared'
import { useState } from 'react'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { AccountButtons, OfflineAlert } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useDraft } from '@/hooks/useDraft'
import { cn } from '@/lib/utils'
import { QrScannerDialog } from './QrScannerDialog'

const codeErrors: Record<string, string> = {
  INVALID_CODE: 'Código não encontrado. Confira as letras e tente de novo.',
  TOO_MANY_ATTEMPTS: 'Muitas tentativas. Espere alguns minutos e tente de novo.',
  UNKNOWN: 'Algo deu errado. Tente de novo.',
}

export function RestoreScreen() {
  const presenter = usePresenter(() => new RestorePresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsRestoreState) => void) => p.observeState(onChange))
  const [isScanning, setScanning] = useState(false)
  useActions(presenter, (p, onOpen: (open: boolean) => void) => p.observeOpenQrScanner(onOpen), () => setScanning(true))
  if (!presenter || !state) return null

  const codeError = state.error ? codeErrors[state.error] : undefined
  return (
    <main className="flex min-h-dvh items-center justify-center bg-muted/40 p-4 sm:p-8">
      <div className="flex w-full max-w-4xl flex-col gap-4">
        <Button variant="ghost" size="sm" className="-ml-2 self-start" onClick={() => presenter.back()}>
          <ArrowLeft /> Voltar
        </Button>
        <div className="grid overflow-hidden rounded-xl border bg-card shadow-sm md:grid-cols-2">
          <section className="flex flex-col gap-6 p-6 sm:p-8 md:border-r">
            <div className="flex flex-col gap-1.5">
              <h1 className="text-2xl font-semibold tracking-tight">Trazer seus grupos</h1>
              <p className="text-sm text-muted-foreground">Entrou com uma conta antes? É só entrar de novo. Se não, use o código de recuperação ou o QR do aparelho antigo.</p>
            </div>
            <AccountButtons short isEnabled={state.areAccountButtonsEnabled} connectingProvider={state.connectingProvider} onClick={(provider) => presenter.clickProvider(provider)} />
            {state.error === 'SIGN_IN_FAILED' && <p className="text-sm text-destructive">Não deu para entrar agora. Tente de novo.</p>}
            {state.showsOfflineNotice && (
              <OfflineAlert>Entrar com conta ou código precisa de internet para baixar seus grupos. O que você digitou fica guardado.</OfflineAlert>
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
              <Label htmlFor="recovery-code">Ou use o código de recuperação</Label>
              <CodeInput value={state.code} hasError={codeError !== undefined} onChange={(value) => presenter.changeCode(value)} />
              <span id="recovery-code-help" className={cn('text-xs', codeError ? 'text-destructive' : 'text-muted-foreground')}>
                {codeError ?? '12 letras e números. Traços são opcionais.'}
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
                <span className="font-medium">Escanear QR com a câmera</span>
                <span className="text-xs text-muted-foreground">No aparelho antigo: Perfil › Recuperação › Mostrar QR</span>
              </span>
              <ChevronRight className="size-4 text-muted-foreground" />
            </button>
            <div className="mt-auto flex flex-col gap-2">
              {state.showsOfflineNotice ? (
                <>
                  <Button type="submit" size="lg">
                    <RotateCw /> Tentar de novo
                  </Button>
                  <Button type="button" variant="link" onClick={() => presenter.startFresh()}>
                    Começar do zero neste navegador
                  </Button>
                </>
              ) : (
                <Button type="submit" size="lg" disabled={!state.canRestore}>
                  {state.isRestoring && <Loader2 className="animate-spin" />}
                  Recuperar
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

function CodeInput({ value, hasError, onChange }: { value: string; hasError: boolean; onChange: (value: string) => void }) {
  const [code, setCode] = useDraft(value, onChange)
  return (
    <Input
      id="recovery-code"
      value={code}
      placeholder="K7QM-4XPA-9TRE"
      autoComplete="one-time-code"
      autoCapitalize="characters"
      spellCheck={false}
      aria-invalid={hasError}
      aria-describedby="recovery-code-help"
      className="h-12 font-mono text-lg font-semibold tracking-[0.15em]"
      onChange={(event) => setCode(event.target.value)}
    />
  )
}
