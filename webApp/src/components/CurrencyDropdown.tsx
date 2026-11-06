import { ChevronDown } from 'lucide-react'
import { CurrencyMenuItems } from '@/components/CurrencyMenuItems'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { currencyLabel } from '@/lib/currency'

interface CurrencyDropdownProps {
  id: string
  describedBy: string
  currencies: readonly string[]
  cryptoCurrencies: readonly string[]
  selected: string
  onSelect: (currency: string) => void
}

/** The group's default currency field of New group and Edit group: a button naming it that opens the choices. */
export function CurrencyDropdown({ id, describedBy, currencies, cryptoCurrencies, selected, onSelect }: CurrencyDropdownProps) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button id={id} type="button" variant="outline" aria-describedby={describedBy} className="justify-between">
          {currencyLabel(selected)}
          <ChevronDown className="text-muted-foreground" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="max-h-72 overflow-y-auto">
        <CurrencyMenuItems currencies={currencies} cryptoCurrencies={cryptoCurrencies} selected={selected} onSelect={onSelect} />
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
