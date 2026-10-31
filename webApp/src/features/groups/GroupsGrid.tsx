import { LayoutGrid, List } from 'lucide-react'
import type { GroupsPresenter, JsGroupRow, JsGroupsState } from 'blitzsplit-shared'
import type { CSSProperties, ReactNode } from 'react'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { Faces, GroupBalance, QueuedBadge, lastActivityText, membersText } from '@/features/groups/GroupRowParts'
import { t, type MessageKey } from '@/i18n'
import { cn } from '@/lib/utils'

const gridSizeLabels: Record<string, MessageKey> = {
  compact: 'groups_gridSizeCompact',
  standard: 'groups_gridSizeStandard',
  large: 'groups_gridSizeLarge',
}

/**
 * Each size's minimum card width, a phone's and from the tablet up: the columns are as many as fit the page's width
 * (a phone 3, 2 and 1; a desktop 5, 4 and 3), never a number picked.
 */
const minCardWidths: Record<string, { phone: string; wide: string }> = {
  compact: { phone: '104px', wide: '180px' },
  standard: { phone: '150px', wide: '240px' },
  large: { phone: '300px', wide: '340px' },
}

const cardSizes: Record<string, { avatar: string; name: string; amount: string; padding: string }> = {
  compact: { avatar: 'size-8 text-xs', name: 'text-sm', amount: 'text-[15px]', padding: 'p-3' },
  standard: { avatar: 'size-10 text-sm', name: 'text-[15px]', amount: 'text-lg', padding: 'p-4' },
  large: { avatar: 'size-12 text-base', name: 'text-[17px]', amount: 'text-[22px]', padding: 'p-5' },
}

/**
 * "Exibição" (5.5): the toggle group Lista / Grade and the Select of the grid's size, saved in this browser. On a
 * phone the Select only shows with the grid; elsewhere it stays, disabled, while the list is picked.
 */
export function GroupsDisplayControls({ presenter, state }: { presenter: GroupsPresenter; state: JsGroupsState }) {
  return (
    <div className="ml-auto flex items-center gap-2 md:ml-0">
      <ToggleGroup
        type="single"
        variant="outline"
        aria-label={t('groups_display')}
        value={state.display}
        // Why: a click on the pressed item would unpress it; the display is always one of the two.
        onValueChange={(display) => display && presenter.changeDisplay(display)}
      >
        <ToggleGroupItem value="list" aria-label={t('groups_displayList')} title={t('groups_displayList')}>
          <List />
        </ToggleGroupItem>
        <ToggleGroupItem value="grid" aria-label={t('groups_displayGrid')} title={t('groups_displayGrid')}>
          <LayoutGrid />
        </ToggleGroupItem>
      </ToggleGroup>
      <div className={cn(!state.showsGrid && 'hidden md:block')}>
        <Select value={state.gridSize} onValueChange={(size) => presenter.changeGridSize(size)} disabled={!state.canPickGridSize}>
          <SelectTrigger aria-label={t('groups_gridSize')} className="w-auto">
            <span className="text-muted-foreground">{t('groups_gridSizePrefix')}</span>
            <SelectValue />
          </SelectTrigger>
          <SelectContent position="popper" align="end">
            {Object.keys(gridSizeLabels).map((size) => (
              <SelectItem key={size} value={size}>
                {t(gridSizeLabels[size])}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  )
}

interface GroupsGridProps {
  presenter: GroupsPresenter
  groups: JsGroupRow[]
  gridSize: string
  menuOf: (group: JsGroupRow) => ReactNode
  onContextMenu: (groupId: string) => void
}

/**
 * The grid (5.6): each open group as a card with the row's content stacked: its avatar, "na fila" and the ⋯, its
 * name in up to two lines, the faces (and, in Grande, the last activity), then the balance with its word. A click
 * opens the group, and a right click or the ⋯ holds its actions, as on the table.
 */
export function GroupsGrid({ presenter, groups, gridSize, menuOf, onContextMenu }: GroupsGridProps) {
  const widths = minCardWidths[gridSize] ?? minCardWidths.standard
  const sizes = cardSizes[gridSize] ?? cardSizes.standard
  return (
    <ul
      aria-label={t('groups_title')}
      style={{ '--card-min-phone': widths.phone, '--card-min-wide': widths.wide } as CSSProperties}
      className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,var(--card-min-phone)),1fr))] gap-4 md:grid-cols-[repeat(auto-fill,minmax(min(100%,var(--card-min-wide)),1fr))]"
    >
      {groups.map((group) => (
        <li
          key={group.id}
          onClick={() => presenter.openGroup(group.id)}
          onContextMenu={(event) => {
            event.preventDefault()
            onContextMenu(group.id)
          }}
          className={cn('flex min-w-0 cursor-pointer flex-col gap-2 rounded-xl border bg-card hover:bg-accent', sizes.padding)}
        >
          <span className="flex items-start gap-2">
            <GroupAvatar initials={group.initials} color={group.color} className={sizes.avatar} />
            {group.isPending && <QueuedBadge />}
            <span className="-mt-1.5 -mr-1.5 ml-auto">{menuOf(group)}</span>
          </span>
          <button
            onClick={(event) => {
              event.stopPropagation()
              presenter.openGroup(group.id)
            }}
            className={cn('line-clamp-2 rounded-sm text-left leading-snug font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring', sizes.name)}
          >
            {group.name}
          </button>
          {gridSize !== 'compact' && (
            <span className="flex min-w-0 items-center gap-2 text-[13px] text-muted-foreground">
              <span aria-label={membersText(group.memberCount)} className="shrink-0">
                <span aria-hidden>
                  <Faces group={group} />
                </span>
              </span>
              {gridSize === 'large' && <span className="truncate">{lastActivityText(group)}</span>}
            </span>
          )}
          <span className="mt-auto">
            <GroupBalance group={group} isStacked amountClassName={sizes.amount} />
          </span>
        </li>
      ))}
    </ul>
  )
}
