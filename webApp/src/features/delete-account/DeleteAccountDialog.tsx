import { Loader2, WifiOff } from 'lucide-react'
import { useEffect } from 'react'
import { DeleteAccountPresenter, type JsDeleteAccountAction, type JsDeleteAccountState } from 'blitzsplit-shared'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { providerLabel } from '@/components/common'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { blockBrowserBack } from '@/lib/browserBack'
import { downloadExport } from '@/lib/export'
import type { AccountProvider } from '@/lib/platform'
import { cn } from '@/lib/utils'

/**
 * "Delete account and data", over the profile: a dialog on desktop and tablet, a drawer with stacked
 * buttons on the phone. Closing it any way (Esc, the overlay, Cancel) is the ViewModel's back, which
 * waits while the deletion runs, and so does the browser's back button; once it's done it goes to the welcome.
 */
export function DeleteAccountDialog() {
  const presenter = usePresenter(() => new DeleteAccountPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsDeleteAccountState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: JsDeleteAccountAction) => void) => p.observeActions(onAction), (action) => {
    if (action.type === 'export') downloadExport(action.value)
  })
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const isDeleting = state?.isDeleting ?? false
  useEffect(() => (isDeleting ? blockBrowserBack() : undefined), [isDeleting])
  if (!presenter || !state) return null

  const title = t('deleteAccount_title')
  const onOpenChange = (open: boolean) => {
    if (!open) presenter.back()
  }
  const content = <DeleteAccountContent presenter={presenter} state={state} />
  const remove = (
    <Button variant="destructive" disabled={!state.canDelete} onClick={() => presenter.delete()} className={cn(!isDesktop && 'h-11')}>
      {state.isDeleting && <Loader2 className="animate-spin" />}
      {t('deleteAccount_delete')}
    </Button>
  )
  const cancel = (
    <Button variant="outline" disabled={state.isDeleting} onClick={() => presenter.back()} className={cn(!isDesktop && 'h-11')}>
      {t('common_cancel')}
    </Button>
  )

  if (isDesktop) {
    return (
      <Dialog open onOpenChange={onOpenChange}>
        <DialogContent aria-describedby={undefined} className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-[460px]">
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
          </DialogHeader>
          {content}
          <DialogFooter>
            {cancel}
            {remove}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    )
  }
  return (
    <Drawer open onOpenChange={onOpenChange}>
      <DrawerContent aria-describedby={undefined} className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
        <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          <DrawerTitle>{title}</DrawerTitle>
        </DrawerHeader>
        <div className="overflow-y-auto px-4">{content}</div>
        <DrawerFooter>
          {remove}
          {cancel}
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

function DeleteAccountContent({ presenter, state }: { presenter: DeleteAccountPresenter; state: JsDeleteAccountState }) {
  const items = [
    t('deleteAccount_deletedProfile'),
    t('deleteAccount_deletedKeys'),
    // Why: the account is gone once deleted, and the screen closes right after
    ...(state.accountProvider ? [t('deleteAccount_deletedAccount', { provider: providerLabel(state.accountProvider as AccountProvider, true) })] : []),
    t('deleteAccount_deletedDevice'),
  ]
  return (
    <div className="flex flex-col gap-4">
      {state.showsOfflineNotice && (
        <Alert role="status">
          <WifiOff />
          <AlertDescription className="text-foreground">
            <p>
              <strong className="font-semibold">{t('deleteAccount_offlineTitle')}</strong> {t('deleteAccount_offlineBody')}
            </p>
          </AlertDescription>
        </Alert>
      )}
      <section aria-labelledby="delete-account-deleted" className="flex flex-col gap-1.5 rounded-lg border p-3 text-sm">
        <h3 id="delete-account-deleted" className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
          {t('deleteAccount_deleted')}
        </h3>
        <ul className="flex flex-col gap-1.5">
          {items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </section>
      {/* Built from this browser's copy, so it works offline too */}
      <Button variant="link" className="self-start px-0" onClick={() => presenter.export()}>
        {t('deleteAccount_export')}
      </Button>
      <label className={cn('flex min-h-8 items-center gap-2.5 text-sm', state.isConfirmEnabled ? 'cursor-pointer' : 'opacity-50')}>
        <input
          type="checkbox"
          className="size-4 accent-destructive"
          checked={state.hasConfirmed}
          disabled={!state.isConfirmEnabled}
          onChange={(event) => presenter.confirmChanged(event.target.checked)}
        />
        {t('deleteAccount_confirm')}
      </label>
      {state.hasFailed && (
        <p role="alert" className="text-sm text-destructive">
          {t('deleteAccount_failed')}
        </p>
      )}
    </div>
  )
}
