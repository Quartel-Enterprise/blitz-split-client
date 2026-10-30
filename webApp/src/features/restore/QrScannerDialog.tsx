import { useRef } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { useQrCamera } from '@/hooks/useQrCamera'
import { t } from '@/i18n'

interface QrScannerDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onScanned: (content: string) => void
}

/**
 * Reads the old device's recovery QR with the camera through the browser's BarcodeDetector. Browsers
 * without it (Safari, Firefox) get a message pointing to the typed code instead.
 */
export function QrScannerDialog({ open, onOpenChange, onScanned }: QrScannerDialogProps) {
  const video = useRef<HTMLVideoElement>(null)
  const { problem } = useQrCamera(video, { isEnabled: open, readsContinuously: false, onScanned })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{t('restore_scanQrTitle')}</DialogTitle>
          <DialogDescription>{t('restore_scanQrBody')}</DialogDescription>
        </DialogHeader>
        {problem ? (
          <p className="rounded-lg bg-muted p-4 text-sm text-muted-foreground">{t(problem === 'unsupported' ? 'qrScanner_unsupported' : 'qrScanner_cameraFailed')}</p>
        ) : (
          <video ref={video} muted playsInline className="aspect-square w-full rounded-lg bg-black object-cover" />
        )}
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {t('common_cancel')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
