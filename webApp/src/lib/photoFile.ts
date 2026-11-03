import type { ChangeEvent } from 'react'

/** What a photo input hands the shared code: the file's bytes, or that it couldn't be read. */
export interface PhotoReceiver {
  photoPicked?: (jpeg: Int8Array) => void
  photoUnreadable?: () => void
}

/** Reads the photo an `<input type="file">` picked and hands its bytes to the presenter. */
export async function onPhotoFile(receiver: PhotoReceiver, event: ChangeEvent<HTMLInputElement>) {
  const file = event.target.files?.[0]
  // Why: picking the same file again still fires a change.
  event.target.value = ''
  if (!file) return
  try {
    receiver.photoPicked?.(new Int8Array(await file.arrayBuffer()))
  } catch {
    receiver.photoUnreadable?.()
  }
}
