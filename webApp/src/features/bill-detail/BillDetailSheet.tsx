import { Pencil, Share2, Trash2, X } from 'lucide-react'
import { BillDetailPresenter, type JsBillDetailAction, type JsBillDetailState, type JsBillPayer, type JsBillShareRow, type JsMyPartNote, type JsRoute } from 'blitzsplit-shared'
import { useRef, type ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar, PendingDot, pendingSurfaceClass, PersonButton } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Drawer, DrawerContent, DrawerDescription, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { Sheet, SheetClose, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { LoadingRegion, Skeleton, SkeletonLine, SkeletonRows } from '@/components/ui/skeleton'
import { balanceTone } from '@/features/balances/BalanceCard'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { formatDayTime } from '@/lib/dates'
import { formatMoney, formatNames } from '@/lib/money'
import { payerNamesText } from '@/lib/payers'
import { CategoryBadge, categoryPath } from '@/lib/categories'
import { cn } from '@/lib/utils'
import { AttachmentViewer, BillAttachments, useLocalPhotoUrls } from './BillAttachments'
import { BillComments, CommentComposer, CommentDialogs, shareComment } from './BillComments'
import { BillDeleteDialog } from './BillDeleteDialog'
import { BillHistory } from './BillHistory'
import { splitTitles } from './billHistoryText'

export type Money = (minor: number) => string

/**
 * "Conta", a bill of the group: a side panel over Despesas do grupo (which stays mounted under it) on desktop and
 * tablet, a drawer on the phone. Editar opens Editar conta in its place, so the panel closes while it is `isCovered`
 * and keeps its presenter, coming back as it was when Editar conta closes. Closing it any way is the ViewModel's back.
 *
 * The bill comes first, its photos ("Anexos", opening in a lightbox) between "Sua parte" and the split, as the board
 * draws them, then Comentários and Histórico, with the composer pinned under them: from the desktop the
 * 880 px panel puts the bill on the left and the conversation on the right, each scrolling on its own; the tablet's
 * 560 px panel and the phone's drawer scroll it all in one column over the composer.
 */
export function BillDetailSheet({ route, isCovered }: { route: JsRoute; isCovered: boolean }) {
  const presenter = usePresenter(() => new BillDetailPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsBillDetailState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const isWide = useMediaQuery('(min-width: 1024px)')
  const composerRef = useRef<HTMLTextAreaElement>(null)
  useActions(presenter, (p, onAction: (action: JsBillDetailAction) => void) => p.observeActions(onAction), (action) => {
    if (action.type === 'share-comment' && action.share) shareComment(action.share)
    const message = actionMessages[action.type]
    if (message) toast(t(message))
  })
  const localPhotoUrls = useLocalPhotoUrls(state?.attachments ?? [])
  if (!presenter || !state) return null
  // Why: the panel opens at once, the bill shimmering in it until it was read, rather than nothing on the tap.
  const isLoaded = state.isLoaded

  const money: Money = (minor) => formatMoney(minor, state.currency, state.fractionDigits)
  const onOpenChange = (open: boolean) => {
    if (!open) presenter.back()
  }
  // Why: "Responder" and "Editar" leave the person typing the reply or the edit.
  const onReply = (commentId: string) => {
    presenter.reply(commentId)
    composerRef.current?.focus()
  }
  const onEditComment = (commentId: string) => {
    presenter.editComment(commentId)
    composerRef.current?.focus()
  }
  const openPerson = (memberId: string) => presenter.openPerson(memberId)
  const actions = isLoaded && <BillActions presenter={presenter} state={state} />
  const groupName = isLoaded ? (
    <span className="mr-auto truncate text-sm font-medium text-muted-foreground">{state.groupName}</span>
  ) : (
    <SkeletonLine className="mr-auto w-28" />
  )
  const offline = isLoaded && !state.isOnline && (
    <p role="status" className={cn('rounded-lg border px-3.5 py-2.5 text-[13px] leading-[18px]', pendingSurfaceClass)}>
      {t('billDetail_offline')}
    </p>
  )
  const conversation = isLoaded && (
    <>
      <BillComments state={state} presenter={presenter} onReply={onReply} onEdit={onEditComment} onPerson={openPerson} />
      <BillHistory history={state.history} isUndoing={state.isUndoing} money={money} onUndo={() => presenter.undo()} onPerson={openPerson} />
    </>
  )
  const composer = (className: string) => isLoaded && <CommentComposer presenter={presenter} state={state} inputRef={composerRef} className={className} />
  const attachments = <BillAttachments presenter={presenter} state={state} localUrls={localPhotoUrls} />
  // Why: outside the panel, like the delete dialog, so the lightbox covers the whole window over the drawer too.
  const viewer = <AttachmentViewer presenter={presenter} state={state} localUrls={localPhotoUrls} />
  const deletion = state.deletion && (
    <BillDeleteDialog
      deletion={state.deletion}
      isDeleting={state.isDeleting}
      onConfirm={() => presenter.confirmDelete()}
      onDismiss={() => presenter.dismissDelete()}
    />
  )
  const commentDialogs = <CommentDialogs state={state} presenter={presenter} />

  if (isDesktop) {
    const body = !isLoaded ? (
      <BillBodySkeleton title={<SheetTitle className="sr-only">{t('common_loading')}</SheetTitle>} />
    ) : (
      <BillBody
        state={state}
        money={money}
        attachments={attachments}
        onPerson={openPerson}
        title={<SheetTitle className={titleClass}>{state.description}</SheetTitle>}
        description={<SheetDescription className={descriptionClass}>{formatDayTime(state.spentAt, state.spentDay, state.hasSpentTime)}</SheetDescription>}
      />
    )
    return (
      <>
        <Sheet open={!isCovered} onOpenChange={onOpenChange}>
          <SheetContent side="right" showCloseButton={false} className="w-full gap-0 sm:max-w-[560px] lg:max-w-[880px]">
            <header className="flex h-14 shrink-0 items-center gap-1 border-b pr-3 pl-6">
              {groupName}
              {actions}
              <SheetClose asChild>
                <Button variant="ghost" size="icon-sm" aria-label={t('common_close')} className="text-muted-foreground">
                  <X />
                </Button>
              </SheetClose>
            </header>
            {offline && <div className="shrink-0 px-6 pt-4">{offline}</div>}
            {isWide && isLoaded ? (
              <div className="grid min-h-0 flex-1 grid-cols-2 gap-8 px-6">
                <div className="min-w-0 overflow-y-auto py-6">{body}</div>
                <div className="flex min-h-0 min-w-0 flex-col">
                  <div className="flex flex-1 flex-col gap-6 overflow-y-auto py-6">{conversation}</div>
                  {composer('shrink-0 border-t pt-3 pb-6')}
                </div>
              </div>
            ) : (
              <>
                <div className="flex min-h-0 flex-1 flex-col gap-6 overflow-y-auto p-6">
                  {body}
                  {conversation}
                </div>
                {composer('shrink-0 border-t px-6 pt-3 pb-6')}
              </>
            )}
          </SheetContent>
        </Sheet>
        {viewer}
        {deletion}
        {commentDialogs}
      </>
    )
  }

  return (
    <>
      <Drawer open={!isCovered} onOpenChange={onOpenChange} repositionInputs={false}>
        <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
          <DrawerHeader className="flex-row items-center gap-1 border-b py-2 pr-2 group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
            {groupName}
            {actions}
          </DrawerHeader>
          <div className="flex min-h-0 flex-1 flex-col gap-6 overflow-y-auto p-4 pb-6">
            {offline}
            {isLoaded ? (
              <BillBody
                state={state}
                money={money}
                attachments={attachments}
                onPerson={openPerson}
                title={<DrawerTitle className={titleClass}>{state.description}</DrawerTitle>}
                description={<DrawerDescription className={descriptionClass}>{formatDayTime(state.spentAt, state.spentDay, state.hasSpentTime)}</DrawerDescription>}
              />
            ) : (
              <BillBodySkeleton title={<DrawerTitle className="sr-only">{t('common_loading')}</DrawerTitle>} />
            )}
            {conversation}
          </div>
          {/* Why: on the phone the composer stays at the bottom, the bill and the conversation scrolling above it. */}
          {composer('shrink-0 border-t px-4 pt-3 pb-[max(env(safe-area-inset-bottom),12px)]')}
        </DrawerContent>
      </Drawer>
      {viewer}
      {deletion}
      {commentDialogs}
    </>
  )
}

// Why: by the action's type, the toast each one is; "share-comment" opens the system share instead.
const actionMessages: Partial<Record<string, MessageKey>> = {
  'delete-error': 'billDetail_deleteError',
  'comment-error': 'billDetail_commentError',
  'comment-change-error': 'billDetail_commentChangeError',
  'comment-gone': 'billDetail_commentGone',
  'undo-error': 'billDetail_undoError',
  'photo-unreadable': 'billDetail_photoUnreadable',
  'photo-no-room': 'billDetail_photoNoRoom',
  'photo-too-large': 'billDetail_photoTooLarge',
  'photo-error': 'billDetail_photoError',
  'remove-photo-error': 'billDetail_removePhotoError',
}

/**
 * The bill until it was read: its category and title, the total and who paid, then each person's part, after 150 ms.
 * `title` is the panel's accessible name meanwhile, which a dialog needs.
 */
function BillBodySkeleton({ title }: { title: ReactNode }) {
  return (
    <>
      {title}
      <LoadingRegion className="flex flex-col gap-6">
        <div className="flex items-center gap-3">
          <Skeleton className="size-10 rounded-xl" />
          <span className="flex flex-1 flex-col gap-2">
            <SkeletonLine className="h-2.5 w-1/3" />
            <SkeletonLine className="h-4 w-1/2" />
            <SkeletonLine className="h-2.5 w-1/4" />
          </span>
        </div>
        <span className="flex flex-col gap-2.5">
          <Skeleton className="h-9 w-40" />
          <SkeletonLine className="w-1/3" />
        </span>
        <SkeletonRows count={3} className="rounded-lg border" />
      </LoadingRegion>
    </>
  )
}

const titleClass = 'truncate text-lg leading-6 font-semibold'
const descriptionClass = 'text-[13px] text-muted-foreground'

/** Compartilhar, Editar and Apagar, which do nothing until the bill is read and while it is being deleted. */
function BillActions({ presenter, state }: { presenter: BillDetailPresenter; state: JsBillDetailState }) {
  return (
    <>
      <Button variant="ghost" size="sm" disabled={!state.areActionsEnabled} onClick={() => presenter.share()}>
        <Share2 />
        {/* Why: three actions beside the group's name leave the phone's header no room for a third label. */}
        <span className="max-sm:sr-only">{t('billDetail_share')}</span>
      </Button>
      <Button variant="ghost" size="sm" disabled={!state.canEdit} onClick={() => presenter.edit()}>
        <Pencil />
        {t('billDetail_edit')}
      </Button>
      <Button variant="ghost" size="sm" disabled={!state.areActionsEnabled} onClick={() => presenter.deleteBill()}>
        <Trash2 />
        {t('billDetail_delete')}
      </Button>
    </>
  )
}

/** What the bill's body reads: Conta's state, or a bill's link page's, which has no "Sua parte" nor queue. */
export type BillBodyState = Pick<JsBillDetailState, 'category' | 'categoryParent' | 'totalMinor' | 'payers' | 'splitMode' | 'participantCount' | 'people'> &
  Partial<Pick<JsBillDetailState, 'isPending' | 'showsMyPart' | 'myBalanceMinor' | 'myPartNote' | 'lonePayerId' | 'showsUnknownCurrencyNote'>>

/**
 * The bill: what, when, how much and who paid, "Sua parte" for someone in it, "Anexos" and each person's part. A
 * person's row, and a lone payer, open their profile through `onPerson`.
 */
export function BillBody({
  state,
  money,
  attachments,
  title,
  description,
  onPerson,
}: {
  state: BillBodyState
  money: Money
  attachments: ReactNode
  title: ReactNode
  description: ReactNode
  onPerson?: (memberId: string) => void
}) {
  const paid = paidText(state.payers)
  const faces = state.payers.filter((payer) => payer.name != null).slice(0, 3)
  const lonePayerId = state.lonePayerId
  return (
    <div className="flex flex-col gap-6">
      <div className="flex min-w-0 flex-col gap-5">
        <div className="flex items-center gap-3">
          <CategoryBadge category={state.category} />
          <div className="flex min-w-0 flex-1 flex-col gap-0.5">
            {/* Why: the bill's category over its title, "Alimentação › Mercado" (7.3). */}
            <span className="text-[13px] text-muted-foreground">{categoryPath(state.category, state.categoryParent)}</span>
            {title}
            {description}
          </div>
        </div>
        <div className="flex flex-col gap-1.5">
          <span className="text-[32px] leading-10 font-semibold tracking-tight tabular-nums">{money(state.totalMinor)}</span>
          <PersonButton
            onOpen={onPerson && lonePayerId ? () => onPerson(lonePayerId) : null}
            className="flex items-center gap-2 self-start text-sm text-muted-foreground"
          >
            {faces.length > 0 && (
              <span aria-hidden className="flex shrink-0">
                {faces.map((payer, index) => (
                  <Avatar key={index} initial={payer.initials} color={payer.color} photoUrl={payer.photoUrl} className={cn('size-5 border-2 border-card text-[10px]', index > 0 && '-ml-1.5')} />
                ))}
              </span>
            )}
            {paid}
          </PersonButton>
          {state.showsUnknownCurrencyNote && (
            <p className="text-sm text-muted-foreground">{t('billDetail_unknownCurrency')}</p>
          )}
          {state.isPending && (
            <span
              role="status"
              className={cn('inline-flex h-[22px] items-center gap-1.5 self-start rounded-md border px-2 text-xs font-semibold', pendingSurfaceClass)}
            >
              <PendingDot />
              {t('billDetail_pending')}
            </span>
          )}
        </div>
        {state.showsMyPart && state.myBalanceMinor != null && <MyPart balance={state.myBalanceMinor} note={state.myPartNote} money={money} />}
      </div>

      {attachments}

      <section aria-labelledby="bill-split-title" className="flex min-w-0 flex-col gap-2">
        <h3 id="bill-split-title" className="text-sm font-medium">
          {t(splitTitles[state.splitMode] ?? 'billDetail_splitEqual', { count: state.participantCount })}
        </h3>
        <ul className="flex flex-col divide-y border-y">
          {state.people.map((row) => (
            <ShareRow key={row.memberId} row={row} money={money} onOpen={onPerson && row.opensProfile ? () => onPerson(row.memberId) : null} />
          ))}
        </ul>
      </section>
    </div>
  )
}

/** "Bruno pagou", "Você pagou", "Um ex-membro pagou", "Você e Ana pagaram" or "3 pessoas pagaram". */
function paidText(payers: JsBillPayer[]): string {
  if (payers.length > 1) return t('billDetail_paidBySeveral', { names: payerNamesText(payers.map((payer) => (payer.isMe ? t('splitBill_you') : nameOrFormer(payer.name)))) })
  const payer = payers[0]
  if (payer == null || payer.name == null) return t('billDetail_paidByFormer')
  return payer.isMe ? t('billDetail_paidByMe') : t('billDetail_paidBy', { name: payer.name })
}

/** "Sua parte": what the bill does to your balance, and with whom it settles. */
function MyPart({ balance, note, money }: { balance: number; note: JsMyPartNote | null | undefined; money: Money }) {
  const amount = balance > 0 ? `+${money(balance)}` : money(Math.abs(balance))
  const word = balance < 0 ? t('billDetail_myPartOwe') : balance > 0 ? t('billDetail_myPartGetBack') : t('billDetail_myPartSettled')
  const tone = balanceTone(balance)
  const noteText = note ? myPartNoteText(note, money) : null
  return (
    <div className="flex flex-col gap-1 rounded-xl border bg-card px-4 py-3.5">
      <span className="text-[13px] text-muted-foreground">{t('billDetail_myPart')}</span>
      <span className="flex flex-wrap items-baseline gap-x-2">
        <span className={cn('text-xl font-semibold tabular-nums', balance === 0 ? 'text-foreground' : tone)}>{amount}</span>
        <span className={cn('font-medium', tone)}>{word}</span>
      </span>
      {noteText && <span className="text-[13px] text-muted-foreground">{noteText}</span>}
    </div>
  )
}

function nameOrFormer(name: string | null | undefined): string {
  return name ?? t('billDetail_formerMember')
}

/** "Entra no que você deve a Bruno em Saldos.", "Ana te deve R$ 50,00." or "Ana, Bruno e Carla te devem R$ 62,40 cada." */
function myPartNoteText(note: JsMyPartNote, money: Money): string {
  const names = note.names.map(nameOrFormer)
  if (note.kind === 'owes-payers') return t('billDetail_noteOwesPayer', { name: formatNames(names) })
  if (names.length === 1) return t('billDetail_noteOwedByOne', { name: names[0], amount: money(note.eachMinor ?? note.totalMinor ?? 0) })
  if (note.eachMinor != null) return t('billDetail_noteOwedByEach', { names: formatNames(names), amount: money(note.eachMinor) })
  return t('billDetail_noteOwedByTotal', { names: formatNames(names), amount: money(note.totalMinor ?? 0) })
}

/**
 * A person's part: "Pagou R$ 642,00", "Ainda não entrou" or "Não participa" under the name, "—" when out of it. The
 * whole row opens their profile.
 */
function ShareRow({ row, money, onOpen }: { row: JsBillShareRow; money: Money; onOpen: (() => void) | null }) {
  const name = row.isMe ? t('splitBill_you') : nameOrFormer(row.name)
  const subtitle =
    row.paidMinor != null
      ? t('billDetail_rowPaid', { amount: money(row.paidMinor) })
      : !row.hasJoined
        ? t('billDetail_rowNotJoined')
        : row.shareMinor == null
          ? t('billDetail_rowNotInBill')
          : null
  return (
    <li className="flex">
      <PersonButton onOpen={onOpen} className="flex min-h-12 min-w-0 flex-1 items-center gap-3 py-2">
      <Avatar initial={row.initials} color={row.color} photoUrl={row.photoUrl} className="size-8 text-xs" />
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="truncate text-sm font-medium">{name}</span>
        {subtitle && <span className="truncate text-xs text-muted-foreground">{subtitle}</span>}
      </span>
      <span className={cn('font-mono text-[13px] tabular-nums', row.shareMinor == null && 'text-muted-foreground')}>
        {row.shareMinor != null ? money(row.shareMinor) : '—'}
      </span>
      </PersonButton>
    </li>
  )
}
