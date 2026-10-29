import { Copy, Download, Loader2, ShieldCheck } from 'lucide-react'
import { RecoveryCodePresenter, type JsRecoveryCodeAction, type JsRecoveryCodeState } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { providerLabel } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { downloadFile } from '@/lib/download'
import { accountProviders } from '@/lib/platform'

/**
 * A dialog on desktop and tablet, a drawer on the phone. Without an account the data lives in the
 * browser, so instead of a password manager the code is offered as a file to download. Closing it
 * counts as "Remind me later", so it comes back on the next visit.
 */
export function RecoveryCodeDialog() {
  const presenter = usePresenter(() => new RecoveryCodePresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsRecoveryCodeState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  useActions(presenter, (p, onAction: (action: JsRecoveryCodeAction) => void) => p.observeActions(onAction), (action) => {
    if (action.type === 'copy') {
      void navigator.clipboard.writeText(action.formattedCode).then(() => toast(t('recoveryCode_copied')))
    } else if (action.type === 'download') {
      downloadCode(action.formattedCode)
    }
  })
  if (!presenter || !state?.formattedCode) return null

  const onOpenChange = (open: boolean) => {
    if (!open) presenter.remindLater()
  }
  const title = t('recoveryCode_title')
  const description = t('recoveryCode_body')
  const body = <RecoveryCodeBody presenter={presenter} state={state} />
  const actions = (
    <>
      <Button variant="outline" onClick={() => presenter.remindLater()}>
        {t('recoveryCode_remindLater')}
      </Button>
      <Button onClick={() => presenter.saved()}>{t('recoveryCode_saved')}</Button>
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
          <Copy /> {t('recoveryCode_copy')}
        </Button>
      </div>
      <Option icon={<Download className="size-4" />} title={t('recoveryCode_download')} onClick={() => presenter.download()} />
      {state.isConnectExpanded ? (
        // Apple first on Apple devices, Google first elsewhere
        <div className="grid grid-cols-2 gap-2">
          {accountProviders.map((provider) => (
            <Button key={provider} variant="outline" disabled={!state.areAccountButtonsEnabled} onClick={() => presenter.clickProvider(provider)}>
              {state.connectingProvider === provider && <Loader2 className="animate-spin" />}
              {providerLabel(provider, true)}
            </Button>
          ))}
        </div>
      ) : (
        <Option
          icon={<ShieldCheck className="size-4" />}
          title={t('recoveryCode_connectInstead')}
          subtitle={t('recoveryCode_connectInsteadBody')}
          disabled={!state.areAccountButtonsEnabled}
          onClick={() => presenter.connectInstead()}
        />
      )}
      {state.hasSignInFailed && <p className="text-sm text-destructive">{t('recoveryCode_signInFailed')}</p>}
      <span className="text-xs text-muted-foreground">{t('recoveryCode_alsoInProfile')}</span>
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
  const text = t('recoveryCode_fileText', { code: formattedCode })
  downloadFile(text, t('recoveryCode_fileName'), 'text/plain;charset=utf-8')
}
