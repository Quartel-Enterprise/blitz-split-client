import { useEffect, useRef, useState, type RefObject } from 'react'

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
}

// Why: reading continuously, the same QR in front of the camera is reported a few times a second, not on every frame.
const continuousInterval = 250

/**
 * Shows the back camera in `video` and reads QR codes from it through the browser's BarcodeDetector,
 * from the moment `isEnabled` turns on until it turns off or the component goes away.
 */
export function useQrCamera(video: RefObject<HTMLVideoElement | null>, { isEnabled, readsContinuously, onScanned, onStarted }: QrCameraOptions) {
  const [problem, setProblem] = useState<QrCameraProblem | null>(null)
  const [track, setTrack] = useState<MediaStreamTrack | null>(null)
  // The latest callbacks, so a parent re-render never restarts the camera
  const callbacks = useRef({ onScanned, onStarted })
  callbacks.current = { onScanned, onStarted }

  useEffect(() => {
    if (!isEnabled) return
    const Detector = window.BarcodeDetector
    if (!Detector || !navigator.mediaDevices?.getUserMedia) {
      setProblem('unsupported')
      return
    }
    setProblem(null)
    const detector = new Detector({ formats: ['qr_code'] })
    let stream: MediaStream | null = null
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
          callbacks.current.onScanned(code.rawValue)
          if (!readsContinuously) return
        }
      } catch {
        // The first frames can arrive before the video has any pixels; keep trying
      }
      next()
    }

    navigator.mediaDevices
      .getUserMedia({ video: { facingMode: 'environment' } })
      .then(async (media) => {
        // Why: the camera may answer after the component went away, and its light would stay on.
        if (!isActive) {
          media.getTracks().forEach((each) => each.stop())
          return
        }
        stream = media
        callbacks.current.onStarted?.()
        if (!video.current) return
        video.current.srcObject = media
        setTrack(media.getVideoTracks()[0] ?? null)
        try {
          await video.current.play()
        } catch {
          // Why: the camera was allowed, so a video that won't play is a failure, not a refusal of the camera.
          if (isActive) setProblem('failed')
          return
        }
        next()
      })
      .catch((error: unknown) => {
        if (!isActive) return
        const isRefused = error instanceof DOMException && (error.name === 'NotAllowedError' || error.name === 'SecurityError')
        setProblem(isRefused ? 'denied' : 'failed')
      })

    return () => {
      isActive = false
      window.clearTimeout(timer)
      cancelAnimationFrame(frame)
      stream?.getTracks().forEach((each) => each.stop())
      setTrack(null)
    }
  }, [isEnabled, readsContinuously, video])

  return { problem, track }
}
