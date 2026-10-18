import { useEffect, useRef, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'

interface DetectedBarcode {
  rawValue: string
}

interface BarcodeDetectorLike {
  detect(source: CanvasImageSource): Promise<DetectedBarcode[]>
}

declare global {
  interface Window {
    BarcodeDetector?: new (options: { formats: string[] }) => BarcodeDetectorLike
  }
}

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
  const [problem, setProblem] = useState<string | null>(null)
  // The latest callback, so a parent re-render never restarts the camera
  const onScannedRef = useRef(onScanned)
  onScannedRef.current = onScanned

  useEffect(() => {
    if (!open) return
    const Detector = window.BarcodeDetector
    if (!Detector || !navigator.mediaDevices?.getUserMedia) {
      setProblem('Este navegador não lê QR pela câmera. Digite o código do aparelho antigo.')
      return
    }
    setProblem(null)
    const detector = new Detector({ formats: ['qr_code'] })
    let stream: MediaStream | null = null
    let frame = 0
    let isActive = true

    const scan = async () => {
      if (!isActive || !video.current) return
      try {
        const [code] = await detector.detect(video.current)
        if (code?.rawValue) {
          onScannedRef.current(code.rawValue)
          return
        }
      } catch {
        // The first frames can arrive before the video has any pixels; keep trying
      }
      frame = requestAnimationFrame(scan)
    }

    navigator.mediaDevices
      .getUserMedia({ video: { facingMode: 'environment' } })
      .then(async (media) => {
        stream = media
        if (!isActive || !video.current) return
        video.current.srcObject = media
        await video.current.play()
        frame = requestAnimationFrame(scan)
      })
      .catch(() => setProblem('Não deu para abrir a câmera. Permita o acesso no navegador ou digite o código.'))

    return () => {
      isActive = false
      cancelAnimationFrame(frame)
      stream?.getTracks().forEach((track) => track.stop())
    }
  }, [open])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Escanear QR com a câmera</DialogTitle>
          <DialogDescription>No aparelho antigo: Perfil › Recuperação › Mostrar QR</DialogDescription>
        </DialogHeader>
        {problem ? (
          <p className="rounded-lg bg-muted p-4 text-sm text-muted-foreground">{problem}</p>
        ) : (
          <video ref={video} muted playsInline className="aspect-square w-full rounded-lg bg-black object-cover" />
        )}
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
