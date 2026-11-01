import { CircleCheck, ListFilter, MessageSquare, Receipt, Send, UserPlus, X } from 'lucide-react'
import type { ActivityPresenter, JsActivityFilter, JsActivityState } from 'blitzsplit-shared'
import { useId, type ReactNode } from 'react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Drawer, DrawerClose, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'
import { activityTypes, typeLabel } from './activityText'

const typeIcons: Record<string, ReactNode> = {
  bills: <Receipt />,
  payments: <CircleCheck />,
  comments: <MessageSquare />,
  'people-and-groups': <UserPlus />,
  charges: <Send />,
}

// Why: "Todos os grupos" has no group id, which the radio group needs as a value.
const allGroups = '__all__'

/**
 * "Filtrar" and what it picks: a Popover under the button on the desktop and the tablet, a Drawer on the phone, as
 * the canvas draws 12.2. Nothing changes until "Mostrar N resultados"; closing it any other way drops the draft.
 */
export function ActivityFilterButton({ presenter, state, isPhone }: { presenter: ActivityPresenter; state: JsActivityState; isPhone: boolean }) {
  const draft = state.filterDraft
  const count = activeFilterCount(state.filter)
  const onOpenChange = (open: boolean) => (open ? presenter.openFilter() : presenter.dismissFilter())
  const trigger = (
    <Button variant="outline" size="sm" aria-haspopup="dialog" aria-expanded={draft != null} onClick={isPhone ? () => presenter.openFilter() : undefined}>
      <ListFilter /> {t('activity_filter')}
      {count > 0 && <span className="flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[11px] text-primary-foreground tabular-nums">{count}</span>}
    </Button>
  )
  if (isPhone) {
    return (
      <>
        {trigger}
        {draft && (
          <Drawer open onOpenChange={onOpenChange}>
            <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
              <DrawerHeader className="flex-row items-center justify-between group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
                <DrawerTitle>{t('activity_filterTitle')}</DrawerTitle>
                <DrawerDescription className="sr-only">{t('activity_filterTitle')}</DrawerDescription>
                <DrawerClose asChild>
                  <Button variant="ghost" size="icon" aria-label={t('common_close')}>
                    <X />
                  </Button>
                </DrawerClose>
              </DrawerHeader>
              <div className="overflow-y-auto px-4">
                <FilterBody presenter={presenter} state={state} draft={draft} rowClassName="h-11 px-2" />
              </div>
              <DrawerFooter className="flex-col-reverse">
                <FilterActions presenter={presenter} count={state.draftResultCount} />
              </DrawerFooter>
            </DrawerContent>
          </Drawer>
        )}
      </>
    )
  }
  return (
    <Popover open={draft != null} onOpenChange={onOpenChange}>
      <PopoverTrigger asChild>{trigger}</PopoverTrigger>
      <PopoverContent align="start" className="flex w-80 flex-col gap-3 p-0" aria-label={t('activity_filterTitle')}>
        <h2 className="px-4 pt-4 text-base font-semibold">{t('activity_filterTitle')}</h2>
        {draft && (
          <div className="max-h-[60dvh] overflow-y-auto px-2">
            <FilterBody presenter={presenter} state={state} draft={draft} rowClassName="h-8 px-2" />
          </div>
        )}
        <div className="flex justify-end gap-2 border-t px-4 py-3">
          <FilterActions presenter={presenter} count={state.draftResultCount} />
        </div>
      </PopoverContent>
    </Popover>
  )
}

function activeFilterCount(filter: JsActivityFilter): number {
  return (filter.groupId ? 1 : 0) + filter.types.length + (filter.hasPerson ? 1 : 0)
}

/** Grupo as radios ("Todos os grupos" first, each group with its shape) and Tipo as checkboxes, each with its icon. */
function FilterBody({ presenter, state, draft, rowClassName }: { presenter: ActivityPresenter; state: JsActivityState; draft: JsActivityFilter; rowClassName: string }) {
  const groupLabelId = useId()
  const typeLabelId = useId()
  const row = cn('flex cursor-pointer items-center gap-2.5 rounded-sm text-sm hover:bg-accent', rowClassName)
  return (
    <div className="flex flex-col gap-1 pb-1">
      <h3 id={groupLabelId} className="px-2 pt-1 text-xs font-medium text-muted-foreground">
        {t('activity_filterGroup')}
      </h3>
      <RadioGroup
        aria-labelledby={groupLabelId}
        value={draft.groupId ?? allGroups}
        onValueChange={(value) => presenter.pickDraftGroup(value === allGroups ? null : value)}
        className="gap-0"
      >
        <label className={row}>
          <RadioGroupItem value={allGroups} />
          {t('activity_filterAllGroups')}
        </label>
        {state.groups.map((group) => (
          <label key={group.id} className={row}>
            <RadioGroupItem value={group.id} />
            <GroupAvatar initials={group.initials} color={group.color} className="size-5 text-[9px]" />
            <span className="truncate">{group.name}</span>
          </label>
        ))}
      </RadioGroup>
      <div className="my-1 border-t" />
      <h3 id={typeLabelId} className="px-2 pt-1 text-xs font-medium text-muted-foreground">
        {t('activity_filterType')}
      </h3>
      <div role="group" aria-labelledby={typeLabelId} className="flex flex-col">
        {activityTypes.map((type) => (
          <label key={type.code} className={row}>
            <Checkbox checked={draft.types.includes(type.code)} onCheckedChange={() => presenter.toggleDraftType(type.code)} />
            <span aria-hidden className="text-muted-foreground [&>svg]:size-4">
              {typeIcons[type.code]}
            </span>
            {t(type.label)}
          </label>
        ))}
      </div>
    </div>
  )
}

/** "Limpar" shows everything and closes; "Mostrar 4 resultados" applies what was picked. */
function FilterActions({ presenter, count }: { presenter: ActivityPresenter; count: number }) {
  return (
    <>
      <Button variant="ghost" onClick={() => presenter.clearFilterDraft()}>
        {t('activity_filterClear')}
      </Button>
      <Button onClick={() => presenter.applyFilter()}>{t(count === 1 ? 'activity_showResultsOne' : 'activity_showResultsOther', { count })}</Button>
    </>
  )
}

/** The filters in use as removable badges (the group, each type, the person), then "Limpar". */
export function ActiveFilters({ presenter, state }: { presenter: ActivityPresenter; state: JsActivityState }) {
  if (!state.isFilterActive) return null
  const chips: { key: string; label: string; onRemove: () => void }[] = []
  if (state.groupChipName) chips.push({ key: 'group', label: state.groupChipName, onRemove: () => presenter.removeGroupChip() })
  for (const code of state.typeChips) chips.push({ key: code, label: typeLabel(code), onRemove: () => presenter.removeTypeChip(code) })
  if (state.personName) chips.push({ key: 'person', label: state.personName, onRemove: () => presenter.removePersonChip() })
  return (
    <ul aria-label={t('activity_activeFilters')} className="contents">
      {chips.map((chip) => (
        <li key={chip.key} className="flex">
          <Badge variant="secondary" className="h-6 gap-1 rounded-md pr-1">
            {chip.label}
            <button
              aria-label={t('activity_removeFilter', { label: chip.label })}
              onClick={chip.onRemove}
              className="flex size-4 items-center justify-center rounded-sm outline-none hover:bg-foreground/10 focus-visible:ring-[3px] focus-visible:ring-ring/50"
            >
              <X className="size-3" />
            </button>
          </Badge>
        </li>
      ))}
      <li className="flex">
        <Button variant="ghost" size="sm" onClick={() => presenter.clearFilters()}>
          {t('activity_clearFilters')}
        </Button>
      </li>
    </ul>
  )
}
