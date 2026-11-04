import { Camera, CameraOff, Check, Flashlight, FlashlightOff, ImageIcon, Loader2, RotateCcw, SwitchCamera } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { useCameras, useCameraStream, type CameraFacing } from '@/hooks/useCameraStream'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

interface Shot {
  blob: Blob
  url: string
}

const JPEG_QUALITY = 0.92

/**
 * "Tirar foto" on the web: the browser's camera live, a shutter, then the photo taken with "Tirar outra" and "Usar foto",
 * which hands its JPEG to `onCaptured` as a file picked would be (the profile's and the group's then go through "Ajustar
 * foto"). A dialog from the tablet up, a drawer on the phone. "Escolher arquivo" leaves for the file chooser, also once
 * the camera was refused. The camera stops as soon as it closes.
 */
export function CameraCaptureDialog({
  facing,
  title,
  onCaptured,
  onUnreadable,
  onChooseFile,
  onClose,
}: {
  facing: CameraFacing
  title?: string
  onCaptured: (jpeg: Int8Array) => void
  onUnreadable?: () => void
  onChooseFile?: () => void
  onClose: () => void
}) {
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const video = useRef<HTMLVideoElement | null>(null)
  // Why: the drawer mounts its video after this renders, so a new element re-renders the dialog to get the stream.
  const [, setVideoElement] = useState<HTMLVideoElement | null>(null)
  const attachVideo = useCallback((element: HTMLVideoElement | null) => {
    video.current = element
    setVideoElement(element)
  }, [])
  const [deviceId, setDeviceId] = useState<string | null>(null)
  const [shot, setShot] = useState<Shot | null>(null)
  const [isTorchOn, setIsTorchOn] = useState(false)
  const isConfirming = useRef(false)
  const { problem, track, isPlaying, hasTorch, retry } = useCameraStream(video, {
    isEnabled: true,
    facingMode: facing,
    deviceId,
    prefersHighResolution: true,
    isTorchOn,
  })
  // Why: listed again once the camera runs, since a browser names every camera (with its id) only after it was allowed.
  const cameraIds = (useCameras(track) ?? []).map((each) => each.deviceId).filter(Boolean)
  // Why: a camera facing the person shows as a mirror, as every camera app does; the photo itself is never mirrored.
  const isMirrored = track !== null && track.getSettings().facingMode !== 'environment'
  const [captureFailed, setCaptureFailed] = useState(false)
  const shownProblem = problem ?? (captureFailed ? 'failed' : null)

  useEffect(() => {
    if (!shot) return
    return () => URL.revokeObjectURL(shot.url)
  }, [shot])
  const takePhoto = () => {
    const element = video.current
    if (!element?.videoWidth) return
    const canvas = document.createElement('canvas')
    canvas.width = element.videoWidth
    canvas.height = element.videoHeight
    canvas.getContext('2d')?.drawImage(element, 0, 0)
    canvas.toBlob(
      (blob) => {
        if (blob) setShot({ blob, url: URL.createObjectURL(blob) })
        else setCaptureFailed(true)
      },
      'image/jpeg',
      JPEG_QUALITY,
    )
  }
  // Why: closes at once, so a second click on "Usar foto" never hands the same photo on twice.
  const confirmPhoto = async () => {
    if (!shot || isConfirming.current) return
    isConfirming.current = true
    onClose()
    try {
      onCaptured(new Int8Array(await shot.blob.arrayBuffer()))
    } catch {
      onUnreadable?.()
    }
  }
  // Why: steps through the cameras themselves, since two webcams report no facing to tell them apart.
  const switchCamera = () => {
    const current = cameraIds.indexOf(track?.getSettings().deviceId ?? '')
    setDeviceId(cameraIds[(current + 1) % cameraIds.length] ?? null)
  }
  const chooseFile = () => {
    onClose()
    // Why: still inside the click, so the browser lets the file chooser open.
    onChooseFile?.()
  }
  const tryAgain = () => {
    setCaptureFailed(false)
    retry()
  }

  const chooseFileButton = onChooseFile && (
    <Button type="button" variant="outline" onClick={chooseFile}>
      <ImageIcon aria-hidden /> {t('camera_chooseFile')}
    </Button>
  )
  const footer = shownProblem ? (
    <>
      {chooseFileButton}
      <Button type="button" onClick={tryAgain}>
        {t('camera_retry')}
      </Button>
    </>
  ) : shot ? (
    <>
      <Button type="button" variant="outline" onClick={() => setShot(null)}>
        <RotateCcw aria-hidden /> {t('camera_retake')}
      </Button>
      <Button type="button" onClick={() => void confirmPhoto()}>
        <Check aria-hidden /> {t('camera_use')}
      </Button>
    </>
  ) : (
    <>
      {chooseFileButton}
      <Button type="button" disabled={!isPlaying} onClick={takePhoto}>
        <Camera aria-hidden /> {t('camera_shutter')}
      </Button>
    </>
  )

  const body = (
    <div className={cn('relative mx-auto w-full overflow-hidden rounded-xl bg-camera text-white', isDesktop ? 'aspect-[4/3]' : 'aspect-[3/4] max-h-[55dvh]')}>
      {/* Always mounted, so a second try after a refusal has the video to show the camera in */}
      <video ref={attachVideo} muted playsInline className={cn('absolute inset-0 size-full object-contain', isMirrored && '-scale-x-100')} />
      {!isPlaying && !shownProblem && <Loader2 aria-label={t('common_loading')} className="absolute inset-0 m-auto size-8 animate-spin text-white/70" />}
      {shot && <img src={shot.url} alt={t('camera_preview')} className="absolute inset-0 size-full bg-camera object-contain" />}
      {shownProblem && (
        <div role="alert" className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-camera px-6 text-center">
          <CameraOff aria-hidden className="size-8 text-white/70" />
          {shownProblem === 'denied' && <p className="font-semibold">{t('camera_deniedTitle')}</p>}
          <p className="text-sm text-white/80">{t(shownProblem === 'denied' ? 'camera_deniedMessage' : 'camera_failed')}</p>
        </div>
      )}
      {!shot && !shownProblem && isPlaying && (
        <div className="absolute top-3 right-3 flex gap-2">
          {cameraIds.length > 1 && (
            <Button
              type="button"
              size="icon"
              variant="secondary"
              aria-label={t('camera_switch')}
              onClick={switchCamera}
              className="rounded-full bg-black/45 text-white hover:bg-black/60"
            >
              <SwitchCamera />
            </Button>
          )}
          {hasTorch && (
            <Button
              type="button"
              size="icon"
              variant="secondary"
              aria-pressed={isTorchOn}
              aria-label={t(isTorchOn ? 'camera_torchOff' : 'camera_torchOn')}
              onClick={() => setIsTorchOn((isOn) => !isOn)}
              className="rounded-full bg-black/45 text-white hover:bg-black/60"
            >
              {isTorchOn ? <FlashlightOff /> : <Flashlight />}
            </Button>
          )}
        </div>
      )}
    </div>
  )

  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      className="sm:max-w-lg"
      pane={{ title: title ?? t('camera_title'), body, footer }}
      onOpenChange={(open) => !open && onClose()}
    />
  )
}
