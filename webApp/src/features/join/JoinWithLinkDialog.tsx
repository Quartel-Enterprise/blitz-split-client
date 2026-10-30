import { CameraOff, ClipboardPaste, Flashlight, FlashlightOff, TriangleAlert, X } from 'lucide-react'
import { JoinWithLinkPresenter, type JsJoinWithLinkState } from 'blitzsplit-shared'
import { useEffect, useRef, useState, type RefObject } from 'react'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

const formId = 'join-with-link-form'
const inputId = 'join-with-link-input'
const errorId = 'join-with-link-error'
const linkPlaceholder = 'blitzsplit.com/g/…'
// Why: the reader keeps reading after a wrong QR, a few times a second rather than on every frame.
const scanInterval = 250

// Why: Firefox only reads the clipboard from an extension, so "Paste" shows where the browser lets the page read it.
const canReadClipboard = typeof navigator !== 'undefined' && typeof navigator.clipboard?.readText === 'function'

// Why: Chrome on Android exposes the flashlight as a constraint of the camera's track; TypeScript doesn't know it yet.
interface TorchCapabilities extends MediaTrackCapabilities {
  torch?: boolean
}

type CameraProblem = 'unsupported' | 'failed'

/**
 * "Entrar com link ou QR": the camera reads an invite's QR through the browser's BarcodeDetector, and the
 * link can be pasted or typed under it. A dialog on desktop and tablet, a drawer on the phone. Closing it any
 * way (Esc, the overlay, the close button) is the ViewModel's back.
 */
export function JoinWithLinkDialog() {
  const presenter = usePresenter(() => new JoinWithLinkPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsJoinWithLinkState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  // Why: a browser has no settings page to open, so "Try again" asks for the camera once more.
  const [attempt, setAttempt] = useState(0)
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), () => setAttempt((count) => count + 1))
  if (!presenter || !state) return null

  const onOpenChange = (open: boolean) => {
    if (!open) presenter.close()
  }
  const body = (
    <div className="flex flex-col gap-5">
      <CameraPane key={attempt} presenter={presenter} state={state} isDesktop={isDesktop} />
      <PastePanel presenter={presenter} state={state} />
    </div>
  )

  return isDesktop ? (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{t('join_linkTitle')}</DialogTitle>
          <DialogDescription className="sr-only">{t('join_linkHint')}</DialogDescription>
        </DialogHeader>
        {body}
      </DialogContent>
    </Dialog>
  ) : (
    <Drawer open onOpenChange={onOpenChange} repositionInputs={false}>
      <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
        <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          <DrawerTitle>{t('join_linkTitle')}</DrawerTitle>
          <DrawerDescription className="sr-only">{t('join_linkHint')}</DrawerDescription>
        </DrawerHeader>
        <div className="overflow-y-auto px-4 pb-6">{body}</div>
      </DrawerContent>
    </Drawer>
  )
}

function CameraPane({ presenter, state, isDesktop }: { presenter: JoinWithLinkPresenter; state: JsJoinWithLinkState; isDesktop: boolean }) {
  const video = useRef<HTMLVideoElement>(null)
  const { problem, hasTorch } = useInviteCamera(video, presenter, state.isTorchOn)
  const isDenied = state.cameraAccess === 'denied'

  return (
    <div className={cn('relative w-full overflow-hidden rounded-xl bg-neutral-950 text-white', isDesktop ? 'aspect-square' : 'aspect-[4/3]')}>
      {/* Always mounted, so a second try after a refusal has the video to show the camera in */}
      <video ref={video} muted playsInline className="absolute inset-0 size-full object-cover" />
      {problem || isDenied ? (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-neutral-950 px-6 text-center">
          <CameraOff aria-hidden className="size-8 text-white/70" />
          {isDenied && !problem && <p className="font-semibold">{t('join_linkDeniedTitle')}</p>}
          <p className="text-sm text-white/80">
            {problem === 'unsupported' ? t('join_linkUnsupported') : problem === 'failed' ? t('join_linkCameraFailed') : t('join_linkDeniedMessage')}
          </p>
          {isDenied && !problem && (
            <Button variant="secondary" onClick={() => presenter.openSettings()}>
              {t('join_retry')}
            </Button>
          )}
        </div>
      ) : (
        <>
          {/* Darker toward the edges, so the corners and the hint read over any picture */}
          <div aria-hidden className="absolute inset-0 bg-[radial-gradient(circle,transparent_35%,rgb(0_0_0/0.55)_100%)]" />
          <ViewfinderCorners isInvalid={state.isQrInvalid} />
          {state.isQrInvalid ? (
            <div role="alert" className="absolute inset-x-3 bottom-3 flex gap-2 rounded-lg bg-destructive px-3 py-2 text-sm text-white">
              <TriangleAlert aria-hidden className="mt-0.5 size-4 shrink-0" />
              <div>
                <p className="font-semibold">{t('join_linkInvalidQr')}</p>
                <p className="text-white/90">{t('join_linkInvalidQrHint')}</p>
              </div>
            </div>
          ) : (
            <p className="absolute inset-x-3 bottom-4 text-center text-sm font-medium [text-shadow:0_1px_3px_rgb(0_0_0/0.6)]">{t('join_linkHint')}</p>
          )}
          {hasTorch && (
            <Button
              type="button"
              size="icon"
              variant="secondary"
              aria-pressed={state.isTorchOn}
              aria-label={t(state.isTorchOn ? 'join_linkTorchOff' : 'join_linkTorchOn')}
              onClick={() => presenter.toggleTorch()}
              className="absolute top-3 right-3 rounded-full bg-black/45 text-white hover:bg-black/60"
            >
              {state.isTorchOn ? <FlashlightOff /> : <Flashlight />}
            </Button>
          )}
        </>
      )}
    </div>
  )
}

// The four rounded corners of the viewfinder, not a whole frame; the error color while a wrong QR is in front of it.
function ViewfinderCorners({ isInvalid }: { isInvalid: boolean }) {
  const corner = 'M 2 30 V 16 A 14 14 0 0 1 16 2 H 30'
  return (
    <svg
      aria-hidden
      viewBox="0 0 100 100"
      className={cn('absolute top-1/2 left-1/2 size-3/5 -translate-x-1/2 -translate-y-[55%] transition-colors', isInvalid ? 'text-red-300' : 'text-white')}
      fill="none"
      stroke="currentColor"
      strokeWidth={3}
      strokeLinecap="round"
    >
      {[0, 90, 180, 270].map((degrees) => (
        <path key={degrees} d={corner} transform={`rotate(${degrees} 50 50)`} />
      ))}
    </svg>
  )
}

/**
 * Opens the back camera and hands each QR it reads to the ViewModel, which tells an invite from anything
 * else and keeps reading after a wrong one. The browser's own prompt is the permission: its answer goes to
 * the ViewModel, while a browser without BarcodeDetector (Safari, Firefox) or without a camera stays here.
 */
function useInviteCamera(video: RefObject<HTMLVideoElement | null>, presenter: JoinWithLinkPresenter, isTorchOn: boolean) {
  const [problem, setProblem] = useState<CameraProblem | null>(null)
  const [track, setTrack] = useState<MediaStreamTrack | null>(null)
  const hasTorch = (track?.getCapabilities?.() as TorchCapabilities | undefined)?.torch === true

  useEffect(() => {
    const Detector = window.BarcodeDetector
    if (!Detector || !navigator.mediaDevices?.getUserMedia) {
      setProblem('unsupported')
      return
    }
    const detector = new Detector({ formats: ['qr_code'] })
    let stream: MediaStream | null = null
    let timer = 0
    let isActive = true

    const scan = async () => {
      if (!isActive || !video.current) return
      try {
        const [code] = await detector.detect(video.current)
        if (isActive && code?.rawValue) presenter.scan(code.rawValue)
      } catch {
        // The first frames can arrive before the video has any pixels; keep trying
      }
      if (isActive) timer = window.setTimeout(scan, scanInterval)
    }

    navigator.mediaDevices
      .getUserMedia({ video: { facingMode: 'environment' } })
      .then(async (media) => {
        stream = media
        if (!isActive) return
        presenter.changeCameraAccess(true)
        if (!video.current) return
        video.current.srcObject = media
        setTrack(media.getVideoTracks()[0] ?? null)
        await video.current.play()
        timer = window.setTimeout(scan, scanInterval)
      })
      .catch((error: unknown) => {
        if (!isActive) return
        // Why: only a refusal is the camera being denied; no camera at all leaves the paste under it.
        if (error instanceof DOMException && (error.name === 'NotAllowedError' || error.name === 'SecurityError')) {
          presenter.changeCameraAccess(false)
        } else {
          setProblem('failed')
        }
      })

    return () => {
      isActive = false
      window.clearTimeout(timer)
      stream?.getTracks().forEach((each) => each.stop())
    }
  }, [presenter, video])

  useEffect(() => {
    if (!track || !hasTorch) return
    track.applyConstraints({ advanced: [{ torch: isTorchOn } as MediaTrackConstraintSet] }).catch(() => {})
  }, [track, hasTorch, isTorchOn])

  return { problem, hasTorch }
}

function PastePanel({ presenter, state }: { presenter: JoinWithLinkPresenter; state: JsJoinWithLinkState }) {
  const [link, setLink] = useDraft(state.link, (value) => presenter.changeLink(value))
  const paste = () => {
    navigator.clipboard
      .readText()
      .then((text) => presenter.paste(text))
      // The browser refused to read the clipboard (permission denied): the field stays as it is
      .catch(() => {})
  }

  return (
    <form
      id={formId}
      className="flex flex-col gap-3"
      onSubmit={(event) => {
        event.preventDefault()
        presenter.open()
      }}
    >
      <div>
        <h3 className="font-semibold">{t('join_linkPasteTitle')}</h3>
        <p className="text-sm text-muted-foreground">{t('join_linkPasteSubtitle')}</p>
      </div>
      <div className="flex flex-col gap-2">
        <Label htmlFor={inputId}>{t('join_linkLabel')}</Label>
        <div className="flex gap-2">
          <Input
            id={inputId}
            // Why: not type="url", whose own check would refuse "blitzsplit.com/g/…" without the ViewModel's word.
            inputMode="url"
            enterKeyHint="go"
            value={link}
            placeholder={linkPlaceholder}
            spellCheck={false}
            autoComplete="off"
            autoCapitalize="none"
            aria-invalid={state.isLinkInvalid}
            aria-describedby={state.isLinkInvalid ? errorId : undefined}
            onChange={(event) => setLink(event.target.value)}
          />
          {link !== '' ? (
            <Button type="button" variant="outline" size="icon" aria-label={t('join_linkClear')} onClick={() => presenter.clear()}>
              <X />
            </Button>
          ) : (
            canReadClipboard && (
              <Button type="button" variant="outline" onClick={paste}>
                <ClipboardPaste /> {t('join_linkPaste')}
              </Button>
            )
          )}
        </div>
        {state.isLinkInvalid && (
          <p id={errorId} role="alert" className="text-[13px] text-destructive">
            {t('join_linkInvalid')}
          </p>
        )}
      </div>
      {state.canOpen && (
        <Button type="submit" size="lg">
          {t('join_linkOpen')}
        </Button>
      )}
    </form>
  )
}
