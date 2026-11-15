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
 */
export function PaidWithField({
  presenter,
  paidWith,
  showsAdd,
}: {
  presenter: SplitBillPresenter
  paidWith: JsPaidWith | null | undefined
  showsAdd: boolean
}) {
  if (!paidWith) {
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
