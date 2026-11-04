import { useCallback, useRef, useState, type RefObject } from 'react'
import { CameraCaptureDialog } from '@/components/CameraCaptureDialog'
import { useCameras, type CameraFacing } from '@/hooks/useCameraStream'
import { onPhotoFile, type PhotoReceiver } from '@/lib/photoFile'

/** Where a photo comes from: the file chooser always, the camera only while the browser lists one. */
export interface PhotoPicker {
  input: RefObject<HTMLInputElement | null>
  /** Null until the browser said whether it has a camera. */
  hasCamera: boolean | null
  isCameraOpen: boolean
  chooseFile: () => void
  takePhoto: () => void
  closeCamera: () => void
}

/** A screen's photo source: hand it to `PhotoPickerInputs`, and open it with `chooseFile` or, with a camera, `takePhoto`. */
export function usePhotoPicker(): PhotoPicker {
  const input = useRef<HTMLInputElement>(null)
  const cameras = useCameras()
  const [isCameraOpen, setIsCameraOpen] = useState(false)
  // Why: stable, so an effect that opens the picker by itself (Início's "Ler cupom fiscal") runs once.
  const chooseFile = useCallback(() => input.current?.click(), [])
  const takePhoto = useCallback(() => setIsCameraOpen(true), [])
  const closeCamera = useCallback(() => setIsCameraOpen(false), [])
  return { input, hasCamera: cameras === null ? null : cameras.length > 0, isCameraOpen, chooseFile, takePhoto, closeCamera }
}

/**
 * The hidden file input and, while open, the camera, both handing the photo to `receiver` the same way, so a photo
 * taken goes through the same steps as one picked ("Ajustar foto" for a profile or a group).
 */
export function PhotoPickerInputs({ picker, receiver, facing, title }: { picker: PhotoPicker; receiver: PhotoReceiver; facing: CameraFacing; title?: string }) {
  return (
    <>
      <input ref={picker.input} type="file" accept="image/*" hidden tabIndex={-1} aria-hidden onChange={(event) => void onPhotoFile(receiver, event)} />
      {picker.isCameraOpen && (
        <CameraCaptureDialog
          facing={facing}
          title={title}
          onCaptured={(jpeg) => receiver.photoPicked?.(jpeg)}
          // Why: called on the receiver, since a presenter's method reads its own `this`.
          onUnreadable={() => receiver.photoUnreadable?.()}
          onChooseFile={picker.chooseFile}
          onClose={picker.closeCamera}
        />
      )}
    </>
  )
}
