import { FlipHorizontal2, FlipVertical2, Image as ImageIcon, RotateCw } from 'lucide-react'
import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent, type ReactNode, type RefObject } from 'react'
import type { JsPhotoCropState, PhotoCropPresenter } from 'blitzsplit-shared'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

/** A profile's photo shows in a circle, a group's in the rounded square of `GroupAvatar`. */
export type PhotoCropFrame = 'circle' | 'rounded-square'

const FRAME_RATIO = 0.9
const MAX_FRAME_SIDE = 360
// Why: a trackpad's pinch (a wheel with Ctrl) of 100 px zooms by about a fifth, as a touch pinch of the same feel.
const WHEEL_ZOOM_SPEED = 0.002

/**
 * "Ajustar foto" (Bible Planner's editor): a picked photo framed before it becomes the profile's or the group's. Dark,
 * as the photo editors are, full screen on a phone and a dialog from the tablet up. Where the photo sits is the
 * ViewModel's (`JsPhotoCropState`); here only the pointer, the wheel and the pinch are read, in fractions of the
 * frame's side.
 */
export function PhotoCropDialog({ crop, presenter, frame }: { crop: JsPhotoCropState; presenter: PhotoCropPresenter; frame: PhotoCropFrame }) {
  return (
    <Dialog open onOpenChange={(open) => !open && presenter.dismiss()}>
      <DialogContent
        className={cn(
          'dark flex flex-col gap-0 overflow-hidden border-0 bg-[#0B0B0F] p-0 text-white',
          // Why: the tablet starts at 768 px, as the forms that open it (a drawer below it, a dialog from it).
          'h-dvh max-w-none rounded-none sm:max-w-none md:h-[min(720px,calc(100dvh-2rem))] md:max-w-[560px] md:rounded-lg',
        )}
      >
        <div className="flex h-14 shrink-0 items-center justify-center px-12">
          <DialogTitle className="text-base">{t('photoCrop_title')}</DialogTitle>
          <DialogDescription className="sr-only">{t('photoCrop_hint')}</DialogDescription>
        </div>
        <PhotoCropArea crop={crop} presenter={presenter} frame={frame} />
        <PhotoCropControls crop={crop} presenter={presenter} />
      </DialogContent>
    </Dialog>
  )
}

function PhotoCropArea({ crop, presenter, frame }: { crop: JsPhotoCropState; presenter: PhotoCropPresenter; frame: PhotoCropFrame }) {
  const area = useRef<HTMLDivElement>(null)
  const side = useFrameSide(area)
  const pointers = useRef(new Map<number, { x: number; y: number }>())
  const sideRef = useRef(side)
  sideRef.current = side

  /*
   * Why: a trackpad's pinch (the browser's wheel with Ctrl, as Ctrl and a mouse wheel) zooms around the pointer, and a
   * scroll moves the photo, as a two-finger drag does. React's wheel listener is passive, so the page would scroll too.
   */
  useEffect(() => {
    const element = area.current
    if (!element) return
    const onWheel = (event: WheelEvent) => {
      event.preventDefault()
      if (event.ctrlKey) {
        presenter.zoomBy(Math.exp(-event.deltaY * WHEEL_ZOOM_SPEED), ...focusIn(element, sideRef.current, event.clientX, event.clientY))
      } else if (sideRef.current > 0) {
        presenter.drag(-event.deltaX / sideRef.current, -event.deltaY / sideRef.current)
      }
    }
    element.addEventListener('wheel', onWheel, { passive: false })
    return () => element.removeEventListener('wheel', onWheel)
  }, [presenter])

  const onPointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    // Why: React events bubble out of the portal to the form, and a phone's form is a drawer a drag would pull down.
    event.stopPropagation()
    event.currentTarget.setPointerCapture(event.pointerId)
    pointers.current.set(event.pointerId, { x: event.clientX, y: event.clientY })
  }
  // Why: one pointer drags; two pinch, by how much farther apart they moved, and drag by how far their middle did.
  const onPointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    const previous = pointers.current.get(event.pointerId)
    if (!previous || sideRef.current <= 0) return
    const before = [...pointers.current.values()]
    pointers.current.set(event.pointerId, { x: event.clientX, y: event.clientY })
    const after = [...pointers.current.values()]
    if (after.length >= 2) {
      const distanceBefore = Math.hypot(before[0].x - before[1].x, before[0].y - before[1].y)
      const distanceAfter = Math.hypot(after[0].x - after[1].x, after[0].y - after[1].y)
      if (distanceBefore > 0) {
        presenter.zoomBy(distanceAfter / distanceBefore, ...focusIn(area.current, sideRef.current, (after[0].x + after[1].x) / 2, (after[0].y + after[1].y) / 2))
      }
      presenter.drag((event.clientX - previous.x) / 2 / sideRef.current, (event.clientY - previous.y) / 2 / sideRef.current)
    } else {
      presenter.drag((event.clientX - previous.x) / sideRef.current, (event.clientY - previous.y) / sideRef.current)
    }
  }
  const onPointerUp = (event: ReactPointerEvent<HTMLDivElement>) => {
    pointers.current.delete(event.pointerId)
  }

  const width = crop.photoWidthInFrame * side
  const height = crop.photoHeightInFrame * side
  const mirror = crop.isMirrored ? -1 : 1
  return (
    <div
      ref={area}
      className="relative min-h-0 flex-1 cursor-grab touch-none overflow-hidden select-none active:cursor-grabbing"
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
      onPointerCancel={onPointerUp}
    >
      {side > 0 && (
        <>
          <img
            src={crop.photoUrl}
            alt=""
            draggable={false}
            className="pointer-events-none absolute top-1/2 left-1/2 max-w-none"
            style={{
              width,
              height,
              marginLeft: -width / 2,
              marginTop: -height / 2,
              // Why: CSS applies these right to left: mirrored, turned, zoomed, then moved, as the ViewModel's model.
              transform: `translate(${crop.offsetX * side}px, ${crop.offsetY * side}px) rotate(${crop.rotationDegrees}deg) scale(${crop.zoom * mirror}, ${crop.zoom})`,
            }}
          />
          <div
            aria-hidden
            className={cn(
              'pointer-events-none absolute top-1/2 left-1/2 border-2 border-white/90 shadow-[0_0_0_9999px_rgba(11,11,15,0.72)]',
              frame === 'circle' ? 'rounded-full' : 'rounded-[32%]',
            )}
            style={{ width: side, height: side, marginLeft: -side / 2, marginTop: -side / 2 }}
          />
        </>
      )}
    </div>
  )
}

// Why: where a point sits from the frame's center (the area's), in fractions of the frame's side.
function focusIn(area: HTMLElement | null, side: number, x: number, y: number): [number, number] {
  const rect = area?.getBoundingClientRect()
  if (!rect || side <= 0) return [0, 0]
  return [(x - rect.left - rect.width / 2) / side, (y - rect.top - rect.height / 2) / side]
}

// Why: the frame follows the area, so a window resized or a phone turned keeps the framing (it is in fractions of it).
function useFrameSide(area: RefObject<HTMLDivElement | null>): number {
  const [side, setSide] = useState(0)
  useEffect(() => {
    const element = area.current
    if (!element) return
    const observer = new ResizeObserver(([entry]) => {
      const { width, height } = entry.contentRect
      setSide(Math.min(width * FRAME_RATIO, height * FRAME_RATIO, MAX_FRAME_SIDE))
    })
    observer.observe(element)
    return () => observer.disconnect()
  }, [area])
  return side
}

function PhotoCropControls({ crop, presenter }: { crop: JsPhotoCropState; presenter: PhotoCropPresenter }) {
  return (
    <div className="flex shrink-0 flex-col gap-3 px-6 pt-4 pb-[max(1.25rem,env(safe-area-inset-bottom))]">
      <div className="flex items-center gap-3">
        <ImageIcon aria-hidden className="size-4 text-white/70" />
        <input
          type="range"
          aria-label={t('photoCrop_zoom')}
          min={crop.minZoom}
          max={crop.maxZoom}
          step={0.01}
          value={crop.zoom}
          onChange={(event) => presenter.changeZoom(Number(event.target.value))}
          className="h-1 flex-1 accent-white"
        />
        <ImageIcon aria-hidden className="size-6 text-white/70" />
      </div>
      <p className="text-center text-xs text-white/60">{t('photoCrop_hint')}</p>
      <div className="flex flex-wrap justify-center gap-2">
        <OrientationButton label={t('photoCrop_mirrorHorizontally')} onClick={() => presenter.mirrorHorizontally()}>
          <FlipHorizontal2 />
        </OrientationButton>
        <OrientationButton label={t('photoCrop_mirrorVertically')} onClick={() => presenter.mirrorVertically()}>
          <FlipVertical2 />
        </OrientationButton>
        <OrientationButton label={t('photoCrop_turn')} onClick={() => presenter.turn()}>
          <RotateCw />
        </OrientationButton>
      </div>
      <div className="mt-1 grid grid-cols-2 gap-3">
        <Button variant="outline" className="h-11 border-white/25 bg-transparent text-white hover:bg-white/10 hover:text-white" onClick={() => presenter.dismiss()}>
          {t('common_cancel')}
        </Button>
        <Button className="h-11" onClick={() => presenter.confirm()}>
          {t('photoCrop_confirm')}
        </Button>
      </div>
    </div>
  )
}

function OrientationButton({ label, onClick, children }: { label: string; onClick: () => void; children: ReactNode }) {
  return (
    <Button variant="ghost" size="sm" className="rounded-full bg-white/12 text-white hover:bg-white/20 hover:text-white" onClick={onClick}>
      {children}
      {label}
    </Button>
  )
}
