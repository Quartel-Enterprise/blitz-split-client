import { Camera, ImageIcon, Loader2, Trash2, XIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { Dialog as DialogPrimitive } from 'radix-ui'
import { usePhotoPicker } from '@/components/PhotoPicker'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

/**
 * A photo big, as Bible Planner's viewer, for the profile's and the group's: the photo over a dimmed and blurred page,
 * with the shortcuts that change it at once under it. Esc, ✕, a click outside them and the browser's back close it.
 * `overlay` draws over the viewer, as "Ajustar foto" does for a picked photo.
 */
export function PhotoViewerDialog({
  title,
  description,
  onDismiss,
  overlay,
  children,
}: {
  title: string
  description: string
  onDismiss: () => void
  overlay?: ReactNode
  children: ReactNode
}) {
  return (
    <DialogPrimitive.Root open onOpenChange={(open) => !open && onDismiss()}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-[#08080e]/80 backdrop-blur-md data-[state=open]:animate-in data-[state=open]:fade-in-0" />
        <DialogPrimitive.Content
          // Why: m-auto centers the photo and still scrolls it whole where the window is short (a phone lying down).
          className="fixed inset-0 z-50 flex overflow-y-auto p-4 text-white outline-none data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95"
          // Why: a click on the backdrop, around the photo and its shortcuts, closes it as a tap outside does.
          onClick={(event) => event.target === event.currentTarget && onDismiss()}
        >
          <DialogPrimitive.Title className="sr-only">{title}</DialogPrimitive.Title>
          <DialogPrimitive.Description className="sr-only">{description}</DialogPrimitive.Description>
          {children}
          <DialogPrimitive.Close
            aria-label={t('common_close')}
            className="absolute top-3.5 right-3.5 flex size-11 items-center justify-center rounded-full bg-black/30 outline-none hover:bg-black/45 focus-visible:ring-[3px] focus-visible:ring-white/60"
          >
            <XIcon className="size-6" />
          </DialogPrimitive.Close>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
      {overlay}
    </DialogPrimitive.Root>
  )
}

/** The photo's size in the viewer: 260 px only where both sides have 600, so a phone lying down keeps the compact one. */
export const PHOTO_VIEWER_SIZE =
  'size-[200px] text-[84px] shadow-2xl [@media(min-width:600px)_and_(min-height:600px)]:size-[260px] [@media(min-width:600px)_and_(min-height:600px)]:text-[110px]'

/** The spinner over a photo that is being prepared; `className` gives it the photo's shape. */
export function PhotoPreparing({ className }: { className?: string }) {
  return (
    <span className={cn('absolute inset-0 flex items-center justify-center bg-black/40', className)}>
      <Loader2 className="size-8 animate-spin" />
    </span>
  )
}

/**
 * "Camera" where there is one, "Gallery" and, with a photo, "Remove": the shortcuts both viewers share, after the
 * ones each adds (`children`). `photo` is the picker whose inputs the viewer renders.
 */
export function PhotoViewerShortcuts({
  photo,
  takePhotoLabel,
  choosePhotoLabel,
  hasPhoto,
  onRemove,
  children,
}: {
  photo: ReturnType<typeof usePhotoPicker>
  takePhotoLabel: string
  choosePhotoLabel: string
  hasPhoto: boolean
  onRemove: () => void
  children?: ReactNode
}) {
  return (
    <div className="flex flex-wrap justify-center gap-4">
      {children}
      {photo.hasCamera && (
        <PhotoShortcut label={t('profilePhoto_camera')} accessibleLabel={takePhotoLabel} onClick={photo.takePhoto}>
          <Camera />
        </PhotoShortcut>
      )}
      <PhotoShortcut label={t('profilePhoto_gallery')} accessibleLabel={choosePhotoLabel} onClick={photo.chooseFile}>
        <ImageIcon />
      </PhotoShortcut>
      {hasPhoto && (
        <PhotoShortcut
          label={t('profilePhoto_remove')}
          accessibleLabel={t('editProfile_removePhoto')}
          className="bg-[#ff7878]/20 text-[#ff9a9a]"
          labelClassName="text-[#ff9a9a]"
          onClick={onRemove}
        >
          <Trash2 />
        </PhotoShortcut>
      )}
    </div>
  )
}

export function PhotoViewerError({ message }: { message: string }) {
  return (
    <p role="alert" className="text-center text-sm text-[#ff9a9a]">
      {message}
    </p>
  )
}

export function PhotoShortcut({
  label,
  accessibleLabel,
  className,
  labelClassName,
  onClick,
  children,
}: {
  label: string
  accessibleLabel: string
  className?: string
  labelClassName?: string
  onClick: () => void
  children: ReactNode
}) {
  return (
    <button
      type="button"
      aria-label={accessibleLabel}
      onClick={onClick}
      className="group flex w-[76px] flex-col items-center gap-2 rounded-xl outline-none focus-visible:ring-[3px] focus-visible:ring-white/60"
    >
      <span
        aria-hidden
        className={cn(
          'flex size-[58px] items-center justify-center rounded-full bg-white/15 transition-colors group-hover:brightness-110 [&_svg]:size-[26px]',
          className,
        )}
      >
        {children}
      </span>
      <span aria-hidden className={cn('text-xs font-medium', labelClassName)}>
        {label}
      </span>
    </button>
  )
}
