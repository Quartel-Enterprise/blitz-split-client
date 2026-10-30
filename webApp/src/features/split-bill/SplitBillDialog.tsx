import { CalendarDays, ChevronsUpDown, Loader2 } from 'lucide-react'
import { SplitBillPresenter, type JsBillPerson, type JsRoute, type JsSplitBillState, type JsSplitEditor } from 'blitzsplit-shared'
import { Popover } from 'radix-ui'
import { useState, type ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { formatWhen, fromDateTimeInputs, toDateInputValue, toTimeInputValue } from '@/lib/dates'
import { amountPlaceholder, currencySymbol, formatMoney } from '@/lib/money'
import { cn } from '@/lib/utils'
import { splitPane } from './SplitPane'
import { personName, splitSummary, type Money } from './splitText'

const formId = 'split-bill-form'
const amountId = 'split-bill-amount'
const amountHelpId = 'split-bill-amount-help'
const descriptionId = 'split-bill-description'
const descriptionHelpId = 'split-bill-description-help'
const payerId = 'split-bill-payer'
const whenId = 'split-bill-when'

// Why: the Divisão row names the split's mode with how many are in it.
const splitTitles: Record<string, MessageKey> = {
  equal: 'splitBill_splitEqual',
  amount: 'splitBill_splitAmount',
  percent: 'splitBill_splitPercent',
  shares: 'splitBill_splitShares',
}

const controlClass =
  'flex h-9 w-full items-center gap-2 rounded-md border bg-card px-3 text-left text-sm outline-none hover:bg-accent/60 focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 dark:bg-input/30'

/**
 * "Rachar conta", over the group's page: a dialog on desktop and tablet, a drawer on the phone, like "Entrar com
 * link ou QR". "Dividir" takes the dialog's place while it is open, as the canvas draws it; "Quem pagou" opens over
 * it on desktop and in its place on the phone. Closing it any way is the ViewModel's: Dividir's back, Quem pagou's
 * dismissal or the bill's close.
 */
export function SplitBillDialog({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new SplitBillPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsSplitBillState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), (action) => {
    if (action === 'save-error') toast(t('splitBill_saveError'))
  })
  if (!presenter || !state?.isLoaded) return null

  const money: Money = (minor) => formatMoney(minor, state.currency, state.fractionDigits)
  const editor = state.splitEditor
  const isPayerOpen = state.payerSelection != null
  const onOpenChange = (open: boolean) => {
    if (open) return
    if (editor) presenter.dismissSplit()
    else if (isPayerOpen) presenter.dismissPayer()
    else presenter.close()
  }

  let pane: PaneParts
  if (editor) pane = splitPane(presenter, state, editor, money)
  else if (isPayerOpen && !isDesktop) pane = payerPane(presenter, state, money)
  else pane = billPane(presenter, state, money)

  return isDesktop ? (
    <>
      <Dialog open onOpenChange={onOpenChange}>
        <DialogContent className={cn('max-h-[calc(100dvh-2rem)] gap-5 overflow-y-auto', editor ? 'sm:max-w-[520px]' : 'sm:max-w-[520px] lg:max-w-[760px]')}>
          <DialogHeader className="pr-8">
            <DialogTitle>{pane.title}</DialogTitle>
            <DialogDescription className={cn(!pane.description && 'sr-only')}>{pane.description ?? pane.title}</DialogDescription>
          </DialogHeader>
          {pane.body}
          <DialogFooter>{pane.footer}</DialogFooter>
        </DialogContent>
      </Dialog>
      {!editor && isPayerOpen && <PayerDialog presenter={presenter} state={state} money={money} />}
    </>
  ) : (
    <Drawer open onOpenChange={onOpenChange} repositionInputs={false}>
      <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
        <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          <DrawerTitle>{pane.title}</DrawerTitle>
          <DrawerDescription className={cn(!pane.description && 'sr-only')}>{pane.description ?? pane.title}</DrawerDescription>
        </DrawerHeader>
        <div className="overflow-y-auto px-4">{pane.body}</div>
        {/* Why: the phone puts the main action first, under the thumb. */}
        <DrawerFooter className="flex-col-reverse">{pane.footer}</DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

/** What the dialog or the drawer draws: its title, an optional lead, the body and the buttons. */
export interface PaneParts {
  title: string
  description?: string
  body: ReactNode
  footer: ReactNode
}

function billPane(presenter: SplitBillPresenter, state: JsSplitBillState, money: Money): PaneParts {
  return {
    title: t('splitBill_title'),
    body: <BillForm presenter={presenter} state={state} money={money} />,
    // Why: keys of their own, so React never turns another pane's button into this submit button mid-click.
    footer: (
      <>
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

  return (
    <form
      id={formId}
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault()
        presenter.save()
      }}
    >
      <div className="grid gap-x-6 gap-y-4 lg:grid-cols-2">
        <div className="flex min-w-0 flex-col gap-4">
          {/* Not a control: the bill stays in this group (no switching groups yet). */}
          <div className="flex flex-col gap-1.5">
            <span className="text-sm font-medium">{t('splitBill_group')}</span>
            <div className="flex h-9 items-center gap-2 rounded-md border bg-muted/40 px-3 text-sm">
              <GroupAvatar initials={state.groupInitials} color={state.groupColor} className="size-5 rounded-md text-[9px]" />
              <span className="truncate">{state.groupName}</span>
            </div>
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor={amountId} className={cn(state.showsAmountError && 'text-destructive')}>
              {t('splitBill_amount')}
            </Label>
            <div
              className={cn(
                'flex h-14 items-center gap-2 rounded-md border bg-card px-3 focus-within:ring-[3px] dark:bg-input/30',
                state.showsAmountError ? 'border-destructive focus-within:ring-destructive/20' : 'focus-within:border-ring focus-within:ring-ring/50',
              )}
            >
              <span aria-hidden className="text-xl font-medium text-muted-foreground">
                {currencySymbol(state.currency)}
              </span>
              <input
                id={amountId}
                inputMode="decimal"
                autoComplete="off"
                autoFocus
                value={amount}
                placeholder={amountPlaceholder(state.fractionDigits)}
                aria-invalid={state.showsAmountError}
                aria-describedby={amountHelp ? amountHelpId : undefined}
                onChange={(event) => setAmount(event.target.value)}
                className="min-w-0 flex-1 bg-transparent text-[28px] font-semibold tabular-nums outline-none placeholder:text-muted-foreground/60"
              />
            </div>
            {amountHelp && (
              <p id={amountHelpId} className={cn('text-[13px]', state.showsAmountError ? 'text-destructive' : 'text-muted-foreground')}>
                {amountHelp}
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
          <div className="flex flex-col gap-1.5">
            <Label htmlFor={payerId}>{t('splitBill_payer')}</Label>
            <button id={payerId} type="button" aria-haspopup="dialog" onClick={() => presenter.openPayer()} className={controlClass}>
              {state.payer && <Avatar initial={state.payer.initials} color={state.payer.color} className="size-5 text-[10px]" />}
              <span className="flex-1 truncate">{state.payer ? personName(state.payer) : ''}</span>
              <ChevronsUpDown aria-hidden className="size-4 text-muted-foreground" />
            </button>
          </div>
          <WhenField presenter={presenter} state={state} />
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
    </form>
  )
}

/**
 * "Quando", date and time: the button says it in words ("Hoje, 28 de out. · 21:30") and opens the browser's own
 * date and time inputs. Each complete change goes to the ViewModel as epoch millis.
 */
function WhenField({ presenter, state }: { presenter: SplitBillPresenter; state: JsSplitBillState }) {
  const [isOpen, setOpen] = useState(false)
  const [date, setDate] = useState(() => toDateInputValue(state.spentAt))
  const [time, setTime] = useState(() => toTimeInputValue(state.spentAt))
  const change = (nextDate: string, nextTime: string) => {
    setDate(nextDate)
    setTime(nextTime)
    const millis = fromDateTimeInputs(nextDate, nextTime)
    if (millis != null) presenter.changeSpentAt(millis)
  }
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={whenId}>{t('splitBill_when')}</Label>
      <Popover.Root
        open={isOpen}
        onOpenChange={(open) => {
          // Why: the inputs open on what the bill says now, not on a half-typed date left from before.
          if (open) {
            presenter.openSpentAt()
            setDate(toDateInputValue(state.spentAt))
            setTime(toTimeInputValue(state.spentAt))
          }
          setOpen(open)
        }}
      >
        <Popover.Trigger asChild>
          <button id={whenId} type="button" className={controlClass}>
            <CalendarDays aria-hidden className="size-4 text-muted-foreground" />
            <span className="flex-1 truncate">{formatWhen(state.spentAt, state.spentDay)}</span>
          </button>
        </Popover.Trigger>
        <Popover.Portal>
          <Popover.Content
            align="start"
            sideOffset={6}
            className="z-50 flex w-72 flex-col gap-3 rounded-lg border bg-popover p-4 text-popover-foreground shadow-md outline-none data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:animate-in data-[state=open]:fade-in-0"
          >
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`${whenId}-date`}>{t('splitBill_date')}</Label>
              <Input id={`${whenId}-date`} type="date" value={date} max={toDateInputValue(state.latestSpentAt)} required onChange={(event) => change(event.target.value, time)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`${whenId}-time`}>{t('splitBill_time')}</Label>
              <Input id={`${whenId}-time`} type="time" value={time} required onChange={(event) => change(date, event.target.value)} />
            </div>
            <Button type="button" size="sm" className="self-end" onClick={() => setOpen(false)}>
              {t('splitBill_done')}
            </Button>
          </Popover.Content>
        </Popover.Portal>
      </Popover.Root>
    </div>
  )
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
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open) presenter.dismissPayer()
      }}
    >
      <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-[420px]">
        <DialogHeader className="pr-8">
          <DialogTitle>{pane.title}</DialogTitle>
          <DialogDescription className={cn(!pane.description && 'sr-only')}>{pane.description ?? pane.title}</DialogDescription>
        </DialogHeader>
        {pane.body}
        <DialogFooter>{pane.footer}</DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function PayerList({ presenter, state }: { presenter: SplitBillPresenter; state: JsSplitBillState }) {
  return (
    <div role="radiogroup" aria-label={t('splitBill_payer')} className="flex flex-col divide-y overflow-hidden rounded-lg border">
      {state.people.map((person) => (
        <PayerRow key={person.id} person={person} isSelected={person.id === state.payerSelection} onPick={() => presenter.pickPayer(person.id)} />
      ))}
    </div>
  )
}

function PayerRow({ person, isSelected, onPick }: { person: JsBillPerson; isSelected: boolean; onPick: () => void }) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={isSelected}
      onClick={onPick}
      className="flex min-h-12 items-center gap-3 px-3 py-2 text-left outline-none hover:bg-accent/60 focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:ring-inset"
    >
      <Avatar initial={person.initials} color={person.color} className="size-8 text-xs" />
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="truncate font-medium">{personName(person)}</span>
        {!person.hasJoined && <span className="text-xs text-muted-foreground">{t('splitBill_notJoined')}</span>}
      </span>
      <span
        aria-hidden
        className={cn('flex size-4 shrink-0 items-center justify-center rounded-full border', isSelected ? 'border-primary' : 'border-muted-foreground/50')}
      >
        {isSelected && <span className="size-2 rounded-full bg-primary" />}
      </span>
    </button>
  )
}
