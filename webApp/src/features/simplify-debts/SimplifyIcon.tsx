import { Badge } from '@/components/ui/badge'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

/**
 * The symbol of "Simplificar dívidas", as the boards draw it: three points joined into one. Drawn like a lucide icon
 * (24 px grid, current color); with a label it is an image a screen reader names ("Par novo da simplificação").
 */
export function SimplifyIcon({ label, className }: { label?: string; className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      role={label ? 'img' : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
      className={cn('size-4 shrink-0', className)}
    >
      <circle cx="6" cy="5" r="2" />
      <circle cx="18" cy="5" r="2" />
      <circle cx="12" cy="19" r="2" />
      <path d="M7 7l4 10M17 7l-4 10" />
    </svg>
  )
}

/** "Dívidas simplificadas", the label of a group whose "Simplificar dívidas" is on, as "Por grupo" shows it (13.6). */
export function SimplifiedBadge({ className }: { className?: string }) {
  return (
    <Badge variant="secondary" className={cn('bg-primary/10 font-medium text-primary', className)}>
      <SimplifyIcon />
      {t('simplify_badge')}
    </Badge>
  )
}
