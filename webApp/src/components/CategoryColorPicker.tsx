import { Check, Plus } from 'lucide-react'
import type { JsColorDraft, JsCustomColorActions, JsCustomColors } from 'blitzsplit-shared'
import { useRef, type PointerEvent } from 'react'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useDraft } from '@/hooks/useDraft'
import { t } from '@/i18n'
import { categoryColorCodes, categoryColorLabel, categoryStyle } from '@/lib/avatar'
import { categoryName } from '@/lib/categories'
import { cn } from '@/lib/utils'

const LONG_PRESS_MS = 550

/**
 * The colors a category may take (7.7): the twelve named ones, then the person's own (a long press or a right click asks
 * to delete one), then "+", which opens "New color" (Pierre's beta feedback of 2026-11-14). categoryKey is the app
 * category "Change color of Food" is open for, null in a form. The dialogs about the person's colors draw here.
 */
export function CategoryColorPicker({
  selected,
  onPick,
  customColors,
  actions,
  categoryKey = null,
  labelledBy,
}: {
  selected: string
  onPick: (code: string) => void
  customColors: JsCustomColors
  actions: JsCustomColorActions
  categoryKey?: string | null
  labelledBy?: string
}) {
  const custom = customColors.swatchesFor(selected)
  const isTouch = typeof window !== 'undefined' && window.matchMedia('(pointer: coarse)').matches
  const note = !customColors.canAdd
    ? t(isTouch ? 'customColor_full' : 'customColor_rightFull')
    : customColors.palette.length > 0
      ? t(isTouch ? 'customColor_hint' : 'customColor_rightHint')
      : null
  return (
    <div className="flex flex-col gap-2">
      <div role="radiogroup" aria-labelledby={labelledBy} className="flex flex-wrap gap-3">
        {[...categoryColorCodes, ...custom].map((code) => (
          <Swatch
            key={code}
            code={code}
            isSelected={code === selected}
            onPick={() => onPick(code)}
            onLongPress={customColors.isDeletable(code) ? () => actions.longPress(code) : undefined}
          />
        ))}
        {customColors.canAdd && (
          <button
            type="button"
            aria-label={t('customColor_add')}
            onClick={() => actions.add(selected, categoryKey)}
            className="flex size-11 items-center justify-center rounded-full border-2 border-muted-foreground/60 text-foreground outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
          >
            <Plus aria-hidden className="size-5" />
          </button>
        )}
      </div>
      {note && <p className="text-sm text-muted-foreground">{note}</p>}
      <CustomColorDialogs customColors={customColors} actions={actions} />
    </div>
  )
}

function Swatch({ code, isSelected, onPick, onLongPress }: { code: string; isSelected: boolean; onPick: () => void; onLongPress?: () => void }) {
  const timer = useRef<number | undefined>(undefined)
  const fired = useRef(false)
  const cancel = () => window.clearTimeout(timer.current)
  return (
    <button
      type="button"
      role="radio"
      aria-checked={isSelected}
      aria-label={categoryColorLabel(code)}
      aria-keyshortcuts={onLongPress ? 'Delete' : undefined}
      onClick={() => {
        if (fired.current) fired.current = false
        else onPick()
      }}
      onContextMenu={(event) => {
        if (!onLongPress) return
        event.preventDefault()
        cancel()
        // Why: a touch long press may also raise contextmenu after the timer already asked.
        if (!fired.current) onLongPress()
      }}
      onPointerDown={(event: PointerEvent) => {
        if (!onLongPress || event.pointerType === 'mouse') return
        fired.current = false
        timer.current = window.setTimeout(() => {
          fired.current = true
          onLongPress()
        }, LONG_PRESS_MS)
      }}
      onPointerUp={cancel}
      onPointerLeave={cancel}
      onKeyDown={(event) => {
        if (onLongPress && (event.key === 'Delete' || event.key === 'Backspace')) onLongPress()
      }}
      style={categoryStyle(code)}
      className={cn(
        'avatar flex size-11 touch-manipulation select-none items-center justify-center rounded-full outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
        isSelected && 'ring-[3px] ring-primary',
      )}
    >
      {isSelected && <Check aria-hidden className="size-5" />}
    </button>
  )
}

/** "New color", then "Delete this color?" or "This color is in use". */
function CustomColorDialogs({ customColors, actions }: { customColors: JsCustomColors; actions: JsCustomColorActions }) {
  const deletion = customColors.deletion
  return (
    <>
      {customColors.draft && <NewColorDialog draft={customColors.draft} actions={actions} />}
      {deletion && deletion.usedBy == null && (
        <ConfirmDialog
          title={t('customColor_deleteTitle')}
          description={t('customColor_deleteMessage')}
          confirmLabel={t('customColor_delete')}
          isBusy={deletion.isDeleting}
          onConfirm={() => actions.confirmDelete()}
          onDismiss={() => actions.dismissDeletion()}
        />
      )}
      {deletion?.usedBy && (
        <AlertDialog open onOpenChange={(open) => !open && actions.dismissDeletion()}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>{t('customColor_inUseTitle')}</AlertDialogTitle>
              <AlertDialogDescription>{t('customColor_inUseMessage', { name: categoryName(deletion.usedBy) })}</AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              {/* Why: the action closes the dialog through onOpenChange, which already dismisses it. */}
              <AlertDialogAction>{t('customColor_ok')}</AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      )}
    </>
  )
}

/** "New color": saturation and brightness in the square, the hue below, and the same color as hex and RGB. */
function NewColorDialog({ draft, actions }: { draft: JsColorDraft; actions: JsCustomColorActions }) {
  const hueColor = `hsl(${draft.hue} 100% 50%)`
  // Why: the field keeps what was typed, so the caret stays put while the ViewModel answers.
  const [hex, setHex] = useDraft(draft.hexText, (text) => {
    actions.changeHex(text)
    return actions.getHexText()
  })
  const fromSquare = (event: PointerEvent<HTMLDivElement>) => {
    const box = event.currentTarget.getBoundingClientRect()
    const saturation = Math.min(Math.max((event.clientX - box.left) / box.width, 0), 1)
    const value = Math.min(Math.max(1 - (event.clientY - box.top) / box.height, 0), 1)
    actions.changeHsv(draft.hue, saturation, value)
  }
  const fromBar = (event: PointerEvent<HTMLDivElement>) => {
    const box = event.currentTarget.getBoundingClientRect()
    actions.changeHsv(Math.min(Math.max(((event.clientX - box.left) / box.width) * 360, 0), 360), draft.saturation, draft.value)
  }
  const drag = (handle: (event: PointerEvent<HTMLDivElement>) => void) => ({
    onPointerDown: (event: PointerEvent<HTMLDivElement>) => {
      event.currentTarget.setPointerCapture(event.pointerId)
      handle(event)
    },
    onPointerMove: (event: PointerEvent<HTMLDivElement>) => {
      if (event.currentTarget.hasPointerCapture(event.pointerId)) handle(event)
    },
  })
  return (
    <Dialog open onOpenChange={(open) => !open && actions.dismiss()}>
      <DialogContent className="sm:max-w-[400px]">
        <DialogHeader>
          <DialogTitle>{t('customColor_title')}</DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-4">
          <div
            role="slider"
            tabIndex={0}
            aria-label={t('customColor_shade')}
            aria-valuetext={`${Math.round(draft.saturation * 100)}%, ${Math.round(draft.value * 100)}%`}
            aria-valuenow={Math.round(draft.value * 100)}
            onKeyDown={(event) => {
              const step = 0.05
              const moves: Record<string, [number, number]> = { ArrowLeft: [-step, 0], ArrowRight: [step, 0], ArrowUp: [0, step], ArrowDown: [0, -step] }
              const move = moves[event.key]
              if (!move) return
              event.preventDefault()
              actions.changeHsv(draft.hue, Math.min(Math.max(draft.saturation + move[0], 0), 1), Math.min(Math.max(draft.value + move[1], 0), 1))
            }}
            className="relative h-40 touch-none rounded-xl outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
            style={{ background: `linear-gradient(to top, #000, transparent), linear-gradient(to right, #fff, ${hueColor})` }}
            {...drag(fromSquare)}
          >
            <span
              aria-hidden
              className="pointer-events-none absolute size-5 -translate-x-1/2 -translate-y-1/2 rounded-full border-[3px] border-white shadow-[0_0_0_1px_rgba(0,0,0,0.6)]"
              style={{ left: `${draft.saturation * 100}%`, top: `${(1 - draft.value) * 100}%` }}
            />
          </div>
          <div
            role="slider"
            tabIndex={0}
            aria-label={t('customColor_hue')}
            aria-valuemin={0}
            aria-valuemax={360}
            aria-valuenow={Math.round(draft.hue)}
            aria-valuetext={`${Math.round(draft.hue)}°`}
            onKeyDown={(event) => {
              const step = event.key === 'ArrowRight' || event.key === 'ArrowUp' ? 5 : event.key === 'ArrowLeft' || event.key === 'ArrowDown' ? -5 : 0
              if (!step) return
              event.preventDefault()
              actions.changeHsv(Math.min(Math.max(draft.hue + step, 0), 360), draft.saturation, draft.value)
            }}
            className="relative h-7 touch-none rounded-full outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
            style={{ background: 'linear-gradient(to right, #f00, #ff0, #0f0, #0ff, #00f, #f0f, #f00)' }}
            {...drag(fromBar)}
          >
            <span
              aria-hidden
              className="pointer-events-none absolute top-1/2 size-6 -translate-x-1/2 -translate-y-1/2 rounded-full border-[3px] border-white shadow-[0_0_0_1px_rgba(0,0,0,0.6)]"
              style={{ left: `clamp(12px, ${(draft.hue / 360) * 100}%, calc(100% - 12px))` }}
            />
          </div>
          <div className="flex items-end gap-3">
            <span aria-hidden className="size-10 shrink-0 rounded-full border border-border" style={{ background: draft.color }} />
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="custom-color-hex">{t('customColor_hex')}</Label>
              <Input
                id="custom-color-hex"
                value={hex}
                autoComplete="off"
                spellCheck={false}
                aria-invalid={draft.isHexInvalid}
                aria-describedby={draft.isHexInvalid ? 'custom-color-error' : undefined}
                onChange={(event) => setHex(event.target.value)}
              />
            </div>
          </div>
          <div className="grid grid-cols-3 gap-2">
            <ChannelField channel="red" label="categoryColor_red" value={draft.redText} isInvalid={draft.isRgbInvalid} actions={actions} />
            <ChannelField channel="green" label="categoryColor_green" value={draft.greenText} isInvalid={draft.isRgbInvalid} actions={actions} />
            <ChannelField channel="blue" label="categoryColor_blue" value={draft.blueText} isInvalid={draft.isRgbInvalid} actions={actions} />
          </div>
          {(draft.isHexInvalid || draft.isRgbInvalid) && (
            <p id="custom-color-error" className="text-sm text-destructive">
              {t(draft.isHexInvalid ? 'customColor_hexError' : 'customColor_rgbError')}
            </p>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => actions.dismiss()}>
            {t('common_cancel')}
          </Button>
          <Button disabled={!draft.canSave} onClick={() => actions.save()}>
            {t('customColor_save')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function ChannelField({
  channel,
  label,
  value,
  isInvalid,
  actions,
}: {
  channel: 'red' | 'green' | 'blue'
  label: 'categoryColor_red' | 'categoryColor_green' | 'categoryColor_blue'
  value: string
  isInvalid: boolean
  actions: JsCustomColorActions
}) {
  // Why: the field keeps what was typed, digits only as the ViewModel keeps them, so the caret stays put.
  const [text, setText] = useDraft(value, (next) => {
    actions.changeChannel(channel, next)
    return actions.getChannelText(channel)
  })
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={`custom-color-${channel}`}>{t(label)}</Label>
      <Input
        id={`custom-color-${channel}`}
        value={text}
        inputMode="numeric"
        aria-invalid={isInvalid}
        aria-describedby={isInvalid ? 'custom-color-error' : undefined}
        onChange={(event) => setText(event.target.value)}
      />
    </div>
  )
}
