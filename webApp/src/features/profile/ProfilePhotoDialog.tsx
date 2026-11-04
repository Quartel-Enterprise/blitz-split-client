import { Camera, ImageIcon, Loader2, Trash2, XIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { Dialog as DialogPrimitive } from 'radix-ui'
import { ProfilePhotoPresenter, type JsProfilePhotoState } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar, ProviderLogo, providerLabel } from '@/components/common'
import { PhotoCropDialog } from '@/components/PhotoCropDialog'
import { PhotoPickerInputs, usePhotoPicker } from '@/components/PhotoPicker'
import { t } from '@/i18n'
import type { AccountProvider } from '@/lib/platform'
import { cn } from '@/lib/utils'

/**
 * The profile's photo big (`Route.ProfilePhoto`), as Bible Planner's viewer: the photo, or the initial on its color,
 * over a dimmed and blurred profile, with the shortcuts that change it at once under it. Esc, ✕, a click outside them
 * and the browser's back close it; a picked photo opens "Ajustar foto" over it.
 */
export function ProfilePhotoDialog() {
  const presenter = usePresenter(() => new ProfilePhotoPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsProfilePhotoState) => void) => p.observeState(onChange))
  if (!presenter || !state) return null
  return (
    <DialogPrimitive.Root open onOpenChange={(open) => !open && presenter.dismiss()}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-[#08080e]/55 backdrop-blur-md data-[state=open]:animate-in data-[state=open]:fade-in-0" />
        <DialogPrimitive.Content
          // Why: m-auto centers the photo and still scrolls it whole where the window is short (a phone lying down).
          className="fixed inset-0 z-50 flex overflow-y-auto p-4 text-white outline-none data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95"
          // Why: a click on the backdrop, around the photo and its shortcuts, closes it as a tap outside does.
          onClick={(event) => event.target === event.currentTarget && presenter.dismiss()}
        >
          <DialogPrimitive.Title className="sr-only">{t('profilePhoto_title')}</DialogPrimitive.Title>
          <DialogPrimitive.Description className="sr-only">{t('profilePhoto_description')}</DialogPrimitive.Description>
          {state.isLoaded && <ProfilePhotoBody presenter={presenter} state={state} />}
          <DialogPrimitive.Close
            aria-label={t('common_close')}
            className="absolute top-3.5 right-3.5 flex size-11 items-center justify-center rounded-full bg-black/30 outline-none hover:bg-black/45 focus-visible:ring-[3px] focus-visible:ring-white/60"
          >
            <XIcon className="size-6" />
          </DialogPrimitive.Close>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
      {state.photoCrop && <PhotoCropDialog crop={state.photoCrop} presenter={presenter.crop} frame="circle" />}
    </DialogPrimitive.Root>
  )
}

function ProfilePhotoBody({ presenter, state }: { presenter: ProfilePhotoPresenter; state: JsProfilePhotoState }) {
  const photo = usePhotoPicker()
  const accountProvider = state.accountPhotoProvider as AccountProvider | null
  return (
    <div className="m-auto flex flex-col items-center gap-7 py-4">
      <div className="relative">
        <Avatar
          initial={state.avatarInitial}
          color={state.avatarColor}
          photoUrl={state.photoUrl}
          // Why: 260 px only where both sides have 600, so a phone lying down keeps the compact photo.
          className="size-[200px] text-[84px] shadow-2xl [@media(min-width:600px)_and_(min-height:600px)]:size-[260px] [@media(min-width:600px)_and_(min-height:600px)]:text-[110px]"
        />
        {state.isPreparingPhoto && (
          <span className="absolute inset-0 flex items-center justify-center rounded-full bg-black/40">
            <Loader2 className="size-8 animate-spin" />
          </span>
        )}
      </div>
      {state.showsPhotoError && (
        <p role="alert" className="text-center text-sm text-[#ff9a9a]">
          {t('editProfile_photoError')}
        </p>
      )}
      <div className="flex flex-wrap justify-center gap-4">
        {accountProvider && (
          <PhotoShortcut
            label={providerLabel(accountProvider, true)}
            accessibleLabel={t('editProfile_useAccountPhoto', { provider: providerLabel(accountProvider, true) })}
            className="bg-white text-black [&_svg]:size-6"
            onClick={() => presenter.useAccountPhoto()}
          >
            <ProviderLogo provider={accountProvider} />
          </PhotoShortcut>
        )}
        {photo.hasCamera && (
          <PhotoShortcut label={t('profilePhoto_camera')} accessibleLabel={t('editProfile_takePhoto')} onClick={photo.takePhoto}>
            <Camera />
          </PhotoShortcut>
        )}
        <PhotoShortcut label={t('profilePhoto_gallery')} accessibleLabel={t('editProfile_choosePhoto')} onClick={photo.chooseFile}>
          <ImageIcon />
        </PhotoShortcut>
        {state.hasPhoto && (
          <PhotoShortcut
            label={t('profilePhoto_remove')}
            accessibleLabel={t('editProfile_removePhoto')}
            className="bg-[#ff7878]/20 text-[#ff9a9a]"
            labelClassName="text-[#ff9a9a]"
            onClick={() => presenter.removePhoto()}
          >
            <Trash2 />
          </PhotoShortcut>
        )}
      </div>
      <PhotoPickerInputs picker={photo} receiver={presenter} facing="user" />
    </div>
  )
}

function PhotoShortcut({
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
