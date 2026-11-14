import { Check, Loader2, Trash2 } from 'lucide-react'
import { PayingMethodPresenter, type JsPayingMethodState, type JsRoute } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { LoadingDialog } from '@/components/LoadingDialog'
import { PayingMethodTile } from '@/components/PayingMethodTile'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { cardColorHexes, cardColorNames, payingMethodTypeName, type CardColor } from '@/lib/payingMethod'
import { cn } from '@/lib/utils'

const formId = 'paying-method-form'
const nameId = 'paying-method-name'
const typeLabelId = 'paying-method-type'
const colorLabelId = 'paying-method-color'

/**
 * A new or saved payment method, over the profile like a payment key: a dialog on desktop and tablet, a drawer on the
 * phone. Closing it any way (Esc, the overlay, Cancel) is the ViewModel's back, which also closes it once saved.
 */
export function PayingMethodDialog({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new PayingMethodPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsPayingMethodState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  // Why: a saved method fills the form once read, so its fields shimmer until then rather than an empty form.
  if (!presenter || !state) return null
  if (!state.isLoaded) return <LoadingDialog isDesktop={isDesktop} onClose={() => presenter.back()} className="sm:max-w-[500px]" />

  const buttonClass = cn(!isDesktop && 'h-11')
  return (
    <>
      <ResponsiveDialog
        isDesktop={isDesktop}
        className="sm:max-w-[500px]"
        onOpenChange={(open) => {
          if (!open) presenter.back()
        }}
        pane={{
          title: t(state.isEditing ? 'payingMethod_titleEdit' : 'payingMethod_titleNew'),
          description: t('payingMethod_sectionNote'),
          body: <PayingMethodForm presenter={presenter} state={state} />,
          footer: (
            <>
              {state.isEditing && (
                <Button
                  type="button"
                  variant="ghost"
                  disabled={state.isSaving}
                  onClick={() => presenter.delete()}
                  className={cn('text-destructive hover:text-destructive', isDesktop ? 'sm:mr-auto' : 'h-11')}
                >
                  <Trash2 /> {t('payingMethod_delete')}
                </Button>
              )}
              <Button type="button" variant="outline" onClick={() => presenter.back()} className={buttonClass}>
                {t('common_cancel')}
              </Button>
              <Button type="submit" form={formId} disabled={!state.canSave} className={buttonClass}>
                {state.isSaving && <Loader2 className="animate-spin" />}
                {t('payingMethod_save')}
              </Button>
            </>
          ),
        }}
      />
      {state.isDeleteConfirmationShown && (
        <ConfirmDialog
          title={t('payingMethod_deleteTitle')}
          description={t('payingMethod_deleteBody')}
          confirmLabel={t('payingMethod_deleteConfirm')}
          onConfirm={() => presenter.confirmDelete()}
          onDismiss={() => presenter.dismissDelete()}
        />
      )}
    </>
  )
}

function PayingMethodForm({ presenter, state }: { presenter: PayingMethodPresenter; state: JsPayingMethodState }) {
  const [name, setName] = useDraft(state.name, (value) => presenter.changeName(value))
  return (
    <form
      id={formId}
      className="flex flex-col gap-5"
      onSubmit={(event) => {
        event.preventDefault()
        presenter.save()
      }}
    >
      <div className="flex flex-col gap-2">
        <span id={typeLabelId} className="text-sm font-medium">
          {t('payingMethod_typeLabel')}
        </span>
        <div role="radiogroup" aria-labelledby={typeLabelId} className="flex flex-wrap gap-2">
          {state.types.map((type) => {
            const isSelected = type === state.type
            return (
              <button
                key={type}
                type="button"
                role="radio"
                aria-checked={isSelected}
                onClick={() => presenter.selectType(type)}
                className={cn(
                  'flex h-9 items-center gap-2 rounded-full border pr-3 pl-1.5 text-[13px] outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
                  isSelected ? 'border-primary bg-primary/10 font-medium' : 'hover:bg-accent',
                )}
              >
                <PayingMethodTile type={type} cardColor={state.cardColor} size="sm" className="rounded-full" />
                {payingMethodTypeName(type)}
              </button>
            )
          })}
        </div>
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor={nameId}>{t(state.isNameRequired ? 'payingMethod_name' : 'payingMethod_nameOptional')}</Label>
        <Input
          id={nameId}
          value={name}
          maxLength={state.nameMaxLength}
          autoComplete="off"
          aria-required={state.isNameRequired}
          onChange={(event) => setName(event.target.value)}
        />
      </div>
      {state.showsCardColors && (
        <div className="flex flex-col gap-2">
          <span id={colorLabelId} className="text-sm font-medium">
            {t('payingMethod_color')}
          </span>
          <div role="radiogroup" aria-labelledby={colorLabelId} className="flex flex-wrap gap-3">
            {state.cardColors.map((color) => {
              const isSelected = color === state.cardColor
              const colorName = t(cardColorNames[color as CardColor] ?? 'payingMethod_colorGraphite')
              return (
                <button
                  key={color}
                  type="button"
                  role="radio"
                  aria-checked={isSelected}
                  aria-label={colorName}
                  title={colorName}
                  onClick={() => presenter.selectCardColor(color)}
                  style={{ backgroundColor: cardColorHexes[color as CardColor] }}
                  className={cn(
                    'flex size-8 items-center justify-center rounded-full text-white outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
                    isSelected && 'ring-2 ring-foreground ring-offset-2 ring-offset-background',
                  )}
                >
                  {isSelected && <Check className="size-4" />}
                </button>
              )
            })}
          </div>
        </div>
      )}
      <label className="flex cursor-pointer items-center gap-3 rounded-lg border p-3">
        <span className="flex min-w-0 flex-1 flex-col gap-0.5">
          <span className="text-sm font-medium">{t('payingMethod_default')}</span>
          <span className="text-[13px] text-muted-foreground">{t('payingMethod_defaultHint')}</span>
        </span>
        <Switch checked={state.isDefault} onCheckedChange={(isChecked) => presenter.changeDefault(isChecked)} aria-label={t('payingMethod_default')} />
      </label>
    </form>
  )
}
