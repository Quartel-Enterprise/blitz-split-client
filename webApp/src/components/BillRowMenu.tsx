import { Copy, EllipsisVertical, Pencil, Share2, Trash2 } from 'lucide-react'
import type { JsBillMenu } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { t } from '@/i18n'

/** One option of a row's ⋮: a destructive one in the error color. */
export interface RowMenuOption {
  icon: ReactNode
  label: string
  isDestructive?: boolean
  onSelect: () => void
}

/**
 * A bill's ⋮ on the group's Expenses, Home's "Latest expenses" and "All expenses": Bill's own options in the same order,
 * plus Duplicate, so they don't need the bill opened first.
 */
export function billMenuOptions({
  onEdit,
  onDuplicate,
  onShare,
  onDelete,
}: {
  onEdit: () => void
  onDuplicate: () => void
  onShare: () => void
  onDelete: () => void
}): RowMenuOption[] {
  return [
    { icon: <Pencil />, label: t('groupDetail_entryEdit'), onSelect: onEdit },
    { icon: <Copy />, label: t('groupDetail_entryDuplicate'), onSelect: onDuplicate },
    { icon: <Share2 />, label: t('groupDetail_entryShare'), onSelect: onShare },
    { icon: <Trash2 />, label: t('groupDetail_entryDelete'), isDestructive: true, onSelect: onDelete },
  ]
}

/** The same options for a bill of any group, through the presenter's JsBillMenu (Home, "All expenses"). */
export function homeBillMenuOptions(menu: JsBillMenu, bill: { groupId: string; id: string }): RowMenuOption[] {
  return billMenuOptions({
    onEdit: () => menu.edit(bill.groupId, bill.id),
    onDuplicate: () => menu.duplicate(bill.groupId, bill.id),
    onShare: () => menu.share(bill.groupId, bill.id),
    onDelete: () => menu.askToDelete(bill.groupId, bill.id),
  })
}

/**
 * The ⋮ at the end of a row, whose caller keeps whether it is open, so a right click on the row opens it too. Its
 * clicks and keys never reach the row, which opens the bill.
 */
export function RowMenuButton({
  title,
  options,
  open,
  onOpenChange,
}: {
  title: string
  options: RowMenuOption[]
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <DropdownMenu open={open} onOpenChange={onOpenChange}>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" aria-label={t('groupDetail_entryOptions', { name: title })} onClick={(event) => event.stopPropagation()}>
          <EllipsisVertical />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-52" onClick={(event) => event.stopPropagation()}>
        {options.map((option) => (
          <DropdownMenuItem key={option.label} variant={option.isDestructive ? 'destructive' : 'default'} onSelect={option.onSelect}>
            {option.icon}
            {option.label}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
