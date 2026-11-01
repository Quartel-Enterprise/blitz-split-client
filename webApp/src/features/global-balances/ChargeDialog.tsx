import { Copy, Info, Share } from 'lucide-react'
import type { GlobalBalancesPresenter, JsChargeDraft, JsGlobalBalancesState } from 'blitzsplit-shared'
import { toast } from 'sonner'
import { Avatar } from '@/components/common'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { nameOrFormer } from '@/features/balances/BalanceCard'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { formatMoney } from '@/lib/money'
import { chargeMessageOf } from './chargeMessage'

const messageId = 'charge-message'
const pixId = 'charge-pix'

/**
 * "Cobrar": a message for whoever owes you, which moves no money and sends nothing by itself. A 480 px dialog on
 * desktop and tablet, a drawer on the phone. Enviar uses the browser's share sheet where there is one, else copies.
 */
export function ChargeDialog({ presenter, state, charge }: { presenter: GlobalBalancesPresenter; state: JsGlobalBalancesState; charge: JsChargeDraft }) {
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const shown = chargeMessageOf(charge, state.pixKeyValue)
  // Why: the ViewModel keeps what was typed, so the field reads it back in the same keystroke.
  const [message, setMessage] = useDraft(shown, (value) => {
    presenter.changeMessage(value)
    return presenter.getEditedMessage() ?? value
  })
  const name = nameOrFormer(charge.name)
  const groups = charge.groupNames.length > 1 ? t('globalBalances_groups', { count: charge.groupNames.length }) : (charge.groupNames[0] ?? '')
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      onOpenChange={(open) => {
        if (!open) presenter.closeCharge()
      }}
      className="gap-4 sm:max-w-[480px]"
      pane={{
        leading: <Avatar initial={charge.initials} color={charge.color ?? 'blue'} className="mb-1 size-12 text-base" />,
        title: t('globalBalances_chargeTitle', { name }),
        description: t('globalBalances_chargeOwes', { name, amount: formatMoney(charge.amountMinor, charge.currency, charge.fractionDigits), groups }),
        body: (
          <div className="flex flex-col gap-4">
            {charge.isGuest && (
              <p role="note" className="flex gap-2.5 rounded-lg bg-muted px-3.5 py-3 text-[13px] leading-[18px] text-muted-foreground">
                <Info aria-hidden className="mt-px size-4 shrink-0" />
                <span>{t('globalBalances_chargeGuest', { name })}</span>
              </p>
            )}
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={messageId}>{t('globalBalances_chargeMessage')}</Label>
              <Textarea id={messageId} value={message} onChange={(event) => setMessage(event.target.value)} rows={4} />
            </div>
            {state.pixKeyValue && (
              <div className="flex items-center gap-4">
                <span className="flex flex-1 flex-col">
                  <Label htmlFor={pixId}>{t('globalBalances_chargePix')}</Label>
                  <span className="text-[13px] text-muted-foreground">
                    {t(`paymentKey_type${state.pixKeyType ?? 'EMAIL'}` as MessageKey)} · {state.pixKeyValue}
                  </span>
                </span>
                <Switch
                  id={pixId}
                  checked={charge.includesPixKey}
                  disabled={!charge.canTogglePixKey}
                  onCheckedChange={(checked) => presenter.togglePixKey(checked)}
                />
              </div>
            )}
          </div>
        ),
        footer: (
          <>
            <Button variant="outline" onClick={() => presenter.copyMessage(message)}>
              <Copy /> {t('globalBalances_chargeCopy')}
            </Button>
            <Button onClick={() => presenter.shareMessage(message)}>
              <Share /> {t('globalBalances_chargeSend')}
            </Button>
          </>
        ),
      }}
    />
  )
}

/** "Copiar mensagem", or Enviar where the browser has no share sheet. */
export function copyChargeMessage(message: string) {
  navigator.clipboard
    .writeText(message)
    .then(() => toast(t('globalBalances_copied')))
    .catch(() => toast(t('globalBalances_copyFailed')))
}

/** Enviar: the browser's share sheet where there is one (phones, Safari), otherwise the message goes to the clipboard. */
export function shareChargeMessage(message: string) {
  if (typeof navigator.share === 'function') {
    navigator.share({ text: message }).catch(() => {})
    return
  }
  copyChargeMessage(message)
}
