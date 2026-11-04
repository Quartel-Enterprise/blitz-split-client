import { Check } from 'lucide-react'
import { DropdownMenuGroup, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator } from '@/components/ui/dropdown-menu'
import { t } from '@/i18n'
import { currencyLabel, isCryptoCurrency } from '@/lib/currency'

interface CurrencyMenuItemsProps {
  currencies: readonly string[]
  selected: string
  onSelect: (currency: string) => void
}

/** "Moeda"'s choices in the shared code's order, the cryptocurrencies under their own header. */
export function CurrencyMenuItems({ currencies, selected, onSelect }: CurrencyMenuItemsProps) {
  const item = (currency: string) => (
    <DropdownMenuItem key={currency} onSelect={() => onSelect(currency)}>
      <span className="flex-1">{currencyLabel(currency)}</span>
      {currency === selected && <Check />}
    </DropdownMenuItem>
  )
  const crypto = currencies.filter(isCryptoCurrency)
  return (
    <>
      {currencies.filter((currency) => !isCryptoCurrency(currency)).map(item)}
      {crypto.length > 0 && (
        <>
          <DropdownMenuSeparator />
          <DropdownMenuGroup>
            <DropdownMenuLabel className="text-xs text-muted-foreground">{t('currency_cryptoHeader')}</DropdownMenuLabel>
            {crypto.map(item)}
          </DropdownMenuGroup>
        </>
      )}
    </>
  )
}
