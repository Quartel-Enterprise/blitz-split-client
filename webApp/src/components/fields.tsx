import { CalendarDays } from 'lucide-react'
import { Popover } from 'radix-ui'
import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { t } from '@/i18n'
import { formatWhen, fromDateTimeInputs, toDateInputValue, toTimeInputValue } from '@/lib/dates'
import { amountPlaceholder, currencySymbol } from '@/lib/money'
import { cn } from '@/lib/utils'

/** A button that looks like a field: "Quem pagou", "Quando". */
export const controlClass =
  'flex h-9 w-full items-center gap-2 rounded-md border bg-card px-3 text-left text-sm outline-none hover:bg-accent/60 focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 dark:bg-input/30'

/**
 * A large amount field with the currency's symbol before it ("R$ 312,00"), the decimal keyboard on phones. The
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
        'flex h-14 items-center gap-2 rounded-md border bg-card px-3 focus-within:ring-[3px] dark:bg-input/30',
        isInvalid ? 'border-destructive focus-within:ring-destructive/20' : 'focus-within:border-ring focus-within:ring-ring/50',
      )}
    >
      <span aria-hidden className="text-xl font-medium text-muted-foreground">
        {currencySymbol(currency)}
      </span>
      <input
        id={id}
        inputMode="decimal"
        autoComplete="off"
        autoFocus={autoFocus}
        value={value}
        placeholder={amountPlaceholder(fractionDigits)}
        aria-invalid={isInvalid}
        aria-describedby={describedBy}
        onChange={(event) => onChange(event.target.value)}
        className="min-w-0 flex-1 bg-transparent text-[28px] font-semibold tabular-nums outline-none placeholder:text-muted-foreground/60"
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
}: {
  id: string
  label: string
  millis: number
  relativeDay: string
  latestMillis: number
  onOpen: () => void
  onChange: (millis: number) => void
}) {
  const [isOpen, setOpen] = useState(false)
  const [date, setDate] = useState(() => toDateInputValue(millis))
  const [time, setTime] = useState(() => toTimeInputValue(millis))
  const change = (nextDate: string, nextTime: string) => {
    setDate(nextDate)
    setTime(nextTime)
    const next = fromDateTimeInputs(nextDate, nextTime)
    if (next != null) onChange(next)
  }
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={id}>{label}</Label>
      <Popover.Root
        open={isOpen}
        onOpenChange={(open) => {
          // Why: the inputs open on what the field says now, not on a half-typed date left from before.
          if (open) {
            onOpen()
            setDate(toDateInputValue(millis))
            setTime(toTimeInputValue(millis))
          }
          setOpen(open)
        }}
      >
        <Popover.Trigger asChild>
          <button id={id} type="button" className={controlClass}>
            <CalendarDays aria-hidden className="size-4 text-muted-foreground" />
            <span className="flex-1 truncate">{formatWhen(millis, relativeDay)}</span>
          </button>
        </Popover.Trigger>
        <Popover.Portal>
          <Popover.Content
            align="start"
            sideOffset={6}
            className="z-50 flex w-72 flex-col gap-3 rounded-lg border bg-popover p-4 text-popover-foreground shadow-md outline-none data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:animate-in data-[state=open]:fade-in-0"
          >
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`${id}-date`}>{t('splitBill_date')}</Label>
              <Input id={`${id}-date`} type="date" value={date} max={toDateInputValue(latestMillis)} required onChange={(event) => change(event.target.value, time)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`${id}-time`}>{t('splitBill_time')}</Label>
              <Input id={`${id}-time`} type="time" value={time} required onChange={(event) => change(date, event.target.value)} />
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
