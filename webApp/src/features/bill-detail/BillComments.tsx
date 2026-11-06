import { Check, CornerDownRight, Ellipsis, Heart, Pencil, SendHorizontal, Share2, Trash2, X } from 'lucide-react'
import type { BillDetailPresenter, JsBillCommentLiker, JsBillCommentRow, JsBillDetailState } from 'blitzsplit-shared'
import { useEffect, useState, type KeyboardEvent, type ReactNode, type Ref } from 'react'
import { toast } from 'sonner'
import { Avatar, PendingDot, PersonButton } from '@/components/common'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { LoadingRegion, SkeletonCircle, SkeletonLine } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { formatShortDate, formatTime } from '@/lib/dates'
import { freeTextProps } from '@/lib/freeText'
import { cn } from '@/lib/utils'

/** What "Comentários" reads: Conta's state, or a bill's link page's, which names the same things the same way. */
export type CommentsState = Pick<JsBillDetailState, 'threads' | 'commentCount' | 'showsNoComments' | 'focusedCommentId'>
export type ComposerState = Pick<JsBillDetailState, 'commentDraft' | 'replyTarget' | 'canSendComment' | 'isEditingComment'>
export type ComposerPresenter = Pick<BillDetailPresenter, 'changeComment' | 'sendComment' | 'cancelReply' | 'cancelEdit'>
/** A comment's heart, its likes, its ⋯ menu and the comment a link opened at, the same on Conta and the link's page. */
export type CommentsPresenter = Pick<BillDetailPresenter, 'likeComment' | 'openLikes' | 'shareComment' | 'deleteComment' | 'focusedCommentShown'>
export type CommentDialogsState = Pick<JsBillDetailState, 'commentToDelete' | 'likesComment'>
export type CommentDialogsPresenter = Pick<BillDetailPresenter, 'confirmDeleteComment' | 'dismissDeleteComment' | 'closeLikes'>

// Why: the row a link scrolls to, found by its comment's id.
const commentElementId = (commentId: string) => `comment-${commentId}`

// Why: the panel slides in first, so the scroll lands where the comment ends up.
const FOCUS_DELAY_MS = 300
// Why: the tint holds a moment, then fades over the row's transition, about two seconds in all.
const HIGHLIGHT_MS = 1000

/**
 * "Comentários", with its count: each comment and, one level under it, its replies, in the order they were written.
 * "Responder" on any of them points the composer at it; without `onReply` (no composer to answer in) it doesn't show.
 * An author's face and name open their profile through `onPerson`, while they are in the group. Each comment has its
 * heart and its ⋯ menu (Compartilhar, and Editar and Apagar on the user's own); `canChange` false (the link's page
 * offline, signed out or with a change on its way) disables the heart and hides Editar and Apagar. The comment a link
 * opened at is scrolled to and tinted for a moment.
 */
export function BillComments({
  state,
  presenter,
  canChange = true,
  onReply,
  onEdit,
  onPerson,
}: {
  state: CommentsState
  presenter: CommentsPresenter
  canChange?: boolean
  onReply?: (commentId: string) => void
  onEdit: (commentId: string) => void
  onPerson?: (memberId: string) => void
}) {
  const highlightedId = useFocusedComment(state, presenter)
  const comment = (row: JsBillCommentRow, isReply: boolean) => (
    <Comment
      row={row}
      isReply={isReply}
      isHighlighted={row.id === highlightedId}
      canChange={canChange}
      presenter={presenter}
      onReply={onReply}
      onEdit={onEdit}
      onPerson={onPerson}
    />
  )
  return (
    <section aria-labelledby="bill-comments-title" className="flex flex-col">
      <h3 id="bill-comments-title" className="flex justify-between text-sm font-medium">
        <span>{t('billDetail_comments')}</span>
        {state.commentCount != null && state.commentCount > 0 && <span className="font-normal text-muted-foreground tabular-nums">{state.commentCount}</span>}
      </h3>
      {state.showsNoComments && <p className="mt-1 text-[13px] text-muted-foreground">{t('billDetail_commentsEmpty')}</p>}
      {!state.threads && <CommentsSkeleton />}
      <ul className="flex flex-col">
        {state.threads?.map((thread) => (
          <li key={thread.comment.id} className="mt-1.5 border-t first:mt-0 first:border-t-0">
            {comment(thread.comment, false)}
            {thread.replies.length > 0 && (
              <ul className="flex flex-col pl-[38px]">
                {thread.replies.map((reply) => (
                  <li key={reply.id}>{comment(reply, true)}</li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}

/**
 * The comment a link opened at: once its row is drawn, scrolled into view and tinted, then `focusedCommentShown`, so
 * it happens once. Returns the comment tinted, null once the tint faded.
 */
function useFocusedComment(state: CommentsState, presenter: CommentsPresenter): string | null {
  const [highlightedId, setHighlightedId] = useState<string | null>(null)
  const focusedId = state.focusedCommentId
  const hasThreads = state.threads != null
  useEffect(() => {
    if (!focusedId || !hasThreads) return
    const timer = window.setTimeout(() => {
      const reducesMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
      document.getElementById(commentElementId(focusedId))?.scrollIntoView({ block: 'center', behavior: reducesMotion ? 'auto' : 'smooth' })
      setHighlightedId(focusedId)
      presenter.focusedCommentShown()
    }, FOCUS_DELAY_MS)
    return () => window.clearTimeout(timer)
  }, [focusedId, hasThreads, presenter])
  useEffect(() => {
    if (!highlightedId) return
    const timer = window.setTimeout(() => setHighlightedId(null), HIGHLIGHT_MS)
    return () => window.clearTimeout(timer)
  }, [highlightedId])
  return highlightedId
}

/** Two comments' places while they are read: the face, the name and a line of text each. */
function CommentsSkeleton() {
  return (
    <LoadingRegion className="flex flex-col gap-4 pt-2">
      {[0, 1].map((index) => (
        <span key={index} className="flex gap-2.5">
          <SkeletonCircle className="size-7" />
          <span className="flex flex-1 flex-col gap-2 pt-1">
            <SkeletonLine className="w-1/4" />
            <SkeletonLine className={index === 0 ? 'w-4/5' : 'w-3/5'} />
          </span>
        </span>
      ))}
    </LoadingRegion>
  )
}

/** "hoje, 22:40", "ontem, 22:40" or "27 de out., 22:40": when a comment was written or a photo attached. */
export function postedText(millis: number, day: string): string {
  const time = formatTime(millis)
  if (day === 'today') return t('billDetail_commentToday', { time })
  if (day === 'yesterday') return t('billDetail_commentYesterday', { time })
  return `${formatShortDate(millis)}, ${time}`
}

function Comment({
  row,
  isReply,
  isHighlighted,
  canChange,
  presenter,
  onReply,
  onEdit,
  onPerson,
}: {
  row: JsBillCommentRow
  isReply: boolean
  isHighlighted: boolean
  canChange: boolean
  presenter: CommentsPresenter
  onReply?: (commentId: string) => void
  onEdit: (commentId: string) => void
  onPerson?: (memberId: string) => void
}) {
  const rowClass = cn(
    '-mx-2 flex gap-2.5 rounded-lg px-2 pb-0.5 transition-colors duration-1000 motion-reduce:transition-none',
    isReply ? 'pt-2' : 'pt-3',
    isHighlighted && 'bg-primary/10 duration-0',
  )
  // Why: a deleted comment kept over its replies has no face, name nor actions, only its place in the thread.
  if (row.isDeleted) {
    return (
      <article id={commentElementId(row.id)} className={rowClass}>
        <p className="pb-2 text-sm text-muted-foreground italic">{t('billDetail_commentDeleted')}</p>
      </article>
    )
  }
  const name = row.isMe ? t('splitBill_you') : (row.authorName ?? t('billDetail_commentSomeone'))
  const posted = postedText(row.postedAt, row.postedDay)
  const postedLine = row.isEdited ? `${posted} · ${t('billDetail_commentEdited')}` : posted
  const authorId = row.authorMemberId
  const openAuthor = onPerson && authorId ? () => onPerson(authorId) : null
  const avatar = <Avatar initial={row.initials} color={row.color} photoUrl={row.photoUrl} className={isReply ? 'size-6 text-[10px]' : 'size-7 text-[11px]'} />
  return (
    <article id={commentElementId(row.id)} aria-label={`${name}, ${postedLine}`} className={rowClass}>
      {/* Why: the name is the one stop for the keyboard and a screen reader; the face is only a bigger target for the pointer. */}
      {openAuthor ? (
        <button type="button" tabIndex={-1} aria-hidden onClick={openAuthor} className="flex shrink-0 self-start rounded-full">
          {avatar}
        </button>
      ) : (
        avatar
      )}
      <div className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="flex items-start gap-1">
          <span className="flex min-w-0 flex-1 flex-wrap items-center gap-x-1.5 gap-y-0.5">
            <PersonButton onOpen={openAuthor} className="text-sm font-medium">
              {name}
            </PersonButton>
            {/* Why: written by someone out of the group through the bill's open link. */}
            {row.viaLink && (
              <Badge variant="outline" className="h-5 rounded-md px-1.5 text-[11px] text-muted-foreground">
                {t('billDetail_commentViaLink')}
              </Badge>
            )}
            <span className="text-xs text-muted-foreground">{postedLine}</span>
          </span>
          {row.canInteract && <CommentMenu row={row} canChange={canChange} presenter={presenter} onEdit={onEdit} />}
        </span>
        <p className="text-sm leading-5 break-words whitespace-pre-wrap">
          {row.replyToName && <span className="font-medium text-primary">@{row.replyToName} </span>}
          {row.body}
        </p>
        {row.isPending && (
          <span role="status" className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <PendingDot />
            {t('billDetail_commentPending')}
          </span>
        )}
        {row.canInteract && (
          <span className="-ml-2 flex items-center gap-2">
            <CommentLikes row={row} canChange={canChange} presenter={presenter} />
            {onReply && (
              <Button variant="link" size="sm" className="h-7 px-0 text-[13px] text-muted-foreground" onClick={() => onReply(row.id)}>
                {t('billDetail_reply')}
              </Button>
            )}
          </span>
        )}
      </div>
    </article>
  )
}

/** "Você" for the user, "Alguém" when the server kept no name. */
function likerName(liker: JsBillCommentLiker): string {
  return liker.isMe ? t('splitBill_you') : (liker.name ?? t('billDetail_commentSomeone'))
}

/**
 * The heart, filled once the user liked it, and the count beside it, which lists who did ("Curtidas"); the pointer
 * reads their names on hover. The count opens the list even while the heart waits (`canChange`).
 */
function CommentLikes({ row, canChange, presenter }: { row: JsBillCommentRow; canChange: boolean; presenter: CommentsPresenter }) {
  const countLabel = row.likeCount === 1 ? t('billDetail_commentLikesOne') : t('billDetail_commentLikesOther', { count: row.likeCount })
  return (
    <span className="flex items-center">
      <Button
        variant="ghost"
        size="icon-sm"
        aria-pressed={row.isLikedByMe}
        aria-label={t(row.isLikedByMe ? 'billDetail_commentUnlike' : 'billDetail_commentLike')}
        disabled={!canChange}
        onClick={() => presenter.likeComment(row.id)}
        className={cn('size-7 text-muted-foreground', row.isLikedByMe && 'text-red-500 hover:text-red-500 dark:text-red-400 dark:hover:text-red-400')}
      >
        <Heart className={cn('size-4', row.isLikedByMe && 'fill-current')} />
      </Button>
      {row.likeCount > 0 && (
        <button
          type="button"
          aria-label={countLabel}
          title={row.likers.map(likerName).join(', ')}
          onClick={() => presenter.openLikes(row.id)}
          className="rounded-sm px-0.5 text-[13px] text-muted-foreground tabular-nums outline-none hover:underline focus-visible:ring-[3px] focus-visible:ring-ring/50"
        >
          {row.likeCount}
        </button>
      )}
    </span>
  )
}

/** The comment's ⋯: Compartilhar always; Editar and Apagar on the user's own while `canChange`. */
function CommentMenu({ row, canChange, presenter, onEdit }: { row: JsBillCommentRow; canChange: boolean; presenter: CommentsPresenter; onEdit: (commentId: string) => void }) {
  const showsChanges = row.canChange && canChange
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon-sm" aria-label={t('billDetail_commentMore')} className="-my-1.5 size-7 shrink-0 text-muted-foreground">
          <Ellipsis />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-44">
        <DropdownMenuItem onSelect={() => presenter.shareComment(row.id)}>
          <Share2 />
          {t('billDetail_commentShare')}
        </DropdownMenuItem>
        {showsChanges && (
          <>
            <DropdownMenuItem onSelect={() => onEdit(row.id)}>
              <Pencil />
              {t('billDetail_commentEdit')}
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem variant="destructive" onSelect={() => presenter.deleteComment(row.id)}>
              <Trash2 />
              {t('billDetail_commentDelete')}
            </DropdownMenuItem>
          </>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

/**
 * "Apagar comentário?" while the ViewModel asks it (`isBusy` while a change is on its way), and "Curtidas", who liked
 * the comment in the order they did: a dialog, a drawer on the phone.
 */
export function CommentDialogs({ state, presenter, isBusy = false }: { state: CommentDialogsState; presenter: CommentDialogsPresenter; isBusy?: boolean }) {
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const likes = state.likesComment
  return (
    <>
      {state.commentToDelete != null && (
        <ConfirmDialog
          title={t('billDetail_deleteCommentTitle')}
          description={t('billDetail_deleteCommentBody')}
          confirmLabel={t('billDetail_commentDelete')}
          isBusy={isBusy}
          onConfirm={() => presenter.confirmDeleteComment()}
          onDismiss={() => presenter.dismissDeleteComment()}
        />
      )}
      {likes && (
        <ResponsiveDialog
          isDesktop={isDesktop}
          className="sm:max-w-[400px]"
          onOpenChange={(open) => {
            if (!open) presenter.closeLikes()
          }}
          pane={{
            title: t('billDetail_likesTitle'),
            description: likes.likeCount === 1 ? t('billDetail_commentLikesOne') : t('billDetail_commentLikesOther', { count: likes.likeCount }),
            body: (
              <ul className="flex flex-col divide-y">
                {likes.likers.map((liker, index) => (
                  <li key={index} className="flex min-h-11 items-center gap-3 py-2 text-sm">
                    <Heart aria-hidden className="size-4 shrink-0 fill-current text-red-500 dark:text-red-400" />
                    <span className={cn('truncate', liker.isMe && 'font-medium')}>{likerName(liker)}</span>
                  </li>
                ))}
              </ul>
            ),
            footer: (
              <Button variant="outline" onClick={() => presenter.closeLikes()}>
                {t('common_close')}
              </Button>
            ),
          }}
        />
      )}
    </>
  )
}

/** The text a comment's "Compartilhar" sends: "Ana comentou em “Jantar”: “…” <link>", "Comentei em…" for the user. */
export function commentShareText(share: { link: string; authorName?: string | null; isMe: boolean; billDescription: string; body: string }): string {
  const params = { author: share.authorName ?? '', bill: share.billDescription, body: share.body, link: share.link }
  if (share.isMe) return t('billDetail_commentShareTextMe', params)
  if (share.authorName == null) return t('billDetail_commentShareTextSomeone', params)
  return t('billDetail_commentShareText', params)
}

/** The browser's share sheet with the comment where there is one (phones, Safari), otherwise it goes to the clipboard. */
export function shareComment(share: Parameters<typeof commentShareText>[0]) {
  const text = commentShareText(share)
  if (typeof navigator.share === 'function') {
    navigator.share({ text }).catch(() => {})
    return
  }
  navigator.clipboard
    .writeText(text)
    .then(() => toast(t('billDetail_commentCopied')))
    .catch(() => toast(t('invite_copyFailed')))
}

/** A line over the composer, "Respondendo a Diego" or "Editando comentário", with its ✕. */
function ComposerStrip({ icon, text, cancelLabel, onCancel }: { icon: ReactNode; text: string; cancelLabel: string; onCancel: () => void }) {
  return (
    <div className="flex items-center gap-1.5 text-[13px] text-muted-foreground">
      {icon}
      <span role="status" className="min-w-0 flex-1 truncate">
        {text}
      </span>
      <Button type="button" variant="ghost" size="icon-sm" aria-label={cancelLabel} className="-my-1 text-muted-foreground" onClick={onCancel}>
        <X />
      </Button>
    </div>
  )
}

/**
 * "Escreva um comentário" and its send button, under "Respondendo a Diego" while a reply is picked, or "Editando
 * comentário" while the user's own is being edited, its text in the field and Salvar in the send button's place. It
 * sends offline too: the comment waits in the queue. ⌘ or Ctrl with Enter sends, Enter alone breaks the line.
 */
export function CommentComposer({ presenter, state, inputRef, className }: { presenter: ComposerPresenter; state: ComposerState; inputRef: Ref<HTMLTextAreaElement>; className?: string }) {
  const [draft, setDraft] = useDraft(state.commentDraft, (text) => presenter.changeComment(text))
  const target = state.replyTarget
  const isEditing = state.isEditingComment
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
      {isEditing && (
        <ComposerStrip
          icon={<Pencil aria-hidden className="size-3.5 shrink-0" />}
          text={t('billDetail_editingComment')}
          cancelLabel={t('billDetail_cancelEdit')}
          onCancel={() => presenter.cancelEdit()}
        />
      )}
      {target && !isEditing && (
        <ComposerStrip
          icon={<CornerDownRight aria-hidden className="size-3.5 shrink-0" />}
          text={target.isMe ? t('billDetail_replyingToMe') : t('billDetail_replyingTo', { name: target.name ?? t('billDetail_commentSomeone') })}
          cancelLabel={t('billDetail_cancelReply')}
          onCancel={() => presenter.cancelReply()}
        />
      )}
      <div className="flex items-end gap-2">
        <Textarea
          ref={inputRef}
          rows={1}
          {...freeTextProps(draft, setDraft)}
          aria-label={t('billDetail_commentPlaceholder')}
          placeholder={t('billDetail_commentPlaceholder')}
          onKeyDown={onKeyDown}
          className="max-h-32 min-h-9 resize-none py-[7px]"
        />
        <Button type="submit" size="icon" aria-label={t(isEditing ? 'billDetail_saveComment' : 'billDetail_sendComment')} disabled={!state.canSendComment}>
          {isEditing ? <Check /> : <SendHorizontal />}
        </Button>
      </div>
    </form>
  )
}
