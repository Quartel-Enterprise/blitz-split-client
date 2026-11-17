import { CalendarPlus } from 'lucide-react'
import { useId } from 'react'
import type { JsReminder } from 'blitzsplit-shared'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { t } from '@/i18n'
import { fromTimeInputValue, toTimeInputValue, weekdayName } from './reminder'

export interface ReminderActions {
  onEnabled: (isEnabled: boolean) => void
  onKind: (kind: string) => void
  onDay: (day: number) => void
  onTime: (minute: number) => void
}

/**
 * "Group reminder": the weekly or monthly schedule of the group's "settle up" push and its banner for debtors, read on
 * each member's own clock. New group puts "Add to my calendar" (a switch) under it, Edit group "Add to calendar" (a button).
 */
export function ReminderSection({
  reminder,
  actions,
  disabled = false,
  calendar,
}: {
  reminder: JsReminder
  actions: ReminderActions
  disabled?: boolean
  calendar?: { kind: 'switch'; isOn: boolean; onChange: (isOn: boolean) => void } | { kind: 'button'; onClick: () => void }
}) {
  const id = useId()
  const switchId = `${id}-enabled`
  const dayId = `${id}-day`
  const timeId = `${id}-time`
  const noteId = `${id}-note`
  const isMonthly = reminder.kind === 'monthly'
  const days = isMonthly ? Array.from({ length: 31 }, (_, index) => index + 1) : [1, 2, 3, 4, 5, 6, 7]
  return (
    <section aria-labelledby={`${id}-title`} className="flex flex-col gap-3 rounded-lg border px-3 py-3">
      <div className="flex items-start gap-3">
        <span className="flex min-w-0 flex-1 flex-col gap-0.5">
          <h3 id={`${id}-title`} className="text-sm font-medium">
            {t('reminder_title')}
          </h3>
          <Label htmlFor={switchId} className="text-[13px] font-normal text-muted-foreground">
            {t('reminder_switch')}
          </Label>
          <span id={noteId} className="text-[13px] text-muted-foreground">
            {t('reminder_note')}
          </span>
        </span>
        <Switch id={switchId} checked={reminder.enabled} disabled={disabled} aria-describedby={noteId} onCheckedChange={actions.onEnabled} className="mt-0.5" />
      </div>
      {reminder.enabled && (
        <>
          <ToggleGroup
            type="single"
            variant="outline"
            value={reminder.kind}
            disabled={disabled}
            aria-label={t('reminder_kindLabel')}
            // Why: a click on the one picked would clear a single toggle group; the kind always has one.
            onValueChange={(kind) => kind && actions.onKind(kind)}
            className="w-full"
          >
            <ToggleGroupItem value="weekly" className="flex-1">
              {t('reminder_weekly')}
            </ToggleGroupItem>
            <ToggleGroupItem value="monthly" className="flex-1">
              {t('reminder_monthly')}
            </ToggleGroupItem>
          </ToggleGroup>
          <div className="flex flex-wrap items-end gap-3">
            <div className="flex min-w-[9rem] flex-1 flex-col gap-1.5">
              <Label htmlFor={dayId}>{t(isMonthly ? 'reminder_dayOfMonth' : 'reminder_day')}</Label>
              <Select value={String(reminder.day)} disabled={disabled} onValueChange={(value) => actions.onDay(Number(value))}>
                <SelectTrigger id={dayId} className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {days.map((day) => (
                    <SelectItem key={day} value={String(day)}>
                      {isMonthly ? day : weekdayName(day)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex min-w-[8rem] flex-col gap-1.5">
              <Label htmlFor={timeId}>{t('reminder_time')}</Label>
              <Input
                id={timeId}
                type="time"
                value={toTimeInputValue(reminder.minute)}
                disabled={disabled}
                onChange={(event) => {
                  const minute = fromTimeInputValue(event.target.value)
                  if (minute != null) actions.onTime(minute)
                }}
              />
            </div>
          </div>
          {calendar?.kind === 'switch' && (
            <div className="flex items-center justify-between gap-3">
              <Label htmlFor={`${id}-calendar`} className="font-normal">
                {t('reminder_addToMyCalendar')}
              </Label>
              <Switch id={`${id}-calendar`} checked={calendar.isOn} disabled={disabled} onCheckedChange={calendar.onChange} />
            </div>
          )}
          {calendar?.kind === 'button' && (
            <Button type="button" variant="outline" size="sm" className="self-start" disabled={disabled} onClick={calendar.onClick}>
              <CalendarPlus />
              {t('reminder_addToCalendar')}
            </Button>
          )}
        </>
      )}
    </section>
  )
}
