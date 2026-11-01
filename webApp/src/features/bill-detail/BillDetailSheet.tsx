import { Pencil, ReceiptText, Trash2, X } from 'lucide-react'
import { BillDetailPresenter, type JsBillDetailState, type JsBillPayer, type JsBillShareRow, type JsMyPartNote, type JsRoute } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Drawer, DrawerContent, DrawerDescription, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { Sheet, SheetClose, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { formatDayTime } from '@/lib/dates'
import { formatMoney, formatNames } from '@/lib/money'
import { payerNamesText } from '@/lib/payers'
import { cn } from '@/lib/utils'
import { BillDeleteDialog } from './BillDeleteDialog'

type Money = (minor: number) => string

// Why: the split's header names its mode with how many are in it.
const splitTitles: Record<string, MessageKey> = {
  equal: 'billDetail_splitEqual',
  amount: 'billDetail_splitAmount',
  percent: 'billDetail_splitPercent',
  shares: 'billDetail_splitShares',
  items: 'billDetail_splitItems',
}

/**
 * "Conta", a bill of the group: a side panel over Despesas do grupo (which stays mounted under it) on desktop and
 * tablet, a drawer on the phone. Editar opens Editar conta in its place, so the panel closes while it is `isCovered`
 * and keeps its presenter, coming back as it was when Editar conta closes. Closing it any way is the ViewModel's back.
 */
export function BillDetailSheet({ route, isCovered }: { route: JsRoute; isCovered: boolean }) {
  const presenter = usePresenter(() => new BillDetailPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsBillDetailState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), (action) => {
    if (action === 'delete-error') toast(t('billDetail_deleteError'))
  })
  if (!presenter || !state?.isLoaded) return null

  const money: Money = (minor) => formatMoney(minor, state.currency, state.fractionDigits)
  const onOpenChange = (open: boolean) => {
    if (!open) presenter.back()
  }
  const actions = <BillActions presenter={presenter} state={state} />
  const deletion = state.deletion && (
    <BillDeleteDialog
      deletion={state.deletion}
      isDeleting={state.isDeleting}
      money={money}
      onConfirm={() => presenter.confirmDelete()}
      onDismiss={() => presenter.dismissDelete()}
    />
  )

  return isDesktop ? (
    <>
      <Sheet open={!isCovered} onOpenChange={onOpenChange}>
        <SheetContent side="right" showCloseButton={false} className="w-full gap-0 sm:max-w-[560px] lg:max-w-[880px]">
          <header className="flex h-14 shrink-0 items-center gap-1 border-b pr-3 pl-6">
            <span className="mr-auto truncate text-sm font-medium text-muted-foreground">{state.groupName}</span>
            {actions}
            <SheetClose asChild>
              <Button variant="ghost" size="icon-sm" aria-label={t('common_close')} className="text-muted-foreground">
                <X />
              </Button>
            </SheetClose>
          </header>
          <div className="flex-1 overflow-y-auto p-6">
            <BillBody state={state} money={money} title={<SheetTitle className={titleClass}>{state.description}</SheetTitle>} description={<SheetDescription className={descriptionClass}>{formatDayTime(state.spentAt, state.spentDay)}</SheetDescription>} />
          </div>
        </SheetContent>
      </Sheet>
      {deletion}
    </>
  ) : (
    <>
      <Drawer open={!isCovered} onOpenChange={onOpenChange} repositionInputs={false}>
        <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
          <DrawerHeader className="flex-row items-center gap-1 border-b py-2 pr-2 group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
            <span className="mr-auto truncate text-sm font-medium text-muted-foreground">{state.groupName}</span>
            {actions}
          </DrawerHeader>
          <div className="overflow-y-auto p-4 pb-6">
            <BillBody state={state} money={money} title={<DrawerTitle className={titleClass}>{state.description}</DrawerTitle>} description={<DrawerDescription className={descriptionClass}>{formatDayTime(state.spentAt, state.spentDay)}</DrawerDescription>} />
          </div>
        </DrawerContent>
      </Drawer>
      {deletion}
    </>
  )
}

const titleClass = 'truncate text-lg leading-6 font-semibold'
const descriptionClass = 'text-[13px] text-muted-foreground'

/** Editar and Apagar, which do nothing until the bill is read and while it is being deleted. */
function BillActions({ presenter, state }: { presenter: BillDetailPresenter; state: JsBillDetailState }) {
  return (
    <>
      <Button variant="ghost" size="sm" disabled={!state.areActionsEnabled} onClick={() => presenter.edit()}>
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

/**
 * The bill: what, when, how much and who paid, "Sua parte" for someone in it, and each person's part. The panel's
 * two columns put the split beside the summary on desktop, one under the other below that.
 */
function BillBody({ state, money, title, description }: { state: JsBillDetailState; money: Money; title: ReactNode; description: ReactNode }) {
  const paid = paidText(state.payers)
  const faces = state.payers.filter((payer) => payer.name != null).slice(0, 3)
  return (
    <div className="flex flex-col gap-6 lg:grid lg:grid-cols-2 lg:items-start lg:gap-8">
      <div className="flex min-w-0 flex-col gap-5">
        <div className="flex items-center gap-3">
          <span aria-hidden className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-muted text-primary">
            <ReceiptText className="size-5" />
          </span>
          <div className="flex min-w-0 flex-1 flex-col gap-0.5">
            {title}
            {description}
          </div>
        </div>
        <div className="flex flex-col gap-1.5">
          <span className="text-[32px] leading-10 font-semibold tracking-tight tabular-nums">{money(state.totalMinor)}</span>
          <span className="flex items-center gap-2 text-sm text-muted-foreground">
            {faces.length > 0 && (
              <span aria-hidden className="flex shrink-0">
                {faces.map((payer, index) => (
                  <Avatar key={index} initial={payer.initials} color={payer.color} className={cn('size-5 border-2 border-card text-[10px]', index > 0 && '-ml-1.5')} />
                ))}
              </span>
            )}
            {paid}
          </span>
          {state.isPending && (
            <span
              role="status"
              className="inline-flex h-[22px] items-center gap-1.5 self-start rounded-md border border-amber-200 bg-amber-50 px-2 text-xs font-semibold text-amber-800 dark:border-amber-900 dark:bg-amber-950/60 dark:text-amber-300"
            >
              <span aria-hidden className="size-1.5 rounded-full bg-orange-500" />
              {t('billDetail_pending')}
            </span>
          )}
        </div>
        {state.showsMyPart && state.myBalanceMinor != null && <MyPart balance={state.myBalanceMinor} note={state.myPartNote} money={money} />}
      </div>

      <section aria-labelledby="bill-split-title" className="flex min-w-0 flex-col gap-2">
        <h3 id="bill-split-title" className="text-sm font-medium">
          {t(splitTitles[state.splitMode] ?? 'billDetail_splitEqual', { count: state.participantCount })}
        </h3>
        <ul className="flex flex-col divide-y border-y">
          {state.people.map((row) => (
            <ShareRow key={row.memberId} row={row} money={money} />
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
  const tone = balance < 0 ? 'text-destructive' : balance > 0 ? 'text-primary' : 'text-muted-foreground'
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

/** A person's part: "Pagou R$ 642,00", "Ainda não entrou" or "Não participa" under the name, "—" when out of it. */
function ShareRow({ row, money }: { row: JsBillShareRow; money: Money }) {
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
    <li className="flex min-h-12 items-center gap-3 py-2">
      <Avatar initial={row.initials} color={row.color} className="size-8 text-xs" />
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="truncate text-sm font-medium">{name}</span>
        {subtitle && <span className="truncate text-xs text-muted-foreground">{subtitle}</span>}
      </span>
      <span className={cn('font-mono text-[13px] tabular-nums', row.shareMinor == null && 'text-muted-foreground')}>
        {row.shareMinor != null ? money(row.shareMinor) : '—'}
      </span>
    </li>
  )
}
