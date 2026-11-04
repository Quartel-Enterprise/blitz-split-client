import { Loader2 } from 'lucide-react'
import type { JsRegisterPaymentState, JsSimplifyChange, RegisterPaymentPresenter } from 'blitzsplit-shared'
import { Avatar } from '@/components/common'
import type { PaneParts } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { nameOrFormer } from '@/features/balances/BalanceCard'
import { SimplifyIcon } from '@/features/simplify-debts/SimplifyIcon'
import { t } from '@/i18n'

/**
 * 13.5c, in place of the form or "Pagar Ana": someone turned "Simplificar dívidas" the other way while it was open and
 * the pair owes nothing anymore. What was owed shows struck through, dimmed, and "Já paguei mesmo assim" records it,
 * since the Pix may already be gone; "Fechar" closes.
 */
export function simplifyChangePane(
  presenter: RegisterPaymentPresenter,
  state: JsRegisterPaymentState,
  change: JsSimplifyChange,
  money: (minor: number) => string,
): PaneParts {
  const to = nameOrFormer(state.to?.name)
  const params = { name: change.changedByName ?? t('simplify_someone'), to, group: change.groupName }
  return {
    leading: (
      <div className="mb-2 flex flex-col gap-4">
        <div role="status" className="flex gap-3 rounded-lg border border-primary/20 bg-primary/10 p-3 text-left text-sm">
          <SimplifyIcon className="mt-0.5 size-5 text-primary" />
          <span className="flex flex-col gap-1">
            <strong className="font-semibold">{t(change.simplifiesDebts ? 'simplifyChange_bannerOn' : 'simplifyChange_bannerOff', params)}</strong>
            <span className="text-muted-foreground">{t('simplifyChange_hint')}</span>
          </span>
        </div>
        {state.to && <Avatar initial={state.to.initials} color={state.to.color} photoUrl={state.to.photoUrl} className="size-12 text-base opacity-50" />}
      </div>
    ),
    title: t('simplifyChange_payTitle', { name: to }),
    description: t('simplifyChange_wasOwed', { group: change.groupName }),
    body: <s className="text-2xl font-semibold text-muted-foreground tabular-nums">{money(change.previousDebtMinor)}</s>,
    footer: (
      <>
        <Button variant="outline" disabled={state.isSaving} onClick={() => presenter.close()}>
          {t('common_close')}
        </Button>
        <Button disabled={!state.canPayAnyway} onClick={() => presenter.payAnyway()}>
          {state.isSaving && <Loader2 className="animate-spin" />}
          {t('simplifyChange_paidAnyway')}
        </Button>
      </>
    ),
  }
}
