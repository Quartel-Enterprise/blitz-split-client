import { CircleAlert, Loader2, ScanText, X } from 'lucide-react'
import type { JsBillReceipt, JsSplitBillState, SplitBillPresenter } from 'blitzsplit-shared'
import { useEffect, useRef, type ChangeEvent, type RefObject } from 'react'
import { Button } from '@/components/ui/button'
import { t, type MessageKey } from '@/i18n'

/*
 * "Ler cupom fiscal" (7.1e to 7.1g) on the web: the browser has no camera of the app's own, so a file input picks the
 * receipt's photo (a phone's browser offers its camera there too). A file that can't be read goes as empty bytes,
 * which read as a receipt that couldn't be read.
 */
export function useReceiptInput(presenter: SplitBillPresenter, state: JsSplitBillState): RefObject<HTMLInputElement | null> {
  const input = useRef<HTMLInputElement>(null)
  // Why: Início's "Ler cupom fiscal" opens the picker with the bill; the browser may refuse it without a fresh click,
  // so the request is dropped either way and the button stays to try again.
  useEffect(() => {
    if (!state.showsReceiptCamera) return
    input.current?.click()
    presenter.dismissReceiptCamera()
  }, [presenter, state.showsReceiptCamera])
  return input
}

export function ReceiptInput({ presenter, input }: { presenter: SplitBillPresenter; input: RefObject<HTMLInputElement | null> }) {
  const onChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    let bytes: Int8Array
    try {
      bytes = new Int8Array(await file.arrayBuffer())
    } catch {
      bytes = new Int8Array(0)
    }
    presenter.receiptPicked(bytes)
  }
  return <input ref={input} type="file" accept="image/*" className="hidden" tabIndex={-1} aria-hidden onChange={onChange} />
}

/** Under "Divisão": "Ler cupom fiscal" until a receipt is picked, then "Foto do cupom", attached once saved, with ×. */
export function ReceiptField({ presenter, state, input }: { presenter: SplitBillPresenter; state: JsSplitBillState; input: RefObject<HTMLInputElement | null> }) {
  const receipt = state.receipt
  if (receipt?.hasPhoto) {
    const isReading = receipt.status === 'reading'
    return (
      <div className="flex items-center gap-2.5 rounded-lg border border-border px-3 py-2.5">
        <ReceiptThumbnail />
        <span className="flex min-w-0 flex-1 flex-col" aria-live="polite">
          <span className="font-medium">{t('splitBill_receiptPhoto')}</span>
          <span className="text-[13px] text-muted-foreground">{isReading ? t('splitBill_receiptReading') : t('splitBill_receiptVisibility')}</span>
        </span>
        {isReading && <Loader2 aria-hidden className="size-4 animate-spin text-muted-foreground" />}
        <Button type="button" variant="ghost" size="icon" className="size-7" aria-label={t('splitBill_receiptRemove')} onClick={() => presenter.removeReceipt()}>
          <X aria-hidden />
        </Button>
      </div>
    )
  }
  if (!state.showsScanReceipt) return null
  return (
    <div className="flex flex-col gap-1.5">
      <Button type="button" variant="outline" className="self-start" disabled={!state.canScanReceipt} onClick={() => input.current?.click()}>
        <ScanText aria-hidden />
        {t('splitBill_scanReceipt')}
      </Button>
      <span className="text-[13px] text-muted-foreground">{state.isOnline ? t('splitBill_scanReceiptHelp') : t('splitBill_scanReceiptOffline')}</span>
    </div>
  )
}

/** "Lido do cupom" and "14 itens" under the description once a receipt was read. */
export function ReceiptReadChips({ receipt }: { receipt: JsBillReceipt | null | undefined }) {
  if (receipt?.status !== 'read') return null
  return (
    <div className="flex gap-1.5">
      <span className="flex h-[22px] items-center rounded-md bg-primary/10 px-2 text-xs font-semibold text-primary">{t('splitBill_receiptRead')}</span>
      {receipt.itemCount > 0 && (
        <span className="flex h-[22px] items-center rounded-md border border-border px-2 text-xs">{t(receipt.itemCount === 1 ? 'splitBill_receiptItemsOne' : 'splitBill_receiptItemsOther', { count: receipt.itemCount })}</span>
      )}
    </div>
  )
}

/**
 * Why the receipt read nothing (7.1g): the fields stay as they were, to fill in by hand. "Enviar outra foto" for a photo
 * nothing could be read from, "Tentar de novo" sends the same photo when the reader didn't answer or the connection
 * dropped, and the day's limit offers neither.
 */
export function ReceiptFailureAlert({
  state,
  presenter,
  input,
}: {
  state: JsSplitBillState
  presenter: Pick<SplitBillPresenter, 'retryReceipt'>
  input: RefObject<HTMLInputElement | null>
}) {
  const error = state.receiptError
  if (!error) return null
  const copy = ERROR_COPY[error] ?? UNREADABLE_COPY
  return (
    <div role="alert" className="flex gap-3 rounded-lg border border-destructive px-4 py-3 text-sm leading-5 text-destructive">
      <CircleAlert aria-hidden className="mt-0.5 size-4 shrink-0" />
      <div className="flex flex-col items-start gap-1">
        <strong className="font-semibold">{t(copy.title)}</strong>
        <span>{t(copy.body)}</span>
        {state.offersReceiptRetake && (
          <Button type="button" variant="link" className="h-auto p-0 text-destructive" disabled={!state.canScanReceipt} onClick={() => input.current?.click()}>
            {t('splitBill_receiptRetake')}
          </Button>
        )}
        {state.offersReceiptRetry && (
          <Button type="button" variant="link" className="h-auto p-0 text-destructive" disabled={!state.canScanReceipt} onClick={() => presenter.retryReceipt()}>
            {t('splitBill_receiptRetry')}
          </Button>
        )}
      </div>
    </div>
  )
}

const UNREADABLE_COPY = { title: 'splitBill_receiptFailedTitle', body: 'splitBill_receiptFailedBody' } as const

const ERROR_COPY: Record<string, { title: MessageKey; body: MessageKey } | undefined> = {
  unreadable: UNREADABLE_COPY,
  reader_unavailable: { title: 'splitBill_receiptUnavailableTitle', body: 'splitBill_receiptUnavailableBody' },
  limit_reached: { title: 'splitBill_receiptLimitTitle', body: 'splitBill_receiptLimitBody' },
  offline: { title: 'splitBill_receiptOfflineTitle', body: 'splitBill_receiptOfflineBody' },
}

function ReceiptThumbnail() {
  return (
    <span aria-hidden className="flex h-[34px] w-7 shrink-0 flex-col gap-1 rounded bg-muted px-1.5 py-2">
      <span className="h-0.5 bg-muted-foreground/60" />
      <span className="h-0.5 w-[70%] bg-muted-foreground/60" />
      <span className="h-0.5 bg-muted-foreground/60" />
    </span>
  )
}
