import { CalendarDays, Clock } from 'lucide-react'
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
  'flex h-9 w-full items-center gap-2 rounded-md border border-field-border bg-card px-3 text-left text-sm outline-none hover:bg-accent/60 focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-50 dark:bg-input/30'

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
}: {
  id: string
  currency: string
  fractionDigits: number
  value: string
  onChange: (value: string) => void
  isInvalid: boolean
  describedBy?: string
  autoFocus?: boolean
}) {
  return (
    <div
      className={cn(
        'flex h-14 items-center gap-2 rounded-md border border-field-border bg-card px-3 focus-within:ring-[3px] has-[:disabled]:opacity-50 dark:bg-input/30',
        isInvalid ? 'border-destructive focus-within:ring-destructive/20' : 'focus-within:border-ring focus-within:ring-ring/50',
      )}
    >
      <span aria-hidden className="text-xl font-medium text-muted-foreground">
        {currencySymbol(currency)}
      </span>
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
      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`${id}-date`}>{t('splitBill_date')}</Label>
        <Input id={`${id}-date`} type="date" value={date} max={toDateInputValue(latestMillis)} required onChange={(event) => change(event.target.value, time)} />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`${id}-time`}>{t('splitBill_time')}</Label>
        <Input id={`${id}-time`} type="time" value={time} required onChange={(event) => change(date, event.target.value)} />
      </div>
    </WhenPopover>
  )
}

/** What a bill's "Quando" tells the ViewModel: a day picked, the time opened, picked or removed. */
export interface BillWhenActions {
  onOpen: () => void
  // Why: the date input's day, its month from 1 for January.
  onDayPick: (year: number, month: number, day: number) => void
  onTimeOpen: () => void
  onTimePick: (hour: number, minute: number) => void
  onTimeClear: () => void
}

/**
 * A bill's "Quando" (beta feedback): its date alone ("Hoje, 28 de out."), with its time once it has one. The popover
 * picks the day, and "Adicionar horário" adds the time it is now, which the time input then changes; "Remover
 * horário" takes it off. A bill without a time is at its day's first instant.
 */
export function BillWhenField({
  id,
  label,
  millis,
  relativeDay,
  hasTime,
  latestMillis,
  actions,
  disabled = false,
}: {
  id: string
  label: string
  millis: number
  relativeDay: string
  hasTime: boolean
  latestMillis: number
  actions: BillWhenActions
  disabled?: boolean
}) {
  const pickDay = (value: string) => {
    const day = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
    if (day) actions.onDayPick(Number(day[1]), Number(day[2]), Number(day[3]))
  }
  const pickTime = (value: string) => {
    const clock = /^(\d{2}):(\d{2})/.exec(value)
    if (clock) actions.onTimePick(Number(clock[1]), Number(clock[2]))
  }
  const addTime = () => {
    actions.onTimeOpen()
    const now = new Date()
    actions.onTimePick(now.getHours(), now.getMinutes())
  }
  return (
    <WhenPopover id={id} label={label} text={formatWhen(millis, relativeDay, hasTime)} disabled={disabled} onOpen={actions.onOpen}>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`${id}-date`}>{t('splitBill_date')}</Label>
        <Input id={`${id}-date`} type="date" value={toDateInputValue(millis)} max={toDateInputValue(latestMillis)} required onChange={(event) => pickDay(event.target.value)} />
      </div>
      {hasTime ? (
        <div className="flex flex-col gap-1.5">
          <Label htmlFor={`${id}-time`}>{t('splitBill_time')}</Label>
          <Input id={`${id}-time`} type="time" value={toTimeInputValue(millis)} required onChange={(event) => pickTime(event.target.value)} />
          <Button type="button" variant="link" size="sm" className="self-start px-0" onClick={actions.onTimeClear}>
            {t('splitBill_removeTime')}
          </Button>
        </div>
      ) : (
        <Button type="button" variant="outline" size="sm" className="self-start" onClick={addTime}>
          <Clock aria-hidden />
          {t('splitBill_addTime')}
        </Button>
      )}
    </WhenPopover>
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
