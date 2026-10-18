import { CloudOff, Link2, ShieldCheck } from 'lucide-react'
import { WelcomePresenter, type JsWelcomeState } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { AccountButtons, BrandMark, OfflineAlert, OrDivider } from '@/components/common'
import { Button } from '@/components/ui/button'

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
            <Benefit icon={<CloudOff className="size-5" />} title="Funciona sem internet" body="Tudo fica no navegador e sincroniza depois." />
            <Benefit icon={<ShieldCheck className="size-5" />} title="Conta é opcional" body="Google ou Apple só servem para não perder nada ao trocar de aparelho." />
            <Benefit icon={<Link2 className="size-5" />} title="Convide por link ou QR" body="Os amigos entram no grupo sem criar conta." />
          </ul>
        </aside>
        <section className="flex flex-col gap-6 p-6 sm:p-10">
          <BrandMark />
          <div className="flex flex-col gap-1.5">
            <h1 className="text-2xl font-semibold tracking-tight">Racha a conta sem complicação</h1>
            <p className="text-sm text-muted-foreground">Entre com uma conta para não perder nada ao trocar de aparelho, ou use sem conta.</p>
          </div>
          {state.showsOfflineNotice && <OfflineAlert>Entrar com conta precisa de internet. Use sem conta agora e conecte depois em Perfil.</OfflineAlert>}
          <AccountButtons isEnabled={state.areAccountButtonsEnabled} connectingProvider={state.connectingProvider} onClick={(provider) => presenter.clickProvider(provider)} />
          {state.hasSignInFailed && <p className="text-sm text-destructive">Não deu para entrar agora. Tente de novo.</p>}
          <OrDivider />
          <div className="flex flex-col gap-2">
            <Button size="lg" onClick={() => presenter.useWithoutAccount()}>
              Usar sem conta
            </Button>
            <Button variant="link" onClick={() => presenter.usedOnAnotherDevice()}>
              Já usei em outro aparelho
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
