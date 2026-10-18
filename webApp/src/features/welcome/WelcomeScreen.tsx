import { CloudOff, Link2, ShieldCheck } from 'lucide-react'
import { WelcomePresenter, type JsWelcomeState } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { AccountButtons, BrandMark, OfflineAlert, OrDivider } from '@/components/common'
import { Button } from '@/components/ui/button'
import { t } from '@/i18n'

/** Welcome: a brand panel beside the actions on wide screens, on top of them when narrow. */
export function WelcomeScreen() {
  const presenter = usePresenter(() => new WelcomePresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsWelcomeState) => void) => p.observeState(onChange))
  if (!presenter || !state) return null

  return (
    <main className="flex min-h-dvh items-center justify-center bg-muted/40 p-4 sm:p-8">
      <div className="grid w-full max-w-4xl overflow-hidden rounded-xl border bg-card shadow-sm md:grid-cols-2">
        <aside className="relative overflow-hidden bg-brand-panel p-6 text-brand-panel-foreground sm:p-10">
          <div aria-hidden className="pointer-events-none absolute -top-9 -right-9 size-50 opacity-90">
            <div className="absolute top-12 left-5 size-28 rounded-[50%_50%_50%_50%/60%_60%_40%_40%] bg-white/15" />
            <div className="absolute top-4 left-25 size-22 rotate-14 rounded-[26px] bg-brand-shape" />
          </div>
          <ul className="relative flex flex-col gap-5 pt-16 text-sm md:pt-40">
            <Benefit icon={<CloudOff className="size-5" />} title={t('welcome_offlineTitle')} body={t('welcome_offlineBody')} />
            <Benefit icon={<ShieldCheck className="size-5" />} title={t('welcome_optionalTitle')} body={t('welcome_optionalBody')} />
            <Benefit icon={<Link2 className="size-5" />} title={t('welcome_inviteTitle')} body={t('welcome_inviteBody')} />
          </ul>
        </aside>
        <section className="flex flex-col gap-6 p-6 sm:p-10">
          <BrandMark />
          <div className="flex flex-col gap-1.5">
            <h1 className="text-2xl font-semibold tracking-tight">{t('welcome_title')}</h1>
            <p className="text-sm text-muted-foreground">{t('welcome_subtitle')}</p>
          </div>
          {state.showsOfflineNotice && <OfflineAlert>{t('welcome_offlineNotice')}</OfflineAlert>}
          <AccountButtons isEnabled={state.areAccountButtonsEnabled} connectingProvider={state.connectingProvider} onClick={(provider) => presenter.clickProvider(provider)} />
          {state.hasSignInFailed && <p className="text-sm text-destructive">{t('common_signInFailed')}</p>}
          <OrDivider />
          <div className="flex flex-col gap-2">
            <Button size="lg" onClick={() => presenter.useWithoutAccount()}>
              {t('welcome_useWithoutAccount')}
            </Button>
            <Button variant="link" onClick={() => presenter.usedOnAnotherDevice()}>
              {t('welcome_usedOnAnotherDevice')}
            </Button>
          </div>
        </section>
      </div>
    </main>
  )
}

function Benefit({ icon, title, body }: { icon: React.ReactNode; title: string; body: string }) {
  return (
    <li className="flex items-start gap-3">
      <span aria-hidden className="mt-0.5">
        {icon}
      </span>
      <span className="flex flex-col gap-0.5">
        <strong className="font-semibold">{title}</strong>
        <span className="opacity-85">{body}</span>
      </span>
    </li>
  )
}
