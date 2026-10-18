import { Copy, Download, Loader2, ShieldCheck } from 'lucide-react'
import { RecoveryCodePresenter, type JsRecoveryCodeAction, type JsRecoveryCodeState } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { useMediaQuery } from '@/hooks/useMediaQuery'

const title = 'Guarde seu código de recuperação'
const description = 'Você está usando sem conta. Este código recupera seus grupos se você limpar os dados do navegador ou trocar de aparelho.'

/**
 * A dialog on desktop and tablet, a drawer on the phone. Without an account the data lives in the
 * browser, so instead of a password manager the code is offered as a file to download. Closing it
 * counts as "Lembrar depois", so it comes back on the next visit.
 */
export function RecoveryCodeDialog() {
  const presenter = usePresenter(() => new RecoveryCodePresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsRecoveryCodeState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  useActions(presenter, (p, onAction: (action: JsRecoveryCodeAction) => void) => p.observeActions(onAction), (action) => {
    if (action.type === 'copy') {
      void navigator.clipboard.writeText(action.formattedCode).then(() => toast('Código copiado'))
    } else if (action.type === 'download') {
      downloadCode(action.formattedCode)
    }
  })
  if (!presenter || !state?.formattedCode) return null

  const onOpenChange = (open: boolean) => {
    if (!open) presenter.remindLater()
  }
  const body = <RecoveryCodeBody presenter={presenter} state={state} />
  const actions = (
    <>
      <Button variant="outline" onClick={() => presenter.remindLater()}>
        Lembrar depois
      </Button>
      <Button onClick={() => presenter.saved()}>Já guardei</Button>
    </>
  )

  if (isDesktop) {
    return (
      <Dialog open onOpenChange={onOpenChange}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
            <DialogDescription>{description}</DialogDescription>
          </DialogHeader>
          {body}
          <DialogFooter>{actions}</DialogFooter>
        </DialogContent>
      </Dialog>
    )
  }
  return (
    <Drawer open onOpenChange={onOpenChange}>
      <DrawerContent>
        <DrawerHeader className="text-left">
          <DrawerTitle>{title}</DrawerTitle>
          <DrawerDescription>{description}</DrawerDescription>
        </DrawerHeader>
        <div className="px-4">{body}</div>
        <DrawerFooter className="flex-col-reverse">{actions}</DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

function RecoveryCodeBody({ presenter, state }: { presenter: RecoveryCodePresenter; state: JsRecoveryCodeState }) {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between gap-2.5 rounded-lg border border-dashed border-muted-foreground/50 bg-muted py-2.5 pr-2.5 pl-3.5">
        <span className="font-mono text-xl font-semibold tracking-[0.12em]">{state.formattedCode}</span>
        <Button variant="outline" size="sm" onClick={() => presenter.copy()}>
          <Copy /> Copiar
        </Button>
      </div>
      <Option icon={<Download className="size-4" />} title="Baixar como arquivo" onClick={() => presenter.download()} />
      {state.isConnectExpanded ? (
        // Google first on the web, as on Android
        <div className="grid grid-cols-2 gap-2">
          <Button variant="outline" disabled={!state.areAccountButtonsEnabled} onClick={() => presenter.clickProvider('google')}>
            {state.connectingProvider === 'google' && <Loader2 className="animate-spin" />}
            Google
          </Button>
          <Button variant="outline" disabled={!state.areAccountButtonsEnabled} onClick={() => presenter.clickProvider('apple')}>
            {state.connectingProvider === 'apple' && <Loader2 className="animate-spin" />}
            Apple
          </Button>
        </div>
      ) : (
        <Option
          icon={<ShieldCheck className="size-4" />}
          title="Conectar Google ou Apple em vez disso"
          subtitle="Dispensa o código"
          disabled={!state.areAccountButtonsEnabled}
          onClick={() => presenter.connectInstead()}
        />
      )}
      {state.hasSignInFailed && <p className="text-sm text-destructive">Não deu para conectar agora. Tente de novo.</p>}
      <span className="text-xs text-muted-foreground">Também fica em Perfil › Recuperação.</span>
    </div>
  )
}

function Option({ icon, title, subtitle, disabled, onClick }: { icon: ReactNode; title: string; subtitle?: string; disabled?: boolean; onClick: () => void }) {
  return (
    <button
      disabled={disabled}
      onClick={onClick}
      className="flex min-h-11 items-center gap-2.5 rounded-lg border px-3 py-2 text-left text-sm font-medium outline-none transition-colors hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
    >
      {icon}
      <span className="flex flex-col gap-0.5">
        {title}
        {subtitle && <span className="text-xs font-normal text-muted-foreground">{subtitle}</span>}
      </span>
    </button>
  )
}

function downloadCode(formattedCode: string) {
  const text = `Bora Rachar · código de recuperação\n\n${formattedCode}\n\nUse em "Já usei em outro aparelho" para trazer seus grupos.\n`
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }))
  const link = Object.assign(document.createElement('a'), { href: url, download: 'bora-rachar-codigo-de-recuperacao.txt' })
  link.click()
  URL.revokeObjectURL(url)
}
