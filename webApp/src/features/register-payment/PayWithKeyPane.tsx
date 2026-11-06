import { Copy, Loader2 } from 'lucide-react'
import type { JsReceiverKey, JsRegisterPaymentState, RegisterPaymentPresenter } from 'blitzsplit-shared'
import { toast } from 'sonner'
import type { PaneParts } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { nameOrFormer } from '@/features/balances/BalanceCard'
import { QrCode } from '@/features/invite/InviteDialog'
import { ProfileFace } from '@/features/register-payment/PaymentFaces'
import { t } from '@/i18n'
import type { PaymentKeyType } from '@/lib/paymentKey'
import { PendingDot } from '@/components/common'

/**
 * "Pagar Ana" (4.5), what Registrar pagamento shows while this person pays someone with a key: the key to copy, the
 * Pix code with the amount in it, and "Marcar como pago" once paid in the bank or the wallet. The app moves no money.
 */
export function payWithKeyPane(
  presenter: RegisterPaymentPresenter,
  state: JsRegisterPaymentState,
  money: (minor: number) => string,
): PaneParts {
  const name = nameOrFormer(state.to?.name)
  return {
    leading: state.to && (
      <span className="mb-1 flex">
        <ProfileFace presenter={presenter} person={state.to} className="size-12 text-base" />
      </span>
    ),
    title: t('payWithKey_title', { name, amount: money(state.debtMinor) }),
    description: state.groupName
      ? t('payWithKey_group', { group: state.groupName })
      : t(state.groupParts.length === 1 ? 'payWithKey_groupsOne' : 'payWithKey_groupsOther', { count: state.groupParts.length }),
    body: <PayWithKeyBody presenter={presenter} state={state} name={name} money={money} />,
    footer: (
      <>
        <Button variant="ghost" disabled={state.isSaving} onClick={() => presenter.close()}>
          {t('payWithKey_later')}
        </Button>
        <Button disabled={!state.canMarkAsPaid} onClick={() => presenter.markAsPaid()}>
          {state.isSaving && <Loader2 className="animate-spin" />}
          {t('payWithKey_markAsPaid')}
        </Button>
      </>
    ),
  }
}

// Why: the page writes the clipboard itself; each caller first tells the shared code which copy it was.
function copy(text: string) {
  navigator.clipboard
    .writeText(text)
    .then(() => toast(t('payWithKey_copied')))
    .catch(() => toast(t('payWithKey_copyFailed')))
}

// Why: the Pix key types as the native apps name them here ("Random key" rather than the form's "Random").
function typeLabel(key: JsReceiverKey): string {
  if (key.type === 'RANDOM') return t('payWithKey_typeRandom')
  return t(`paymentKey_type${key.type as PaymentKeyType}`)
}

/**
 * "Outras chaves de Ana", and the form's "Chaves Pix de Ana" with Pix picked, each with its own copy button; onCopy
 * tells the shared code which copy it was before the page writes the clipboard.
 */
export function ReceiverKeysList({
  title,
  keys,
  name,
  onCopy,
}: {
  title: string
  keys: JsReceiverKey[]
  name: string
  onCopy: () => void
}) {
  return (
    <section className="flex flex-col gap-1.5">
      <h3 className="text-sm text-muted-foreground">{title}</h3>
      <ul className="divide-y rounded-xl border">
        {keys.map((key) => (
          <li key={key.id} className="flex items-center gap-2 py-2 pr-1 pl-4">
            <div className="min-w-0 flex-1">
              <p className="text-xs text-muted-foreground">{typeLabel(key)}</p>
              <p className="font-medium break-all">{key.text}</p>
            </div>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              aria-label={t('payWithKey_copyOther', { type: typeLabel(key), name })}
              onClick={() => {
                onCopy()
                copy(key.value)
              }}
            >
              <Copy aria-hidden />
            </Button>
          </li>
        ))}
      </ul>
    </section>
  )
}

function PayWithKeyBody({
  presenter,
  state,
  name,
  money,
}: {
  presenter: RegisterPaymentPresenter
  state: JsRegisterPaymentState
  name: string
  money: (minor: number) => string
}) {
  const key = state.defaultKey
  return (
    <div className="flex flex-col gap-4">
      {state.showsKeyMethodPicker && (
        <Tabs value={state.keyMethod} onValueChange={(method) => presenter.pickKeyMethod(method)}>
          <TabsList className="w-full" aria-label={t('payWithKey_method')}>
            {state.keyMethods.map((method) => (
              <TabsTrigger key={method} value={method}>
                {t(method === 'PIX' ? 'payWithKey_methodPix' : 'payWithKey_methodBitcoin')}
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>
      )}

      {key && (
        <section className="flex flex-col gap-3 rounded-xl border p-4">
          <div className="flex items-center justify-between gap-2">
            <h3 className="text-sm text-muted-foreground">{t('payWithKey_defaultKey', { name })}</h3>
            <span className="rounded-md bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">{typeLabel(key)}</span>
          </div>
          <p className="text-xl font-bold break-all">{key.text}</p>
          <Button
            onClick={() => {
              presenter.trackKeyCopy()
              copy(key.value)
            }}
          >
            <Copy aria-hidden />
            {t('payWithKey_copyKey')}
          </Button>
        </section>
      )}

      {state.pixCode && (
        <section className="flex items-center gap-4 rounded-xl border p-4">
          {state.pixQrSize > 0 && <QrCode size={state.pixQrSize} modules={state.pixQrModules} className="size-28 shrink-0 border" />}
          <div className="flex min-w-0 flex-col items-start gap-1">
            <h3 className="font-semibold">{t('payWithKey_pixCode')}</h3>
            <p className="text-[13px] text-muted-foreground">{t('payWithKey_pixCodeAmount', { amount: money(state.debtMinor) })}</p>
            <Button
              variant="outline"
              size="sm"
              className="mt-1"
              onClick={() => {
                presenter.trackPixCodeCopy()
                copy(state.pixCode!)
              }}
            >
              {t('payWithKey_copyCode')}
            </Button>
          </div>
        </section>
      )}

      {state.otherKeys.length > 0 && (
        <ReceiverKeysList
          title={t('payWithKey_otherKeys', { name })}
          keys={state.otherKeys}
          name={name}
          onCopy={() => presenter.trackOtherKeyCopy()}
        />
      )}

      <p className="text-[13px] leading-[18px] text-muted-foreground">
        {state.keyMethod === 'PIX' ? t('payWithKey_notePix', { name }) : t('payWithKey_noteBitcoin')}
      </p>
      {state.showsOfflineNotice && (
        <p className="flex gap-2 text-[13px] leading-[18px] text-muted-foreground">
          <PendingDot className="mt-1.5 size-2" />
          {t('registerPayment_offline')}
        </p>
      )}
      <Button variant="link" className="self-start px-0" disabled={state.isSaving} onClick={() => presenter.payOtherwise()}>
        {t('payWithKey_payOtherwise')}
      </Button>
    </div>
  )
}
