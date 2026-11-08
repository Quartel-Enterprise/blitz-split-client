import { GroupPhotoPresenter, type JsGroupPhotoState, type JsRoute } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { PhotoCropDialog } from '@/components/PhotoCropDialog'
import { PhotoPickerInputs, usePhotoPicker } from '@/components/PhotoPicker'
import { PHOTO_VIEWER_SIZE, PhotoPreparing, PhotoViewerDialog, PhotoViewerError, PhotoViewerShortcuts } from '@/components/PhotoViewer'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { t } from '@/i18n'

/**
 * The group's photo big (`Route.GroupPhoto`), the profile's viewer for a group: the photo, or the initials on the
 * color, in the group's rounded square, with the shortcuts that change it at once under it. A picked photo opens
 * "Ajustar foto" over it, framed in the same square.
 */
export function GroupPhotoDialog({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new GroupPhotoPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsGroupPhotoState) => void) => p.observeState(onChange))
  if (!presenter || !state) return null
  return (
    <PhotoViewerDialog
      title={t('groupPhoto_title')}
      description={t('profilePhoto_description')}
      onDismiss={() => presenter.dismiss()}
      overlay={state.photoCrop && <PhotoCropDialog crop={state.photoCrop} presenter={presenter.crop} frame="rounded-square" />}
    >
      {state.isLoaded && <GroupPhotoBody presenter={presenter} state={state} />}
    </PhotoViewerDialog>
  )
}

function GroupPhotoBody({ presenter, state }: { presenter: GroupPhotoPresenter; state: JsGroupPhotoState }) {
  const photo = usePhotoPicker()
  return (
    <div className="m-auto flex flex-col items-center gap-7 py-4">
      <div className="relative">
        <GroupAvatar initials={state.initials} color={state.avatarColor} photoUrl={state.photoUrl} className={PHOTO_VIEWER_SIZE} />
        {state.isPreparingPhoto && <PhotoPreparing className="rounded-[32%]" />}
      </div>
      {state.showsPhotoError && <PhotoViewerError message={t('editGroup_photoError')} />}
      <PhotoViewerShortcuts
        photo={photo}
        takePhotoLabel={t('editGroup_takePhoto')}
        choosePhotoLabel={t('editGroup_usePhoto')}
        hasPhoto={state.hasPhoto}
        onRemove={() => presenter.removePhoto()}
      />
      <PhotoPickerInputs picker={photo} receiver={presenter} facing="environment" />
    </div>
  )
}
