import { ProfilePhotoPresenter, type JsProfilePhotoState } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar, ProviderLogo, providerLabel } from '@/components/common'
import { PhotoCropDialog } from '@/components/PhotoCropDialog'
import { PhotoPickerInputs, usePhotoPicker } from '@/components/PhotoPicker'
import { PHOTO_VIEWER_SIZE, PhotoPreparing, PhotoShortcut, PhotoViewerDialog, PhotoViewerError, PhotoViewerShortcuts } from '@/components/PhotoViewer'
import { t } from '@/i18n'
import type { AccountProvider } from '@/lib/platform'

/**
 * The profile's photo big (`Route.ProfilePhoto`), as Bible Planner's viewer: the photo, or the initial on its color,
 * over a dimmed and blurred profile, with the shortcuts that change it at once under it. A picked photo opens "Ajustar
 * foto" over it.
 */
export function ProfilePhotoDialog() {
  const presenter = usePresenter(() => new ProfilePhotoPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsProfilePhotoState) => void) => p.observeState(onChange))
  if (!presenter || !state) return null
  return (
    <PhotoViewerDialog
      title={t('profilePhoto_title')}
      description={t('profilePhoto_description')}
      onDismiss={() => presenter.dismiss()}
      overlay={state.photoCrop && <PhotoCropDialog crop={state.photoCrop} presenter={presenter.crop} frame="circle" />}
    >
      {state.isLoaded && <ProfilePhotoBody presenter={presenter} state={state} />}
    </PhotoViewerDialog>
  )
}

function ProfilePhotoBody({ presenter, state }: { presenter: ProfilePhotoPresenter; state: JsProfilePhotoState }) {
  const photo = usePhotoPicker()
  const accountProvider = state.accountPhotoProvider as AccountProvider | null
  return (
    <div className="m-auto flex flex-col items-center gap-7 py-4">
      <div className="relative">
        <Avatar initial={state.avatarInitial} color={state.avatarColor} photoUrl={state.photoUrl} className={PHOTO_VIEWER_SIZE} />
        {state.isPreparingPhoto && <PhotoPreparing className="rounded-full" />}
      </div>
      {state.showsPhotoError && <PhotoViewerError message={t('editProfile_photoError')} />}
      <PhotoViewerShortcuts
        photo={photo}
        takePhotoLabel={t('editProfile_takePhoto')}
        choosePhotoLabel={t('editProfile_choosePhoto')}
        hasPhoto={state.hasPhoto}
        onRemove={() => presenter.removePhoto()}
      >
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
      </PhotoViewerShortcuts>
      <PhotoPickerInputs picker={photo} receiver={presenter} facing="user" />
    </div>
  )
}
