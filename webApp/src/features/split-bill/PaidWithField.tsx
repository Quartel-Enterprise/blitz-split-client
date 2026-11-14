import { Check, ChevronsUpDown } from 'lucide-react'
import type { JsPaidWith, SplitBillPresenter } from 'blitzsplit-shared'
import { controlClass } from '@/components/fields'
import { PayingMethodLabel } from '@/components/PayingMethodTile'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { t } from '@/i18n'

const paidWithId = 'split-bill-paid-with'
const paidWithHelpId = 'split-bill-paid-with-help'

/**
 * "Paid with", under "Who paid" while the ViewModel shows it: the person's own method they paid with, or "None". Only
 * they see it, which the caption under it says.
 */
export function PaidWithField({ presenter, paidWith }: { presenter: SplitBillPresenter; paidWith: JsPaidWith }) {
  const picked = paidWith.methods.find((method) => method.id === paidWith.methodId)
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={paidWithId} className="text-sm font-medium">
        {t('payingMethod_paidWith')}
      </label>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button id={paidWithId} type="button" aria-describedby={paidWithHelpId} className={controlClass}>
            {picked ? (
              <PayingMethodLabel method={picked} size="sm" className="flex-1" />
            ) : (
              <span className="flex-1 truncate">{t('payingMethod_paidWithNone')}</span>
            )}
            <ChevronsUpDown aria-hidden className="size-4 text-muted-foreground" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start" className="max-h-72 w-(--radix-dropdown-menu-trigger-width) overflow-y-auto">
          <DropdownMenuItem onSelect={() => presenter.pickPaidWith(null)}>
            <span className="flex-1">{t('payingMethod_paidWithNone')}</span>
            {!picked && <Check />}
          </DropdownMenuItem>
          {paidWith.methods.map((method) => (
            <DropdownMenuItem key={method.id} onSelect={() => presenter.pickPaidWith(method.id)}>
              <PayingMethodLabel method={method} size="sm" className="flex-1" />
              {method.id === picked?.id && <Check />}
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
      <p id={paidWithHelpId} className="text-[13px] text-muted-foreground">
        {t('payingMethod_paidWithPrivate')}
      </p>
    </div>
  )
}
