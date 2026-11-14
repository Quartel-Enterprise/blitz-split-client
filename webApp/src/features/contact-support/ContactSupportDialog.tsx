import { Copy, Mail } from 'lucide-react'
import { toast } from 'sonner'
import { ContactSupportPresenter, type JsContactSupportAction, type JsContactSupportState } from 'blitzsplit-shared'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
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
 * whether an email app took it, so the address stays there, selectable.
 */
export function ContactSupportDialog() {
  const presenter = usePresenter(() => new ContactSupportPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsContactSupportState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: JsContactSupportAction) => void) => p.observeActions(onAction), (action) => {
    if (action.type === 'copy') {
      // Why: a browser may refuse the clipboard (no permission, no focus); the notice that said "copied" then goes.
      navigator.clipboard.writeText(action.value).catch(() => {
        presenter?.copyEmailFailed()
        toast(t('payWithKey_copyFailed'))
      })
      return
    }
    // Why: an email app opens over the page, which stays as it is; a new tab would be left behind, blank. A browser
    // with a web mail handler (Gmail) takes this tab instead, and back returns to the app.
    window.location.href = action.value
  })
  const isDesktop = useMediaQuery('(min-width: 768px)')
  if (!presenter || !state) return null

  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      onOpenChange={(open) => {
        if (!open) presenter.dismiss()
      }}
      className="sm:max-w-[460px]"
      pane={{
        title: t('contactSupport_title'),
        description: t('contactSupport_description'),
        body: <ContactSupportContent presenter={presenter} state={state} />,
        // Why: the drawer stacks them the other way round, "Open email" first, under the thumb
        footer: (
          <>
            <Button variant="outline" onClick={() => presenter.copyEmail()} className={cn(!isDesktop && 'h-11')}>
              {t('contactSupport_copyEmail')}
            </Button>
            <Button disabled={!state.canOpenEmail} onClick={() => presenter.openEmail()} className={cn(!isDesktop && 'h-11')}>
              <Mail /> {t('contactSupport_openEmail')}
            </Button>
          </>
        ),
      }}
    />
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
