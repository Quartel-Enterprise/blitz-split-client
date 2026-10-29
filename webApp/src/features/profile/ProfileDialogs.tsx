import { Copy, Loader2, WifiOff } from 'lucide-react'
import type { JsProfileState, ProfilePresenter } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { providerLabel } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import type { AccountProvider } from '@/lib/platform'

/** The profile's dialogs come from the shared state: closing any of them tells the ViewModel. */
export function ProfileDialogs({ presenter, state }: { presenter: ProfilePresenter; state: JsProfileState }) {
  const onClose = () => presenter.dismissDialog()
  switch (state.dialog) {
    case 'VISIBILITY_INFO':
      return (
        <ResponsiveDialog title={t('profile_visibility')} description={t('profile_visibilityBody')} onClose={onClose}>
          <Button onClick={onClose}>{t('profile_visibilityOk')}</Button>
        </ResponsiveDialog>
      )
    case 'SIGN_OUT':
      return (
        <ResponsiveDialog
          title={t('profile_signOutTitle')}
          description={t('profile_signOutBody', { provider: providerLabel(state.accountProvider as AccountProvider, true) })}
          onClose={onClose}
          body={state.hasSignOutFailed && <SignOutFailed />}
        >
          <Button variant="outline" onClick={onClose}>
            {t('common_cancel')}
          </Button>
          <Button variant="destructive" disabled={state.isSigningOut} onClick={() => presenter.confirmSignOut()}>
            {state.isSigningOut && <Loader2 className="animate-spin" />}
            {t('profile_signOut')}
          </Button>
        </ResponsiveDialog>
      )
    case 'SIGN_OUT_PENDING':
      return (
        <ResponsiveDialog
          title={t(state.pendingChangeCount === 1 ? 'profile_signOutPendingTitleOne' : 'profile_signOutPendingTitleOther', { count: state.pendingChangeCount })}
          description={t(state.showsOfflineNotice ? 'profile_signOutPendingBodyOffline' : 'profile_signOutPendingBody')}
          onClose={onClose}
          body={state.hasSignOutFailed && <SignOutFailed />}
        >
          <Button variant="outline" onClick={onClose}>
            {t('common_cancel')}
          </Button>
          {/* A sync that never succeeds (offline for good, a rejected push) can't keep the person signed in */}
          <Button variant="destructive" disabled={state.isSigningOut} onClick={() => presenter.signOutNow()}>
            {state.isSigningOut && <Loader2 className="animate-spin" />}
            {t('profile_signOutNow')}
          </Button>
          <Button disabled={state.isSigningOut} onClick={() => presenter.signOutWhenSynced()}>
            {t('profile_signOutWhenSynced')}
          </Button>
        </ResponsiveDialog>
      )
    case 'ERASE_PROFILE':
      return <EraseProfileDialog presenter={presenter} state={state} onClose={onClose} />
    case 'UNLINK_ACCOUNT':
      return <UnlinkAccountDialog presenter={presenter} state={state} onClose={onClose} />
    default:
      return null
  }
}

/**
 * Without Google or Apple the profile only comes back with the recovery code, so "Erase and sign out"
 * waits for the box saying it is kept, or that losing it is understood when there is none yet.
 */
function EraseProfileDialog({ presenter, state, onClose }: { presenter: ProfilePresenter; state: JsProfileState; onClose: () => void }) {
  const code = state.formattedRecoveryCode
  return (
    <ResponsiveDialog
      title={t('profile_eraseTitle')}
      description={t(state.hasRecoveryCode ? 'profile_eraseBody' : 'profile_eraseBodyNoCode')}
      onClose={onClose}
      body={
        <div className="flex flex-col gap-3">
          {state.hasPendingChanges && (
            <p className="text-sm text-destructive">
              {t(state.pendingChangeCount === 1 ? 'profile_erasePendingOne' : 'profile_erasePendingOther', { count: state.pendingChangeCount })}
            </p>
          )}
          {code && (
            <div className="flex items-center justify-between gap-2.5 rounded-lg border border-dashed border-muted-foreground/50 bg-muted py-2.5 pr-2.5 pl-3.5">
              <span className="font-mono text-xl font-semibold tracking-[0.12em]">{code}</span>
              <Button variant="outline" size="sm" onClick={() => presenter.copyCode()}>
                <Copy /> {t('recoveryCode_copy')}
              </Button>
            </div>
          )}
          <label className="flex items-center gap-2.5 text-sm">
            <input
              type="checkbox"
              className="size-4 accent-primary"
              checked={state.hasConfirmedCodeSaved}
              onChange={(event) => presenter.codeSavedChanged(event.target.checked)}
            />
            {t(state.hasRecoveryCode ? 'profile_eraseCodeSaved' : 'profile_eraseNoCodeAck')}
          </label>
          <Button variant="link" className="self-start px-0" disabled={!state.canConnect} onClick={() => presenter.connectInstead()}>
            {t('profile_eraseConnectInstead')}
          </Button>
          {state.hasSignOutFailed && <SignOutFailed />}
        </div>
      }
    >
      <Button variant="outline" onClick={onClose}>
        {t('common_cancel')}
      </Button>
      <Button variant="destructive" disabled={!state.canErase} onClick={() => presenter.confirmErase()}>
        {state.isSigningOut && <Loader2 className="animate-spin" />}
        {t('profile_eraseConfirm')}
      </Button>
    </ResponsiveDialog>
  )
}

/**
 * "Disconnect your Google account?": the browser goes on without an account, and the recovery code
 * shows by itself once it's done. It opens offline too, to say why "Disconnect" waits.
 */
function UnlinkAccountDialog({ presenter, state, onClose }: { presenter: ProfilePresenter; state: JsProfileState; onClose: () => void }) {
  const provider = providerLabel(state.accountProvider as AccountProvider, true)
  const notice = state.showsOfflineNotice || state.hasUnlinkFailed
  return (
    <ResponsiveDialog
      title={t('profile_unlinkTitle', { provider })}
      description={t('profile_unlinkBody')}
      onClose={onClose}
      body={
        notice && (
          <div className="flex flex-col gap-2">
            {state.showsOfflineNotice && (
              <p role="status" className="flex items-center gap-2 text-sm text-muted-foreground">
                <WifiOff className="size-4 shrink-0" />
                {t('profile_unlinkOffline')}
              </p>
            )}
            {state.hasUnlinkFailed && (
              <p role="alert" className="text-sm text-destructive">
                {t('profile_unlinkFailed')}
              </p>
            )}
          </div>
        )
      }
    >
      {/* The ViewModel ignores closing while it unlinks, so Cancel waits too */}
      <Button variant="outline" disabled={state.isUnlinking} onClick={onClose}>
        {t('common_cancel')}
      </Button>
      <Button variant="destructive" disabled={!state.canUnlink} onClick={() => presenter.confirmUnlink()}>
        {state.isUnlinking && <Loader2 className="animate-spin" />}
        {t('profile_unlinkConfirm')}
      </Button>
    </ResponsiveDialog>
  )
}

function SignOutFailed() {
  return (
    <p role="alert" className="text-sm text-destructive">
      {t('profile_signOutFailed')}
    </p>
  )
}

interface ResponsiveDialogProps {
  title: string
  description: string
  onClose: () => void
  body?: ReactNode
  children: ReactNode
}

/** A dialog on desktop and tablet, a drawer on the phone, as the recovery code's. */
function ResponsiveDialog({ title, description, onClose, body, children }: ResponsiveDialogProps) {
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const onOpenChange = (open: boolean) => {
    if (!open) onClose()
  }
  if (isDesktop) {
    return (
      <Dialog open onOpenChange={onOpenChange}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
            <DialogDescription>{description}</DialogDescription>
          </DialogHeader>
          {body}
          <DialogFooter>{children}</DialogFooter>
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
        {body && <div className="px-4">{body}</div>}
        <DrawerFooter className="flex-col-reverse">{children}</DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}
