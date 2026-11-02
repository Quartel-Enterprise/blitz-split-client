import {
  BookOpen,
  Car,
  Dumbbell,
  Gift,
  Heart,
  House,
  Music4,
  Ellipsis,
  PawPrint,
  Plane,
  Repeat,
  ShoppingBag,
  ShoppingCart,
  Sun,
  Ticket,
  UtensilsCrossed,
  Volleyball,
  Wine,
  type LucideIcon,
} from 'lucide-react'
import type { JsCategory } from 'blitzsplit-shared'
import { t, type MessageKey } from '@/i18n'
import { avatarStyle } from '@/lib/avatar'
import { cn } from '@/lib/utils'

/** The lucide icon of each CategoryIcon code, the same pictures the other platforms draw. */
const icons: Record<string, LucideIcon> = {
  food: UtensilsCrossed,
  cart: ShoppingCart,
  home: House,
  car: Car,
  plane: Plane,
  ticket: Ticket,
  repeat: Repeat,
  bag: ShoppingBag,
  heart: Heart,
  book: BookOpen,
  sun: Sun,
  ball: Volleyball,
  paw: PawPrint,
  gift: Gift,
  glass: Wine,
  music: Music4,
  gym: Dumbbell,
  dots: Ellipsis,
}

/** An icon code a newer app added draws as "Outros". */
export function categoryIconOf(code: string): LucideIcon {
  return icons[code] ?? Ellipsis
}

/** "Talheres", "Pata": what a screen reader says for each icon of "Nova categoria". */
export function categoryIconLabel(code: string): string {
  const key = `categoryIcon_${code}` as MessageKey
  return key in iconKeys ? t(key) : code
}

const iconKeys: Record<string, true> = Object.fromEntries(Object.keys(icons).map((code) => [`categoryIcon_${code}`, true]))

/** What a category is called: the app's in the page's language, the group's as someone named it. */
export function categoryName(category: JsCategory): string {
  if (category.appKey) return t(`category_${category.appKey.replace('.', '_')}` as MessageKey)
  return category.name ?? ''
}

/** "Alimentação › Mercado", or just the name of a main category. */
export function categoryPath(category: JsCategory, parent: JsCategory | null | undefined): string {
  if (!parent) return categoryName(category)
  return t('category_path', { parent: categoryName(parent), name: categoryName(category) })
}

/** A category's icon on its main category's color, the same in a bill's row, the picker and the pie. */
export function CategoryBadge({ category, className }: { category: JsCategory; className?: string }) {
  const Icon = categoryIconOf(category.icon)
  return (
    <span aria-hidden style={avatarStyle(category.color)} className={cn('avatar flex size-10 shrink-0 items-center justify-center rounded-xl', className)}>
      <Icon className="size-[55%]" />
    </span>
  )
}

/** The pie's slice color, the avatar color's background in light and dark. */
export function categoryFill(color: string): React.CSSProperties {
  return avatarStyle(color)
}
