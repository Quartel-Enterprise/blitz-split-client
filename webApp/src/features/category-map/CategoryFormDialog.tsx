import type { CategoryMapPresenter, JsCategoryForm, JsCategoryMapState } from 'blitzsplit-shared'
import { categoryFormPane } from '@/components/CategoryForm'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { t } from '@/i18n'

const groupId = 'category-group'

/**
 * "Nova categoria", "Nova subcategoria" or "Editar categoria" from the map (9.4): a bill's Categoria form in a Dialog
 * (a Drawer on a phone), with "Grupo" above the name while a new one may go in more than one group.
 */
export function CategoryFormDialog({ presenter, state, form, isDesktop }: { presenter: CategoryMapPresenter; state: JsCategoryMapState; form: JsCategoryForm; isDesktop: boolean }) {
  const groupField = state.canPickCategoryGroup && (
    <div className="flex flex-col gap-2">
      <Label htmlFor={groupId}>{t('categoryMap_categoryGroup')}</Label>
      <Select value={form.groupId} onValueChange={(id) => presenter.pickGroup(id)}>
        <SelectTrigger id={groupId} className="w-full">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {state.groups.map((group) => (
            <SelectItem key={group.id} value={group.id}>
              {group.name}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      className="sm:max-w-[520px] lg:max-w-[760px]"
      onOpenChange={(open) => {
        if (!open) presenter.dismissForm()
      }}
      pane={categoryFormPane(presenter, form, state.icons, groupField || undefined)}
    />
  )
}
