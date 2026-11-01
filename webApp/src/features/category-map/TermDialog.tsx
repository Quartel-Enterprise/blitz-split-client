import type { CategoryMapPresenter, JsCategoryMapState, JsTermForm } from 'blitzsplit-shared'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useDraft } from '@/hooks/useDraft'
import { t } from '@/i18n'
import { categoryName, categoryPath } from '@/lib/categories'
import { cn } from '@/lib/utils'

function termHelp(form: JsTermForm): string {
  if (form.error === 'too-short') return t('categoryMap_termTooShort')
  if (form.error === 'invalid-characters') return t('categoryMap_termInvalid')
  if (form.duplicateTerm && form.duplicateCategory) {
    return t('categoryMap_termDuplicate', { term: form.duplicateTerm, path: categoryPath(form.duplicateCategory, form.duplicateParent) })
  }
  return form.isAppTerm ? t('categoryMap_termHelpApp') : t('categoryMap_termHelp')
}

function categoryHelp(form: JsTermForm): string {
  if (form.duplicateCategory) return t('categoryMap_categoryHelpMove', { name: categoryName(form.duplicateCategory) })
  return form.isEditing ? t('categoryMap_categoryHelpEdit') : t('categoryMap_categoryHelp')
}

/** "Novo termo" or "Editar termo" (9.2) in a Dialog, a Drawer on a phone; a word another category has turns Salvar into Mover. */
export function TermDialog({ presenter, state, form, isDesktop }: { presenter: CategoryMapPresenter; state: JsCategoryMapState; form: JsTermForm; isDesktop: boolean }) {
  const [text, setText] = useDraft(form.text, (value) => {
    presenter.changeTermText(value)
    return presenter.getTermText()
  })
  const typed = text.trim()
  const isError = !!form.error || !!form.duplicateTerm
  const example =
    typed && form.category
      ? `${form.duplicateTerm ? t('categoryMap_exampleMove') : t('categoryMap_example')}: “${typed.charAt(0).toUpperCase()}${typed.slice(1)}” → ${categoryPath(form.category, form.categoryParent)}`
      : null
  const saveLabel = form.duplicateTerm && form.category ? t('categoryMap_moveTo', { name: categoryName(form.category) }) : t('categoryMap_save')
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      onOpenChange={(open) => {
        if (!open) presenter.dismissTerm()
      }}
      pane={{
        title: form.isEditing ? t('categoryMap_editTerm') : t('categoryMap_newTerm'),
        body: (
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="term-text">{t('categoryMap_termLabel')}</Label>
              <Input id="term-text" value={text} onChange={(event) => setText(event.target.value)} aria-invalid={isError} autoComplete="off" />
              <p className={cn('text-xs', isError ? 'text-destructive' : 'text-muted-foreground')}>{termHelp(form)}</p>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>{t('categoryMap_category')}</Label>
              <Select value={form.categoryId} onValueChange={(id) => presenter.pickTermCategory(id)}>
                <SelectTrigger className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {state.options.map((option) => (
                    <SelectItem key={option.category.id} value={option.category.id} className={cn(option.category.parentId && 'pl-6')}>
                      {categoryPath(option.category, option.parent)}
                      {option.groupName && ` · ${t('categoryMap_onlyInGroup', { group: option.groupName })}`}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">{categoryHelp(form)}</p>
            </div>
            {example && <p className="text-sm text-muted-foreground">{example}</p>}
          </div>
        ),
        footer: (
          <>
            <Button disabled={!form.canSave} onClick={() => presenter.saveTerm()}>
              {saveLabel}
            </Button>
            <Button variant="outline" onClick={() => presenter.dismissTerm()}>
              {t('categoryMap_cancel')}
            </Button>
            {form.isEditing && (
              <Button variant="ghost" className="text-destructive hover:text-destructive sm:mr-auto" onClick={() => presenter.removeTerm()}>
                {t('categoryMap_remove')}
              </Button>
            )}
          </>
        ),
      }}
    />
  )
}
