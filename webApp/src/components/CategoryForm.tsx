import { Check, CloudOff } from 'lucide-react'
import type { JsCategoryDeletion, JsCategoryForm } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import type { PaneParts } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useDraft } from '@/hooks/useDraft'
import { t } from '@/i18n'
import { categoryColorCodes, categoryColorLabel, categoryStyle } from '@/lib/avatar'
import { categoryIconLabel, categoryIconOf, categoryName } from '@/lib/categories'
import { freeTextProps } from '@/lib/freeText'
import { cn } from '@/lib/utils'

const nameId = 'category-name'
const nameHelpId = 'category-name-help'
const parentId = 'category-parent'
const noParent = '__none__'

/** What "Nova categoria" sends, as both a bill's Categoria (SplitCategoriesPresenter) and the map (CategoryMapPresenter) name it. */
export interface CategoryFormActions {
  changeName(text: string): void
  getName(): string
  pickParent(parentId: string | null): void
  pickIcon(code: string): void
  pickColor(code: string): void
  save(): void
  dismissForm(): void
  askToDelete(): void
  confirmDelete(): void
  dismissDelete(): void
}

/**
 * "Nova categoria", "Nova subcategoria" or "Editar categoria" (7.8), in a bill's picker or over the map (9.4).
 * groupField is the map's "Grupo", above the name.
 */
export function categoryFormPane(actions: CategoryFormActions, form: JsCategoryForm, icons: string[], groupField?: ReactNode): PaneParts {
  const title = form.isEditing ? 'splitBill_categoryFormEdit' : form.isSubcategory ? 'splitBill_categoryFormSub' : 'splitBill_categoryFormNew'
  return {
    title: t(title),
    body: <CategoryFormBody actions={actions} form={form} icons={icons} groupField={groupField} />,
    footer: (
      <>
        {form.isEditing && (
          <Button key="category-delete" variant="ghost" className="text-destructive sm:mr-auto" disabled={!form.canDelete} onClick={() => actions.askToDelete()}>
            {t('splitBill_categoryDelete')}
          </Button>
        )}
        <Button key="category-cancel" variant="outline" onClick={() => actions.dismissForm()}>
          {t('common_cancel')}
        </Button>
        <Button key="category-save" disabled={!form.canSave} onClick={() => actions.save()}>
          {t('editGroup_save')}
        </Button>
      </>
    ),
  }
}

function CategoryFormBody({ actions, form, icons, groupField }: { actions: CategoryFormActions; form: JsCategoryForm; icons: string[]; groupField?: ReactNode }) {
  const [name, setName] = useDraft(form.name, (text) => {
    actions.changeName(text)
    return actions.getName()
  })
  const parent = form.parent
  const trimmed = form.name.trim()
  const nameHelp = form.isNameTaken
    ? parent
      ? t('splitBill_categoryNameTakenIn', { name: trimmed, parent: categoryName(parent) })
      : t('splitBill_categoryNameTaken', { name: trimmed })
    : form.isEditing
      ? [
          form.creatorName && t('splitBill_categoryCreatedBy', { name: form.creatorName }),
          form.usage == null ? null : form.usage === 1 ? t('splitBill_categoryUsedInOne') : t('splitBill_categoryUsedIn', { count: form.usage }),
        ]
          .filter(Boolean)
          .join(' · ')
      : t('splitBill_categoryNameHelp')
  const parentHelp = form.isParentLocked
    ? t('splitBill_categoryParentLocked')
    : parent
      ? t('splitBill_categoryParentHelpSub', { name: categoryName(parent) })
      : t('splitBill_categoryParentHelp')
  const Icon = categoryIconOf(form.icon)
  const usage = form.isEditing ? (form.usage ?? null) : null
  const note =
    usage === null
      ? t('splitBill_categoryGroupNote', { group: form.groupName })
      : usage === 1
        ? t('splitBill_categoryEditNoteOne', { group: form.groupName, name: trimmed })
        : t('splitBill_categoryEditNote', { group: form.groupName, count: usage, name: trimmed })
  return (
    <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] lg:gap-8">
      <div className="flex min-w-0 flex-col gap-4">
        {groupField}
        <div className="flex flex-col gap-2">
          <Label htmlFor={nameId} className={cn(form.isNameTaken && 'text-destructive')}>
            {t('splitBill_categoryName')}
          </Label>
          <Input
            id={nameId}
            autoComplete="off"
            maxLength={30}
            {...freeTextProps(name, setName)}
            aria-invalid={form.isNameTaken}
            aria-describedby={nameHelpId}
          />
          <p id={nameHelpId} className={cn('text-[13px]', form.isNameTaken ? 'text-destructive' : 'text-muted-foreground')}>
            {nameHelp}
          </p>
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor={parentId}>{t('splitBill_categoryParent')}</Label>
          <Select value={form.parentId ?? noParent} disabled={form.isParentLocked} onValueChange={(value) => actions.pickParent(value === noParent ? null : value)}>
            <SelectTrigger id={parentId} className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={noParent}>{t('splitBill_categoryParentNone')}</SelectItem>
              {form.parents.map((option) => (
                <SelectItem key={option.id} value={option.id}>
                  {categoryName(option)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <p className="text-[13px] text-muted-foreground">{parentHelp}</p>
        </div>
        <div className="flex items-center gap-3 rounded-2xl bg-card p-3">
          <span aria-hidden style={categoryStyle(form.color)} className="avatar flex size-10 items-center justify-center rounded-xl">
            <Icon className="size-5" />
          </span>
          <span>
            <span className="block text-[13px] text-muted-foreground">{t('splitBill_categoryPreview')}</span>
            <span className="block font-semibold">{parent ? t('category_path', { parent: categoryName(parent), name: trimmed }) : trimmed}</span>
          </span>
        </div>
        {form.showsOfflineNotice && (
          <p role="status" className="flex gap-2 text-[13px] text-muted-foreground">
            <CloudOff aria-hidden className="size-4 shrink-0" />
            {t('splitBill_categoryOffline')}
          </p>
        )}
        <p className="text-[13px] text-muted-foreground">{note}</p>
      </div>
      {!form.isSubcategory && (
        <div className="flex min-w-0 flex-col gap-2">
          <span id="category-icon-label" className="text-sm font-semibold text-muted-foreground">
            {t('splitBill_categoryIcon')}
          </span>
          <div role="radiogroup" aria-labelledby="category-icon-label" className="grid grid-cols-6 gap-2">
            {icons.map((code) => {
              const IconOf = categoryIconOf(code)
              const isSelected = code === form.icon
              return (
                <button
                  key={code}
                  type="button"
                  role="radio"
                  aria-checked={isSelected}
                  aria-label={categoryIconLabel(code)}
                  onClick={() => actions.pickIcon(code)}
                  // Why: the picked icon sits on the category's own color, as the preview and the saved badge draw it.
                  style={isSelected ? categoryStyle(form.color) : undefined}
                  className={cn(
                    'flex aspect-square items-center justify-center rounded-xl outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
                    isSelected ? 'avatar avatar-edge' : 'bg-muted text-muted-foreground',
                  )}
                >
                  <IconOf aria-hidden className="size-5" />
                </button>
              )
            })}
          </div>
          <span id="category-color-label" className="mt-3 text-sm font-semibold text-muted-foreground">
            {t('splitBill_categoryColor')}
          </span>
          <div role="radiogroup" aria-labelledby="category-color-label" className="flex flex-wrap gap-3">
            {categoryColorCodes.map((code) => {
              const isSelected = code === form.color
              return (
                <button
                  key={code}
                  type="button"
                  role="radio"
                  aria-checked={isSelected}
                  aria-label={categoryColorLabel(code)}
                  onClick={() => actions.pickColor(code)}
                  style={categoryStyle(code)}
                  className={cn(
                    'avatar flex size-11 items-center justify-center rounded-full outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
                    isSelected && 'ring-[3px] ring-primary',
                  )}
                >
                  {isSelected && <Check aria-hidden className="size-5" />}
                </button>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}

/** "Apagar “Churrasco”?" (7.8f), saying where its bills go. */
export function CategoryDeleteDialog({ actions, deletion, groupName }: { actions: CategoryFormActions; deletion: JsCategoryDeletion; groupName: string }) {
  const name = categoryName(deletion.category)
  const message =
    deletion.billCount === 0
      ? t('splitBill_categoryDeleteNone', { group: groupName })
      : deletion.billCount === 1
        ? t('splitBill_categoryDeleteTextOne', { name, to: categoryName(deletion.movesTo), group: groupName })
        : t('splitBill_categoryDeleteText', { count: deletion.billCount, name, to: categoryName(deletion.movesTo), group: groupName })
  return (
    <ConfirmDialog
      title={t('splitBill_categoryDeleteTitle', { name })}
      description={message}
      confirmLabel={t('splitBill_categoryDeleteConfirm')}
      isBusy={deletion.isDeleting}
      onConfirm={() => actions.confirmDelete()}
      onDismiss={() => actions.dismissDelete()}
    />
  )
}
