import { ChevronLeft, ChevronRight, Download, ImageOff, Loader2, Plus, Trash2, X } from 'lucide-react'
import type { BillDetailPresenter, JsBillAttachment, JsBillDetailState } from 'blitzsplit-shared'
import { useRef, type ChangeEvent, type KeyboardEvent } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog'
import { t } from '@/i18n'
import { formatShortDate, formatTime } from '@/lib/dates'
import { downloadBlob } from '@/lib/download'
import { cn } from '@/lib/utils'
import { postedText } from './BillComments'

async function onPhotoFile(presenter: BillDetailPresenter, event: ChangeEvent<HTMLInputElement>) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  try {
    presenter.photoPicked(new Int8Array(await file.arrayBuffer()))
  } catch {
    presenter.photoUnreadable()
  }
}

function adderText(attachment: JsBillAttachment): string {
  if (attachment.isAddedByMe) return t('splitBill_you')
  return attachment.adderName ?? t('billDetail_commentSomeone')
}

/** "Foto 2" or "Cupom fiscal". */
function labelOf(attachment: JsBillAttachment): string {
  return attachment.photoNumber != null ? t('billDetail_photoLabel', { number: attachment.photoNumber }) : t('billDetail_receipt')
}

/**
 * "Anexos", with "2 de 5 fotos": the receipt the bill was read from first, then its photos, then "Adicionar foto"
 * while there is room for another. The add tile opens the browser's file chooser, which phones offer the camera in.
 */
export function BillAttachments({ presenter, state }: { presenter: BillDetailPresenter; state: JsBillDetailState }) {
  const input = useRef<HTMLInputElement>(null)
  return (
    <section aria-labelledby="bill-attachments-title" className="flex min-w-0 flex-col gap-2">
      <h3 id="bill-attachments-title" className="flex justify-between text-sm font-medium">
        <span>{t('billDetail_attachments')}</span>
        <span className="font-normal text-muted-foreground tabular-nums">{t('billDetail_photoCount', { count: state.photoCount, limit: state.photoLimit })}</span>
      </h3>
      <ul className="flex flex-wrap gap-2">
        {state.attachments.map((attachment) => (
          <li key={attachment.id}>
            <AttachmentTile attachment={attachment} onClick={() => presenter.openAttachment(attachment.id)} />
          </li>
        ))}
        {state.photoCount < state.photoLimit && (
          <li>
            <button
              type="button"
              aria-label={t('billDetail_addPhoto')}
              title={t('billDetail_addPhoto')}
              disabled={!state.canAddPhoto}
              onClick={() => input.current?.click()}
              className={cn(tileClass, 'border-dashed text-muted-foreground hover:bg-accent hover:text-accent-foreground disabled:pointer-events-none disabled:opacity-50')}
            >
              {state.isAddingPhoto ? <Loader2 className="size-5 animate-spin" /> : <Plus className="size-5" />}
            </button>
          </li>
        )}
      </ul>
      <input ref={input} type="file" accept="image/*" className="hidden" onChange={(event) => onPhotoFile(presenter, event)} />
    </section>
  )
}

const tileClass =
  'relative flex h-[66px] w-[88px] items-center justify-center overflow-hidden rounded-lg border outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50'

/** A photo or the receipt, cropped to fill, with "Cupom" or "Na fila" in its corner; its name and who added it on hover. */
function AttachmentTile({ attachment, onClick }: { attachment: JsBillAttachment; onClick: () => void }) {
  const title = `${labelOf(attachment)} · ${adderText(attachment)} · ${postedText(attachment.addedAt, attachment.addedDay)}`
  const badge = attachment.isPending ? t('billDetail_attachmentQueued') : attachment.isReceipt ? t('billDetail_receiptBadge') : null
  return (
    <button type="button" aria-label={badge ? `${title} · ${badge}` : title} title={title} onClick={onClick} className={cn(tileClass, 'bg-muted hover:opacity-90')}>
      {attachment.imageUrl ? (
        <img src={attachment.imageUrl} alt="" className="size-full object-cover" />
      ) : (
        <ImageOff aria-hidden className="size-5 text-muted-foreground" />
      )}
      {badge && (
        <span
          aria-hidden
          className={cn(
            'absolute bottom-1 left-1 flex h-[18px] items-center gap-1 rounded px-1.5 text-[11px] font-medium',
            attachment.isPending ? 'bg-amber-50 text-amber-800 dark:bg-amber-950 dark:text-amber-300' : 'bg-foreground text-card',
          )}
        >
          {attachment.isPending && <span className="size-1.5 rounded-full bg-orange-500" />}
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
 * "Baixar": the image as "bora-rachar-foto-2.jpg". Why fetch: a link's download attribute is ignored for another
 * origin's signed URL, so the image is read into a blob first; when the storage refuses that, it opens in a tab.
 */
async function download(attachment: JsBillAttachment) {
  const url = attachment.imageUrl
  if (!url) return
  const name = attachment.photoNumber != null ? t('billDetail_photoFileName', { number: attachment.photoNumber }) : t('billDetail_receiptFileName')
  try {
    const response = await fetch(url)
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const blob = await response.blob()
    downloadBlob(blob, `${name}.${extensions[blob.type] ?? 'jpg'}`)
  } catch {
    window.open(url, '_blank', 'noopener')
  }
}

const lightButton = 'text-white hover:bg-white/10 hover:text-white disabled:text-white/50'

/**
 * The lightbox over Conta, from the phone to the desktop: the attachment as large as the window allows, its title,
 * Baixar and Remover foto on top, who added it and when under it, and the arrows (buttons, ← and →) to the others.
 */
export function AttachmentViewer({ presenter, state }: { presenter: BillDetailPresenter; state: JsBillDetailState }) {
  const index = state.viewedAttachmentIndex
  const attachment = index != null ? state.attachments[index] : undefined
  const previous = index != null ? state.attachments[index - 1] : undefined
  const next = index != null ? state.attachments[index + 1] : undefined
  const pages = state.attachments.length
  const onKeyDown = (event: KeyboardEvent) => {
    const target = event.key === 'ArrowLeft' ? previous : event.key === 'ArrowRight' ? next : undefined
    if (!target) return
    event.preventDefault()
    presenter.showAttachment(target.id)
  }
  const hints = [attachment?.canRemove && t('billDetail_attachmentRemoveHint'), pages > 1 && t('billDetail_attachmentPagingHint')].filter(Boolean).join(' ')
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
                {attachment.photoNumber != null ? t('billDetail_viewerPhotoTitle', { number: attachment.photoNumber, count: state.photoCount }) : t('billDetail_receipt')}
              </DialogTitle>
              <Button variant="ghost" size="sm" className={lightButton} disabled={!attachment.imageUrl} onClick={() => download(attachment)}>
                <Download />
                <span className="max-sm:sr-only">{t('billDetail_download')}</span>
              </Button>
              {attachment.canRemove && (
                <Button variant="ghost" size="sm" className={lightButton} onClick={() => presenter.removeAttachment(attachment.id)}>
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
              {attachment.imageUrl ? (
                <img
                  key={attachment.id}
                  src={attachment.imageUrl}
                  alt={labelOf(attachment)}
                  width={attachment.width ?? undefined}
                  height={attachment.height ?? undefined}
                  className="max-h-full max-w-full rounded-sm object-contain shadow-2xl"
                />
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
                  <span aria-hidden className="size-1.5 rounded-full bg-orange-500" />
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
