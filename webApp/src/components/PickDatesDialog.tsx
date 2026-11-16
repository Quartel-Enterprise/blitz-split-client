import { useState } from 'react'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { t } from '@/i18n'
import { fromDateTimeInputs, toDateInputValue } from '@/lib/dates'

/**
 * "Pick dates" of Statistics and of All expenses' Period: the first and the last day, never after today; a dialog, a
 * drawer on the phone. The description says what the days narrow.
 */
export function PickDatesDialog({
  isDesktop,
  description,
  initialStart,
  initialEnd,
  onPick,
  onClose,
}: {
  isDesktop: boolean
  description: string
  initialStart: number
  initialEnd: number
  onPick: (startMillis: number, endMillis: number) => void
  onClose: () => void
}) {
  const [start, setStart] = useState(toDateInputValue(initialStart))
  const [end, setEnd] = useState(toDateInputValue(initialEnd))
  const today = toDateInputValue(Date.now())
  // Why: noon of the day typed, so the day stays the same in the browser's time zone.
  const startMillis = fromDateTimeInputs(start, '12:00')
  const endMillis = fromDateTimeInputs(end, '12:00')
  const canApply = startMillis != null && endMillis != null
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
      className="sm:max-w-[400px]"
      pane={{
        title: t('stats_pickDatesTitle'),
        description,
        body: (
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-2">
              <Label htmlFor="statsFrom">{t('stats_from')}</Label>
              <Input id="statsFrom" type="date" max={today} value={start} onChange={(event) => setStart(event.target.value)} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="statsTo">{t('stats_to')}</Label>
              <Input id="statsTo" type="date" max={today} value={end} onChange={(event) => setEnd(event.target.value)} />
            </div>
          </div>
        ),
        footer: (
          <>
            <Button variant="outline" onClick={onClose}>
              {t('common_cancel')}
            </Button>
            <Button disabled={!canApply} onClick={() => canApply && onPick(startMillis, endMillis)}>
              {t('stats_apply')}
            </Button>
          </>
        ),
      }}
    />
  )
}
