import { avatarStyle } from '@/lib/avatar'
import { cn } from '@/lib/utils'

/** A group's avatar: its initials on one of the palette's colors, in a rounded square so it never reads as a person. */
export function GroupAvatar({ initials, color, className }: { initials: string; color: string; className?: string }) {
  return (
    <span aria-hidden style={avatarStyle(color)} className={cn('avatar flex shrink-0 items-center justify-center rounded-[32%] font-extrabold', className)}>
      {initials}
    </span>
  )
}
