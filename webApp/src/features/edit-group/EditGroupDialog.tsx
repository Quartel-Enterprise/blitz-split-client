import { Check, ChevronDown, CircleAlert, Loader2, Lock } from 'lucide-react'
import { EditGroupPresenter, type JsEditGroupState, type JsRoute } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { GroupTypePicker } from '@/features/create-group/GroupTypePicker'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { currencyLabel } from '@/lib/currency'
import { cn } from '@/lib/utils'

const formId = 'edit-group-form'
const nameId = 'edit-group-name'
const nameHelpId = 'edit-group-name-help'
const currencyId = 'edit-group-currency'
const currencyNoteId = 'edit-group-currency-note'

/**
 * "Edit group", over the group: a dialog on desktop and tablet, a drawer with stacked buttons on the phone.
 * Closing it any way (Esc, the overlay, Cancel) is the ViewModel's close.
 */
export function EditGroupDialog({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new EditGroupPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsEditGroupState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  if (!presenter || !state?.isLoaded) return null

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

  return isDesktop ? (
    <Dialog open onOpenChange={onOpenChange}>
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
      </DialogContent>
    </Dialog>
  ) : (
    <Drawer open onOpenChange={onOpenChange} repositionInputs={false}>
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
      </DrawerContent>
    </Drawer>
  )
}

function EditGroupForm({ presenter, state }: { presenter: EditGroupPresenter; state: JsEditGroupState }) {
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
        <button
          type="button"
          aria-label={t('editGroup_changeColorDescription')}
          onClick={() => presenter.changeColor()}
          className="rounded-2xl outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
        >
          <GroupAvatar initials={state.initials} color={state.color} className="size-16 text-xl" />
        </button>
        <div className="flex flex-col gap-1.5">
          <span className="text-sm font-medium">{t('editGroup_icon')}</span>
          <Button type="button" variant="outline" size="sm" className="self-start" onClick={() => presenter.changeColor()}>
            {t('editGroup_changeColor')}
          </Button>
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <Label htmlFor={nameId}>{t('editGroup_nameLabel')}</Label>
        <Input
          id={nameId}
          value={name}
          placeholder={t('editGroup_namePlaceholder')}
          aria-invalid={state.showsNameError}
          aria-describedby={nameHelpId}
          autoComplete="off"
          onChange={(event) => setName(event.target.value)}
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
          {state.isCurrencyLocked ? (
            // Why: a disabled button keeps the label's name and says it can't change, which a plain span wouldn't.
            <Button id={currencyId} type="button" variant="outline" disabled aria-describedby={currencyNoteId} className="justify-between">
              {currencyLabel(state.currency)}
              <Lock aria-label={t('editGroup_currencyLocked')} />
            </Button>
          ) : (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button id={currencyId} type="button" variant="outline" aria-describedby={currencyNoteId} className="justify-between">
                  {currencyLabel(state.currency)}
                  <ChevronDown className="text-muted-foreground" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="max-h-72 overflow-y-auto">
                {state.currencies.map((currency) => (
                  <DropdownMenuItem key={currency} onSelect={() => presenter.selectCurrency(currency)}>
                    <span className="flex-1">{currencyLabel(currency)}</span>
                    {currency === state.currency && <Check />}
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>
          )}
          <span id={currencyNoteId} className="text-[13px] text-muted-foreground">
            {t('editGroup_currencyNote')}
          </span>
        </div>
      </div>

      <p className="text-[13px] text-muted-foreground">{t(state.showsOfflineNotice ? 'editGroup_offlineNote' : 'editGroup_note')}</p>
    </form>
  )
}
