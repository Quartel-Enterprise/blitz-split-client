import type { JsPersonGroup } from 'blitzsplit-shared'
import { balanceTone } from '@/features/balances/BalanceCard'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { SimplifiedBadge } from '@/features/simplify-debts/SimplifyIcon'
import { t } from '@/i18n'
import { formatSignedMoney } from '@/lib/money'
import { cn } from '@/lib/utils'

/**
 * "Por grupo": what each group adds to the net with someone, then the net, "Líquido, já compensado" over several groups
 * and "Total" for one. Saldos opens it in a row, and Registrar pagamento shows it when settling up across groups.
 * Where onGroupClick is given, a group's badge and name open it. A group whose "Simplificar dívidas" is on says so.
 */
export function PersonGroupsList({
  groups,
  netMinor,
  currency,
  fractionDigits,
  label,
  className,
  onGroupClick,
}: {
  groups: JsPersonGroup[]
  netMinor: number
  currency: string
  fractionDigits: number
  label?: string
  className?: string
  onGroupClick?: (groupId: string) => void
}) {
  const signed = (minor: number) => formatSignedMoney(minor, currency, fractionDigits)
  return (
    <div role="region" aria-label={label} className={cn('flex flex-col rounded-lg bg-muted/60 px-3', className)}>
      {groups.map((group) => {
        const content = (
          <>
            <GroupAvatar initials={group.initials} color={group.color} photoUrl={group.photoUrl} className="size-6 rounded-md text-[10px]" />
            <span className="flex min-w-0 flex-1 flex-col text-left">
              <span className="truncate">{group.name}</span>
              <span className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-muted-foreground">
                {t(group.amountMinor < 0 ? 'globalBalances_rowYouOwe' : 'globalBalances_rowOwesYou')}
                {group.isSimplified && <SimplifiedBadge />}
              </span>
            </span>
            <span className={cn('tabular-nums', balanceTone(group.amountMinor))}>{signed(group.amountMinor)}</span>
          </>
        )
        return onGroupClick ? (
          <button
            key={group.groupId}
            type="button"
            onClick={() => onGroupClick(group.groupId)}
            className="-mx-3 flex items-center gap-2.5 border-b px-3 py-2 text-sm outline-none hover:bg-accent/50 focus-visible:ring-[3px] focus-visible:ring-inset focus-visible:ring-ring/50"
          >
            {content}
          </button>
        ) : (
          <span key={group.groupId} className="flex items-center gap-2.5 border-b py-2 text-sm">
            {content}
          </span>
        )
      })}
      <span className="flex items-center justify-between gap-3 py-2.5 text-sm font-semibold">
        <span>{t(groups.length > 1 ? 'globalBalances_net' : 'globalBalances_total')}</span>
        <span className={cn('tabular-nums', balanceTone(netMinor))}>{signed(netMinor)}</span>
      </span>
    </div>
  )
}
