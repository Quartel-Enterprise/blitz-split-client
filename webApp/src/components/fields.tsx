import { CalendarDays } from 'lucide-react'
import { Popover } from 'radix-ui'
import { type ReactNode, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { t } from '@/i18n'
import { formatWhen, fromDateTimeInputs, toDateInputValue, toTimeInputValue } from '@/lib/dates'
import { amountPlaceholder, currencySymbol } from '@/lib/money'
import { cn } from '@/lib/utils'

/** A button that looks like a field: "Quem pagou", "Quando". */
export const controlClass =
  'flex h-9 w-full items-center gap-2 rounded-md border border-field-border bg-card px-3 text-left text-sm outline-none hover:bg-accent/60 focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 dark:bg-input/30'

/**
 * A large amount field with the currency's symbol before it ("R$ 312,00"), the number keyboard on phones. The
 * ViewModel filters what is typed; `value` is what it kept.
 */
export function AmountInput({
  id,
  currency,
  fractionDigits,
  value,
  onChange,
  isInvalid,
  describedBy,
  autoFocus = false,
  symbolAction,
}: {
  id: string
  currency: string
  fractionDigits: number
  value: string
  onChange: (value: string) => void
  isInvalid: boolean
  describedBy?: string
  autoFocus?: boolean
  // Why: Rachar conta's symbol opens the currency menu beside the field (beta feedback); without it, plain text.
  symbolAction?: { label: string; disabled: boolean; onClick: () => void }
}) {
  return (
    <div
      className={cn(
        'flex h-14 items-center gap-2 rounded-md border border-field-border bg-card px-3 focus-within:ring-[3px] has-[input:disabled]:opacity-50 dark:bg-input/30',
        isInvalid ? 'border-destructive focus-within:ring-destructive/20' : 'focus-within:border-ring focus-within:ring-ring',
      )}
    >
      {symbolAction ? (
        <button
          type="button"
          aria-label={symbolAction.label}
          disabled={symbolAction.disabled}
          onClick={symbolAction.onClick}
          className="-mx-1 rounded-sm px-1 text-xl font-medium text-muted-foreground outline-none hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring disabled:pointer-events-none"
        >
          {currencySymbol(currency)}
        </button>
      ) : (
        <span aria-hidden className="text-xl font-medium text-muted-foreground">
          {currencySymbol(currency)}
        </span>
      )}
      <input
        id={id}
        inputMode="numeric"
        autoComplete="off"
        autoFocus={autoFocus}
        value={value}
        placeholder={amountPlaceholder(fractionDigits, currency)}
        aria-invalid={isInvalid}
        aria-describedby={describedBy}
        onChange={(event) => onChange(event.target.value)}
        className="min-w-0 flex-1 bg-transparent text-[28px] font-semibold tabular-nums outline-none placeholder:text-muted-foreground"
      />
    </div>
  )
}

/**
 * "Quando", date and time: the button says it in words ("Hoje, 28 de out. · 21:30") and opens the browser's own
 * date and time inputs, the date capped at `latestMillis`. Opening it is `onOpen`, and each complete change goes to
 * `onChange` as epoch millis.
 */
export function WhenField({
  id,
  label,
  millis,
  relativeDay,
  latestMillis,
  onOpen,
  onChange,
  disabled = false,
}: {
  id: string
  label: string
  millis: number
  relativeDay: string
  latestMillis: number
  onOpen: () => void
  onChange: (millis: number) => void
  // Why: a form locked while the popover is open (a receipt being read) closes it, since it portals out of the fieldset.
  disabled?: boolean
}) {
  const [date, setDate] = useState(() => toDateInputValue(millis))
  const [time, setTime] = useState(() => toTimeInputValue(millis))
  const change = (nextDate: string, nextTime: string) => {
    setDate(nextDate)
    setTime(nextTime)
    const next = fromDateTimeInputs(nextDate, nextTime)
    if (next != null) onChange(next)
  }
  return (
    <WhenPopover
      id={id}
      label={label}
      text={formatWhen(millis, relativeDay)}
      disabled={disabled}
      onOpen={() => {
        // Why: the inputs open on what the field says now, not on a half-typed date left from before.
        onOpen()
        setDate(toDateInputValue(millis))
        setTime(toTimeInputValue(millis))
      }}
    >
      <WhenInputs id={id} date={date} time={time} latestMillis={latestMillis} onDate={(value) => change(value, time)} onTime={(value) => change(date, value)} />
    </WhenPopover>
  )
}

/** What a bill's "Quando" tells the ViewModel: a day or a time picked. */
export interface BillWhenActions {
  onOpen: () => void
  // Why: the date input's day, its month from 1 for January.
  onDayPick: (year: number, month: number, day: number) => void
  onTimePick: (hour: number, minute: number) => void
}

/**
 * A bill's "Quando" (beta feedback): its date and time, a new bill's now. The popover picks the day, which keeps the
 * bill's time, and the time input changes the time. The inputs keep what is being typed and send only a complete
 * value, so a year typed digit by digit never dates the bill in year 2.
 */
export function BillWhenField({
  id,
  label,
  millis,
  relativeDay,
  latestMillis,
  actions,
  disabled = false,
}: {
  id: string
  label: string
  millis: number
  relativeDay: string
  latestMillis: number
  actions: BillWhenActions
  disabled?: boolean
}) {
  const [date, setDate] = useState(() => toDateInputValue(millis))
  const [time, setTime] = useState(() => toTimeInputValue(millis))
  // Why: what the ViewModel made of a complete value (now for a later time) shows.
  const [shownMillis, setShownMillis] = useState(millis)
  if (millis !== shownMillis) {
    setShownMillis(millis)
    setDate(toDateInputValue(millis))
    setTime(toTimeInputValue(millis))
  }
  const pickDay = (value: string) => {
    setDate(value)
    const day = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
    if (day && Number(day[1]) >= MIN_TYPED_YEAR) actions.onDayPick(Number(day[1]), Number(day[2]), Number(day[3]))
  }
  const pickTime = (value: string) => {
    setTime(value)
    const clock = /^(\d{2}):(\d{2})/.exec(value)
    if (clock) actions.onTimePick(Number(clock[1]), Number(clock[2]))
  }
  return (
    <WhenPopover
      id={id}
      label={label}
      text={formatWhen(millis, relativeDay)}
      disabled={disabled}
      onOpen={() => {
        // Why: the inputs open on what the field says now, not on a half-typed value left from before.
        actions.onOpen()
        setDate(toDateInputValue(millis))
        setTime(toTimeInputValue(millis))
      }}
    >
      <WhenInputs id={id} date={date} time={time} latestMillis={latestMillis} onDate={pickDay} onTime={pickTime} />
    </WhenPopover>
  )
}

// Why: a date input fires each digit of the year as it is typed ("0002"); only a four-digit year of this era is a day.
const MIN_TYPED_YEAR = 1900

// Why: the date and the time inputs "Quando"'s popovers share; the date never goes past the latest instant.
function WhenInputs({
  id,
  date,
  time,
  latestMillis,
  onDate,
  onTime,
}: {
  id: string
  date: string
  time: string
  latestMillis: number
  onDate: (value: string) => void
  onTime: (value: string) => void
}) {
  return (
    <>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`${id}-date`}>{t('splitBill_date')}</Label>
        <Input id={`${id}-date`} type="date" value={date} max={toDateInputValue(latestMillis)} required onChange={(event) => onDate(event.target.value)} />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`${id}-time`}>{t('splitBill_time')}</Label>
        <Input id={`${id}-time`} type="time" value={time} required onChange={(event) => onTime(event.target.value)} />
      </div>
    </>
  )
}

/** The button of "Quando", in words, and the popover with its inputs and "Concluído". */
function WhenPopover({
  id,
  label,
  text,
  disabled,
  onOpen,
  children,
}: {
  id: string
  label: string
  text: string
  disabled: boolean
  onOpen: () => void
  children: ReactNode
}) {
  const [isOpen, setOpen] = useState(false)
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={id}>{label}</Label>
      <Popover.Root
        open={isOpen && !disabled}
        onOpenChange={(open) => {
          if (open) onOpen()
          setOpen(open)
        }}
      >
        <Popover.Trigger asChild>
          <button id={id} type="button" disabled={disabled} className={controlClass}>
            <CalendarDays aria-hidden className="size-4 text-muted-foreground" />
            <span className="flex-1 truncate">{text}</span>
          </button>
        </Popover.Trigger>
        <Popover.Portal>
          <Popover.Content
            align="start"
            sideOffset={6}
            className="z-50 flex w-72 flex-col gap-3 rounded-lg border bg-popover p-4 text-popover-foreground shadow-md outline-none data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:animate-in data-[state=open]:fade-in-0"
          >
            {children}
            <Button type="button" size="sm" className="self-end" onClick={() => setOpen(false)}>
              {t('splitBill_done')}
            </Button>
          </Popover.Content>
        </Popover.Portal>
      </Popover.Root>
    </div>
  )
}
