import { useCallback, useEffect, useRef, useState, type RefObject } from 'react'

/** Which camera to ask for: the one facing the person (a selfie) or the one on the back. Desktops have only the first. */
export type CameraFacing = 'user' | 'environment'

/** Why the camera shows nothing: `denied` when the person or the browser refused it, `failed` for anything else. */
export type CameraProblem = 'denied' | 'failed'

interface CameraStreamOptions {
  isEnabled: boolean
  facingMode: CameraFacing
  // Why: a photo wants the camera's detail; reading a QR doesn't, and a smaller stream reads faster.
  prefersHighResolution?: boolean
  onStarted?: () => void
}

/**
 * Shows the camera in `video` from the moment `isEnabled` turns on until it turns off, the facing changes or the
 * component goes away, and then stops it, so the camera's light goes off. `retry` asks for the camera once more, since a
 * browser has no settings page to open after a refusal.
 */
export function useCameraStream(video: RefObject<HTMLVideoElement | null>, { isEnabled, facingMode, prefersHighResolution = false, onStarted }: CameraStreamOptions) {
  const [problem, setProblem] = useState<CameraProblem | null>(null)
  const [track, setTrack] = useState<MediaStreamTrack | null>(null)
  const [isPlaying, setIsPlaying] = useState(false)
  const [attempt, setAttempt] = useState(0)
  // The latest callback, so a parent re-render never restarts the camera
  const started = useRef(onStarted)
  started.current = onStarted

  useEffect(() => {
    if (!isEnabled) return
    if (!navigator.mediaDevices?.getUserMedia) {
      setProblem('failed')
      return
    }
    setProblem(null)
    let stream: MediaStream | null = null
    let isActive = true
    const constraints: MediaTrackConstraints = prefersHighResolution ? { facingMode, width: { ideal: 1920 }, height: { ideal: 1080 } } : { facingMode }

    navigator.mediaDevices
      .getUserMedia({ video: constraints })
      .then(async (media) => {
        // Why: the camera may answer after the component went away, and its light would stay on.
        if (!isActive) {
          media.getTracks().forEach((each) => each.stop())
          return
        }
        stream = media
        started.current?.()
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
        if (isActive) setIsPlaying(true)
      })
      .catch((error: unknown) => {
        if (!isActive) return
        const isRefused = error instanceof DOMException && (error.name === 'NotAllowedError' || error.name === 'SecurityError')
        setProblem(isRefused ? 'denied' : 'failed')
      })

    return () => {
      isActive = false
      stream?.getTracks().forEach((each) => each.stop())
      setTrack(null)
      setIsPlaying(false)
    }
  }, [isEnabled, facingMode, prefersHighResolution, video, attempt])

  const retry = useCallback(() => setAttempt((count) => count + 1), [])
  return { problem, track, isPlaying, retry }
}

/**
 * How many cameras the browser lists, null until it answered. A browser lists them before the camera is allowed (with
 * no names), so a computer without a webcam never offers to take a photo. Follows a webcam plugged in or out.
 */
export function useCameraCount(): number | null {
  const [count, setCount] = useState<number | null>(null)
  useEffect(() => {
    const devices = navigator.mediaDevices
    if (!devices?.enumerateDevices || !devices.getUserMedia) {
      setCount(0)
      return
    }
    let isActive = true
    const check = () => {
      devices
        .enumerateDevices()
        .then((list) => {
          if (isActive) setCount(list.filter((each) => each.kind === 'videoinput').length)
        })
        .catch(() => {
          if (isActive) setCount(0)
        })
    }
    check()
    devices.addEventListener('devicechange', check)
    return () => {
      isActive = false
      devices.removeEventListener('devicechange', check)
    }
  }, [])
  return count
}
