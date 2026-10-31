import { Ellipsis, LogOut, SquareArrowOutUpRight, Trash2, UserPlus } from 'lucide-react'
import type { GroupsPresenter, JsGroupRow } from 'blitzsplit-shared'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { t } from '@/i18n'
import { formatMoney } from '@/lib/money'

/**
 * The ⋯ at the end of a row (5.4): Abrir grupo, Convidar pessoas, Sair do grupo and Apagar grupo. A right click on
 * the row opens it too, through `open`. Offline, Sair and Apagar say they need the internet; online, what blocks them
 * shows before the click, which still opens the dialog listing what is open.
 */
export function GroupRowMenu({
  presenter,
  group,
  isOnline,
  open,
  onOpenChange,
}: {
  presenter: GroupsPresenter
  group: JsGroupRow
  isOnline: boolean
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const leaveHint = !isOnline
    ? t('groupExit_needsInternet')
    : group.settleBeforeLeavingMinor > 0
      ? t('groups_leaveHint', { amount: formatMoney(group.settleBeforeLeavingMinor, group.currency, group.fractionDigits) })
      : null
  const deleteHint = !isOnline ? t('groupExit_needsInternet') : group.hasOpenDebts ? t('groups_deleteHint') : null
  return (
    <DropdownMenu open={open} onOpenChange={onOpenChange}>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          aria-label={t('groups_rowActions', { group: group.name })}
          className="text-muted-foreground"
          // Why: the row under it opens the group on a click.
          onClick={(event) => event.stopPropagation()}
        >
          <Ellipsis />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-60" onClick={(event) => event.stopPropagation()}>
        <DropdownMenuItem onSelect={() => presenter.openGroup(group.id)}>
          <SquareArrowOutUpRight />
          {t('groups_openGroup')}
        </DropdownMenuItem>
        <DropdownMenuItem onSelect={() => presenter.invite(group.id)}>
          <UserPlus />
          {t('groupDetail_invitePeople')}
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem disabled={!isOnline} onSelect={() => presenter.leaveGroup(group.id)}>
          <LogOut />
          <ActionLabel label={t('groupDetail_leaveGroup')} hint={leaveHint} />
        </DropdownMenuItem>
        <DropdownMenuItem variant="destructive" disabled={!isOnline} onSelect={() => presenter.deleteGroup(group.id)}>
          <Trash2 />
          <ActionLabel label={t('groupDetail_deleteGroup')} hint={deleteHint} />
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

function ActionLabel({ label, hint }: { label: string; hint: string | null }) {
  return (
    <span className="flex flex-col">
      {label}
      {hint && <span className="text-xs text-muted-foreground">{hint}</span>}
    </span>
  )
}
