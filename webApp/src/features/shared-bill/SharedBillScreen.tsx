import { CloudOff, Info, Link2Off, Lock } from 'lucide-react'
import { SharedBillPresenter, type JsRoute, type JsSharedBillAction, type JsSharedBillState } from 'blitzsplit-shared'
import { useRef, type ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { BrandMark, PageProblem } from '@/components/common'
import { Button } from '@/components/ui/button'
import { LoadingRegion, Skeleton, SkeletonLine, SkeletonRows } from '@/components/ui/skeleton'
import { AttachmentViewer, BillAttachments, useLocalPhotoUrls } from '@/features/bill-detail/BillAttachments'
import { BillComments, CommentComposer, CommentDialogs, shareComment } from '@/features/bill-detail/BillComments'
import { BillBody, type Money } from '@/features/bill-detail/BillDetailSheet'
import { t } from '@/i18n'
import { formatDayTime } from '@/lib/dates'
import { formatMoney } from '@/lib/money'

/**
 * A bill's link opened (7.3h), blitzsplit.com/c/<code>: a page of its own, outside the home's frame, in a centered
 * column (the phone's whole width). It draws the bill as Conta does, read-only (no Editar, Apagar, Histórico nor
 * adding or removing photos), its attachments and comments; whoever is signed in comments, in the group or not, and
 * likes comments and edits and deletes their own, online only. A link shared at a comment scrolls to it.
 */
export function SharedBillScreen({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new SharedBillPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsSharedBillState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: JsSharedBillAction) => void) => p.observeActions(onAction), (action) => {
    if (action.type === 'comment-error') toast(t('sharedBill_commentError'))
    if (action.type === 'needs-name') toast(t('sharedBill_needsName'))
    if (action.type === 'comment-change-error') toast(t('sharedBill_commentChangeError'))
    if (action.type === 'comment-gone') toast(t('billDetail_commentGone'))
    if (action.type === 'share-comment' && action.share) shareComment(action.share)
  })
  if (!presenter || !state) return null
  return (
    <div className="min-h-dvh bg-background">
      <header className="sticky top-0 z-10 border-b bg-background/95 backdrop-blur">
        <div className="mx-auto flex h-14 w-full max-w-[720px] items-center gap-2 px-4">
          <button
            type="button"
            onClick={() => presenter.close()}
            className="mr-auto rounded-md outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
          >
            <BrandMark />
          </button>
          {state.canOpenInApp && (
            <Button variant="outline" size="sm" onClick={() => presenter.openInApp()}>
              {t('sharedBill_openInApp')}
            </Button>
          )}
          {state.showsSignIn && (
            <Button size="sm" onClick={() => presenter.signIn()}>
              {t('sharedBill_signIn')}
            </Button>
          )}
        </div>
      </header>
      <main className="mx-auto flex w-full max-w-[720px] flex-col gap-6 px-4 pt-6 pb-12">
        {state.status === 'loading' && (
          <LoadingRegion isDelayed={false} label={t('sharedBill_loading')} className="flex flex-col gap-6">
            <SkeletonLine className="h-4 w-1/2" />
            <span className="flex items-center gap-3">
              <Skeleton className="size-10 rounded-xl" />
              <span className="flex flex-1 flex-col gap-2">
                <SkeletonLine className="h-4 w-1/2" />
                <SkeletonLine className="h-2.5 w-1/4" />
              </span>
            </span>
            <Skeleton className="h-9 w-40" />
            <SkeletonRows count={3} className="rounded-lg border" />
          </LoadingRegion>
        )}
        {state.status === 'open' && <OpenBill presenter={presenter} state={state} />}
        {state.status === 'not-found' && (
          <Problem icon={<Link2Off className="size-10" />} title={t('sharedBill_notFoundTitle')} message={t('sharedBill_notFoundBody')} onLeave={() => presenter.close()} />
        )}
        {state.status === 'members-only' && (
          <Problem icon={<Lock className="size-10" />} title={t('sharedBill_membersOnlyTitle')} message={t(state.showsSignIn ? 'sharedBill_membersOnlyBody' : 'sharedBill_membersOnlyOutsideBody')} onLeave={() => presenter.close()}>
            {state.showsSignIn && (
              <Button size="lg" onClick={() => presenter.signIn()}>
                {t('sharedBill_signIn')}
              </Button>
            )}
          </Problem>
        )}
        {state.status === 'failed' && (
          <Problem
            icon={<CloudOff className="size-10" />}
            title={t('sharedBill_failedTitle')}
            message={state.isOnline ? undefined : t('sharedBill_offline')}
            onLeave={() => presenter.close()}
          >
            <Button size="lg" onClick={() => presenter.retry()}>
              {t('sharedBill_retry')}
            </Button>
          </Problem>
        )}
      </main>
    </div>
  )
}

function OpenBill({ presenter, state }: { presenter: SharedBillPresenter; state: JsSharedBillState }) {
  const composerRef = useRef<HTMLTextAreaElement>(null)
  const localUrls = useLocalPhotoUrls(state.attachments)
  const money: Money = (minor) => formatMoney(minor, state.currency, state.fractionDigits)
  // Why: "Responder" and "Editar" leave the person typing the reply or the edit.
  const onReply = (commentId: string) => {
    presenter.reply(commentId)
    composerRef.current?.focus()
  }
  const onEditComment = (commentId: string) => {
    presenter.editComment(commentId)
    composerRef.current?.focus()
  }
  return (
    <>
      <p className="flex items-start gap-2.5 rounded-lg border bg-muted/50 px-3.5 py-3 text-[13px] leading-[18px] text-muted-foreground">
        <Info aria-hidden className="mt-px size-4 shrink-0" />
        {t('sharedBill_groupBanner', { group: state.groupName })}
      </p>
      <BillBody
        state={state}
        money={money}
        attachments={state.attachments.length > 0 && <BillAttachments presenter={presenter} state={state} localUrls={localUrls} />}
        title={<h1 className="truncate text-lg leading-6 font-semibold">{state.description}</h1>}
        description={<p className="text-[13px] text-muted-foreground">{formatDayTime(state.spentAt, state.spentDay, state.hasSpentTime)}</p>}
      />
      <div className="flex flex-col gap-3">
        {/* Why: the heart, Editar and Apagar go straight to the server, so they wait while it can't be reached. */}
        <BillComments
          state={state}
          presenter={presenter}
          canChange={state.canChangeComments}
          onReply={state.showsComposer ? onReply : undefined}
          onEdit={onEditComment}
        />
        {state.showsComposer && <CommentComposer presenter={presenter} state={state} inputRef={composerRef} className="border-t pt-3" />}
        {state.showsSignInToComment && (
          <div className="flex flex-col items-start gap-3 rounded-lg border px-4 py-3.5 sm:flex-row sm:items-center">
            <p className="flex-1 text-sm text-muted-foreground">{t('sharedBill_signInToCommentBody')}</p>
            <Button variant="outline" onClick={() => presenter.signIn()}>
              {t('sharedBill_signInToComment')}
            </Button>
          </div>
        )}
      </div>
      <AttachmentViewer presenter={presenter} state={state} localUrls={localUrls} />
      <CommentDialogs state={state} presenter={presenter} isBusy={state.isChangingComment} />
    </>
  )
}

/** Why it doesn't open, what may help, and "Ir para o Bora Rachar". */
function Problem({ icon, title, message, onLeave, children }: { icon: ReactNode; title: string; message?: string; onLeave: () => void; children?: ReactNode }) {
  return (
    <PageProblem icon={icon} title={title} message={message}>
      {children}
      <Button variant="link" onClick={onLeave}>
        {t('sharedBill_goToApp')}
      </Button>
    </PageProblem>
  )
}
