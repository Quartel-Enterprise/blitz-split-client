import { CornerDownRight, SendHorizontal, X } from 'lucide-react'
import type { BillDetailPresenter, JsBillCommentRow, JsBillDetailState } from 'blitzsplit-shared'
import type { KeyboardEvent, Ref } from 'react'
import { Avatar } from '@/components/common'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { useDraft } from '@/hooks/useDraft'
import { t } from '@/i18n'
import { formatShortDate, formatTime } from '@/lib/dates'
import { cn } from '@/lib/utils'

/**
 * "Comentários", with its count: each comment and, one level under it, its replies, in the order they were written.
 * "Responder" on any of them points the composer at it.
 */
export function BillComments({ state, onReply }: { state: JsBillDetailState; onReply: (commentId: string) => void }) {
  return (
    <section aria-labelledby="bill-comments-title" className="flex flex-col">
      <h3 id="bill-comments-title" className="flex justify-between text-sm font-medium">
        <span>{t('billDetail_comments')}</span>
        {state.commentCount > 0 && <span className="font-normal text-muted-foreground tabular-nums">{state.commentCount}</span>}
      </h3>
      {state.showsNoComments && <p className="mt-1 text-[13px] text-muted-foreground">{t('billDetail_commentsEmpty')}</p>}
      <ul className="flex flex-col">
        {state.threads.map((thread) => (
          <li key={thread.comment.id} className="mt-1.5 border-t first:mt-0 first:border-t-0">
            <Comment row={thread.comment} onReply={onReply} />
            {thread.replies.length > 0 && (
              <ul className="flex flex-col pl-[38px]">
                {thread.replies.map((reply) => (
                  <li key={reply.id}>
                    <Comment row={reply} isReply onReply={onReply} />
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}

/** "hoje, 22:40", "ontem, 22:40" or "27 de out., 22:40". */
function postedText(row: JsBillCommentRow): string {
  const time = formatTime(row.postedAt)
  if (row.postedDay === 'today') return t('billDetail_commentToday', { time })
  if (row.postedDay === 'yesterday') return t('billDetail_commentYesterday', { time })
  return `${formatShortDate(row.postedAt)}, ${time}`
}

function Comment({ row, isReply = false, onReply }: { row: JsBillCommentRow; isReply?: boolean; onReply: (commentId: string) => void }) {
  const name = row.isMe ? t('splitBill_you') : (row.authorName ?? t('billDetail_commentSomeone'))
  const posted = postedText(row)
  return (
    <article aria-label={`${name}, ${posted}`} className={cn('flex gap-2.5 pb-0.5', isReply ? 'pt-2' : 'pt-3')}>
      <Avatar initial={row.initials} color={row.color} className={isReply ? 'size-6 text-[10px]' : 'size-7 text-[11px]'} />
      <div className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="flex flex-wrap items-center gap-x-1.5 gap-y-0.5">
          <span className="text-sm font-medium">{name}</span>
          {/* Why: written by someone out of the group through the bill's open link. */}
          {row.viaLink && (
            <Badge variant="outline" className="h-5 rounded-md px-1.5 text-[11px] text-muted-foreground">
              {t('billDetail_commentViaLink')}
            </Badge>
          )}
          <span className="text-xs text-muted-foreground">{posted}</span>
        </span>
        <p className="text-sm leading-5 break-words whitespace-pre-wrap">
          {row.replyToName && <span className="font-medium text-primary">@{row.replyToName} </span>}
          {row.body}
        </p>
        {row.isPending && (
          <span role="status" className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <span aria-hidden className="size-1.5 shrink-0 rounded-full bg-orange-500" />
            {t('billDetail_commentPending')}
          </span>
        )}
        <Button variant="link" size="sm" className="h-7 self-start px-0 text-[13px] text-muted-foreground" onClick={() => onReply(row.id)}>
          {t('billDetail_reply')}
        </Button>
      </div>
    </article>
  )
}

/**
 * "Escreva um comentário" and its send button, under "Respondendo a Diego" while a reply is picked. It sends offline
 * too: the comment waits in the queue. ⌘ or Ctrl with Enter sends, Enter alone breaks the line.
 */
export function CommentComposer({ presenter, state, inputRef, className }: { presenter: BillDetailPresenter; state: JsBillDetailState; inputRef: Ref<HTMLTextAreaElement>; className?: string }) {
  const [draft, setDraft] = useDraft(state.commentDraft, (text) => presenter.changeComment(text))
  const target = state.replyTarget
  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key !== 'Enter' || !(event.metaKey || event.ctrlKey)) return
    event.preventDefault()
    presenter.sendComment()
  }
  return (
    <form
      className={cn('flex flex-col gap-1.5', className)}
      onSubmit={(event) => {
        event.preventDefault()
        presenter.sendComment()
      }}
    >
      {target && (
        <div className="flex items-center gap-1.5 text-[13px] text-muted-foreground">
          <CornerDownRight aria-hidden className="size-3.5 shrink-0" />
          <span role="status" className="min-w-0 flex-1 truncate">
            {target.isMe ? t('billDetail_replyingToMe') : t('billDetail_replyingTo', { name: target.name ?? t('billDetail_commentSomeone') })}
          </span>
          <Button type="button" variant="ghost" size="icon-sm" aria-label={t('billDetail_cancelReply')} className="-my-1 text-muted-foreground" onClick={() => presenter.cancelReply()}>
            <X />
          </Button>
        </div>
      )}
      <div className="flex items-end gap-2">
        <Textarea
          ref={inputRef}
          rows={1}
          value={draft}
          aria-label={t('billDetail_commentPlaceholder')}
          placeholder={t('billDetail_commentPlaceholder')}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={onKeyDown}
          className="max-h-32 min-h-9 resize-none py-[7px]"
        />
        <Button type="submit" size="icon" aria-label={t('billDetail_sendComment')} disabled={!state.canSendComment}>
          <SendHorizontal />
        </Button>
      </div>
    </form>
  )
}
