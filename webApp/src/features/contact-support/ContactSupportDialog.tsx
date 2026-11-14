import { Copy, Mail } from 'lucide-react'
import { ContactSupportPresenter, type JsContactSupportAction, type JsContactSupportState } from 'blitzsplit-shared'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { LoadingRegion, SkeletonLine } from '@/components/ui/skeleton'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { cn } from '@/lib/utils'

const PLATFORM_NAMES: Record<string, string> = { android: 'Android', ios: 'iOS', macos: 'macOS', web: 'Web' }

/** The account as the diagnostics name it; Google's and Apple's names are the same in every language. */
function accountLabel(account: string): string {
  if (account === 'google') return 'Google'
  if (account === 'apple') return 'Apple'
  return t(account === 'local_only' ? 'contactSupport_accountLocal' : 'contactSupport_accountAnonymous')
}

const NOTICE_KEYS: Record<string, MessageKey> = {
  email_copied: 'contactSupport_noticeCopied',
  no_email_app: 'contactSupport_noticeNoApp',
}

/**
 * "Contact support", over the profile: a dialog on desktop and tablet, a drawer with stacked buttons on the phone.
 * "Open email" hands the browser a mailto link with the subject and the diagnostics written; a browser never says
 * whether an email app took it, nor always whether it let the address be copied, so the address stays there, selectable.
 */
export function ContactSupportDialog() {
  const presenter = usePresenter(() => new ContactSupportPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsContactSupportState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: JsContactSupportAction) => void) => p.observeActions(onAction), (action) => {
    // Why: a browser that refuses the clipboard leaves the address on screen, selectable, so the refusal is dropped.
    if (action.type === 'copy') navigator.clipboard.writeText(action.value).catch(() => undefined)
    // Why: an email app opens over the page, which stays as it is; a new tab would be left behind, blank. A browser
    // with a web mail handler (Gmail) takes this tab instead, and back returns to the app.
    else window.location.href = action.value
  })
  const isDesktop = useMediaQuery('(min-width: 768px)')
  if (!presenter || !state) return null

  const title = t('contactSupport_title')
  const description = t('contactSupport_description')
  const onOpenChange = (open: boolean) => {
    if (!open) presenter.dismiss()
  }
  const content = <ContactSupportContent presenter={presenter} state={state} />
  const open = (
    <Button disabled={!state.canOpenEmail} onClick={() => presenter.openEmail()} className={cn(!isDesktop && 'h-11')}>
      <Mail /> {t('contactSupport_openEmail')}
    </Button>
  )
  const copy = (
    <Button variant="outline" onClick={() => presenter.copyEmail()} className={cn(!isDesktop && 'h-11')}>
      {t('contactSupport_copyEmail')}
    </Button>
  )

  if (isDesktop) {
    return (
      <Dialog open onOpenChange={onOpenChange}>
        <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-[460px]">
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
            <DialogDescription>{description}</DialogDescription>
          </DialogHeader>
          {content}
          <DialogFooter>
            {copy}
            {open}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    )
  }
  return (
    <Drawer open onOpenChange={onOpenChange}>
      <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
        <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          <DrawerTitle>{title}</DrawerTitle>
          <DrawerDescription>{description}</DrawerDescription>
        </DrawerHeader>
        <div className="overflow-y-auto px-4">{content}</div>
        <DrawerFooter>
          {open}
          {copy}
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

function ContactSupportContent({ presenter, state }: { presenter: ContactSupportPresenter; state: JsContactSupportState }) {
  const rows: [MessageKey, string][] = state.isDiagnosticsLoading
    ? []
    : [
        ['contactSupport_appVersion', state.appVersion ?? ''],
        ['contactSupport_platform', PLATFORM_NAMES[state.platform ?? ''] ?? ''],
        ['contactSupport_language', state.languageTag ?? `${t('contactSupport_languageSystem')} (${state.spokenLanguage ?? ''})`],
        ['contactSupport_account', accountLabel(state.account ?? '')],
        ...(state.profileLink ? ([['contactSupport_profile', state.profileLink]] as [MessageKey, string][]) : []),
      ]
  const notice = state.notice ? NOTICE_KEYS[state.notice] : undefined
  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-3 rounded-lg border p-3">
        <div className="flex min-w-0 flex-1 flex-col gap-0.5">
          <span className="text-xs text-muted-foreground">{t('contactSupport_to')}</span>
          <span className="text-sm font-medium break-all select-all">{state.email}</span>
        </div>
        <Button variant="ghost" size="icon" aria-label={t('contactSupport_copyEmail')} onClick={() => presenter.copyEmail()}>
          <Copy />
        </Button>
      </div>
      <section aria-labelledby="contact-support-diagnostics" className="flex flex-col gap-1.5 rounded-lg border p-3 text-sm">
        <h3 id="contact-support-diagnostics" className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
          {t('contactSupport_diagnostics')}
        </h3>
        {state.isDiagnosticsLoading ? (
          <LoadingRegion className="flex flex-col gap-2">
            <SkeletonLine className="w-3/5" />
            <SkeletonLine className="w-2/5" />
            <SkeletonLine className="w-1/2" />
          </LoadingRegion>
        ) : (
          <dl className="flex flex-col gap-1.5">
            {rows.map(([label, value]) => (
              <div key={label} className="flex items-baseline justify-between gap-4">
                <dt className="shrink-0 text-muted-foreground">{t(label)}</dt>
                <dd className="min-w-0 text-right break-all">{value}</dd>
              </div>
            ))}
          </dl>
        )}
      </section>
      {/* What the last tap did, announced since nothing else in the dialog changes */}
      <p role="status" className={cn('text-sm text-muted-foreground', !notice && 'sr-only')}>
        {notice ? t(notice) : ''}
      </p>
    </div>
  )
}
