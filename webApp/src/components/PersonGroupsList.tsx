import type { JsPersonGroup } from 'blitzsplit-shared'
import { balanceTone } from '@/features/balances/BalanceCard'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { t } from '@/i18n'
import { formatSignedMoney } from '@/lib/money'
import { cn } from '@/lib/utils'

/**
 * "Por grupo": what each group adds to the net with someone, then the net, "Líquido, já compensado" over several groups
 * and "Total" for one. Saldos opens it in a row, and Registrar pagamento shows it when settling up across groups.
 */
export function PersonGroupsList({
  groups,
  netMinor,
  currency,
  fractionDigits,
  label,
  className,
}: {
  groups: JsPersonGroup[]
  netMinor: number
  currency: string
  fractionDigits: number
  label?: string
  className?: string
}) {
  const signed = (minor: number) => formatSignedMoney(minor, currency, fractionDigits)
  return (
    <div role="region" aria-label={label} className={cn('flex flex-col rounded-lg bg-muted/60 px-3', className)}>
      {groups.map((group) => (
        <span key={group.groupId} className="flex items-center gap-2.5 border-b py-2 text-sm">
          <GroupAvatar initials={group.initials} color={group.color} className="size-6 rounded-md text-[10px]" />
          <span className="flex min-w-0 flex-1 flex-col">
            <span className="truncate">{group.name}</span>
            <span className="text-xs text-muted-foreground">{t(group.amountMinor < 0 ? 'globalBalances_rowYouOwe' : 'globalBalances_rowOwesYou')}</span>
          </span>
          <span className={cn('tabular-nums', balanceTone(group.amountMinor))}>{signed(group.amountMinor)}</span>
        </span>
      ))}
      <span className="flex items-center justify-between gap-3 py-2.5 text-sm font-semibold">
        <span>{t(groups.length > 1 ? 'globalBalances_net' : 'globalBalances_total')}</span>
        <span className={cn('tabular-nums', balanceTone(netMinor))}>{signed(netMinor)}</span>
      </span>
    </div>
  )
}
