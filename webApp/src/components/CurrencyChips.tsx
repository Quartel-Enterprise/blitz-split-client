import { t } from '@/i18n'
import { cn } from '@/lib/utils'

/**
 * The currency selector of a group's screens: one chip per currency the group has money in, labeled by its code
 * ("BRL"), the one picked pressed. It draws only with more than one, since a single currency has nothing to pick.
 */
export function CurrencyChips({
  currencies,
  selected,
  onSelect,
  className,
}: {
  currencies: readonly string[]
  selected: string
  onSelect: (currency: string) => void
  className?: string
}) {
  if (currencies.length < 2) return null
  return (
    <div role="group" aria-label={t('currency_chipsLabel')} className={cn('flex flex-wrap items-center gap-2', className)}>
      {currencies.map((currency) => {
        const isSelected = currency === selected
        return (
          <button
            key={currency}
            type="button"
            aria-pressed={isSelected}
            onClick={() => {
              if (!isSelected) onSelect(currency)
            }}
            className={cn(
              'inline-flex h-8 items-center rounded-full border px-3 text-[13px] font-medium outline-none transition-colors focus-visible:ring-[3px] focus-visible:ring-ring',
              isSelected ? 'border-transparent bg-primary/15 text-primary' : 'bg-card hover:bg-accent/60',
            )}
          >
            {currency}
          </button>
        )
      })}
    </div>
  )
}
