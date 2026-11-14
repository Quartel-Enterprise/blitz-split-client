import { Banknote, Bitcoin, CreditCard, Ellipsis, Landmark } from 'lucide-react'
import type { JsPayingMethod } from 'blitzsplit-shared'
import type { ComponentType } from 'react'
import { PixIcon } from '@/components/PixIcon'
import { cardColorHexes, payingMethodLabel, payingMethodSubtitle, type CardColor, type PayingMethodType } from '@/lib/payingMethod'
import { cn } from '@/lib/utils'

type Look = { Icon: ComponentType<{ className?: string }>; tileClass: string }

const neutralTile = 'bg-muted text-muted-foreground'
const cardTile = 'text-white'

// Why: a Record, so a type added later can't fall through to another's glyph. Never a bank's or a card brand's logo.
const looks: Record<PayingMethodType, Look> = {
  credit_card: { Icon: CreditCard, tileClass: cardTile },
  debit_card: { Icon: CreditCard, tileClass: cardTile },
  cash: { Icon: Banknote, tileClass: neutralTile },
  // Why: the same tiles as Profile's keys, Pix's mark on the blue one and Bitcoin on the orange one.
  pix: { Icon: PixIcon, tileClass: 'bg-primary-container text-primary-container-foreground' },
  bank_transfer: { Icon: Landmark, tileClass: neutralTile },
  bitcoin: { Icon: Bitcoin, tileClass: 'bg-bitcoin-tile text-bitcoin-tile-foreground' },
  other: { Icon: Ellipsis, tileClass: neutralTile },
}

const sizes = {
  md: { tile: 'size-10 rounded-xl', icon: 'size-6' },
  sm: { tile: 'size-6 rounded-md', icon: 'size-4' },
}

/** A payment method's tile: a card filled with its color, Pix's and Bitcoin's as the keys draw them, the rest neutral. */
export function PayingMethodTile({
  type,
  cardColor,
  size = 'md',
  className,
}: {
  type: string
  cardColor: string | null | undefined
  size?: keyof typeof sizes
  className?: string
}) {
  const { Icon, tileClass } = looks[type as PayingMethodType] ?? looks.other
  const hex = cardColor ? cardColorHexes[cardColor as CardColor] : undefined
  return (
    <span
      aria-hidden
      className={cn('flex shrink-0 items-center justify-center', sizes[size].tile, tileClass, className)}
      style={tileClass === cardTile ? { backgroundColor: hex ?? cardColorHexes.graphite } : undefined}
    >
      {/* text-current: a menu item would otherwise grey every icon in it, the card's white glyph too */}
      <Icon className={cn('text-current', sizes[size].icon)} />
    </span>
  )
}

/** The method's tile and its label, the type under a named one when `showsSubtitle`. */
export function PayingMethodLabel({
  method,
  size = 'md',
  showsSubtitle = false,
  className,
}: {
  method: JsPayingMethod
  size?: keyof typeof sizes
  showsSubtitle?: boolean
  className?: string
}) {
  const subtitle = showsSubtitle ? payingMethodSubtitle(method) : null
  return (
    <span className={cn('flex min-w-0 items-center', size === 'md' ? 'gap-3' : 'gap-2', className)}>
      <PayingMethodTile type={method.type} cardColor={method.cardColor} size={size} />
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className={cn('truncate', size === 'md' && 'text-sm font-medium')}>{payingMethodLabel(method)}</span>
        {subtitle && <span className="truncate text-[13px] text-muted-foreground">{subtitle}</span>}
      </span>
    </span>
  )
}
