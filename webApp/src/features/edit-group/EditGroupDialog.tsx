import { Camera, ChevronRight, CircleAlert, ImageIcon, Loader2 } from 'lucide-react'
import { EditGroupPresenter, type JsEditGroupState, type JsRoute } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { AvatarColorPicker, PendingDot } from '@/components/common'
import { LoadingDialog } from '@/components/LoadingDialog'
import { PhotoCropDialog } from '@/components/PhotoCropDialog'
import { PhotoPickerInputs, usePhotoPicker } from '@/components/PhotoPicker'
import { CurrencyDropdown } from '@/components/CurrencyDropdown'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { GroupTypePicker } from '@/features/create-group/GroupTypePicker'
import { SimplifyDebtsConfirmDialog } from '@/features/simplify-debts/SimplifyDebtsConfirmDialog'
import { SimplifyIcon } from '@/features/simplify-debts/SimplifyIcon'
import { useSimplifyConflictNotice } from '@/features/simplify-debts/simplify'
import { defaultSplitModeLabel } from '@/features/split-bill/splitText'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { freeTextProps } from '@/lib/freeText'
import { cn } from '@/lib/utils'

const formId = 'edit-group-form'
const nameId = 'edit-group-name'
const nameHelpId = 'edit-group-name-help'
const currencyId = 'edit-group-currency'
const currencyNoteId = 'edit-group-currency-note'
const iconColorId = 'edit-group-icon-color'
const paymentsId = 'edit-group-payments'
const simplifyId = 'edit-group-simplify'
const simplifyStatusId = 'edit-group-simplify-status'

/**
 * "Edit group", over the group: a dialog on desktop and tablet, a drawer with stacked buttons on the phone.
 * Closing it any way (Esc, the overlay, Cancel) is the ViewModel's close. "Divisão padrão" and "Como funciona" open in
 * its place, so it stays mounted (closed) while `isCovered` and comes back with what was typed. "Simplificar dívidas"
 * asks first over it, and applies at once, apart from Salvar.
 */
export function EditGroupDialog({ route, isCovered }: { route: JsRoute; isCovered: boolean }) {
  const presenter = usePresenter(() => new EditGroupPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsEditGroupState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  useSimplifyConflictNotice(presenter, state?.simplifyConflict)
  if (!presenter || !state || isCovered) return null
  if (!state.isLoaded) return <LoadingDialog isDesktop={isDesktop} title={t('editGroup_title')} onClose={() => presenter.close()} />

  const onOpenChange = (open: boolean) => {
    if (!open) presenter.close()
  }
  const save = (
    <Button type="submit" form={formId} disabled={!state.canSave} className={cn(!isDesktop && 'h-11')}>
      {state.isSaving && <Loader2 className="animate-spin" />}
      {t('editGroup_save')}
    </Button>
  )
  const cancel = (
    <Button type="button" variant="outline" disabled={state.isSaving} onClick={() => presenter.close()} className={cn(!isDesktop && 'h-11')}>
      {t('common_cancel')}
    </Button>
  )
  const form = <EditGroupForm presenter={presenter} state={state} />
  const photoCrop = state.photoCrop && <PhotoCropDialog crop={state.photoCrop} presenter={presenter.crop} frame="rounded-square" />
  const simplifyConfirmation = state.simplifyConfirmation && (
    <SimplifyDebtsConfirmDialog
      confirmation={state.simplifyConfirmation}
      isBusy={state.isChangingSimplifyDebts}
      isDesktop={isDesktop}
      events={presenter}
    />
  )

  return isDesktop ? (
    <Dialog open={!isCovered} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>{t('editGroup_title')}</DialogTitle>
          <DialogDescription>{t('editGroup_description')}</DialogDescription>
        </DialogHeader>
        {form}
        <DialogFooter>
          {cancel}
          {save}
        </DialogFooter>
        {photoCrop}
        {simplifyConfirmation}
      </DialogContent>
    </Dialog>
  ) : (
    <Drawer open={!isCovered} onOpenChange={onOpenChange} repositionInputs={false}>
      <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
        <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          <DrawerTitle>{t('editGroup_title')}</DrawerTitle>
          <DrawerDescription>{t('editGroup_description')}</DrawerDescription>
        </DrawerHeader>
        <div className="overflow-y-auto px-4">{form}</div>
        <DrawerFooter>
          {save}
          {cancel}
        </DrawerFooter>
        {photoCrop}
        {simplifyConfirmation}
      </DrawerContent>
    </Drawer>
  )
}

// Why: "Usar uma foto" and "Tirar foto" hand the photo's bytes to the ViewModel, which makes the group's 512x512 JPEG.
function EditGroupForm({ presenter, state }: { presenter: EditGroupPresenter; state: JsEditGroupState }) {
  const photo = usePhotoPicker()
  // Why: the ViewModel caps the name as it handles it, so the field reads the capped text back.
  const [name, setName] = useDraft(state.name, (value) => {
    presenter.changeName(value)
    return presenter.getName()
  })
  return (
    <form
      id={formId}
      onSubmit={(event) => {
        event.preventDefault()
        presenter.save()
      }}
      className="flex flex-col gap-5"
    >
      {state.showsSaveError && (
        <p role="alert" className="flex items-center gap-1.5 text-sm text-destructive">
          <CircleAlert className="size-4 shrink-0" />
          {t('editGroup_saveError')}
        </p>
      )}
      <div className="flex items-center gap-4">
        <div className="relative">
          <GroupAvatar initials={state.initials} color={state.color} photoUrl={state.photoUrl} className="size-16 text-xl" />
          {state.isPreparingPhoto && <Loader2 aria-hidden className="absolute inset-0 m-auto size-6 animate-spin text-foreground" />}
        </div>
        <div className="flex flex-col gap-2">
          <span className="text-sm font-medium">{t('editGroup_icon')}</span>
          <span id={iconColorId} className="sr-only">
            {t('editGroup_iconColor')}
          </span>
          <AvatarColorPicker colors={state.colors} selected={state.selectedColor ?? ''} onSelect={(code) => presenter.selectColor(code)} labelledBy={iconColorId} />
          <PhotoPickerInputs picker={photo} receiver={presenter} facing="environment" />
          <div className="flex flex-wrap gap-2">
            <Button type="button" variant="outline" size="sm" disabled={state.isSaving} onClick={photo.chooseFile}>
              <ImageIcon />
              {t('editGroup_usePhoto')}
            </Button>
            {photo.hasCamera && (
              <Button type="button" variant="outline" size="sm" disabled={state.isSaving} onClick={photo.takePhoto}>
                <Camera />
                {t('editGroup_takePhoto')}
              </Button>
            )}
          </div>
          {state.showsPhotoError && (
            <p role="alert" className="text-[13px] text-destructive">
              {t('editGroup_photoError')}
            </p>
          )}
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor={nameId}>{t('editGroup_nameLabel')}</Label>
        <Input
          id={nameId}
          {...freeTextProps(name, setName)}
          placeholder={t('editGroup_namePlaceholder')}
          aria-invalid={state.showsNameError}
          aria-describedby={nameHelpId}
          autoComplete="off"
        />
        <span id={nameHelpId} className={cn('flex justify-between gap-3 text-[13px]', state.showsNameError ? 'text-destructive' : 'text-muted-foreground')}>
          <span>{t(state.showsNameError ? 'editGroup_nameError' : 'editGroup_nameHelp')}</span>
          <span className="tabular-nums">
            {state.nameLength}/{state.maxNameLength}
          </span>
        </span>
      </div>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
        <GroupTypePicker label={t('editGroup_typeLabel')} types={state.types} selected={state.type} onSelect={(type) => presenter.selectType(type)} />
        <div className="flex flex-col gap-2">
          <Label htmlFor={currencyId}>{t('editGroup_currency')}</Label>
          <CurrencyDropdown id={currencyId} describedBy={currencyNoteId} currencies={state.currencies} cryptoCurrencies={state.cryptoCurrencies} selected={state.currency} onSelect={(currency) => presenter.selectCurrency(currency)} />
          <span id={currencyNoteId} className="text-[13px] text-muted-foreground">
            {t('editGroup_currencyNote')}
          </span>
        </div>
      </div>

      <button
        type="button"
        disabled={state.isSaving}
        onClick={() => presenter.openDefaultSplit()}
        className="flex items-center gap-3 rounded-lg border px-3 py-2.5 text-left outline-none hover:bg-accent/60 focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring disabled:opacity-50"
      >
        <span className="flex min-w-0 flex-1 flex-col gap-0.5">
          <span className="text-sm font-medium">{t('editGroup_defaultSplit')}</span>
          <span className="text-[13px] text-muted-foreground">{t('editGroup_defaultSplitHelp')}</span>
        </span>
        <span className="text-sm text-muted-foreground">{defaultSplitModeLabel(state.defaultSplitMode)}</span>
        <ChevronRight aria-hidden className="size-4 text-muted-foreground" />
      </button>

      <SimplifyDebtsRow presenter={presenter} state={state} />

      <p className="text-[13px] text-muted-foreground">
        {t(state.showsSimplifyOfflineNotice ? 'editGroup_simplifyOfflineNote' : state.showsOfflineNotice ? 'editGroup_offlineNote' : 'editGroup_note')}
      </p>
    </form>
  )
}

/** The line under "Simplificar dívidas", by `simplifyStatus` (13.1, 13.1c, 13.1d). */
function simplifyStatusText(state: JsEditGroupState): string {
  switch (state.simplifyStatus) {
    case 'pending':
      return t(state.simplifiesDebts ? 'editGroup_simplifyPendingOn' : 'editGroup_simplifyPendingOff')
    case 'needs-third-person':
      return t('editGroup_simplifyNeedsThird')
    case 'on': {
      // Why: one row per currency, each simplifying on its own: "BRL · 7 → 5 pagamentos".
      if (state.simplifications.length > 1) {
        return state.simplifications
          .map((row) => t(row.paymentCount === 1 ? 'editGroup_simplifyCurrencyRowOne' : 'editGroup_simplifyCurrencyRowOther', { currency: row.currency, before: row.unsimplifiedPaymentCount, after: row.paymentCount }))
          .join('\n')
      }
      const simplification = state.simplifications[0]
      if (!simplification || simplification.isAlreadyMinimal) return t('editGroup_simplifyOnMinimal')
      return t('editGroup_simplifyOn', { count: simplification.paymentCount, before: simplification.unsimplifiedPaymentCount })
    }
    default:
      return t('editGroup_simplifyOff')
  }
}

/**
 * "Pagamentos": the switch of "Simplificar dívidas" (13.1), which shows how the group has it and never flips by itself:
 * a click opens the confirmation. "Como funciona" opens 13.2 in the dialog's place.
 */
function SimplifyDebtsRow({ presenter, state }: { presenter: EditGroupPresenter; state: JsEditGroupState }) {
  const isPending = state.simplifyStatus === 'pending'
  return (
    <section aria-labelledby={paymentsId} className="flex flex-col gap-2">
      <h3 id={paymentsId} className="text-sm font-medium">
        {t('editGroup_payments')}
      </h3>
      <div className="flex items-start gap-3 rounded-lg border px-3 py-3">
        <span aria-hidden className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted text-primary">
          <SimplifyIcon />
        </span>
        <span className="flex min-w-0 flex-1 flex-col gap-1">
          <Label htmlFor={simplifyId}>{t('simplify_title')}</Label>
          <span
            id={simplifyStatusId}
            className={cn('flex items-center gap-1.5 text-[13px] leading-[18px] whitespace-pre-line', isPending ? 'text-pending' : 'text-muted-foreground')}
          >
            {isPending && <PendingDot />}
            {simplifyStatusText(state)}
          </span>
          <Button type="button" variant="link" size="sm" className="h-auto self-start p-0 text-[13px]" onClick={() => presenter.openSimplifyDebtsInfo()}>
            {t('simplify_howItWorks')}
          </Button>
        </span>
        {/* Why: controlled by the group's state; the click asks first, so it never flips by itself. */}
        <Switch
          id={simplifyId}
          checked={state.simplifiesDebts}
          disabled={!state.canChangeSimplifyDebts || state.isSaving}
          aria-describedby={simplifyStatusId}
          onCheckedChange={() => presenter.clickSimplifyDebts()}
          className="mt-0.5"
        />
      </div>
    </section>
  )
}
