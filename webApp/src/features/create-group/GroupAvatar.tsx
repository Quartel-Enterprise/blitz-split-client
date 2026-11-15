import type { HTMLAttributes } from 'react'
import { AvatarPhoto } from '@/components/common'
import { avatarStyle, GROUP_SHAPE } from '@/lib/avatar'
import { cn } from '@/lib/utils'

/**
 * A group's avatar: its initials on one of the palette's colors, or its photo, in a rounded square so it never reads
 * as a person. Only Android draws the group's shape. The initials stay under the photo, so they show while it loads or
 * when it fails to. Other attributes reach the square, such as the `data-group-avatar` mark of a shared transition.
 */
export function GroupAvatar({
  initials,
  color,
  photoUrl,
  className,
  ...attributes
}: { initials: string; color: string; photoUrl?: string | null; className?: string } & Omit<HTMLAttributes<HTMLSpanElement>, 'color' | 'style'>) {
  return (
    <span {...attributes} aria-hidden style={avatarStyle(color)} className={cn('avatar relative flex shrink-0 items-center justify-center overflow-hidden font-extrabold', GROUP_SHAPE, className)}>
      {initials}
      {photoUrl && <AvatarPhoto key={photoUrl} src={photoUrl} />}
    </span>
  )
}
