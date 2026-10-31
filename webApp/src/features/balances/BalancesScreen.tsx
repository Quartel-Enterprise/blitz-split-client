import { ArrowRight, CircleCheck, Scale } from 'lucide-react'
import { BalancesPresenter, type JsBalancesState, type JsDebtRow, type JsGroupFace, type JsRoute } from 'blitzsplit-shared'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { Button } from '@/components/ui/button'
import { GroupPage } from '@/features/group-detail/GroupPage'
import { t, type MessageKey } from '@/i18n'
import { formatMoney, formatSignedMoney } from '@/lib/money'
import { cn } from '@/lib/utils'
import { balanceTone, nameOrFormer, useRecordedPaymentNotice } from './BalanceCard'

type Money = (minor: number) => string

// Why: the group's currency, plain ("R$ 64,00") and signed ("−R$ 118,00").
interface Formats {
  money: Money
  signed: Money
}

// Why: each pair's button says what recording its payment means for whoever looks: "Acertar", "Recebi" or "Já pagou".
const debtButtons: Record<string, MessageKey> = {
  'you-owe': 'balances_settle',
  'owes-you': 'balances_gotPaid',
  'between-others': 'balances_alreadyPaid',
}

/**
 * "Saldos", the group's tab beside Despesas and Membros: who owes whom, each pair once and already netted, "Com você"
 * first and "Entre os outros" under it. Each pair opens Registrar pagamento over it.
 */
export function BalancesScreen({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new BalancesPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsBalancesState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), (action) => {
    if (action === 'coming-soon') toast(t('groups_comingSoon'))
    if (action === 'change-error') toast(t('groupDetail_changeError'))
  })
  useRecordedPaymentNotice(presenter, state?.recordedPayment)
  if (!presenter || !state?.isLoaded) return null

  const { header } = state
  const formats: Formats = {
    money: (minor) => formatMoney(minor, header.currency, header.fractionDigits),
    signed: (minor) => formatSignedMoney(minor, header.currency, header.fractionDigits),
  }
  return (
    <GroupPage
      header={header}
      tab="balances"
      hasExpenses={state.hasBills}
      showsSplitBillButton={state.showsSplitBillButton}
      events={presenter}
      onSelectTab={(tab) => {
        if (tab === 'expenses') presenter.expensesTab()
        else if (tab === 'members') presenter.members()
      }}
    >
      {state.isEmpty ? (
        <section className="flex flex-col items-center gap-2 rounded-xl border border-dashed px-6 py-10 text-center">
          <span aria-hidden className="flex size-11 items-center justify-center rounded-full bg-muted text-muted-foreground">
            <Scale className="size-5" />
          </span>
          <h2 className="text-base font-semibold">{t('balances_emptyTitle')}</h2>
          <p className="max-w-[420px] text-sm text-muted-foreground">{t('balances_emptyBody')}</p>
        </section>
      ) : (
        <>
          {header.isEveryoneSettled && (
            <section className="flex flex-col gap-3 rounded-xl border border-primary/30 bg-primary/10 p-4 sm:flex-row sm:items-center">
              <CircleCheck aria-hidden className="size-5 shrink-0 text-primary" />
              <div className="flex-1">
                <h2 className="font-semibold">{t('balances_allSettledTitle')}</h2>
                <p className="text-sm text-muted-foreground">{t('balances_allSettledBody')}</p>
              </div>
              <Button variant="outline" onClick={() => presenter.deleteGroup()} className="self-start text-destructive hover:text-destructive sm:self-center">
                {t('groupDetail_deleteGroup')}
              </Button>
            </section>
          )}
          {state.myDebts.length > 0 && (
            <DebtSection id="balances-with-you" title={t('balances_withYou')} hint={myHint(state, formats.money)} debts={state.myDebts} presenter={presenter} formats={formats} />
          )}
          {state.otherDebts.length > 0 && (
            <DebtSection
              id="balances-between-others"
              title={t('balances_betweenOthers')}
              hint={t(state.otherDebts.length === 1 ? 'balances_toSettleOne' : 'balances_toSettleOther', { count: state.otherDebts.length })}
              debts={state.otherDebts}
              presenter={presenter}
              formats={formats}
            />
          )}
          {state.showsDebtNote && <p className="text-[13px] leading-[18px] text-muted-foreground">{t('balances_note')}</p>}
        </>
      )}
    </GroupPage>
  )
}

/** "recebe R$ 332,90 · deve R$ 118,00", without the part that is zero. */
function myHint(state: JsBalancesState, money: Money): string | null {
  const balance = state.header.myBalance
  if (!balance) return null
  const parts: string[] = []
  if (balance.receivesMinor > 0) parts.push(t('balances_hintGet', { amount: money(balance.receivesMinor) }))
  if (balance.owesMinor > 0) parts.push(t('balances_hintOwe', { amount: money(balance.owesMinor) }))
  return parts.length > 0 ? parts.join(' · ') : null
}

function DebtSection({
  id,
  title,
  hint,
  debts,
  presenter,
  formats,
}: {
  id: string
  title: string
  hint: string | null
  debts: JsDebtRow[]
  presenter: BalancesPresenter
  formats: Formats
}) {
  return (
    <section aria-labelledby={id} className="flex flex-col gap-2">
      <div className="flex items-baseline justify-between gap-3">
        <h2 id={id} className="text-sm font-semibold">
          {title}
        </h2>
        {hint && <span className="text-[13px] text-muted-foreground">{hint}</span>}
      </div>
      <ul className="flex flex-col divide-y overflow-hidden rounded-xl border bg-card">
        {debts.map((debt) => (
          <DebtRowItem key={`${debt.fromMemberId}/${debt.toMemberId}`} debt={debt} presenter={presenter} formats={formats} />
        ))}
      </ul>
    </section>
  )
}

/** "Você deve a Bruno", "Ana te deve" or "Diego deve a Bruno". */
function debtSentence(debt: JsDebtRow): string {
  if (debt.kind === 'you-owe') return t('balances_youOwe', { name: nameOrFormer(debt.toName) })
  if (debt.kind === 'owes-you') return t('balances_owesYou', { name: nameOrFormer(debt.fromName) })
  return t('balances_owes', { from: nameOrFormer(debt.fromName), to: nameOrFormer(debt.toName) })
}

/** "−R$ 118,00" what you owe, "+R$ 162,30" what you get back, "R$ 64,00" between the others. */
function debtAmount(debt: JsDebtRow, formats: Formats): { text: string; tone: string } {
  if (debt.kind === 'you-owe') return { text: formats.signed(-debt.amountMinor), tone: balanceTone(-1) }
  if (debt.kind === 'owes-you') return { text: formats.signed(debt.amountMinor), tone: balanceTone(1) }
  return { text: formats.money(debt.amountMinor), tone: 'text-foreground' }
}

/** A pair: whoever owes → whoever gets it, the sentence, the amount and the button; the whole row opens the payment. */
function DebtRowItem({ debt, presenter, formats }: { debt: JsDebtRow; presenter: BalancesPresenter; formats: Formats }) {
  const open = () => presenter.openDebt(debt.fromMemberId, debt.toMemberId)
  const sentence = debtSentence(debt)
  const amount = debtAmount(debt, formats)
  const label = t(debtButtons[debt.kind] ?? 'balances_alreadyPaid')
  return (
    <li onClick={open} className="flex min-h-14 cursor-pointer items-center gap-3 px-4 py-2.5 hover:bg-accent/60">
      <span aria-hidden className="flex shrink-0 items-center gap-1">
        <Face face={debt.fromFace} />
        <ArrowRight className="size-3.5 text-muted-foreground" />
        <Face face={debt.toFace} />
      </span>
      <span className="flex min-w-0 flex-1 flex-col sm:flex-row sm:items-center sm:gap-3">
        <span className="truncate font-medium sm:flex-1">{sentence}</span>
        <span className={cn('text-sm font-semibold whitespace-nowrap tabular-nums sm:text-base', amount.tone)}>{amount.text}</span>
      </span>
      <Button
        size="sm"
        variant={debt.kind === 'you-owe' ? 'default' : 'outline'}
        aria-label={`${label}: ${sentence}, ${amount.text}`}
        onClick={(event) => {
          event.stopPropagation()
          open()
        }}
      >
        {label}
      </Button>
    </li>
  )
}

/** A member's face, or a blank avatar for someone no longer in the group. */
function Face({ face }: { face: JsGroupFace | null | undefined }) {
  if (!face) return <span className="size-8 shrink-0 rounded-full border border-dashed bg-muted" />
  return <Avatar initial={face.initials} color={face.color} className="size-8 text-xs" />
}
