import { useCallback, useEffect, useMemo, useRef, useState, type RefObject } from 'react'

/** Which camera to ask for: the one facing the person (a selfie) or the one on the back. Desktops have only the first. */
export type CameraFacing = 'user' | 'environment'

/** Why the camera shows nothing: `denied` when the person or the browser refused it, `failed` for anything else. */
export type CameraProblem = 'denied' | 'failed'

// Why: Chrome on Android exposes the flashlight as a constraint of the camera's track; TypeScript doesn't know it yet.
interface TorchCapabilities extends MediaTrackCapabilities {
  torch?: boolean
}

interface CameraStreamOptions {
  isEnabled: boolean
  facingMode: CameraFacing
  // Why: two webcams report no facing, so switching between them asks for the device itself.
  deviceId?: string | null
  // Why: a photo wants the camera's detail; reading a QR doesn't, and a smaller stream reads faster.
  prefersHighResolution?: boolean
  isTorchOn?: boolean
  onStarted?: () => void
}

/**
 * Shows the camera in `video` from the moment `isEnabled` turns on until it turns off, the camera asked for changes or
 * the component goes away, and then stops it, so the camera's light goes off. `retry` asks for the camera once more,
 * since a browser has no settings page to open after a refusal. `hasTorch` says whether the camera has a flashlight,
 * which `isTorchOn` turns on.
 */
export function useCameraStream(
  video: RefObject<HTMLVideoElement | null>,
  { isEnabled, facingMode, deviceId = null, prefersHighResolution = false, isTorchOn = false, onStarted }: CameraStreamOptions,
) {
  const [problem, setProblem] = useState<CameraProblem | null>(null)
  const [track, setTrack] = useState<MediaStreamTrack | null>(null)
  const [isPlaying, setIsPlaying] = useState(false)
  const [attempt, setAttempt] = useState(0)
  const stream = useRef<MediaStream | null>(null)
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
    let isActive = true
    const constraints: MediaTrackConstraints = {
      ...(deviceId ? { deviceId: { exact: deviceId } } : { facingMode }),
      ...(prefersHighResolution && { width: { ideal: 1920 }, height: { ideal: 1080 } }),
    }

    navigator.mediaDevices
      .getUserMedia({ video: constraints })
      .then(async (media) => {
        // Why: the camera may answer after the component went away, and its light would stay on.
        if (!isActive) {
          media.getTracks().forEach((each) => each.stop())
          return
        }
        stream.current = media
        started.current?.()
        setTrack(media.getVideoTracks()[0] ?? null)
        // Why: a video not mounted yet gets the stream, and plays, from the effect below once it is.
        const element = video.current
        if (!element) return
        element.srcObject = media
        try {
          await element.play()
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
      stream.current?.getTracks().forEach((each) => each.stop())
      stream.current = null
      setTrack(null)
      setIsPlaying(false)
    }
  }, [isEnabled, facingMode, deviceId, prefersHighResolution, video, attempt])

  // Why: the video may be mounted only after the camera answered, or be a new element while it runs (a dialog becoming
  // a drawer as a phone turns), which shows nothing until it gets the stream again.
  useEffect(() => {
    const element = video.current
    const media = stream.current
    if (!element || !media || element.srcObject === media) return
    element.srcObject = media
    element
      .play()
      .then(() => {
        if (stream.current === media) setIsPlaying(true)
      })
      .catch(() => {})
  })

  const hasTorch = useMemo(() => (track?.getCapabilities?.() as TorchCapabilities | undefined)?.torch === true, [track])
  const litTrack = useRef<MediaStreamTrack | null>(null)
  useEffect(() => {
    if (!track || !hasTorch) return
    // Why: untouched until turned on, since new constraints may drop the resolution the camera was asked for.
    if (!isTorchOn && litTrack.current !== track) return
    litTrack.current = track
    track.applyConstraints({ advanced: [{ torch: isTorchOn } as MediaTrackConstraintSet] }).catch(() => {})
  }, [track, hasTorch, isTorchOn])

  const retry = useCallback(() => setAttempt((count) => count + 1), [])
  return { problem, track, isPlaying, hasTorch, retry }
}

/**
 * The cameras the browser lists, null until it answered. A browser lists one before the camera is allowed (with no
 * names nor ids), so a computer without a webcam never offers to take a photo; `refresh` lists them again, all of them
 * once the camera was allowed. Follows a webcam plugged in or out.
 */
export function useCameras(refresh?: unknown): MediaDeviceInfo[] | null {
  const [cameras, setCameras] = useState<MediaDeviceInfo[] | null>(null)
  useEffect(() => {
    const devices = navigator.mediaDevices
    if (!devices?.enumerateDevices || !devices.getUserMedia) {
      setCameras([])
      return
    }
    let isActive = true
    const check = () => {
      devices
        .enumerateDevices()
        .then((list) => {
          if (isActive) setCameras(list.filter((each) => each.kind === 'videoinput'))
        })
        .catch(() => {
          if (isActive) setCameras([])
        })
    }
    check()
    devices.addEventListener('devicechange', check)
    return () => {
      isActive = false
      devices.removeEventListener('devicechange', check)
    }
  }, [refresh])
  return cameras
}
