import { CircleAlert, CircleCheck, ClipboardPaste, Info, Link2, Loader2, Trash2, Zap } from 'lucide-react'
import { PaymentKeyPresenter, type JsPaymentKeyState, type JsRoute } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import type { BitcoinKeyType, PaymentKeyError, PaymentKeyFormat, PaymentKeyType, PaymentMethod } from '@/lib/paymentKey'
import { cn } from '@/lib/utils'

const formId = 'payment-key-form'
const inputId = 'payment-key-input'
const helpId = 'payment-key-help'

const inputModes: Record<PaymentKeyType, 'numeric' | 'tel' | 'email' | 'text'> = {
  CPF: 'numeric',
  PHONE: 'tel',
  EMAIL: 'email',
  RANDOM: 'text',
  LIGHTNING: 'text',
  ONCHAIN: 'text',
}

const acceptedFormats: Record<BitcoinKeyType, { sample: MessageKey; name: MessageKey }[]> = {
  LIGHTNING: [
    { sample: 'paymentKey_acceptedLightningAddressSample', name: 'paymentKey_acceptedLightningAddress' },
    { sample: 'paymentKey_acceptedLnurlSample', name: 'paymentKey_acceptedLnurl' },
  ],
  ONCHAIN: [
    { sample: 'paymentKey_acceptedSegwitSample', name: 'paymentKey_acceptedSegwit' },
    { sample: 'paymentKey_acceptedTaprootSample', name: 'paymentKey_acceptedTaproot' },
  ],
}

// Why: Firefox only reads the clipboard from an extension, so "Paste" shows where the browser lets the page read it.
const canReadClipboard = typeof navigator !== 'undefined' && typeof navigator.clipboard?.readText === 'function'

/**
 * A new or saved Pix or Bitcoin key, over the profile: a dialog on desktop and tablet, a drawer with
 * stacked buttons on the phone. Closing it any way (Esc, the overlay, Cancel) is the ViewModel's back.
 */
export function PaymentKeyDialog({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new PaymentKeyPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsPaymentKeyState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  // Why: a saved key fills the form once read, so nothing shows until then rather than an empty form.
  if (!presenter || !state?.isLoaded) return null

  const method = state.method as PaymentMethod
  const title = t(state.isEditing ? `paymentKey_titleEdit${method}` : `paymentKey_titleNew${method}`)
  const description = t(`paymentKey_intro${method}`)
  const onOpenChange = (open: boolean) => {
    if (!open) presenter.back()
  }
  const save = (
    <Button type="submit" form={formId} disabled={!state.canSave} className={cn(!isDesktop && 'h-11')}>
      {state.isSaving && <Loader2 className="animate-spin" />}
      {t('paymentKey_save')}
    </Button>
  )
  const cancel = (
    <Button type="button" variant="outline" onClick={() => presenter.back()} className={cn(!isDesktop && 'h-11')}>
      {t('common_cancel')}
    </Button>
  )
  const remove = state.isEditing && (
    <Button
      type="button"
      variant="ghost"
      disabled={state.isSaving}
      onClick={() => presenter.delete()}
      className={cn('text-destructive hover:text-destructive', isDesktop ? 'sm:mr-auto' : 'h-11')}
    >
      <Trash2 /> {t('paymentKey_delete')}
    </Button>
  )
  const form = <PaymentKeyForm presenter={presenter} state={state} />

  return (
    <>
      {isDesktop ? (
        <Dialog open onOpenChange={onOpenChange}>
          <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-[500px]">
            <DialogHeader>
              <DialogTitle>{title}</DialogTitle>
              <DialogDescription>{description}</DialogDescription>
            </DialogHeader>
            {form}
            <DialogFooter>
              {remove}
              {cancel}
              {save}
            </DialogFooter>
          </DialogContent>
        </Dialog>
      ) : (
        <Drawer open onOpenChange={onOpenChange} repositionInputs={false}>
          <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
            <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
              <DrawerTitle>{title}</DrawerTitle>
              <DrawerDescription>{description}</DrawerDescription>
            </DrawerHeader>
            <div className="overflow-y-auto px-4">{form}</div>
            <DrawerFooter>
              {save}
              {cancel}
              {remove}
            </DrawerFooter>
          </DrawerContent>
        </Drawer>
      )}
      <DeleteConfirmation presenter={presenter} isOpen={state.isDeleteConfirmationShown} />
    </>
  )
}

function PaymentKeyForm({ presenter, state }: { presenter: PaymentKeyPresenter; state: JsPaymentKeyState }) {
  const method = state.method as PaymentMethod
  const type = state.type as PaymentKeyType
  // Why: the ViewModel masks what was typed as it handles it, so the field shows the masked text right away.
  const [input, setInput] = useDraft(state.input, (value) => {
    presenter.changeInput(value)
    return presenter.getInput()
  })
  const paste = () => {
    navigator.clipboard
      .readText()
      .then((text) => presenter.paste(text))
      // The browser refused to read the clipboard (permission denied): the field stays as it is
      .catch(() => {})
  }
  const fieldProps = {
    id: inputId,
    value: input,
    spellCheck: false,
    autoComplete: 'off',
    autoCapitalize: 'none',
    'aria-invalid': state.error !== null,
    'aria-describedby': helpId,
    onChange: (event: { target: { value: string } }) => setInput(event.target.value),
    onBlur: () => presenter.inputFocusLost(),
  }

  return (
    <form
      id={formId}
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault()
        presenter.save()
      }}
    >
      {method === 'PIX' ? <PixTypes presenter={presenter} state={state} /> : <BitcoinNetworks presenter={presenter} state={state} />}
      <div className="flex flex-col gap-2">
        <Label htmlFor={inputId}>{t(`paymentKey_label${type}`)}</Label>
        {/* Pix: one line with "Paste" beside it; Bitcoin: an address wraps, with "Paste" under the helper */}
        {method === 'PIX' ? (
          <>
            <div className="flex gap-2">
              <Input {...fieldProps} inputMode={inputModes[type]} className={cn(type === 'RANDOM' && 'font-mono text-[13px] md:text-[13px]')} />
              {canReadClipboard && (
                <Button type="button" variant="outline" onClick={paste}>
                  <ClipboardPaste /> {t('paymentKey_paste')}
                </Button>
              )}
            </div>
            <Helper state={state} />
          </>
        ) : (
          <>
            <Textarea
              {...fieldProps}
              rows={2}
              className="resize-none font-mono text-[13px] break-all md:text-[13px]"
              onKeyDown={(event) => {
                // A key is one line: Enter saves, as in the Pix field
                if (event.key !== 'Enter') return
                event.preventDefault()
                event.currentTarget.form?.requestSubmit()
              }}
            />
            <Helper state={state} />
            {state.hasSwitchedNetwork && (
              <p role="status" className="flex gap-1.5 text-[13px] leading-[18px]">
                <Info className="mt-px size-4 shrink-0 text-primary" />
                {t(`paymentKey_switched${type as BitcoinKeyType}`)}
              </p>
            )}
            {canReadClipboard && (
              <Button type="button" variant="outline" size="sm" className="self-start" onClick={paste}>
                <ClipboardPaste /> {t('paymentKey_paste')}
              </Button>
            )}
          </>
        )}
      </div>
      {state.showsCpfNotice && (
        <Notice icon={<Info className="mt-0.5 size-4 shrink-0" />}>
          <span>{t('paymentKey_cpfNotice')}</span>
        </Notice>
      )}
      {method === 'BITCOIN' && <AcceptedFormats type={type as BitcoinKeyType} />}
      <DefaultRow presenter={presenter} state={state} />
      <p className="text-xs leading-[18px] text-muted-foreground">{t(`paymentKey_footer${method}`)}</p>
    </form>
  )
}

function PixTypes({ presenter, state }: { presenter: PaymentKeyPresenter; state: JsPaymentKeyState }) {
  return (
    <div role="radiogroup" aria-label={t('paymentKey_pixTypes')} className="grid grid-cols-4 gap-1 rounded-lg bg-muted p-[3px]">
      {(state.types as PaymentKeyType[]).map((type) => {
        const isSelected = type === state.type
        return (
          <button
            key={type}
            type="button"
            role="radio"
            aria-checked={isSelected}
            onClick={() => presenter.selectType(type)}
            className={cn(
              'h-[30px] min-w-0 truncate rounded-md px-1 text-[13px] outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
              isSelected ? 'bg-background font-medium shadow-sm' : 'text-muted-foreground hover:text-foreground',
            )}
          >
            {t(`paymentKey_type${type}`)}
          </button>
        )
      })}
    </div>
  )
}

function BitcoinNetworks({ presenter, state }: { presenter: PaymentKeyPresenter; state: JsPaymentKeyState }) {
  const icons: Record<BitcoinKeyType, ReactNode> = {
    LIGHTNING: <Zap className="size-[18px] shrink-0" />,
    ONCHAIN: <Link2 className="size-[18px] shrink-0" />,
  }
  return (
    <div role="radiogroup" aria-label={t('paymentKey_networks')} className="grid grid-cols-2 gap-2">
      {(state.types as BitcoinKeyType[]).map((type) => {
        const isSelected = type === state.type
        return (
          <button
            key={type}
            type="button"
            role="radio"
            aria-checked={isSelected}
            onClick={() => presenter.selectType(type)}
            className={cn(
              'flex h-14 items-center gap-2.5 rounded-lg px-3 text-left outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
              isSelected ? 'border-2 border-primary bg-primary/10' : 'border hover:bg-accent',
            )}
          >
            {icons[type]}
            <span className="flex min-w-0 flex-col">
              <span className="text-sm font-medium">{t(`paymentKey_type${type}`)}</span>
              <span className="truncate text-xs text-muted-foreground">{t(`paymentKey_type${type}Detail`)}</span>
            </span>
          </button>
        )
      })}
    </div>
  )
}

/** The error once there is one to show, else what the valid key turned out to be, else how to type it. */
function Helper({ state }: { state: JsPaymentKeyState }) {
  if (state.error) {
    return (
      <p id={helpId} role="alert" className="flex gap-1.5 text-[13px] leading-[18px] font-medium text-destructive">
        <CircleAlert className="mt-px size-4 shrink-0" />
        {t(`paymentKey_error${state.error as PaymentKeyError}`)}
      </p>
    )
  }
  if (state.format) {
    return (
      <p id={helpId} className="flex gap-1.5 text-[13px] leading-[18px] text-muted-foreground">
        <CircleCheck className="mt-px size-4 shrink-0 text-primary" />
        {t(`paymentKey_valid${state.format as PaymentKeyFormat}`)}
      </p>
    )
  }
  return (
    <p id={helpId} className="text-[13px] leading-[18px] text-muted-foreground">
      {t(`paymentKey_hint${state.type as PaymentKeyType}`)}
    </p>
  )
}

function AcceptedFormats({ type }: { type: BitcoinKeyType }) {
  return (
    <div className="rounded-lg border">
      <div className="px-3 pt-2 pb-1 text-xs font-medium tracking-wide text-muted-foreground uppercase">{t('paymentKey_accepted')}</div>
      {acceptedFormats[type].map((format) => (
        <div key={format.name} className="flex h-9 items-center justify-between gap-3 px-3 text-[13px]">
          <span className="truncate font-mono">{t(format.sample)}</span>
          <span className="shrink-0 text-muted-foreground">{t(format.name)}</span>
        </div>
      ))}
    </div>
  )
}

function DefaultRow({ presenter, state }: { presenter: PaymentKeyPresenter; state: JsPaymentKeyState }) {
  const method = state.method as PaymentMethod
  return (
    <label className={cn('flex items-center gap-3 rounded-lg border p-3', !state.isDefaultLocked && 'cursor-pointer')}>
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="text-sm font-medium">{t(`paymentKey_default${method}`)}</span>
        <span className="text-[13px] text-muted-foreground">{defaultDetail(state)}</span>
      </span>
      <Switch
        checked={state.isDefault}
        disabled={state.isDefaultLocked}
        onCheckedChange={(isChecked) => presenter.changeDefault(isChecked)}
        aria-label={t(`paymentKey_default${method}`)}
      />
    </label>
  )
}

// Why: "Shown first when settling up." and then why the switch is locked, or which key is the default now.
function defaultDetail(state: JsPaymentKeyState): string {
  const detail = t(`paymentKey_defaultDetail${state.method as PaymentMethod}`)
  if (state.isDefaultLocked) {
    return `${detail} ${t(state.isEditing ? 'paymentKey_defaultLockedEditing' : 'paymentKey_defaultLockedNew')}`
  }
  if (state.currentDefaultType) {
    const type = t(`paymentKey_defaultType${state.currentDefaultType as PaymentKeyType}`)
    return `${detail} ${t('paymentKey_defaultNow', { type })}`
  }
  return detail
}

function Notice({ icon, children }: { icon: ReactNode; children: ReactNode }) {
  return (
    <div role="note" className="flex gap-2.5 rounded-lg border p-3 text-[13px] leading-[1.45] text-foreground/80">
      {icon}
      {children}
    </div>
  )
}

/** "Delete this key?": the ViewModel closes it, after the deletion or on Cancel. */
function DeleteConfirmation({ presenter, isOpen }: { presenter: PaymentKeyPresenter; isOpen: boolean }) {
  return (
    <AlertDialog
      open={isOpen}
      onOpenChange={(open) => {
        if (!open) presenter.dismissDelete()
      }}
    >
      <AlertDialogContent className="sm:max-w-md">
        <AlertDialogHeader>
          <AlertDialogTitle>{t('paymentKey_deleteTitle')}</AlertDialogTitle>
          <AlertDialogDescription>{t('paymentKey_deleteBody')}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>{t('common_cancel')}</AlertDialogCancel>
          {/* Not an AlertDialogAction, which would close it before the ViewModel deletes */}
          <Button variant="destructive" onClick={() => presenter.confirmDelete()}>
            {t('paymentKey_deleteConfirm')}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
