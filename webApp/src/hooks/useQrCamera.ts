import { useEffect, useRef, type RefObject } from 'react'
import { useCameraStream } from '@/hooks/useCameraStream'

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

/**
 * Why the camera can't read: `unsupported` without BarcodeDetector (Safari, Firefox), `denied` when the
 * person or the browser refused the camera, `failed` for anything else, such as no camera at all.
 */
export type QrCameraProblem = 'unsupported' | 'denied' | 'failed'

interface QrCameraOptions {
  isEnabled: boolean
  // Why: the invite reader keeps reading after a wrong QR; the recovery code needs only the first one.
  readsContinuously: boolean
  onScanned: (content: string) => void
  onStarted?: () => void
  isTorchOn?: boolean
}

// Why: reading continuously, the same QR in front of the camera is reported a few times a second, not on every frame.
const continuousInterval = 250

// Why: Safari and Firefox have no BarcodeDetector, so they can't read a QR from the camera.
export const canReadQr = Boolean(window.BarcodeDetector && navigator.mediaDevices?.getUserMedia)

/**
 * Shows the back camera in `video` (`useCameraStream`) and reads QR codes from it through the browser's
 * BarcodeDetector, from the moment `isEnabled` turns on until it turns off or the component goes away.
 */
export function useQrCamera(video: RefObject<HTMLVideoElement | null>, { isEnabled, readsContinuously, onScanned, onStarted, isTorchOn }: QrCameraOptions) {
  const { problem, track, isPlaying, hasTorch } = useCameraStream(video, { isEnabled: isEnabled && canReadQr, facingMode: 'environment', isTorchOn, onStarted })
  // The latest callback, so a parent re-render never restarts the reading
  const scanned = useRef(onScanned)
  scanned.current = onScanned

  useEffect(() => {
    const Detector = window.BarcodeDetector
    if (!isPlaying || !Detector) return
    const detector = new Detector({ formats: ['qr_code'] })
    let timer = 0
    let frame = 0
    let isActive = true

    const next = () => {
      if (!isActive) return
      if (readsContinuously) timer = window.setTimeout(scan, continuousInterval)
      else frame = requestAnimationFrame(scan)
    }
    const scan = async () => {
      if (!isActive || !video.current) return
      try {
        const [code] = await detector.detect(video.current)
        if (isActive && code?.rawValue) {
          scanned.current(code.rawValue)
          if (!readsContinuously) return
        }
      } catch {
        // The first frames can arrive before the video has any pixels; keep trying
      }
      next()
    }
    next()

    return () => {
      isActive = false
      window.clearTimeout(timer)
      cancelAnimationFrame(frame)
    }
  }, [isPlaying, readsContinuously, video])

  const qrProblem: QrCameraProblem | null = isEnabled && !canReadQr ? 'unsupported' : problem
  return { problem: qrProblem, track, hasTorch }
}
