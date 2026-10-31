import { Minus, Plus } from 'lucide-react'
import type { JsSplitBillState, JsSplitEditor, JsSplitRow, SplitBillPresenter } from 'blitzsplit-shared'
import { Avatar } from '@/components/common'
import { Button } from '@/components/ui/button'
import { useDraft } from '@/hooks/useDraft'
import { t, type MessageKey } from '@/i18n'
import { amountPlaceholder, currencySymbol } from '@/lib/money'
import { payerNamesText } from '@/lib/payers'
import { cn } from '@/lib/utils'
import type { PaneParts } from '@/components/ResponsiveDialog'
import { personName, splitStatus, type Money } from './splitText'

// Why: Itens isn't built yet, so the four modes of the shared SplitMode.
const modes: { mode: string; label: MessageKey }[] = [
  { mode: 'equal', label: 'split_modeEqual' },
  { mode: 'amount', label: 'split_modeAmount' },
  { mode: 'percent', label: 'split_modePercent' },
  { mode: 'shares', label: 'split_modeShares' },
]

const listLabels: Record<string, MessageKey> = {
  equal: 'split_listEqual',
  amount: 'split_listAmount',
  percent: 'split_listPercent',
  shares: 'split_listShares',
}

const hints: Record<string, MessageKey> = {
  equal: 'split_hintEqual',
  amount: 'split_hintAmount',
  percent: 'split_hintPercent',
  shares: 'split_hintShares',
}

const maxParts = 99

/**
 * "Dividir": the mode, a status that says whether the split adds up, and one row per person. It edits a copy the
 * ViewModel keeps until "Pronto", which only works once it adds up; "Voltar" leaves the bill's split as it was.
 */
export function splitPane(presenter: SplitBillPresenter, state: JsSplitBillState, editor: JsSplitEditor, money: Money): PaneParts {
  const several = state.severalPayers
  const payerNames = several?.paying.map((row) => (row.person ? personName(row.person) : t('splitBill_formerMember'))) ?? []
  // Why: "Ana pagou" once the other payers of a new bill left the group and only one is left paying part of it.
  const paid = several
    ? payerNames.length === 1
      ? t('split_paid', { name: payerNames[0] })
      : t('split_paidSeveral', { names: payerNamesText(payerNames) })
    : state.payer == null
      ? null
      : state.payer.isMe
        ? t('split_youPaid')
        : t('split_paid', { name: state.payer.name })
  const lead = [state.description.trim(), paid, money(editor.totalMinor)].filter(Boolean).join(' · ')
  return {
    title: t('split_title'),
    description: lead,
    body: <SplitBody presenter={presenter} state={state} editor={editor} money={money} />,
    footer: (
      <>
        <Button key="split-back" variant="outline" onClick={() => presenter.dismissSplit()}>
          {t('common_back')}
        </Button>
        <Button key="split-done" disabled={!editor.addsUp} onClick={() => presenter.confirmSplit()}>
          {t('splitBill_done')}
        </Button>
      </>
    ),
  }
}

function SplitBody({ presenter, state, editor, money }: { presenter: SplitBillPresenter; state: JsSplitBillState; editor: JsSplitEditor; money: Money }) {
  const status = splitStatus(editor, money)
  const excludedIds = new Set(editor.excluded.map((person) => person.id))
  const missingIds = new Set(editor.addsUp ? [] : editor.withoutValue.map((person) => person.id))
  return (
    <div className="flex flex-col gap-4">
      <div role="radiogroup" aria-label={t('split_modes')} className="grid h-9 grid-cols-4 rounded-lg bg-muted p-[3px] text-muted-foreground">
        {modes.map(({ mode, label }) => (
          <button
            key={mode}
            type="button"
            role="radio"
            aria-checked={editor.mode === mode}
            onClick={() => presenter.changeSplitMode(mode)}
            className={cn(
              'flex items-center justify-center rounded-md text-sm font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
              editor.mode === mode ? 'bg-background text-foreground shadow-sm dark:bg-input/30' : 'hover:text-foreground',
            )}
          >
            {t(label)}
          </button>
        ))}
      </div>

      <div role="status" className={cn('flex flex-col gap-2 rounded-lg border px-4 py-3', status.isError && 'border-destructive text-destructive')}>
        <strong className="font-semibold">{status.title}</strong>
        {status.progress != null && (
          <span aria-hidden className="flex h-1.5 overflow-hidden rounded-full bg-muted">
            <span className={cn('rounded-full', status.isError ? 'bg-destructive' : 'bg-primary')} style={{ width: `${status.progress}%` }} />
          </span>
        )}
        <span className="text-[13px] text-muted-foreground">{status.body}</span>
      </div>

      <div className="flex flex-col gap-2">
        <div className="flex min-h-8 items-center justify-between gap-2">
          <span className="text-sm font-medium">{t(listLabels[editor.mode] ?? 'split_listEqual')}</span>
          {editor.mode === 'equal' && (
            <Button variant="link" size="sm" className="h-8 px-0" onClick={() => presenter.toggleEveryone()}>
              {t(editor.isEveryoneIncluded ? 'split_uncheckAll' : 'split_checkAll')}
            </Button>
          )}
        </div>
        <ul className="flex flex-col divide-y overflow-hidden rounded-lg border">
          {editor.rows.map((row) => (
            <SplitPersonRow
              key={row.person.id}
              presenter={presenter}
              row={row}
              mode={editor.mode}
              currency={state.currency}
              fractionDigits={state.fractionDigits}
              paidLabel={state.payerIds.includes(row.person.id) ? (state.severalPayers ? 'split_paidPart' : 'split_paidBill') : null}
              isOut={excludedIds.has(row.person.id)}
              isMissing={missingIds.has(row.person.id)}
              money={money}
            />
          ))}
        </ul>
      </div>

      <p className="text-[13px] text-muted-foreground">{t(hints[editor.mode] ?? 'split_hintEqual', { zero: money(0) })}</p>
    </div>
  )
}

interface RowProps {
  presenter: SplitBillPresenter
  row: JsSplitRow
  mode: string
  currency: string
  fractionDigits: number
  // Why: "Pagou a conta", or "Pagou parte da conta" for one of several payers; null for whoever didn't pay.
  paidLabel: MessageKey | null
  // Why: out of the split in this mode: unchecked, blank or at 0.
  isOut: boolean
  // Why: "Sem valor", a blank part in Valores while the split doesn't add up.
  isMissing: boolean
  money: Money
}

function SplitPersonRow({ presenter, row, mode, currency, fractionDigits, paidLabel, isOut, isMissing, money }: RowProps) {
  const person = row.person
  const name = personName(person)
  // Why: "Parte de Pierre" reads better to a screen reader than "Parte de Você".
  const ariaName = person.name
  const share = row.shareMinor != null ? money(row.shareMinor) : null
  // Why: Valores keeps everyone bright, since a blank part there is flagged rather than out.
  const isDimmed = isOut && mode !== 'amount'

  let subtitle: string | null
  let control
  switch (mode) {
    case 'amount':
      subtitle = paidLabel
        ? t(paidLabel)
        : isMissing
          ? t('split_noValue')
          : !person.hasJoined
            ? t('splitBill_notJoined')
            : t('split_theirPart')
      control = (
        <ValueField presenter={presenter} row={row} prefix={currencySymbol(currency)} placeholder={amountPlaceholder(fractionDigits)} isInvalid={isMissing} label={t('split_amountLabel', { name: ariaName })} className="w-28" />
      )
      break
    case 'percent':
      subtitle = isOut ? t('split_out') : share
      control = <ValueField presenter={presenter} row={row} suffix="%" placeholder="0" isInvalid={false} label={t('split_percentLabel', { name: ariaName })} className="w-[72px]" />
      break
    case 'shares':
      subtitle = isOut ? t('split_out') : share
      control = (
        <span className="flex items-center gap-1">
          <Button variant="outline" size="icon-sm" disabled={row.parts <= 0} aria-label={t('split_fewerShares', { name: ariaName })} onClick={() => presenter.decreaseParts(person.id)}>
            <Minus />
          </Button>
          <span aria-live="polite" aria-label={t('split_sharesLabel', { name: ariaName, count: row.parts })} className="w-7 text-center font-mono text-[13px] tabular-nums">
            {row.parts}
          </span>
          <Button variant="outline" size="icon-sm" disabled={row.parts >= maxParts} aria-label={t('split_moreShares', { name: ariaName })} onClick={() => presenter.increaseParts(person.id)}>
            <Plus />
          </Button>
        </span>
      )
      break
    default:
      subtitle = paidLabel ? t(paidLabel) : row.isIncluded ? t('split_in') : t('split_out')
      control = <span className="font-mono text-[13px] tabular-nums">{share ?? '—'}</span>
  }

  const content = (
    <>
      {mode === 'equal' && (
        <input
          type="checkbox"
          checked={row.isIncluded}
          onChange={() => presenter.togglePerson(person.id)}
          aria-label={t('split_includeLabel', { name: ariaName })}
          className="size-4 shrink-0 accent-primary"
        />
      )}
      <Avatar initial={person.initials} color={person.color} className={cn('size-7 text-[11px]', isDimmed && 'opacity-50')} />
      <span className="flex min-w-0 flex-1 flex-col">
        <span className={cn('truncate font-medium', isDimmed && 'text-muted-foreground')}>{name}</span>
        {subtitle && <span className={cn('truncate text-xs', isMissing ? 'text-destructive' : 'text-muted-foreground')}>{subtitle}</span>}
      </span>
      {control}
    </>
  )
  // Why: in Igual the whole row toggles the checkbox, as a label does.
  return (
    <li>
      {mode === 'equal' ? (
        <label className="flex min-h-12 cursor-pointer items-center gap-2.5 px-3 py-1.5 hover:bg-accent/40">{content}</label>
      ) : (
        <div className="flex min-h-12 items-center gap-2.5 px-3 py-1.5">{content}</div>
      )}
    </li>
  )
}

interface ValueFieldProps {
  presenter: SplitBillPresenter
  row: JsSplitRow
  prefix?: string
  suffix?: string
  placeholder: string
  isInvalid: boolean
  label: string
  className?: string
}

/** The field of Valores and %: what is typed goes to the ViewModel, which filters it and hands it back. */
function ValueField({ presenter, row, prefix, suffix, placeholder, isInvalid, label, className }: ValueFieldProps) {
  const id = row.person.id
  const [value, setValue] = useDraft(row.valueText, (next) => {
    presenter.changePersonValue(id, next)
    return presenter.getPersonValue(id)
  })
  return (
    <span
      className={cn(
        'flex h-8 shrink-0 items-center gap-1 rounded-md border bg-card px-2 focus-within:ring-[3px] dark:bg-input/30',
        isInvalid ? 'border-destructive focus-within:ring-destructive/20' : 'focus-within:border-ring focus-within:ring-ring/50',
        className,
      )}
    >
      {prefix && (
        <span aria-hidden className="text-xs text-muted-foreground">
          {prefix}
        </span>
      )}
      <input
        inputMode={suffix === '%' ? 'numeric' : 'decimal'}
        autoComplete="off"
        value={value}
        placeholder={placeholder}
        aria-label={label}
        aria-invalid={isInvalid}
        onChange={(event) => setValue(event.target.value)}
        className="w-full min-w-0 bg-transparent text-right font-mono text-[13px] tabular-nums outline-none placeholder:text-muted-foreground/60"
      />
      {suffix && (
        <span aria-hidden className="text-xs text-muted-foreground">
          {suffix}
        </span>
      )}
    </span>
  )
}
