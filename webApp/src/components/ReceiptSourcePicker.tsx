import { Camera, ImageIcon } from 'lucide-react'
import { useEffect } from 'react'
import { PhotoPickerInputs, usePhotoPicker, type PhotoPicker } from '@/components/PhotoPicker'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import type { PhotoReceiver } from '@/lib/photoFile'

/**
 * What "Ler cupom fiscal"'s picker needs of a screen: Rachar conta's presenter and Início's both have it, since Início
 * asks where the photo comes from too, then opens Rachar conta reading it (beta feedback, part 3, item 16).
 */
export interface ReceiptSourcePresenter {
  scanReceipt(): void
  takeReceiptPhoto(): void
  chooseReceiptFile(): void
  skipReceiptChoice(): void
  dismissReceiptPicker(): void
  receiptPicked(photo: Int8Array): void
}

/** The request as the screen's state has it: the choice asked for, and the picker picked on it ("camera" or "files"). */
export interface ReceiptSourceState {
  showsReceiptSourceChoice: boolean
  receiptSource?: string | null
}

/** Where the receipt's photo comes from, and `pick`, which asks: "Tirar foto" or "Escolher foto". */
export interface ReceiptPicker extends PhotoPicker {
  pick: () => void
}

/*
 * "Ler cupom fiscal" (7.1e to 7.1g) on the web asks where the photo comes from first (beta feedback, part 2, item 7):
 * the browser's camera, as the apps' own, wherever the browser lists one (a webcam too), or the file chooser, the
 * phone's gallery. Without a camera the file chooser opens at once. A file that can't be read goes as empty bytes, which
 * read as a receipt that couldn't be read. Null until the screen's presenter and state are there.
 */
export function useReceiptPicker(presenter: ReceiptSourcePresenter | null, state: ReceiptSourceState | null | undefined): ReceiptPicker {
  const picker = usePhotoPicker()
  const { chooseFile, hasCamera, takePhoto, closeCamera } = picker
  const showsChoice = state?.showsReceiptSourceChoice ?? false
  const source = state?.receiptSource
  // Why: a choice asked for once the browser said it has no camera: there is nothing to ask, and a file chooser may be
  // refused without a fresh click, so the button stays to try again.
  useEffect(() => {
    if (!presenter || !showsChoice || hasCamera !== false) return
    chooseFile()
    presenter.skipReceiptChoice()
  }, [presenter, showsChoice, hasCamera, chooseFile])
  // Why: the camera opens and closes with the request, as the apps' does: it closes once the photo is read, or once the
  // screen can no longer read one (a group turned to bitcoin, a save, Início left for Rachar conta).
  useEffect(() => {
    if (source === 'camera') takePhoto()
    else closeCamera()
  }, [source, takePhoto, closeCamera])
  // Why: the file chooser opens inside the click and has no close to report, so its request ends as it opens.
  useEffect(() => {
    if (source === 'files') presenter?.dismissReceiptPicker()
  }, [presenter, source])
  const pick = () => {
    if (!presenter) return
    // Why: without a camera the file chooser opens inside the click, which a browser may require; the ViewModel
    // gets the tap and the skipped choice in the same task, so the choice never draws.
    if (hasCamera === false) chooseFile()
    presenter.scanReceipt()
    if (hasCamera === false) presenter.skipReceiptChoice()
  }
  // Why: the camera closed by hand, or left for a photo taken or a file, ends the request.
  return { ...picker, pick, closeCamera: () => presenter?.dismissReceiptPicker() }
}

export function ReceiptInput({ presenter, state, picker }: { presenter: ReceiptSourcePresenter; state: ReceiptSourceState; picker: ReceiptPicker }) {
  const receiver: PhotoReceiver = {
    photoPicked: (bytes) => presenter.receiptPicked(bytes),
    photoUnreadable: () => presenter.receiptPicked(new Int8Array(0)),
  }
  return (
    <>
      <PhotoPickerInputs picker={picker} receiver={receiver} facing="environment" title={t('splitBill_scanReceipt')} />
      {state.showsReceiptSourceChoice && picker.hasCamera && <ReceiptSourceDialog presenter={presenter} picker={picker} />}
    </>
  )
}

/** "Ler cupom fiscal": "Tirar foto" or "Escolher foto", a dialog from the tablet up, a drawer on the phone. */
function ReceiptSourceDialog({ presenter, picker }: { presenter: ReceiptSourcePresenter; picker: ReceiptPicker }) {
  const isDesktop = useMediaQuery('(min-width: 768px)')
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      className="sm:max-w-sm"
      onOpenChange={(open) => {
        if (!open) presenter.dismissReceiptPicker()
      }}
      pane={{
        title: t('splitBill_scanReceipt'),
        description: t('splitBill_scanReceiptHelp'),
        body: (
          <div className="flex flex-col gap-2">
            <Button
              type="button"
              variant="outline"
              size="lg"
              className="justify-start"
              onClick={() => presenter.takeReceiptPhoto()}
            >
              <Camera aria-hidden /> {t('splitBill_receiptTakePhoto')}
            </Button>
            <Button
              type="button"
              variant="outline"
              size="lg"
              className="justify-start"
              onClick={() => {
                // Why: still inside the click, so the browser lets the file chooser open.
                picker.chooseFile()
                presenter.chooseReceiptFile()
              }}
            >
              <ImageIcon aria-hidden /> {t('splitBill_receiptChoosePhoto')}
            </Button>
          </div>
        ),
        footer: null,
      }}
    />
  )
}
