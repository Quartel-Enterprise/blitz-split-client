import { ChevronsUpDown, Info, Loader2, Plus } from 'lucide-react'
import {
  SplitBillPresenter,
  type JsBillConflict,
  type JsPayerAmountRow,
  type JsPayerAmounts,
  type JsRoute,
  type JsSplitBillState,
  type JsSplitEditor,
} from 'blitzsplit-shared'
import { useEffect, useState, type ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { AmountInput, controlClass, WhenField } from '@/components/fields'
import { ResponsiveDialog, type PaneParts } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { BillDeleteDialog } from '@/features/bill-detail/BillDeleteDialog'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { formatTimeAgo } from '@/lib/dates'
import { amountPlaceholder, currencySymbol, formatMoney } from '@/lib/money'
import { payerNamesText } from '@/lib/payers'
import { cn } from '@/lib/utils'
import { splitPane } from './SplitPane'
import { personName, splitSummary, type Money } from './splitText'

const formId = 'split-bill-form'
const groupId = 'split-bill-group'
const amountId = 'split-bill-amount'
const amountHelpId = 'split-bill-amount-help'
const amountEditHelpId = 'split-bill-amount-edit-help'
const descriptionId = 'split-bill-description'
const descriptionHelpId = 'split-bill-description-help'
const payerId = 'split-bill-payer'
const payerHelpId = 'split-bill-payer-help'
const whenId = 'split-bill-when'

// Why: the Divisão row names the split's mode with how many are in it.
const splitTitles: Record<string, MessageKey> = {
  equal: 'splitBill_splitEqual',
  amount: 'splitBill_splitAmount',
  percent: 'splitBill_splitPercent',
  shares: 'splitBill_splitShares',
}

/**
 * "Rachar conta", over the group's page: a dialog on desktop and tablet, a drawer on the phone, like "Entrar com
 * link ou QR". "Dividir" takes the dialog's place while it is open, as the canvas draws it; "Quem pagou" and "Várias
 * pessoas pagaram" open over it on desktop and in its place on the phone, and so does "Rachar em qual grupo?", which the
 * home's "Rachar conta" opens first. Closing it any way is the ViewModel's:
 * Dividir's back, the payer sheets' dismissal or the bill's close. With a bill to edit it is "Editar conta", over
 * Conta, and can delete the bill.
 */
export function SplitBillDialog({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new SplitBillPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsSplitBillState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), (action) => {
    if (action === 'save-error') toast(t('splitBill_saveError'))
    else if (action === 'delete-error') toast(t('billDetail_deleteError'))
  })
  if (!presenter || !state?.isLoaded) return null

  const money: Money = (minor) => formatMoney(minor, state.currency, state.fractionDigits)
  const editor = state.splitEditor
  const isPayerOpen = state.isPayerOpen
  const isGroupOpen = state.groupSelection != null
  const payerAmounts = state.payerAmounts
  const onOpenChange = (open: boolean) => {
    if (open) return
    if (editor) presenter.dismissSplit()
    else if (isGroupOpen) presenter.dismissGroup()
    else if (isPayerOpen) presenter.dismissPayer()
    else if (payerAmounts) presenter.dismissSeveralPayers()
    else presenter.close()
  }

  let pane: PaneParts
  if (editor) pane = splitPane(presenter, state, editor, money)
  else if (isGroupOpen && !isDesktop) pane = groupPane(presenter, state)
  else if (isPayerOpen && !isDesktop) pane = payerPane(presenter, state, money)
  else if (payerAmounts && !isDesktop) pane = severalPayersPane(presenter, state, payerAmounts, money)
  else pane = billPane(presenter, state, money)
  const deletion = state.deletion && (
    <BillDeleteDialog
      deletion={state.deletion}
      isDeleting={state.isDeleting}
      money={money}
      onConfirm={() => presenter.confirmDelete()}
      onDismiss={() => presenter.dismissDelete()}
    />
  )

  return (
    <>
      <ResponsiveDialog
        isDesktop={isDesktop}
        pane={pane}
        onOpenChange={onOpenChange}
        className={cn('gap-5', editor ? 'sm:max-w-[520px]' : 'sm:max-w-[520px] lg:max-w-[760px]')}
      />
      {isDesktop && !editor && isGroupOpen && <GroupDialog presenter={presenter} state={state} />}
      {isDesktop && !editor && isPayerOpen && <PayerDialog presenter={presenter} state={state} money={money} />}
      {isDesktop && !editor && payerAmounts && <SeveralPayersDialog presenter={presenter} state={state} amounts={payerAmounts} money={money} />}
      {deletion}
    </>
  )
}

function billPane(presenter: SplitBillPresenter, state: JsSplitBillState, money: Money): PaneParts {
  return {
    title: t(state.isEditing ? 'splitBill_editTitle' : 'splitBill_title'),
    body: <BillForm presenter={presenter} state={state} money={money} />,
    // Why: keys of their own, so React never turns another pane's button into this submit button mid-click.
    footer: (
      <>
        {state.isEditing && (
          <Button
            key="bill-delete"
            variant="ghost"
            disabled={!state.canDelete}
            onClick={() => presenter.deleteBill()}
            className="text-destructive hover:text-destructive sm:mr-auto"
          >
            {t('splitBill_delete')}
          </Button>
        )}
        <Button key="bill-cancel" variant="outline" onClick={() => presenter.close()}>
          {t('common_cancel')}
        </Button>
        <Button key="bill-save" type="submit" form={formId} disabled={!state.canSave}>
          {state.isSaving && <Loader2 className="animate-spin" />}
          {t('splitBill_save')}
        </Button>
      </>
    ),
  }
}

function BillForm({ presenter, state, money }: { presenter: SplitBillPresenter; state: JsSplitBillState; money: Money }) {
  // Why: the ViewModel filters what is typed, so each field reads its text back in the same keystroke.
  const [amount, setAmount] = useDraft(state.amountText, (value) => {
    presenter.changeAmount(value)
    return presenter.getAmountText()
  })
  const [description, setDescription] = useDraft(state.description, (value) => {
    presenter.changeDescription(value)
    return presenter.getDescription()
  })
  const amountHelp = state.showsAmountError
    ? t('splitBill_amountError')
    : state.refundMinor != null
      ? t('splitBill_refund', { amount: money(state.refundMinor) })
      : state.myShareMinor != null
        ? t('splitBill_myShare', { amount: money(state.myShareMinor) })
        : null
  // Why: editing a by-amount split, a new total changes everyone's value, not only the total.
  const showsEditAmountHelp = state.showsProportionalAmountHint
  const amountDescribedBy = [amountHelp && amountHelpId, showsEditAmountHelp && amountEditHelpId].filter(Boolean).join(' ') || undefined

  return (
    <form
      id={formId}
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault()
        presenter.save()
      }}
    >
      {state.conflict && <ConflictBanner conflict={state.conflict} />}
      <div className="grid gap-x-6 gap-y-4 lg:grid-cols-2">
        <div className="flex min-w-0 flex-col gap-4">
          <GroupField presenter={presenter} state={state} />
          <div className="flex flex-col gap-1.5">
            <Label htmlFor={amountId} className={cn(state.showsAmountError && 'text-destructive')}>
              {t('splitBill_amount')}
            </Label>
            <AmountInput
              id={amountId}
              currency={state.currency}
              fractionDigits={state.fractionDigits}
              value={amount}
              onChange={setAmount}
              isInvalid={state.showsAmountError}
              describedBy={amountDescribedBy}
              autoFocus
            />
            {amountHelp && (
              <p id={amountHelpId} className={cn('text-[13px]', state.showsAmountError ? 'text-destructive' : 'text-muted-foreground')}>
                {amountHelp}
              </p>
            )}
            {showsEditAmountHelp && (
              <p id={amountEditHelpId} className="text-[13px] text-muted-foreground">
                {t('splitBill_editAmountHelp')}
              </p>
            )}
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor={descriptionId} className={cn(state.showsDescriptionError && 'text-destructive')}>
              {t('splitBill_description')}
            </Label>
            <Input
              id={descriptionId}
              autoComplete="off"
              value={description}
              placeholder={t('splitBill_descriptionPlaceholder')}
              aria-invalid={state.showsDescriptionError}
              aria-describedby={state.showsDescriptionError ? descriptionHelpId : undefined}
              onChange={(event) => setDescription(event.target.value)}
            />
            {state.showsDescriptionError && (
              <p id={descriptionHelpId} className="text-[13px] text-destructive">
                {t('splitBill_descriptionError')}
              </p>
            )}
          </div>
        </div>

        <div className="flex min-w-0 flex-col gap-4">
          <PayerField presenter={presenter} state={state} money={money} />
          <WhenField
            id={whenId}
            label={t('splitBill_when')}
            millis={state.spentAt}
            relativeDay={state.spentDay}
            latestMillis={state.latestSpentAt}
            onOpen={() => presenter.openSpentAt()}
            onChange={(millis) => presenter.changeSpentAt(millis)}
          />
          <div className="flex flex-col gap-1.5">
            <span className="text-sm font-medium">{t('splitBill_split')}</span>
            <SplitSummary presenter={presenter} split={state.split} money={money} />
          </div>
        </div>
      </div>

      {state.showsOfflineNotice && (
        <p className="flex gap-2 text-[13px] leading-[18px] text-muted-foreground">
          <span aria-hidden className="mt-1.5 size-2 shrink-0 rounded-full bg-orange-500" />
          {t('splitBill_offline')}
        </p>
      )}
      {state.showsAnyoneCanEditHint && <p className="text-[13px] leading-[18px] text-muted-foreground">{t('splitBill_anyoneCanEdit')}</p>}
    </form>
  )
}

/**
 * "Grupo": a new bill's opens "Rachar em qual grupo?" once there is another group to pick; a saved bill's, or the only
 * group's, is plain text.
 */
function GroupField({ presenter, state }: { presenter: SplitBillPresenter; state: JsSplitBillState }) {
  const content = (
    <>
      <GroupAvatar initials={state.groupInitials} color={state.groupColor} className="size-5 rounded-md text-[9px]" />
      <span className="flex-1 truncate text-left">{state.groupName}</span>
    </>
  )
  return (
    <div className="flex flex-col gap-1.5">
      {state.canChangeGroup ? (
        <>
          <Label htmlFor={groupId}>{t('splitBill_group')}</Label>
          <button id={groupId} type="button" aria-haspopup="dialog" onClick={() => presenter.openGroup()} className={controlClass}>
            {content}
            <ChevronsUpDown aria-hidden className="size-4 text-muted-foreground" />
          </button>
        </>
      ) : (
        <>
          <span className="text-sm font-medium">{t('splitBill_group')}</span>
          <div className="flex h-9 items-center gap-2 rounded-md border bg-muted/40 px-3 text-sm">{content}</div>
        </>
      )}
    </div>
  )
}

function groupPane(presenter: SplitBillPresenter, state: JsSplitBillState): PaneParts {
  return {
    title: t('splitBill_groupTitle'),
    description: t('splitBill_groupLead'),
    body: <GroupList presenter={presenter} state={state} />,
    footer: (
      <Button key="group-continue" onClick={() => presenter.confirmGroup()}>
        {t('splitBill_groupContinue')}
      </Button>
    ),
  }
}

/** "Rachar em qual grupo?" over the bill on desktop and tablet; its own dismissal leaves the bill in its group. */
function GroupDialog({ presenter, state }: { presenter: SplitBillPresenter; state: JsSplitBillState }) {
  return (
    <ResponsiveDialog
      isDesktop
      pane={groupPane(presenter, state)}
      onOpenChange={(open) => {
        if (!open) presenter.dismissGroup()
      }}
      className="sm:max-w-[420px]"
    />
  )
}

/** The groups, the most recently used first, each with how many people are in it. */
function GroupList({ presenter, state }: { presenter: SplitBillPresenter; state: JsSplitBillState }) {
  return (
    <div role="radiogroup" aria-label={t('splitBill_groupTitle')} className="flex flex-col divide-y overflow-hidden rounded-lg border">
      {state.groups.map((group) => (
        <PayerRow key={group.id} isSelected={group.id === state.groupSelection} onPick={() => presenter.pickGroup(group.id)}>
          <GroupAvatar initials={group.initials} color={group.color} className="size-8 text-xs" />
          <span className="flex min-w-0 flex-1 flex-col">
            <span className="truncate font-medium">{group.name}</span>
            <span className="text-xs text-muted-foreground">
              {t(group.memberCount === 1 ? 'splitBill_groupPeopleOne' : 'splitBill_groupPeopleOther', { count: group.memberCount })}
            </span>
          </span>
        </PayerRow>
      ))}
    </div>
  )
}

/** "Quem pagou": whoever paid, or several people with what is missing while their amounts don't add up. */
function PayerField({ presenter, state, money }: { presenter: SplitBillPresenter; state: JsSplitBillState; money: Money }) {
  const several = state.severalPayers
  const faces = several ? several.paying.flatMap((row) => (row.person ? [row.person] : [])).slice(0, 3) : state.payer ? [state.payer] : []
  // Why: an edited bill keeps whoever paid it after they left the group.
  const name = several
    ? payerNamesText(several.paying.map(payerRowName))
    : state.payer
      ? personName(state.payer)
      : state.isPayerFormerMember
        ? t('splitBill_formerMember')
        : ''
  const mismatch = several?.showsMismatch ? payersMismatch(several, money) : null
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={payerId} className={cn(mismatch && 'text-destructive')}>
        {t('splitBill_payer')}
      </Label>
      <button
        id={payerId}
        type="button"
        aria-haspopup="dialog"
        aria-invalid={mismatch != null}
        aria-describedby={mismatch ? payerHelpId : undefined}
        onClick={() => presenter.openPayer()}
        className={cn(controlClass, mismatch && 'border-destructive')}
      >
        {faces.length > 0 && (
          <span aria-hidden className="flex shrink-0">
            {faces.map((person, index) => (
              <Avatar key={person.id} initial={person.initials} color={person.color} className={cn('size-5 border-2 border-card text-[10px]', index > 0 && '-ml-1.5')} />
            ))}
          </span>
        )}
        <span className="flex-1 truncate">{name}</span>
        <ChevronsUpDown aria-hidden className="size-4 text-muted-foreground" />
      </button>
      {mismatch && (
        <p id={payerHelpId} className="text-[13px] text-destructive">
          {mismatch}
        </p>
      )}
    </div>
  )
}

/** "Você", a name, or "Ex-membro" for someone who left the group. */
function payerRowName(row: JsPayerAmountRow): string {
  return row.person ? personName(row.person) : t('splitBill_formerMember')
}

/** "Faltam R$ 112,00" or "Passou R$ 12,00": how far what several people paid is from the total. */
function payersMismatch(amounts: JsPayerAmounts, money: Money): string {
  const amount = money(Math.abs(amounts.remainingMinor))
  return t(amounts.remainingMinor > 0 ? 'split_amountLeft' : 'split_amountOver', { amount })
}

/**
 * Someone saved the bill while it was open here: "Ana mudou esta conta há 2 minutos", and that the last saved edit
 * wins. How long ago follows the clock, so it is redrawn every few seconds.
 */
function ConflictBanner({ conflict }: { conflict: JsBillConflict }) {
  const now = useNow(15_000)
  const when = formatTimeAgo(conflict.changedAt, now) ?? t('splitBill_conflictJustNow')
  const title = conflict.changerIsMe
    ? t('splitBill_conflictMe', { when })
    : conflict.changerName != null
      ? t('splitBill_conflictOther', { name: conflict.changerName, when })
      : t('splitBill_conflictSomeone', { when })
  return (
    <div role="status" className="flex gap-3 rounded-lg border border-primary/20 bg-primary/10 px-4 py-3 text-sm leading-5">
      <Info aria-hidden className="mt-0.5 size-4 shrink-0 text-primary" />
      <p className="flex flex-col gap-1">
        <strong className="font-semibold">{title}</strong>
        <span>{t('splitBill_conflictBody')}</span>
      </p>
    </div>
  )
}

/** The current time, updated every `intervalMs`. */
function useNow(intervalMs: number): number {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), intervalMs)
    return () => window.clearInterval(id)
  }, [intervalMs])
  return now
}

/** "Divisão": the faces of who is in, "Igual entre 5" and "R$ 62,40 cada · Diego não participa"; opens "Dividir". */
function SplitSummary({ presenter, split, money }: { presenter: SplitBillPresenter; split: JsSplitEditor; money: Money }) {
  const title = t(splitTitles[split.mode] ?? 'splitBill_splitEqual', { count: split.includedCount })
  const details = splitSummary(split, money)
  const faces = split.included.slice(0, 3)
  const extra = split.included.length - faces.length
  return (
    <button
      type="button"
      onClick={() => presenter.openSplit()}
      className="flex items-center gap-2.5 rounded-lg border px-3 py-2.5 text-left outline-none hover:bg-accent/60 focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
    >
      <span aria-hidden className="flex shrink-0">
        {faces.map((person, index) => (
          <Avatar key={person.id} initial={person.initials} color={person.color} className={cn('size-6 border-2 border-card text-[10px]', index > 0 && '-ml-2')} />
        ))}
        {extra > 0 && (
          <span className="-ml-2 flex h-6 min-w-6 items-center justify-center rounded-full border-2 border-card bg-muted px-1 text-[10px] font-semibold">+{extra}</span>
        )}
      </span>
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="truncate font-medium">{title}</span>
        {details && <span className="truncate text-[13px] text-muted-foreground">{details}</span>}
      </span>
      <span className="text-[13px] font-medium text-primary">{t('splitBill_change')}</span>
    </button>
  )
}

/** "R$ 312,00 · Mercado": the amount while it is valid and the description while it isn't blank. */
function payerLead(state: JsSplitBillState, money: Money): string | undefined {
  const parts: string[] = []
  if (state.isAmountValid && state.amountMinor != null) parts.push(money(state.amountMinor))
  if (state.description.trim()) parts.push(state.description.trim())
  return parts.length > 0 ? parts.join(' · ') : undefined
}

function payerPane(presenter: SplitBillPresenter, state: JsSplitBillState, money: Money): PaneParts {
  return {
    title: t('splitBill_payer'),
    description: payerLead(state, money),
    body: <PayerList presenter={presenter} state={state} />,
    footer: (
      <Button key="payer-done" onClick={() => presenter.confirmPayer()}>
        {t('splitBill_done')}
      </Button>
    ),
  }
}

/** "Quem pagou" over the bill on desktop and tablet; its own dismissal leaves the payer as it was. */
function PayerDialog({ presenter, state, money }: { presenter: SplitBillPresenter; state: JsSplitBillState; money: Money }) {
  const pane = payerPane(presenter, state, money)
  return (
    <ResponsiveDialog
      isDesktop
      pane={pane}
      onOpenChange={(open) => {
        if (!open) presenter.dismissPayer()
      }}
      className="sm:max-w-[420px]"
    />
  )
}

// Why: "Várias pessoas" is marked while the bill has several payers and nobody else was picked; it opens their amounts.
function PayerList({ presenter, state }: { presenter: SplitBillPresenter; state: JsSplitBillState }) {
  return (
    <div role="radiogroup" aria-label={t('splitBill_payer')} className="flex flex-col divide-y overflow-hidden rounded-lg border">
      {state.people.map((person) => (
        <PayerRow key={person.id} isSelected={person.id === state.payerSelection} onPick={() => presenter.pickPayer(person.id)}>
          <Avatar initial={person.initials} color={person.color} className="size-8 text-xs" />
          <span className="flex min-w-0 flex-1 flex-col">
            <span className="truncate font-medium">{personName(person)}</span>
            {!person.hasJoined && <span className="text-xs text-muted-foreground">{t('splitBill_notJoined')}</span>}
          </span>
        </PayerRow>
      ))}
      <PayerRow isSelected={state.payerSelection == null} onPick={() => presenter.openSeveralPayers()}>
        <span aria-hidden className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground">
          <Plus className="size-4" />
        </span>
        <span className="flex min-w-0 flex-1 flex-col">
          <span className="truncate font-medium">{t('splitBill_severalPayers')}</span>
          <span className="text-xs text-muted-foreground">{t('splitBill_severalPayersHint')}</span>
        </span>
      </PayerRow>
    </div>
  )
}

function PayerRow({ isSelected, onPick, children }: { isSelected: boolean; onPick: () => void; children: ReactNode }) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={isSelected}
      onClick={onPick}
      className="flex min-h-12 items-center gap-3 px-3 py-2 text-left outline-none hover:bg-accent/60 focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:ring-inset"
    >
      {children}
      <span
        aria-hidden
        className={cn('flex size-4 shrink-0 items-center justify-center rounded-full border', isSelected ? 'border-primary' : 'border-muted-foreground/50')}
      >
        {isSelected && <span className="size-2 rounded-full bg-primary" />}
      </span>
    </button>
  )
}

function severalPayersPane(presenter: SplitBillPresenter, state: JsSplitBillState, amounts: JsPayerAmounts, money: Money): PaneParts {
  return {
    title: t('splitBill_severalPayersTitle'),
    description:
      amounts.totalMinor > 0 ? t('splitBill_severalPayersLead', { total: money(amounts.totalMinor) }) : t('splitBill_severalPayersLeadNoTotal'),
    body: <SeveralPayersBody presenter={presenter} state={state} amounts={amounts} money={money} />,
    footer: (
      <>
        <Button key="payers-back" variant="outline" onClick={() => presenter.dismissSeveralPayers()}>
          {t('common_back')}
        </Button>
        <Button key="payers-done" disabled={!amounts.addsUp} onClick={() => presenter.confirmSeveralPayers()}>
          {t('splitBill_done')}
        </Button>
      </>
    ),
  }
}

/** "Várias pessoas pagaram" over the bill on desktop and tablet; its own dismissal leaves who paid as it was. */
function SeveralPayersDialog({ presenter, state, amounts, money }: { presenter: SplitBillPresenter; state: JsSplitBillState; amounts: JsPayerAmounts; money: Money }) {
  return (
    <ResponsiveDialog
      isDesktop
      pane={severalPayersPane(presenter, state, amounts, money)}
      onOpenChange={(open) => {
        if (!open) presenter.dismissSeveralPayers()
      }}
      className="sm:max-w-[420px]"
    />
  )
}

/** One field per person, and whether what they paid adds up to the total: "R$ 312,00 de R$ 312,00. Fechou." */
function SeveralPayersBody({ presenter, state, amounts, money }: { presenter: SplitBillPresenter; state: JsSplitBillState; amounts: JsPayerAmounts; money: Money }) {
  const status = amounts.addsUp
    ? t('splitBill_payersAddUp', { paid: money(amounts.paidMinor), total: money(amounts.totalMinor) })
    : amounts.showsMismatch
      ? payersMismatch(amounts, money)
      : null
  return (
    <div className="flex flex-col gap-3">
      <ul role="group" aria-label={t('splitBill_severalPayersTitle')} className="flex flex-col divide-y overflow-hidden rounded-lg border">
        {amounts.rows.map((row) => (
          <li key={row.memberId} className="flex min-h-12 items-center gap-3 px-3 py-1.5">
            {row.person ? (
              <Avatar initial={row.person.initials} color={row.person.color} className="size-8 text-xs" />
            ) : (
              <span aria-hidden className="size-8 shrink-0 rounded-full bg-muted" />
            )}
            <span className="min-w-0 flex-1 truncate font-medium">{payerRowName(row)}</span>
            <PayerAmountField presenter={presenter} row={row} currency={state.currency} fractionDigits={state.fractionDigits} />
          </li>
        ))}
      </ul>
      {status && (
        <p role="status" className={cn('text-sm font-semibold', amounts.addsUp ? 'text-primary' : 'text-destructive')}>
          {status}
        </p>
      )}
    </div>
  )
}

/** How much one person paid: what is typed goes to the ViewModel, which filters it and hands it back. */
function PayerAmountField({ presenter, row, currency, fractionDigits }: { presenter: SplitBillPresenter; row: JsPayerAmountRow; currency: string; fractionDigits: number }) {
  const [value, setValue] = useDraft(row.amountText, (next) => {
    presenter.changePayerAmount(row.memberId, next)
    return presenter.getPayerAmount(row.memberId)
  })
  // Why: "Quanto Pierre pagou" reads better to a screen reader than "Quanto Você pagou".
  const name = row.person?.name ?? t('splitBill_formerMember')
  return (
    <span className="flex h-8 w-28 shrink-0 items-center gap-1 rounded-md border bg-card px-2 focus-within:border-ring focus-within:ring-[3px] focus-within:ring-ring/50 dark:bg-input/30">
      <span aria-hidden className="text-xs text-muted-foreground">
        {currencySymbol(currency)}
      </span>
      <input
        inputMode="decimal"
        autoComplete="off"
        value={value}
        placeholder={amountPlaceholder(fractionDigits)}
        aria-label={t('splitBill_payerAmountLabel', { name })}
        onChange={(event) => setValue(event.target.value)}
        className="w-full min-w-0 bg-transparent text-right font-mono text-[13px] tabular-nums outline-none placeholder:text-muted-foreground/60"
      />
    </span>
  )
}
