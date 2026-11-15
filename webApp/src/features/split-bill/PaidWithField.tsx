import { Check, ChevronDown, CreditCard, Plus } from 'lucide-react'
import type { JsPaidWith, SplitBillPresenter } from 'blitzsplit-shared'
import { PayingMethodLabel, PayingMethodTile } from '@/components/PayingMethodTile'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { t } from '@/i18n'
import { payingMethodLabel } from '@/lib/payingMethod'

/**
 * "Paid with" under Split while this device's user pays the bill, as discreet as a chip (Pierre's call, feedback of
 * November 15, 2026): the method they paid with, or "None", whose menu picks another. With no method yet, "Add a
 * payment method" in its place, whose new method comes back picked. Only they see it, which screen readers hear.
 * While they don't pay it, a dimmed chip on "None" that still takes the click, which says why it can't change (Pierre's
 * call, beta feedback of November 15, 2026).
 */
export function PaidWithField({
  presenter,
  paidWith,
  showsAdd,
  showsLocked,
}: {
  presenter: SplitBillPresenter
  paidWith: JsPaidWith | null | undefined
  showsAdd: boolean
  showsLocked: boolean
}) {
  if (!paidWith) {
    if (showsLocked) {
      return (
        <Button
          type="button"
          variant="outline"
          size="sm"
          // Why: aria-disabled, never disabled, so the click still reaches the ViewModel and its toast.
          aria-disabled
          className="self-start rounded-full opacity-50"
          aria-label={`${t('payingMethod_paidWith')}: ${t('payingMethod_paidWithNone')}`}
          aria-description={t('payingMethod_paidWithOnlyPayers')}
          title={t('payingMethod_paidWithOnlyPayers')}
          onClick={() => presenter.lockedPaidWithClicked()}
        >
          <CreditCard aria-hidden className="text-muted-foreground" />
          <span className="truncate">{t('payingMethod_paidWithNone')}</span>
          <ChevronDown aria-hidden className="text-muted-foreground" />
        </Button>
      )
    }
    if (!showsAdd) return null
    return (
      <Button
        type="button"
        variant="secondary"
        size="sm"
        className="self-start rounded-full"
        aria-description={t('payingMethod_paidWithPrivate')}
        onClick={() => presenter.addPayingMethod()}
      >
        <Plus aria-hidden /> {t('payingMethod_add')}
      </Button>
    )
  }
  const picked = paidWith.methods.find((method) => method.id === paidWith.methodId)
  const pickedLabel = picked ? payingMethodLabel(picked) : t('payingMethod_paidWithNone')
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="max-w-full self-start rounded-full"
          aria-label={`${t('payingMethod_paidWith')}: ${pickedLabel}`}
          aria-description={t('payingMethod_paidWithPrivate')}
          title={t('payingMethod_paidWithPrivate')}
        >
          {picked ? (
            <PayingMethodTile type={picked.type} cardColor={picked.cardColor} size="xs" />
          ) : (
            <CreditCard aria-hidden className="text-muted-foreground" />
          )}
          <span className="truncate">{pickedLabel}</span>
          <ChevronDown aria-hidden className="text-muted-foreground" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="max-h-72 min-w-56 overflow-y-auto">
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
  )
}
