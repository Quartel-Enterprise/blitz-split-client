import { ArrowRight, Check, Info, Loader2 } from 'lucide-react'
import { RegisterPaymentPresenter, type JsPaymentPerson, type JsRegisterPaymentState, type JsRoute } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { AmountInput, WhenField } from '@/components/fields'
import { PersonGroupsList } from '@/components/PersonGroupsList'
import { ResponsiveDialog, type PaneParts } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { nameOrFormer, paymentMethodCodes, paymentMethodLabel } from '@/features/balances/BalanceCard'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { formatMoney } from '@/lib/money'
import { cn } from '@/lib/utils'

const formId = 'register-payment-form'
const amountId = 'register-payment-amount'
const amountHelpId = 'register-payment-amount-help'
const methodId = 'register-payment-method'
const whenId = 'register-payment-when'

// Why: each sentence follows who records it: "received" (you got paid), "paid" (you paid) or "between-others".
const sentences: Record<string, { title: MessageKey; debt: MessageKey; remaining: MessageKey; confirm: MessageKey }> = {
  received: {
    title: 'registerPayment_titleReceived',
    debt: 'registerPayment_debtReceived',
    remaining: 'registerPayment_remainingReceived',
    confirm: 'registerPayment_confirmReceived',
  },
  paid: {
    title: 'registerPayment_titlePaid',
    debt: 'registerPayment_debtPaid',
    remaining: 'registerPayment_remainingPaid',
    confirm: 'registerPayment_confirmPaid',
  },
  'between-others': {
    title: 'registerPayment_titleOthers',
    debt: 'registerPayment_debtOthers',
    remaining: 'registerPayment_remainingOthers',
    confirm: 'registerPayment_confirmOthers',
  },
}

/**
 * "Registrar pagamento", a payment made outside the app, over Saldos, Despesas do grupo or Membros, or settling up with
 * someone across groups over the home's Saldos ("settle-up"): a dialog on
 * desktop and tablet, a drawer on the phone, like "Rachar conta". Closing it any way is the ViewModel's close; it
 * closes by itself once saved, and when the debt is gone.
 */
export function RegisterPaymentDialog({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new RegisterPaymentPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsRegisterPaymentState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), (action) => {
    if (action === 'save-error') toast(t('registerPayment_saveError'))
  })
  if (!presenter || !state?.isLoaded) return null

  const words = sentences[state.kind] ?? sentences['between-others']
  const from = personName(state.from)
  const to = personName(state.to)
  // Why: the person the sentence is about, the other one besides "você" when one of them is this person.
  const names = { name: state.kind === 'received' ? from : to, from, to }
  const money = (minor: number) => formatMoney(minor, state.currency, state.fractionDigits)
  const pane: PaneParts = {
    leading: <PairFaces from={state.from} to={state.to} />,
    title: t(words.title, names),
    description: t(words.debt, { ...names, amount: money(state.debtMinor) }),
    body: <PaymentForm presenter={presenter} state={state} words={words} names={names} money={money} />,
    footer: (
      <>
        <Button variant="outline" onClick={() => presenter.close()}>
          {t('common_cancel')}
        </Button>
        <Button type="submit" form={formId} disabled={!state.canConfirm}>
          {state.isSaving && <Loader2 className="animate-spin" />}
          {t(words.confirm)}
        </Button>
      </>
    ),
  }
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      pane={pane}
      onOpenChange={(open) => {
        if (!open) presenter.close()
      }}
      className="gap-5 sm:max-w-[480px]"
    />
  )
}

function personName(person: JsPaymentPerson | null | undefined): string {
  return nameOrFormer(person?.name)
}

/** A member's face, blank for someone no longer in the group, as Saldos draws them. */
function PersonFace({ person }: { person: JsPaymentPerson | null | undefined }) {
  if (!person) return <span className="size-10 rounded-full bg-muted" />
  return <Avatar initial={person.initials} color={person.color} className="size-10 text-sm" />
}

/** Whoever paid → whoever received, at the top. */
function PairFaces({ from, to }: { from: JsPaymentPerson | null | undefined; to: JsPaymentPerson | null | undefined }) {
  return (
    <span aria-hidden className="mb-1 flex items-center gap-2">
      <PersonFace person={from} />
      <ArrowRight className="size-4 text-muted-foreground" />
      <PersonFace person={to} />
    </span>
  )
}

function PaymentForm({
  presenter,
  state,
  words,
  names,
  money,
}: {
  presenter: RegisterPaymentPresenter
  state: JsRegisterPaymentState
  words: (typeof sentences)[string]
  names: Record<string, string>
  money: (minor: number) => string
}) {
  // Why: the ViewModel filters what is typed, so the field reads its text back in the same keystroke.
  const [amount, setAmount] = useDraft(state.amountText, (value) => {
    presenter.changeAmount(value)
    return presenter.getAmountText()
  })
  const amountHelp = state.showsOverDebtError
    ? t('registerPayment_overDebt', { debt: t(words.debt, { ...names, amount: money(state.debtMinor) }) })
    : state.remainingMinor != null
      ? t(words.remaining, { ...names, amount: money(state.remainingMinor) })
      : null
  return (
    <form
      id={formId}
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault()
        presenter.confirm()
      }}
    >
      {state.isAmountLocked ? (
        // Why: across several groups only the whole net settles, so the groups it closes show instead of the field.
        <div className="flex flex-col gap-1.5">
          <PersonGroupsList
            groups={state.groupParts}
            netMinor={state.groupParts.reduce((sum, part) => sum + part.amountMinor, 0)}
            currency={state.currency}
            fractionDigits={state.fractionDigits}
          />
          <p className="text-[13px] text-muted-foreground">{t('globalBalances_oneMessagePerGroup')}</p>
        </div>
      ) : (
      <div className="flex flex-col gap-1.5">
        <Label htmlFor={amountId} className={cn(state.showsOverDebtError && 'text-destructive')}>
          {t('registerPayment_amount')}
        </Label>
        <AmountInput
          id={amountId}
          currency={state.currency}
          fractionDigits={state.fractionDigits}
          value={amount}
          onChange={setAmount}
          isInvalid={state.showsOverDebtError}
          describedBy={amountHelp ? amountHelpId : undefined}
          autoFocus
        />
        {amountHelp && (
          <p id={amountHelpId} className={cn('text-[13px]', state.showsOverDebtError ? 'text-destructive' : 'text-muted-foreground')}>
            {amountHelp}
          </p>
        )}
        <Chip isSelected={state.isFullDebt} role={undefined} onClick={() => presenter.fullDebt()}>
          {t('registerPayment_fullDebt')}
        </Chip>
      </div>
      )}

      <div className="flex flex-col gap-1.5">
        <span id={methodId} className="text-sm font-medium">
          {t('registerPayment_method')}
        </span>
        <div role="radiogroup" aria-labelledby={methodId} className="flex flex-wrap gap-2">
          {paymentMethodCodes.map((code) => (
            <Chip key={code} role="radio" isSelected={state.method === code} onClick={() => presenter.pickMethod(code)}>
              {paymentMethodLabel(code)}
            </Chip>
          ))}
        </div>
      </div>

      <WhenField
        id={whenId}
        label={t('registerPayment_when')}
        millis={state.paidAt}
        relativeDay={state.paidDay}
        latestMillis={state.latestPaidAt}
        onOpen={() => presenter.openPaidAt()}
        onChange={(millis) => presenter.changePaidAt(millis)}
      />

      <p className="flex gap-2 text-[13px] leading-[18px] text-muted-foreground">
        <Info aria-hidden className="mt-px size-4 shrink-0" />
        <span>
          {t('registerPayment_info')}
          {state.kind === 'between-others' && <> {t('registerPayment_forOthers', names)}</>}
        </span>
      </p>
      {state.showsOfflineNotice && (
        <p className="flex gap-2 text-[13px] leading-[18px] text-muted-foreground">
          <span aria-hidden className="mt-1.5 size-2 shrink-0 rounded-full bg-orange-500" />
          {t('registerPayment_offline')}
        </p>
      )}
    </form>
  )
}

/**
 * A pill to pick: "Toda a dívida" (pressed while the amount is the whole debt) or a method of "Como foi pago" (a radio).
 */
function Chip({ isSelected, role, onClick, children }: { isSelected: boolean; role: 'radio' | undefined; onClick: () => void; children: ReactNode }) {
  return (
    <button
      type="button"
      role={role}
      aria-checked={role === 'radio' ? isSelected : undefined}
      aria-pressed={role === 'radio' ? undefined : isSelected}
      onClick={onClick}
      className={cn(
        'inline-flex h-8 items-center gap-1.5 self-start rounded-full border px-3 text-sm font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50',
        isSelected ? 'border-primary bg-primary/10 text-primary' : 'hover:bg-accent/60',
      )}
    >
      {isSelected && <Check aria-hidden className="size-3.5" />}
      {children}
    </button>
  )
}
