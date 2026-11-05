import { Camera, CameraOff, ClipboardPaste, Flashlight, FlashlightOff, ScanQrCode, TriangleAlert, X } from 'lucide-react'
import { JoinWithLinkPresenter, type JsJoinWithLinkState, type JsRoute } from 'blitzsplit-shared'
import { useEffect, useRef, useState, type ReactNode, type RefObject } from 'react'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useDraft } from '@/hooks/useDraft'
import { useCameras } from '@/hooks/useCameraStream'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { canReadQr, useQrCamera } from '@/hooks/useQrCamera'
import { t, type MessageKey } from '@/i18n'
import { canReadClipboard } from '@/lib/platform'
import { cn } from '@/lib/utils'

const formId = 'join-with-link-form'
const inputId = 'join-with-link-input'
const errorId = 'join-with-link-error'
const linkPlaceholder = 'blitzsplit.com/g/…'

/**
 * "Join with an invite": the camera reads an invite's QR through the browser's BarcodeDetector, and the link can be
 * pasted or typed under it. The camera only starts after "Open camera", so opening the dialog never asks the browser
 * for it. A dialog on desktop and tablet, a drawer on the phone. Closing it any way (Esc, the overlay, the close
 * button) is the ViewModel's back.
 */
export function JoinWithLinkDialog({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new JoinWithLinkPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsJoinWithLinkState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  // Why: a browser has no settings page to open, so "Try again" asks for the camera once more.
  const [attempt, setAttempt] = useState(0)
  useActions(
    presenter,
    (p, onAction: (action: string) => void) => p.observeActions(onAction),
    (action) => {
      if (action === 'open-camera-settings') setAttempt((count) => count + 1)
    },
  )
  // Why: a computer without a webcam, or a browser that can't read QR codes, never offers "Open camera", only the link.
  const cameras = useCameras()
  const hasCamera = !canReadQr ? false : cameras === null ? null : cameras.length > 0
  useEffect(() => {
    if (presenter && hasCamera !== null) presenter.checkCamera(hasCamera)
  }, [presenter, hasCamera])
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

// Why: what the camera's place says when there is no camera to show. A browser that can't read QR codes is the
// ViewModel's no camera, worded as such.
const problemMessages = {
  unsupported: 'join_linkUnsupported',
  no_camera: 'join_linkNoCamera',
  denied: 'join_linkDeniedMessage',
  failed: 'join_linkCameraFailed',
} as const satisfies Record<string, MessageKey>
type CameraProblem = keyof typeof problemMessages

function CameraPane({ presenter, state, isDesktop }: { presenter: JoinWithLinkPresenter; state: JsJoinWithLinkState; isDesktop: boolean }) {
  const video = useRef<HTMLVideoElement>(null)
  useInviteCamera(video, presenter, state)
  const stage = state.cameraStage
  const problem: CameraProblem | null = stage === 'no_camera' && !canReadQr ? 'unsupported' : stage in problemMessages ? (stage as CameraProblem) : null

  return (
    <div className={cn('relative w-full overflow-hidden rounded-xl bg-camera text-white', isDesktop ? 'aspect-square' : 'aspect-[4/3]')}>
      {/* Always mounted, so a second try after a refusal has the video to show the camera in */}
      <video ref={video} muted playsInline className="absolute inset-0 size-full object-cover" />
      {problem ? (
        <CameraMessage
          icon={<CameraOff aria-hidden className="size-8 text-white/70" />}
          title={problem === 'denied' ? t('join_linkDeniedTitle') : undefined}
          message={t(problemMessages[problem])}
          className="bg-camera"
        >
          {problem === 'denied' && (
            <Button variant="secondary" onClick={() => presenter.openSettings()}>
              {t('join_retry')}
            </Button>
          )}
        </CameraMessage>
      ) : stage === 'prompt' ? (
        // Why: in the camera's place until "Open camera", which is the only thing that asks the browser for it.
        <CameraMessage
          icon={
            <div className="flex size-16 items-center justify-center rounded-2xl bg-white/10">
              <ScanQrCode aria-hidden className="size-8" />
            </div>
          }
          title={t('join_linkPromptTitle')}
          message={t('join_linkPromptMessage')}
        >
          <Button type="button" onClick={() => presenter.requestCamera()} className="mt-1">
            <Camera /> {t('join_linkOpenCamera')}
          </Button>
        </CameraMessage>
      ) : (
        <>
          {/* Darker toward the edges, so the corners and the hint read over any picture */}
          <div aria-hidden className="absolute inset-0 bg-[radial-gradient(circle,transparent_35%,rgb(0_0_0/0.55)_100%)]" />
          <ViewfinderCorners isInvalid={state.isQrInvalid} />
          {state.isQrInvalid ? (
            <div role="alert" className="absolute inset-x-3 bottom-3 flex gap-2 rounded-lg bg-camera-error-container px-3 py-2 text-sm text-camera-error-container-foreground">
              <TriangleAlert aria-hidden className="mt-0.5 size-4 shrink-0" />
              <div>
                <p className="font-semibold">{t('join_linkInvalidQr')}</p>
                <p>{t('join_linkInvalidQrHint')}</p>
              </div>
            </div>
          ) : (
            <p className="absolute inset-x-3 bottom-4 text-center text-sm font-medium [text-shadow:0_1px_3px_rgb(0_0_0/0.6)]">{t('join_linkHint')}</p>
          )}
          {state.showsTorch && (
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

// What the camera's place says instead of the camera: an icon, the words and what can be done.
function CameraMessage({ icon, title, message, className, children }: { icon: ReactNode; title?: string; message: string; className?: string; children?: ReactNode }) {
  return (
    <div className={cn('absolute inset-0 flex flex-col items-center justify-center gap-3 px-6 text-center', className)}>
      {icon}
      <div className="flex flex-col gap-1">
        {title && <p className="font-semibold">{title}</p>}
        <p className="text-sm text-white/80">{message}</p>
      </div>
      {children}
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
      className={cn('absolute top-1/2 left-1/2 size-3/5 -translate-x-1/2 -translate-y-[55%] transition-colors', isInvalid ? 'text-camera-error' : 'text-white')}
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
 * Starts once "Open camera" was tapped, and the camera's answer goes to the ViewModel: allowed or refused, opened with
 * or without a flashlight, or failed. The flashlight is a constraint of the camera's track in Chrome on Android.
 */
function useInviteCamera(video: RefObject<HTMLVideoElement | null>, presenter: JoinWithLinkPresenter, state: JsJoinWithLinkState) {
  const { problem, track, hasTorch } = useQrCamera(video, {
    isEnabled: state.isCameraRequested,
    readsContinuously: true,
    onScanned: (content) => presenter.scan(content),
    onStarted: () => presenter.changeCameraAccess(true),
    isTorchOn: state.isTorchOn,
  })
  useEffect(() => {
    if (problem === 'denied') presenter.changeCameraAccess(false)
    else if (problem === 'failed') presenter.failCamera()
  }, [problem, presenter])
  useEffect(() => {
    if (track) presenter.openCamera(hasTorch)
  }, [track, hasTorch, presenter])
}

function PastePanel({ presenter, state }: { presenter: JoinWithLinkPresenter; state: JsJoinWithLinkState }) {
  const [link, setLink] = useDraft(state.link, (value) => presenter.changeLink(value))
  const paste = () => {
    navigator.clipboard
      .readText()
      .then((text) => presenter.paste(text))
      // The browser refused to read the clipboard (permission denied): an empty paste, as an empty clipboard is on
      // Android and iOS, so the tap still counts and the field stays as it is
      .catch(() => presenter.paste(''))
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
          {!state.showsPaste ? (
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
