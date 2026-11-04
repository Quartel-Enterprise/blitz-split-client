import { RotateCcw, Scale, TriangleAlert, UserRound, type LucideIcon } from 'lucide-react'
import { SimplifyDebtsInfoPresenter, type JsRoute } from 'blitzsplit-shared'
import { useState } from 'react'
import { Avatar } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { formatMoney } from '@/lib/money'
import { cn } from '@/lib/utils'
import { paymentCount } from './simplify'

const facts: { icon: LucideIcon; title: MessageKey; text: MessageKey }[] = [
  { icon: Scale, title: 'simplifyInfo_totalTitle', text: 'simplifyInfo_totalText' },
  { icon: TriangleAlert, title: 'simplifyInfo_strangerTitle', text: 'simplifyInfo_strangerText' },
  { icon: UserRound, title: 'simplifyInfo_anyoneTitle', text: 'simplifyInfo_anyoneText' },
  { icon: RotateCcw, title: 'simplifyInfo_offTitle', text: 'simplifyInfo_offText' },
]

/**
 * "Como funciona" of "Simplificar dívidas" (13.2), the same words for everyone with the example's amounts in the
 * group's currency: a dialog in Editar grupo's place (or over the group's Saldos) on desktop and tablet, a drawer on
 * the phone. Closing it any way goes back to whichever opened it.
 */
export function SimplifyDebtsInfoDialog({ route }: { route: JsRoute }) {
  const [presenter] = useState(() => new SimplifyDebtsInfoPresenter(route))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const onOpenChange = (open: boolean) => {
    if (!open) presenter.close()
  }
  // Why: 20 and 121,40 in the group's currency, whatever its decimals.
  const major = (value: number) => formatMoney(Math.round(value * 10 ** presenter.fractionDigits), presenter.currency, presenter.fractionDigits)
  const body = <InfoBody example={major(20)} total={major(121.4)} />
  return isDesktop ? (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-[560px]">
        <DialogHeader className="pr-8 text-left">
          <DialogTitle>{t('simplify_title')}</DialogTitle>
          <DialogDescription>{t('simplifyInfo_intro')}</DialogDescription>
        </DialogHeader>
        {body}
      </DialogContent>
    </Dialog>
  ) : (
    <Drawer open onOpenChange={onOpenChange}>
      <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
        <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          <DrawerTitle>{t('simplify_title')}</DrawerTitle>
          <DrawerDescription>{t('simplifyInfo_intro')}</DrawerDescription>
        </DrawerHeader>
        <div className="overflow-y-auto px-4">{body}</div>
        <DrawerFooter>
          <Button variant="outline" className="h-11" onClick={() => presenter.close()}>
            {t('common_close')}
          </Button>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

function InfoBody({ example, total }: { example: string; total: string }) {
  return (
    <div className="flex flex-col gap-4">
      <section aria-labelledby="simplify-example" className="flex flex-col gap-3 rounded-xl border bg-muted/40 p-4">
        <h3 id="simplify-example" className="text-sm font-semibold">
          {t('simplifyInfo_exampleTitle')}
        </h3>
        <p className="text-sm text-muted-foreground">{t('simplifyInfo_example', { amount: example })}</p>
        <div className="grid grid-cols-2 gap-3">
          <ExampleFigure isSimplified={false} amount={example} />
          <ExampleFigure isSimplified amount={example} />
        </div>
        <p className="text-sm text-muted-foreground">{t('simplifyInfo_exampleResult', { amount: example })}</p>
      </section>
      <ul className="flex flex-col">
        {facts.map((fact) => (
          <li key={fact.title} className="flex items-start gap-3 py-2.5">
            <fact.icon aria-hidden className="mt-0.5 size-5 shrink-0 text-primary" />
            <span className="flex min-w-0 flex-1 flex-col gap-0.5">
              <strong className="text-sm font-semibold">{t(fact.title)}</strong>
              <span className="text-sm text-muted-foreground">{t(fact.text, { amount: total })}</span>
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}

/**
 * The boards' diagram: Carla, Elisa and you, with Carla → Elisa → you before and Carla → you once simplified (Elisa,
 * who pays nothing, dimmed). The arrows are an SVG under the faces, which keep the app's avatar colors in both themes.
 */
function ExampleFigure({ isSimplified, amount }: { isSimplified: boolean; amount: string }) {
  const label = t(isSimplified ? 'simplifyInfo_afterLabel' : 'simplifyInfo_beforeLabel', { amount })
  const node = 'absolute size-9 text-[11px]'
  return (
    <figure className={cn('m-0 flex flex-col gap-1.5 rounded-xl px-1.5 pt-2.5 pb-3', isSimplified ? 'bg-primary/10' : 'border bg-card')}>
      <figcaption className="px-1 text-[13px] font-semibold text-muted-foreground">{t(isSimplified ? 'simplifyInfo_after' : 'simplifyInfo_before')}</figcaption>
      <div role="img" aria-label={label} className="relative mx-auto h-[132px] w-[164px]">
        <svg viewBox="0 0 164 132" aria-hidden className="absolute inset-0 size-full overflow-visible text-primary">
          <g fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
            {isSimplified ? (
              <path d="M40 46L66 90M57 87l9 4 1-10" />
            ) : (
              <>
                <path d="M52 28H110M104 22l7 6-7 6" />
                <path d="M124 46L98 90M97 81l1 10 9-4" />
              </>
            )}
          </g>
          <g fill="currentColor" fontSize={11} fontWeight={600} textAnchor="middle">
            {isSimplified ? (
              <text x="30" y="76">{amount}</text>
            ) : (
              <>
                <text x="82" y="16">{amount}</text>
                <text x="134" y="76">{amount}</text>
              </>
            )}
          </g>
        </svg>
        <Avatar initial="CA" color="lilac" className={cn(node, 'top-[10px] left-[12px]')} />
        <Avatar initial="EL" color="sand" className={cn(node, 'top-[10px] left-[116px]', isSimplified && 'opacity-40')} />
        <Avatar initial={t('simplify_youCapital')} color="blue" className={cn(node, 'top-[90px] left-[64px] text-[10px]')} />
      </div>
      <strong className="text-center text-[13px] leading-[18px]">{paymentCount(isSimplified ? 1 : 2)}</strong>
    </figure>
  )
}
