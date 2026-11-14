import { getStackedExtraCount, getStackedFaceCount } from 'blitzsplit-shared'
import { Avatar } from '@/components/common'
import { cn } from '@/lib/utils'

/** One face of a stack, whoever it stands for: a member, a payer, someone in a split. */
export type StackFace = { initials: string; color: string; photoUrl?: string | null }

const sizes = {
  xs: { face: 'size-5 text-[9px]', extra: 'h-5 min-w-5 px-1 text-[9px]', overlap: '-ml-1.5' },
  sm: { face: 'size-6 text-[10px]', extra: 'h-6 min-w-6 px-1 text-[10px]', overlap: '-ml-2' },
  md: { face: 'size-7 text-[11px]', extra: 'h-7 min-w-7 px-1.5 text-[11px]', overlap: '-ml-2' },
  lg: { face: 'size-9 text-xs', extra: 'h-9 min-w-9 px-1.5 text-xs', overlap: '-ml-2.5' },
}

/** Everyone at once, cut to the shared FaceStack rule's two bubbles: both faces of two, the first and "+N" of three or more. */
export function stackOf<T>(people: T[]): { faces: T[]; extraCount: number } {
  return { faces: people.slice(0, getStackedFaceCount(people.length)), extraCount: getStackedExtraCount(people.length) }
}

/**
 * Every stack of faces in the app, as Android and Apple draw it (beta feedback): at most two bubbles, each ringed in the
 * surface behind it (`ring`, a Tailwind ring color) so its neighbor's edge reads, never shrunk by a tight row. Screen
 * readers skip it; the row around it says who. `faces` and `extraCount` come from the ViewModel or `stackOf`.
 */
export function AvatarStack({
  faces,
  extraCount,
  size = 'sm',
  ring = 'ring-card',
  className,
}: {
  faces: StackFace[]
  extraCount: number
  size?: keyof typeof sizes
  ring?: string
  className?: string
}) {
  const classes = sizes[size]
  return (
    <span aria-hidden className={cn('flex shrink-0 items-center', className)}>
      {faces.map((face, index) => (
        <Avatar key={index} initial={face.initials} color={face.color} photoUrl={face.photoUrl} className={cn(classes.face, 'ring-2', ring, index > 0 && classes.overlap)} />
      ))}
      {extraCount > 0 && (
        <span
          className={cn(
            'flex shrink-0 items-center justify-center rounded-full bg-muted font-semibold whitespace-nowrap text-muted-foreground tabular-nums ring-2',
            ring,
            classes.extra,
            faces.length > 0 && classes.overlap,
          )}
        >
          +{extraCount}
        </span>
      )}
    </span>
  )
}
