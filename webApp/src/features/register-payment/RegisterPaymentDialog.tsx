import { ArrowRight, Check, Info, Loader2 } from 'lucide-react'
import { RegisterPaymentPresenter, type JsPaymentPerson, type JsRegisterPaymentState, type JsRoute, type JsSimplifiedPairNote } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { AmountInput, WhenField } from '@/components/fields'
import { LoadingDialog } from '@/components/LoadingDialog'
import { PersonGroupsList } from '@/components/PersonGroupsList'
import { ResponsiveDialog, type PaneParts } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { nameOrFormer, paymentMethodLabel } from '@/features/balances/BalanceCard'
import { ProfileFace } from '@/features/register-payment/PaymentFaces'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { formatMoney, formatNames } from '@/lib/money'
import { cn } from '@/lib/utils'
import { simplifyName } from '@/features/simplify-debts/simplify'
import { SimplifyIcon } from '@/features/simplify-debts/SimplifyIcon'
import { payWithKeyPane, ReceiverKeysList } from './PayWithKeyPane'
import { simplifyChangePane } from './SimplifyChangePane'
import { PendingDot } from '@/components/common'

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
export function RegisterPaymentDialog({ route, isCovered = false }: { route: JsRoute; isCovered?: boolean }) {
  const presenter = usePresenter(() => new RegisterPaymentPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsRegisterPaymentState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), (action) => {
    if (action === 'save-error') toast(t('registerPayment_saveError'))
  })
  // Why: a profile opened from a face covers the dialog, which stays mounted (closed) and comes back with what was typed.
  if (!presenter || !state || isCovered) return null
  if (!state.isLoaded) return <LoadingDialog isDesktop={isDesktop} onClose={() => presenter.close()} className="sm:max-w-[480px]" />
  // Why: whether "Pagar Ana" or the form opens waits for Ana's keys, which a restored account's full sync brings.
  if (state.isLoadingReceiverKeys) {
    return (
      <LoadingDialog isDesktop={isDesktop} onClose={() => presenter.close()} className="sm:max-w-[480px]">
        <Skeleton className="h-8 w-40" />
        <Skeleton className="mx-auto size-48 rounded-xl" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </LoadingDialog>
    )
  }

  const words = sentences[state.kind] ?? sentences['between-others']
  const from = personName(state.from)
  const to = personName(state.to)
  // Why: the person the sentence is about, the other one besides "você" when one of them is this person.
  const names = { name: state.kind === 'received' ? from : to, from, to }
  const money = (minor: number) => formatMoney(minor, state.currency, state.fractionDigits)
  // Why: paying someone with a key opens "Pagar Ana" (4.5); anything else records a payment made outside. Someone
  // turning "Simplificar dívidas" the other way while it was open takes the place of both (13.5c).
  const pane: PaneParts = state.simplifyChange
    ? simplifyChangePane(presenter, state, state.simplifyChange, money)
    : state.showsReceiverKeys ? payWithKeyPane(presenter, state, money) : {
    leading: <PairFaces presenter={presenter} from={state.from} to={state.to} />,
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

/** Whoever paid → whoever received, at the top, each face opening their profile. */
function PairFaces({
  presenter,
  from,
  to,
}: {
  presenter: RegisterPaymentPresenter
  from: JsPaymentPerson | null | undefined
  to: JsPaymentPerson | null | undefined
}) {
  return (
    <span className="mb-1 flex items-center gap-2">
      <ProfileFace presenter={presenter} person={from} />
      <ArrowRight aria-hidden className="size-4 text-muted-foreground" />
      <ProfileFace presenter={presenter} person={to} />
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
        {/*
          Why: a plain action, not a pill, so it doesn't compete with "Como foi pago" (beta feedback 5). It stays
          focusable once done (aria-disabled), so a press from the keyboard doesn't drop the focus out of the dialog.
        */}
        <Button
          type="button"
          variant="ghost"
          size="sm"
          aria-disabled={state.isFullDebt}
          onClick={() => !state.isFullDebt && presenter.fullDebt()}
          className="mt-1.5 -ml-2.5 self-start text-primary hover:text-primary aria-disabled:pointer-events-none aria-disabled:opacity-60"
        >
          {state.isFullDebt && <Check aria-hidden />}
          {t('registerPayment_fullDebt')}
        </Button>
      </div>
      )}

      <div className="flex flex-col gap-1.5">
        <span id={methodId} className="text-sm font-medium">
          {t('registerPayment_method')}
        </span>
        <div role="radiogroup" aria-labelledby={methodId} className="flex flex-wrap gap-2">
          {state.methods.map((code) => (
            <Chip key={code} isSelected={state.method === code} onClick={() => presenter.pickMethod(code)}>
              {paymentMethodLabel(code)}
            </Chip>
          ))}
        </div>
      </div>

      {/* Why: with Pix picked, where to send it, or that the receiver has no Pix key here (beta feedback 3 item 26). */}
      {state.formPixKeys.length > 0 && (
        <ReceiverKeysList
          title={t('registerPayment_pixKeys', { name: names.to })}
          keys={state.formPixKeys}
          name={names.to}
          onCopy={() => presenter.trackFormPixKeyCopy()}
        />
      )}
      {state.showsNoPixKeyNotice && (
        <p className="flex gap-2 text-[13px] leading-[18px] text-muted-foreground">
          <Info aria-hidden className="mt-px size-4 shrink-0" />
          <span>{t('registerPayment_noPixKey', { name: names.to })}</span>
        </p>
      )}

      <WhenField
        id={whenId}
        label={t('registerPayment_when')}
        millis={state.paidAt}
        relativeDay={state.paidDay}
        latestMillis={state.latestPaidAt}
        onOpen={() => presenter.openPaidAt()}
        onChange={(millis) => presenter.changePaidAt(millis)}
      />

      {state.simplifiedNote && <SimplifiedNote note={state.simplifiedNote} state={state} />}
      <p className="flex gap-2 text-[13px] leading-[18px] text-muted-foreground">
        <Info aria-hidden className="mt-px size-4 shrink-0" />
        <span>
          {t('registerPayment_info')}
          {state.kind === 'between-others' && <> {t('registerPayment_forOthers', names)}</>}
        </span>
      </p>
      {state.showsOfflineNotice && (
        <p className="flex gap-2 text-[13px] leading-[18px] text-muted-foreground">
          <PendingDot className="mt-1.5 size-2" />
          {t('registerPayment_offline')}
        </p>
      )}
    </form>
  )
}

/**
 * "Dívida simplificada: Diego paga a você o que devia a Bruno." for a pair no bill made, "…: inclui o que Ana devia a
 * você." for one that grew (13.5).
 */
function SimplifiedNote({ note, state }: { note: JsSimplifiedPairNote; state: JsRegisterPaymentState }) {
  // Why: the user among whom the payer owed is the object of "owed": "lo que antes era para ti", "der dir geschuldet war".
  const insteadOf = formatNames(note.insteadOf.map((person) => (person.isMe ? t('registerPayment_youOwed') : simplifyName(person))))
  const payer = state.from ? simplifyName(state.from) : nameOrFormer(null)
  const receiver = state.to ? simplifyName(state.to) : nameOrFormer(null)
  const text =
    note.insteadOf.length === 0
      ? t('registerPayment_simplifiedPlain')
      : !note.isNewPair
        ? state.from?.isMe
          ? t('registerPayment_simplifiedGrownMe', { insteadOf })
          : t('registerPayment_simplifiedGrown', { payer, insteadOf })
        : state.from?.isMe
          ? t('registerPayment_simplifiedNewPairMe', { receiver, insteadOf })
          : state.to?.isMe
            ? t('registerPayment_simplifiedNewPairToMe', { payer, insteadOf })
            : t('registerPayment_simplifiedNewPair', { payer, receiver, insteadOf })
  return (
    <p className="flex gap-2 text-[13px] leading-[18px] text-muted-foreground">
      <SimplifyIcon className="mt-px" />
      <span>{text}</span>
    </p>
  )
}

/**
 * A pill of "Como foi pago", a radio.
 */
function Chip({ isSelected, onClick, children }: { isSelected: boolean; onClick: () => void; children: ReactNode }) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={isSelected}
      onClick={onClick}
      className={cn(
        'inline-flex h-8 items-center gap-1.5 self-start rounded-full border px-3 text-sm font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
        isSelected ? 'border-primary bg-primary/10 text-primary' : 'hover:bg-accent/60',
      )}
    >
      {isSelected && <Check aria-hidden className="size-3.5" />}
      {children}
    </button>
  )
}
