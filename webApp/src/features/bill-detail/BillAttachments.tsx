import { ChevronLeft, ChevronRight, Download, ImageOff, Loader2, Plus, Trash2, X } from 'lucide-react'
import type { BillDetailPresenter, JsBillAttachment, JsBillDetailState } from 'blitzsplit-shared'
import { useEffect, useReducer, useRef, useState, type ComponentProps, type KeyboardEvent } from 'react'
import { toast } from 'sonner'
import { PhotoPickerInputs, PhotoSourceMenu, usePhotoPicker } from '@/components/PhotoPicker'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog'
import { LoadingRegion, Skeleton } from '@/components/ui/skeleton'
import { t } from '@/i18n'
import { formatShortDate, formatTime } from '@/lib/dates'
import { downloadBlob } from '@/lib/download'
import { cn } from '@/lib/utils'
import { postedText } from './BillComments'
import { PendingDot, pendingSurfaceClass } from '@/components/common'

/**
 * What "Anexos" and the viewer read and send: Conta's, or a bill's link page's, which adds and removes nothing (no
 * photoPicked nor removeAttachment, no photoLimit nor showsAddPhoto).
 */
export type AttachmentsPresenter = Pick<BillDetailPresenter, 'openAttachment' | 'showAttachment' | 'closeViewer' | 'trackDownload'> &
  Partial<Pick<BillDetailPresenter, 'photoPicked' | 'photoUnreadable' | 'removeAttachment'>>
export type AttachmentsState = Pick<JsBillDetailState, 'attachments' | 'photoCount' | 'viewedAttachmentIndex' | 'isOnline'> &
  Partial<Pick<JsBillDetailState, 'photoLimit' | 'showsAddPhoto' | 'canAddPhoto' | 'isAddingPhoto'>>

function adderText(attachment: JsBillAttachment): string {
  if (attachment.isAddedByMe) return t('splitBill_you')
  return attachment.adderName ?? t('billDetail_commentSomeone')
}

/** "Foto 2" or "Cupom fiscal". */
function labelOf(attachment: JsBillAttachment): string {
  return attachment.photoNumber != null ? t('billDetail_photoLabel', { number: attachment.photoNumber }) : t('billDetail_receipt')
}

/** Why the cast: Kotlin's ByteArray is typed over ArrayBufferLike, which BlobPart refuses for a SharedArrayBuffer it never is. */
function jpegOf(bytes: Int8Array): Blob {
  return new Blob([bytes as BlobPart], { type: 'image/jpeg' })
}

/** A photo's object URLs by attachment id, from [useLocalPhotoUrls]. */
export type LocalPhotoUrls = ReadonlyMap<string, string>

/**
 * One object URL per photo added on this device that isn't up yet (its localImage), by attachment id. Why a cache:
 * the shared code hands the same Int8Array on every state, so re-renders reuse its URL; a URL is revoked once its id
 * is gone from the attachments (or its bytes change), and all of them on unmount. StrictMode's unmount and remount
 * revokes them too, so the remount draws again with new ones.
 */
export function useLocalPhotoUrls(attachments: JsBillAttachment[]): LocalPhotoUrls {
  const cache = useRef(new Map<string, { bytes: Int8Array; url: string }>())
  const replaced = useRef<string[]>([])
  const wasRevoked = useRef(false)
  const [, redraw] = useReducer((count: number) => count + 1, 0)
  const urls = new Map<string, string>()
  for (const attachment of attachments) {
    const bytes = attachment.localImage
    if (!bytes) continue
    let entry = cache.current.get(attachment.id)
    if (entry?.bytes !== bytes) {
      if (entry) replaced.current.push(entry.url)
      entry = { bytes, url: URL.createObjectURL(jpegOf(bytes)) }
      cache.current.set(attachment.id, entry)
    }
    urls.set(attachment.id, entry.url)
  }
  useEffect(() => {
    replaced.current.splice(0).forEach((url) => URL.revokeObjectURL(url))
    for (const [id, entry] of cache.current) {
      if (urls.get(id) !== entry.url) {
        URL.revokeObjectURL(entry.url)
        cache.current.delete(id)
      }
    }
  })
  useEffect(() => {
    const entries = cache.current
    if (wasRevoked.current) {
      wasRevoked.current = false
      redraw()
    }
    return () => {
      entries.forEach((entry) => URL.revokeObjectURL(entry.url))
      entries.clear()
      wasRevoked.current = true
    }
  }, [])
  return urls
}

/** The photo's own bytes while it isn't up yet, else its signed URL; undefined offline or until it is signed. */
function sourceOf(attachment: JsBillAttachment, localUrls: LocalPhotoUrls): string | undefined {
  return localUrls.get(attachment.id) ?? attachment.imageUrl ?? undefined
}

/**
 * "Anexos", with "2 de 5 fotos": the receipt the bill was read from first, then its photos, then "Adicionar foto"
 * while there is room for another. The add tile opens the browser's file chooser or, where the browser has a camera,
 * asks whether to take the photo with it (`CameraCaptureDialog`).
 * A bill's link page has no limit to count against, nor the add tile.
 */
export function BillAttachments({ presenter, state, localUrls }: { presenter: AttachmentsPresenter; state: AttachmentsState; localUrls: LocalPhotoUrls }) {
  return (
    <section aria-labelledby="bill-attachments-title" className="flex min-w-0 flex-col gap-2">
      <h3 id="bill-attachments-title" className="flex justify-between text-sm font-medium">
        <span>{t('billDetail_attachments')}</span>
        {state.photoLimit != null && state.photoCount != null && (
          <span className="font-normal text-muted-foreground tabular-nums">{t('billDetail_photoCount', { count: state.photoCount, limit: state.photoLimit })}</span>
        )}
      </h3>
      {!state.attachments && (
        <LoadingRegion className="flex gap-2">
          <Skeleton className="h-[66px] w-[88px] rounded-lg" />
          <Skeleton className="h-[66px] w-[88px] rounded-lg" />
        </LoadingRegion>
      )}
      <ul className="flex flex-wrap gap-2">
        {state.attachments?.map((attachment) => (
          <li key={attachment.id}>
            <AttachmentTile
              attachment={attachment}
              src={sourceOf(attachment, localUrls)}
              onClick={() => presenter.openAttachment(attachment.id)}
            />
          </li>
        ))}
        {state.showsAddPhoto && (
          <li>
            <AddPhoto presenter={presenter} state={state} />
          </li>
        )}
      </ul>
    </section>
  )
}

const tileClass =
  'relative flex h-[66px] w-[88px] items-center justify-center overflow-hidden rounded-lg border outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50'

/** "Adicionar foto": the file chooser, or a menu with the camera where the browser has one. */
function AddPhoto({ presenter, state }: { presenter: AttachmentsPresenter; state: AttachmentsState }) {
  const photo = usePhotoPicker()
  return (
    <>
      <PhotoSourceMenu
        picker={photo}
        align="start"
        takePhotoLabel={t('billDetail_takePhoto')}
        choosePhotoLabel={t('billDetail_choosePhoto')}
        renderTrigger={(props) => <AddPhotoTile isAdding={state.isAddingPhoto} disabled={!state.canAddPhoto} {...props} />}
      />
      <PhotoPickerInputs picker={photo} receiver={presenter} facing="environment" />
    </>
  )
}

// "Adicionar foto", a menu's trigger too, so it takes the trigger's props and ref.
function AddPhotoTile({ isAdding, ...props }: { isAdding: boolean | undefined } & ComponentProps<'button'>) {
  return (
    <button
      type="button"
      aria-label={t('billDetail_addPhoto')}
      title={t('billDetail_addPhoto')}
      className={cn(tileClass, 'border-dashed text-muted-foreground hover:bg-accent hover:text-accent-foreground disabled:pointer-events-none disabled:opacity-50')}
      {...props}
    >
      {isAdding ? <Loader2 className="size-5 animate-spin" /> : <Plus className="size-5" />}
    </button>
  )
}

/** A photo or the receipt, cropped to fill, with "Cupom" or "Na fila" in its corner; its name and who added it on hover. */
function AttachmentTile({ attachment, src, onClick }: { attachment: JsBillAttachment; src: string | undefined; onClick: () => void }) {
  // Why: the image downloading shimmers as its URL being signed does (beta feedback, part 3, item 21), never a blank tile.
  const [loadedSrc, setLoadedSrc] = useState<string>()
  const [failedSrc, setFailedSrc] = useState<string>()
  const isDownloading = src !== undefined && src !== loadedSrc && src !== failedSrc
  const title = `${labelOf(attachment)} · ${adderText(attachment)} · ${postedText(attachment.addedAt, attachment.addedDay)}`
  const badge = attachment.isPending ? t('billDetail_attachmentQueued') : attachment.isReceipt ? t('billDetail_receiptBadge') : null
  return (
    <button type="button" aria-label={badge ? `${title} · ${badge}` : title} title={title} onClick={onClick} className={cn(tileClass, 'bg-muted hover:opacity-90')}>
      {/* Why: online its signed URL is on its way; offline, or once it failed, it can't come, which the neutral icon says. */}
      {isDownloading || (!src && attachment.isImageLoading) ? (
        <Skeleton className="absolute inset-0 rounded-none" />
      ) : (
        (!src || src === failedSrc) && <ImageOff aria-hidden className="size-5 text-muted-foreground" />
      )}
      {src && src !== failedSrc && (
        <img
          src={src}
          alt=""
          onLoad={() => setLoadedSrc(src)}
          onError={() => setFailedSrc(src)}
          className={cn('relative size-full object-cover', isDownloading && 'opacity-0')}
        />
      )}
      {badge && (
        <span
          aria-hidden
          className={cn(
            'absolute bottom-1 left-1 flex h-[18px] items-center gap-1 rounded px-1.5 text-[11px] font-medium',
            attachment.isPending ? pendingSurfaceClass : 'bg-foreground text-card',
          )}
        >
          {attachment.isPending && <PendingDot />}
          {badge}
        </span>
      )}
    </button>
  )
}

/** "Ana anexou ontem às 22:40" or "Bruno leu o cupom ontem às 22:08". */
function addedText(attachment: JsBillAttachment): string {
  const time = formatTime(attachment.addedAt)
  const when =
    attachment.addedDay === 'today'
      ? t('billDetail_atToday', { time })
      : attachment.addedDay === 'yesterday'
        ? t('billDetail_atYesterday', { time })
        : t('billDetail_atDate', { date: formatShortDate(attachment.addedAt), time })
  const name = adderText(attachment)
  return attachment.isReceipt ? t('billDetail_receiptReadBy', { name, when }) : t('billDetail_attachedBy', { name, when })
}

const extensions: Record<string, string> = { 'image/jpeg': 'jpg', 'image/png': 'png', 'image/webp': 'webp', 'image/heic': 'heic' }

/**
 * "Baixar": the image as "bora-rachar-foto-2.jpg", straight from its bytes while it isn't up yet. Why fetch: a link's
 * download attribute is ignored for another origin's signed URL, so the image is read into a blob first; when that
 * fails (offline, CORS, an expired URL) a toast says so, since opening a tab after the await is blocked as a popup.
 */
async function download(attachment: JsBillAttachment) {
  const name = attachment.photoNumber != null ? t('billDetail_photoFileName', { number: attachment.photoNumber }) : t('billDetail_receiptFileName')
  if (attachment.localImage) {
    downloadBlob(jpegOf(attachment.localImage), `${name}.jpg`)
    return
  }
  const url = attachment.imageUrl
  if (!url) return
  try {
    const response = await fetch(url)
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const blob = await response.blob()
    downloadBlob(blob, `${name}.${extensions[blob.type] ?? 'jpg'}`)
  } catch {
    toast(t('billDetail_downloadPhotoError'))
  }
}

const lightButton = 'text-white hover:bg-white/10 hover:text-white disabled:text-white/50'

/**
 * The lightbox over Conta, from the phone to the desktop: the attachment as large as the window allows, its title,
 * Baixar and Remover foto on top, who added it and when under it, and the arrows (buttons, ← and →) to the others.
 */
export function AttachmentViewer({ presenter, state, localUrls }: { presenter: AttachmentsPresenter; state: AttachmentsState; localUrls: LocalPhotoUrls }) {
  const index = state.viewedAttachmentIndex
  const attachments = state.attachments ?? []
  const attachment = index != null ? attachments[index] : undefined
  const src = attachment && sourceOf(attachment, localUrls)
  const previous = index != null ? attachments[index - 1] : undefined
  const next = index != null ? attachments[index + 1] : undefined
  const pages = attachments.length
  const onKeyDown = (event: KeyboardEvent) => {
    const target = event.key === 'ArrowLeft' ? previous : event.key === 'ArrowRight' ? next : undefined
    if (!target) return
    event.preventDefault()
    presenter.showAttachment(target.id)
  }
  const removeAttachment = attachment?.canRemove ? presenter.removeAttachment?.bind(presenter) : undefined
  const hints = [removeAttachment && t('billDetail_attachmentRemoveHint'), pages > 1 && t('billDetail_attachmentPagingHint')].filter(Boolean).join(' ')
  return (
    <Dialog open={attachment != null} onOpenChange={(open) => !open && presenter.closeViewer()}>
      {/* Why: the dialog's own overlay is black at 50%; this layer over it hides the panel under the photo, as a lightbox does. */}
      <DialogContent
        showCloseButton={false}
        onKeyDown={onKeyDown}
        className="inset-0 flex h-dvh w-screen max-w-none translate-x-0 translate-y-0 flex-col gap-0 rounded-none border-0 bg-black/80 p-0 text-white shadow-none sm:max-w-none"
      >
        {attachment && (
          <>
            <header className="flex h-14 shrink-0 items-center gap-1 px-2 sm:px-4">
              <DialogTitle className="mr-auto truncate pl-2 text-base font-medium">
                {attachment.photoNumber != null ? t('billDetail_viewerPhotoTitle', { number: attachment.photoNumber, count: state.photoCount ?? pages }) : t('billDetail_receipt')}
              </DialogTitle>
              <Button
                variant="ghost"
                size="sm"
                className={lightButton}
                disabled={!attachment.localImage && !attachment.imageUrl}
                onClick={() => {
                  presenter.trackDownload()
                  void download(attachment)
                }}
              >
                <Download />
                <span className="max-sm:sr-only">{t('billDetail_download')}</span>
              </Button>
              {removeAttachment && (
                <Button variant="ghost" size="sm" className={lightButton} onClick={() => removeAttachment(attachment.id)}>
                  <Trash2 />
                  <span className="max-sm:sr-only">{t('billDetail_removePhoto')}</span>
                </Button>
              )}
              <Button variant="ghost" size="icon-sm" aria-label={t('common_close')} className={lightButton} onClick={() => presenter.closeViewer()}>
                <X />
              </Button>
            </header>
            <div
              className="relative flex min-h-0 flex-1 items-center justify-center px-4 sm:px-16"
              onClick={(event) => {
                if (event.target === event.currentTarget) presenter.closeViewer()
              }}
            >
              {src ? (
                <img
                  key={attachment.id}
                  src={src}
                  alt={labelOf(attachment)}
                  width={attachment.width ?? undefined}
                  height={attachment.height ?? undefined}
                  className="max-h-full max-w-full rounded-sm object-contain shadow-2xl"
                />
              ) : state.isOnline ? (
                // Why: online, its signed URL is on the way (or tried again each minute)
                <Loader2 aria-hidden className="size-8 animate-spin text-white/80" />
              ) : (
                <div className="flex aspect-[4/3] w-full max-w-sm flex-col items-center justify-center gap-3 rounded-lg bg-white/10 p-6 text-center text-sm text-white/80">
                  <ImageOff aria-hidden className="size-8" />
                  {t('billDetail_photoOffline')}
                </div>
              )}
              {pages > 1 && (
                <>
                  <PageButton side="left" target={previous} onShow={(id) => presenter.showAttachment(id)} />
                  <PageButton side="right" target={next} onShow={(id) => presenter.showAttachment(id)} />
                </>
              )}
            </div>
            <footer className="mx-auto flex w-full max-w-xl shrink-0 flex-col gap-1 px-4 pt-3 pb-[max(env(safe-area-inset-bottom),20px)] text-center">
              <DialogDescription className="text-sm font-medium text-white">{addedText(attachment)}</DialogDescription>
              {hints && <span className="text-[13px] leading-[18px] text-white/70">{hints}</span>}
              {attachment.isPending && (
                <span role="status" className="flex items-center justify-center gap-1.5 text-[13px] text-white/70">
                  <PendingDot className="bg-pending-on-dark" />
                  {t('billDetail_attachmentPending')}
                </span>
              )}
            </footer>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}

/** The arrow on either side of the image, disabled at the first and the last page. */
function PageButton({ side, target, onShow }: { side: 'left' | 'right'; target: JsBillAttachment | undefined; onShow: (id: string) => void }) {
  return (
    <Button
      variant="ghost"
      size="icon"
      aria-label={t(side === 'left' ? 'billDetail_previousAttachment' : 'billDetail_nextAttachment')}
      disabled={!target}
      onClick={() => target && onShow(target.id)}
      className={cn('absolute top-1/2 -translate-y-1/2 rounded-full bg-black/40', lightButton, side === 'left' ? 'left-2 sm:left-4' : 'right-2 sm:right-4')}
    >
      {side === 'left' ? <ChevronLeft /> : <ChevronRight />}
    </Button>
  )
}
