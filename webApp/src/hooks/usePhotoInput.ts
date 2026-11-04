import { useRef } from 'react'
import { useMediaQuery } from '@/hooks/useMediaQuery'

/**
 * A profile photo's hidden `<input type="file">` (its `ref`): "Take photo" opens the camera where `canTakePhoto`,
 * "Choose photo" the file chooser. Editar perfil's menu and the photo viewer's shortcuts open it the same way.
 */
export function usePhotoInput() {
  const ref = useRef<HTMLInputElement>(null)
  // Why: only a touch device has a camera the "capture" attribute can open; a desktop browser shows a file chooser.
  const canTakePhoto = useMediaQuery('(pointer: coarse)')
  const choosePhoto = (useCamera: boolean) => {
    const input = ref.current
    if (!input) return
    if (useCamera) input.setAttribute('capture', 'user')
    else input.removeAttribute('capture')
    input.click()
  }
  return { ref, canTakePhoto, choosePhoto }
}
