import { Check, Copy, Info, Link2, Send, WifiOff } from 'lucide-react'
import { ShareBillPresenter, type JsRoute, type JsShareBillAction, type JsShareBillState } from 'blitzsplit-shared'
import { useId } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { LoadingRegion, Skeleton } from '@/components/ui/skeleton'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { formatMoney, formatNames } from '@/lib/money'
import { cn } from '@/lib/utils'

/** The text to the clipboard with "Copiado", or why it failed. */
function copyText(text: string) {
  navigator.clipboard
    .writeText(text)
    .then(() => toast(t('shareBill_copied')))
    .catch(() => toast(t('invite_copyFailed')))
}

/**
 * "Compartilhar conta" over Conta (7.3): the bill's link, who may open it, "Enviar link" and "Desativar link", drawn
 * as Contatos' "Convidar com link ou QR": a 480 px dialog, a drawer on the phone. The server keeps the link, so it is
 * made, changed and turned off online only; once it is known, copying and sending it work offline too.
 */
export function ShareBillDialog({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new ShareBillPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsShareBillState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  useActions(presenter, (p, onAction: (action: JsShareBillAction) => void) => p.observeActions(onAction), (action) => {
    if (action.type === 'copy' && action.link) {
      copyText(action.link)
    } else if (action.type === 'send' && action.link) {
      // Why: the browser's share sheet where there is one (phones, Safari), otherwise the message to the clipboard.
      const text = t('shareBill_message', { description: action.description ?? '', link: action.link })
      if (typeof navigator.share === 'function') navigator.share({ text }).catch(() => {})
      else copyText(text)
    } else if (action.type === 'error') {
      toast(t(action.error === 'disable-failed' ? 'shareBill_disableError' : 'shareBill_updateError'))
    }
  })
  if (!presenter || !state) return null

  const amount = formatMoney(state.totalMinor, state.currency, state.fractionDigits)
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      className="sm:max-w-[480px]"
      onOpenChange={(open) => {
        if (!open) presenter.close()
      }}
      pane={{
        title: t('shareBill_title'),
        description: state.description ? `${state.description} · ${amount}` : undefined,
        body: <ShareBody presenter={presenter} state={state} />,
        footer: (
          <>
            <Button
              variant="outline"
              disabled={!state.canChangeLink}
              onClick={() => presenter.disable()}
              className="text-destructive hover:text-destructive"
            >
              {t('shareBill_disable')}
            </Button>
            <Button disabled={!state.canSendLink} onClick={() => presenter.send()}>
              <Send />
              {t('shareBill_send')}
            </Button>
          </>
        ),
      }}
    />
  )
}

function ShareBody({ presenter, state }: { presenter: ShareBillPresenter; state: JsShareBillState }) {
  const whoLabelId = useId()
  const membersText =
    state.memberNames.length === 0
      ? t('shareBill_membersBodyEmpty')
      : t(state.memberNames.length === 1 ? 'shareBill_membersBodyOne' : 'shareBill_membersBody', { names: formatNames(state.memberNames) })
  return (
    <div className="flex flex-col gap-5">
      <LinkRow presenter={presenter} state={state} />

      <div className="flex flex-col gap-2">
        <h3 id={whoLabelId} className="text-sm font-medium">
          {t('shareBill_whoCanOpen')}
        </h3>
        <RadioGroup
          aria-labelledby={whoLabelId}
          value={state.visibility ?? ''}
          disabled={!state.canChangeLink}
          onValueChange={(value) => presenter.pickVisibility(value)}
          className="gap-2"
        >
          <VisibilityOption value="members" title={t('shareBill_members')} body={membersText} isChecked={state.visibility === 'members'} isEnabled={state.canChangeLink} />
          <VisibilityOption value="anyone" title={t('shareBill_anyone')} body={t('shareBill_anyoneBody')} isChecked={state.visibility === 'anyone'} isEnabled={state.canChangeLink} />
        </RadioGroup>
      </div>

      <p className="flex items-start gap-2 text-[13px] leading-[18px] text-muted-foreground">
        <Info aria-hidden className="mt-px size-4 shrink-0" />
        {t('shareBill_note')}
      </p>

      {state.showsOfflineNotice && (
        <p role="status" className="flex items-start gap-1.5 text-xs text-muted-foreground">
          <WifiOff aria-hidden className="mt-0.5 size-3 shrink-0" />
          {t('shareBill_offline')}
        </p>
      )}
    </div>
  )
}

/** The link with "Copiar", a shimmer while it is read or made, or "Tentar de novo" when that failed. */
function LinkRow({ presenter, state }: { presenter: ShareBillPresenter; state: JsShareBillState }) {
  const row = 'flex min-h-10 items-center gap-2 rounded-md border pr-1 pl-3'
  // Why: the server is asked for the link, so the shimmer shows at once in the link's shape.
  if (state.isLoading) {
    return (
      <LoadingRegion isDelayed={false} label={t('shareBill_loading')}>
        <Skeleton className="h-10 w-full" />
      </LoadingRegion>
    )
  }
  if (state.hasFailed) {
    return (
      <div role="alert" className={cn(row, 'border-dashed')}>
        <span className="min-w-0 flex-1 text-[13px] text-muted-foreground">{t('shareBill_failed')}</span>
        <Button variant="ghost" size="sm" disabled={state.showsOfflineNotice} onClick={() => presenter.retry()}>
          {t('shareBill_retry')}
        </Button>
      </div>
    )
  }
  return (
    <div className={cn(row, 'bg-muted')}>
      <Link2 aria-hidden className="size-4 shrink-0 text-muted-foreground" />
      <span className={cn('min-w-0 flex-1 truncate font-mono text-[13px] select-all', !state.displayLink && 'text-muted-foreground')}>{state.displayLink ?? '—'}</span>
      <Button variant="ghost" size="sm" disabled={!state.canSendLink} onClick={() => presenter.copy()} aria-live="polite">
        {state.isLinkCopied ? <Check /> : <Copy />}
        {state.isLinkCopied ? t('shareBill_copied') : t('shareBill_copy')}
      </Button>
    </div>
  )
}

function VisibilityOption({ value, title, body, isChecked, isEnabled }: { value: string; title: string; body: string; isChecked: boolean; isEnabled: boolean }) {
  return (
    <label
      className={cn(
        'flex items-start gap-3 rounded-lg border px-3.5 py-3',
        isEnabled ? 'cursor-pointer hover:bg-accent/50' : 'cursor-not-allowed opacity-70',
        isChecked && 'border-primary ring-1 ring-primary',
      )}
    >
      <RadioGroupItem value={value} className="mt-0.5" />
      <span className="flex min-w-0 flex-col gap-0.5">
        <span className="text-sm font-medium">{title}</span>
        <span className="text-[13px] leading-[18px] text-muted-foreground">{body}</span>
      </span>
    </label>
  )
}
