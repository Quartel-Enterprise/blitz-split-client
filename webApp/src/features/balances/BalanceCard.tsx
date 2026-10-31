import type { JsMemberBalance, JsPaymentNotice } from 'blitzsplit-shared'
import { useEffect } from 'react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { t, type MessageKey } from '@/i18n'
import { formatMoney, formatSignedMoney } from '@/lib/money'
import { cn } from '@/lib/utils'

/** "Ex-membro" for someone no longer in the group. */
export function nameOrFormer(name: string | null | undefined): string {
  return name ?? t('balance_formerMember')
}

// Why: "Como foi pago", by the SettlementMethod's code, in the order Registrar pagamento offers them.
const methodLabels: Record<string, MessageKey> = {
  pix: 'payment_methodPix',
  cash: 'payment_methodCash',
  transfer: 'payment_methodTransfer',
  other: 'payment_methodOther',
}

export const paymentMethodCodes = Object.keys(methodLabels)

/** "Pix", "Dinheiro", "Transferência" or "Outro". */
export function paymentMethodLabel(code: string): string {
  return t(methodLabels[code] ?? 'payment_methodOther')
}

/** The color of a signed balance: positive to get back, destructive to owe, muted at zero. */
export function balanceTone(minor: number): string {
  return minor > 0 ? 'text-primary' : minor < 0 ? 'text-destructive' : 'text-muted-foreground'
}

/**
 * "Seu saldo neste grupo", over the group's tabs on Despesas, Saldos and Membros: what this person gets back or owes
 * over every pair they're in, and "Acertar" while they owe anyone.
 */
export function BalanceCard({
  balance,
  isEveryoneSettled,
  currency,
  fractionDigits,
  onSettle,
}: {
  balance: JsMemberBalance
  isEveryoneSettled: boolean
  currency: string
  fractionDigits: number
  onSettle: () => void
}) {
  const money = (minor: number) => formatMoney(minor, currency, fractionDigits)
  const net = balance.netMinor
  const details = balance.isSettled
    ? t(isEveryoneSettled ? 'balance_everyoneSettled' : 'balance_othersSettling')
    : balance.receivesMinor > 0 && balance.owesMinor > 0
      ? t('balance_getAndOwe', { received: money(balance.receivesMinor), owed: money(balance.owesMinor) })
      : balance.receivesMinor > 0
        ? t('balance_get', { amount: money(balance.receivesMinor) })
        : t('balance_owe', { amount: money(balance.owesMinor) })
  return (
    <section aria-labelledby="balance-card-title" className="flex items-center gap-3 rounded-xl border bg-card px-4 py-3.5">
      <div className="flex min-w-0 flex-1 flex-col gap-0.5">
        <h2 id="balance-card-title" className="text-[13px] text-muted-foreground">
          {t('balance_title')}
        </h2>
        {balance.isSettled ? (
          <span className="text-lg font-semibold tabular-nums">{t('balance_settled', { amount: money(0) })}</span>
        ) : (
          <span className="flex flex-wrap items-baseline gap-x-2">
            <span className={cn('text-xl font-semibold tabular-nums', net === 0 ? 'text-foreground' : balanceTone(net))}>
              {formatSignedMoney(net, currency, fractionDigits)}
            </span>
            {net !== 0 && <span className={cn('font-medium', balanceTone(net))}>{t(net > 0 ? 'balance_toGetBack' : 'balance_youOwe')}</span>}
          </span>
        )}
        <span className="text-[13px] text-muted-foreground">{details}</span>
      </div>
      {balance.canSettle && <Button onClick={onSettle}>{t('balance_settle')}</Button>}
    </section>
  )
}

/** "Pagamento de Ana registrado.", "Pagamento para Bruno registrado." or "Pagamento registrado.". */
function recordedPaymentText(notice: JsPaymentNotice): string {
  if (notice.fromIsMe) return notice.toName != null ? t('balance_paymentRecordedTo', { name: notice.toName }) : t('balance_paymentRecorded')
  return notice.fromName != null ? t('balance_paymentRecordedFrom', { name: notice.fromName }) : t('balance_paymentRecorded')
}

interface RecordedPaymentEvents {
  undoPayment(): void
  recordedPaymentNoticeShown(): void
}

/**
 * The toast after Registrar pagamento closed over this screen, with Desfazer. Once it goes away without Desfazer the
 * ViewModel hears it was shown, so it never shows twice. The screen leaving takes the toast with it, quietly, since
 * Desfazer needs this screen's ViewModel.
 */
export function useRecordedPaymentNotice(presenter: RecordedPaymentEvents | null, notice: JsPaymentNotice | null | undefined) {
  const settlementId = notice?.settlementId
  useEffect(() => {
    if (!presenter || !notice) return
    let isClosed = false
    const shown = () => {
      if (!isClosed) presenter.recordedPaymentNoticeShown()
    }
    const id = toast(recordedPaymentText(notice), {
      duration: 6000,
      action: {
        label: t('balance_undo'),
        onClick: () => {
          if (!isClosed) presenter.undoPayment()
        },
      },
      onAutoClose: shown,
      onDismiss: shown,
    })
    return () => {
      isClosed = true
      toast.dismiss(id)
    }
    // The notice is the same while its settlement is; a redrawn state doesn't show it again
  }, [presenter, settlementId])
}
