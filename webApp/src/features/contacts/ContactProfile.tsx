import { ArrowRight, Check, ChevronLeft, CircleDollarSign, Download, Ellipsis, EyeOff, Eye, Info, Merge, ReceiptText, UserPlus, Wallet, WifiOff } from 'lucide-react'
import {
  ContactProfilePresenter,
  type JsContact,
  type JsContactHistoryItem,
  type JsContactProfileAction,
  type JsContactProfileState,
} from 'blitzsplit-shared'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { balanceTone, paymentMethodLabel } from '@/features/balances/BalanceCard'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { t } from '@/i18n'
import { formatDayTime, formatShortDate } from '@/lib/dates'
import { formatMoney, formatNames, formatSignedMoney } from '@/lib/money'
import { copyLink, shareOrCopy } from '@/lib/share'
import { cn } from '@/lib/utils'
import { AddToGroupDialog, MergeDialog } from './ContactProfileDialogs'
import { netWord } from './contactWords'

/** The profile's ViewModel for one person, with what it asks the page to do (copy, share, a toast). */
export function useContactProfile(personKey: string) {
  const presenter = usePresenter(() => new ContactProfilePresenter(personKey))
  const state = useWatch(presenter, (p, onChange: (state: JsContactProfileState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: JsContactProfileAction) => void) => p.observeActions(onAction), onProfileAction)
  return { presenter, state }
}

function onProfileAction(action: JsContactProfileAction) {
  switch (action.type) {
    case 'copy':
      if (action.link) {
        copyLink(action.link)
        toast(t('invite_linkCopied'))
      }
      break
    case 'share':
      if (action.link) shareOrCopy(t('contactProfile_mergeShareMessage', { name: action.name ?? '', link: action.link }), action.link)
      break
    case 'added':
      toast(t('contactProfile_added', { name: action.name ?? '', group: action.groupName ?? '' }))
      break
    case 'cancel-merge-error':
      toast(t('contactProfile_cancelMergeError'))
      break
    case 'change-error':
      toast(t('contacts_changeError'))
      break
    default:
      toast(t('groups_comingSoon'))
  }
}

/**
 * "Perfil do contato": who the person is, the net with them and what to do about it, the groups in common with what
 * each adds, and the latest bills and payments between you. Beside Contatos on the desktop, in its place elsewhere
 * (with `onBack`).
 */
export function ContactProfileView({
  presenter,
  state,
  contact,
  onBack,
}: {
  presenter: ContactProfilePresenter
  state: JsContactProfileState
  contact: JsContact
  onBack?: () => void
}) {
  const name = contact.name
  const groupsMeta = contact.groups.length === 1 ? t('contactProfile_groupsOne') : t('contacts_groupsInCommon', { count: contact.groups.length })
  return (
    <section aria-label={t('contactProfile_pane', { name })} className="@container flex min-w-0 flex-col gap-5">
      {onBack && (
        <Button variant="ghost" size="sm" onClick={onBack} className="-mb-1 -ml-2 self-start">
          <ChevronLeft />
          {t('contacts_title')}
        </Button>
      )}
      <header className="flex items-center gap-3.5">
        <Avatar initial={contact.initials} color={contact.color} className="size-12 text-lg @md:size-14" />
        <div className="flex min-w-0 flex-1 flex-col gap-1.5">
          <h2 className="truncate text-xl font-semibold tracking-tight @md:text-2xl">{name}</h2>
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant={contact.isGuest ? 'outline' : 'secondary'}>{t(contact.isGuest ? 'contactProfile_guest' : 'contactProfile_usesApp')}</Badge>
            <span className="text-[13px] text-muted-foreground">{groupsMeta}</span>
          </div>
        </div>
        <ProfileMenu presenter={presenter} state={state} contact={contact} />
      </header>
      {state.showsOfflineNotice && (
        <span className="inline-flex items-center gap-1.5 self-start rounded-md border px-2 py-1 text-xs text-muted-foreground">
          <WifiOff aria-hidden className="size-3.5" />
          {t('contactProfile_offline')}
        </span>
      )}
      {state.showsGuestCard && (
        <div role="note" className="flex gap-3 rounded-lg border px-4 py-3.5">
          <Info aria-hidden className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
          <div className="flex flex-1 flex-col gap-1">
            <span className="text-sm font-medium">{t('contactProfile_guestTitle', { name })}</span>
            <span className="text-[13px] text-muted-foreground">{t('contactProfile_guestBody', { name })}</span>
            <div className="mt-1.5 flex flex-wrap gap-2">
              {state.canMerge && (
                <Button variant="outline" size="sm" onClick={() => presenter.merge()}>
                  {t('contactProfile_mergeButton')}
                </Button>
              )}
              <Button variant="secondary" size="sm" onClick={() => presenter.inviteGuest()}>
                {t('contactProfile_invite', { name })}
              </Button>
            </div>
          </div>
        </div>
      )}
      {state.showsPendingMerge && state.pendingMergeRequestedAt != null && (
        <div role="status" className="flex items-center gap-3 rounded-lg border px-4 py-3.5">
          <span className="flex min-w-0 flex-1 flex-col gap-0.5">
            <span className="text-sm font-medium">{t('contactProfile_pendingTitle', { name })}</span>
            <span className="text-[13px] text-muted-foreground">
              {t('contactProfile_pendingBody', { when: formatDayTime(state.pendingMergeRequestedAt, 'other') })}
            </span>
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={state.isCancellingMerge}
            aria-label={t('contactProfile_cancelRequest', { name })}
            onClick={() => presenter.cancelMerge()}
          >
            {t('common_cancel')}
          </Button>
        </div>
      )}
      <BalanceSection presenter={presenter} state={state} contact={contact} />
      <GroupsInCommon contact={contact} isNetOfSeveralGroups={state.isNetOfSeveralGroups} />
      {state.history.length > 0 && (
        <section aria-label={t('contactProfile_history', { name })} className="flex flex-col gap-2.5">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold">{t('contactProfile_history', { name })}</h3>
            {state.hasMoreHistory && (
              <Button variant="ghost" size="sm" onClick={() => presenter.seeAllHistory()} className="-mr-2 h-7">
                {t('contactProfile_seeAll')}
              </Button>
            )}
          </div>
          <ul className="overflow-hidden rounded-lg border">
            {state.history.map((item) => (
              <HistoryRow key={item.id} item={item} name={name} />
            ))}
          </ul>
        </section>
      )}
      {state.isHideConfirmOpen && (
        <ConfirmDialog
          title={t('contactProfile_hideTitle', { name })}
          description={t('contactProfile_hideBody', { name, groups: formatNames(contact.groups.map((group) => group.name)) })}
          confirmLabel={t('contactProfile_hideConfirm')}
          isDestructive={false}
          onConfirm={() => presenter.confirmHide()}
          onDismiss={() => presenter.dismissHide()}
        />
      )}
      {state.addToGroup && <AddToGroupDialog presenter={presenter} draft={state.addToGroup} contact={contact} />}
      {state.merge && <MergeDialog presenter={presenter} sheet={state.merge} contact={contact} isOnline={state.isOnline} />}
    </section>
  )
}

/** ⋯: Juntar (a guest's, online only), "Ver em Saldos", then Ocultar at zero or Mostrar for someone hidden. */
function ProfileMenu({ presenter, state, contact }: { presenter: ContactProfilePresenter; state: JsContactProfileState; contact: JsContact }) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" size="icon" aria-label={t('contactProfile_more')} className="self-start">
          <Ellipsis />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-68">
        {contact.isGuest && (
          <DropdownMenuItem disabled={!state.canMerge} onSelect={() => presenter.merge()} className="items-start">
            <Merge className="mt-0.5" />
            <MenuLabel label={t('contactProfile_merge', { name: contact.name })} note={state.isOnline ? null : t('contactProfile_needsInternet')} />
          </DropdownMenuItem>
        )}
        <DropdownMenuItem onSelect={() => presenter.viewBalances()}>
          <Wallet />
          {t('contactProfile_viewBalances')}
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        {state.canShow ? (
          <DropdownMenuItem onSelect={() => presenter.show()}>
            <Eye />
            {t('contactProfile_show')}
          </DropdownMenuItem>
        ) : (
          <DropdownMenuItem disabled={!state.canHide} onSelect={() => presenter.hide()} className="items-start">
            <EyeOff className="mt-0.5" />
            <MenuLabel label={t('contactProfile_hide')} note={state.canHide ? null : t('contactProfile_hideBlocked')} />
          </DropdownMenuItem>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

function MenuLabel({ label, note }: { label: string; note: string | null }) {
  return (
    <span className="flex flex-col gap-0.5">
      <span>{label}</span>
      {note && <span className="text-xs text-muted-foreground">{note}</span>}
    </span>
  )
}

/** The net with the person, across every group, and the actions its sign allows; "Adicionar a um grupo" always. */
function BalanceSection({ presenter, state, contact }: { presenter: ContactProfilePresenter; state: JsContactProfileState; contact: JsContact }) {
  const name = contact.name
  const net = contact.netMinor
  const label = net > 0 ? t('contactProfile_owesYou', { name }) : net < 0 ? t('contactProfile_youOwe', { name }) : t('contactProfile_settled')
  const isAllSettled = contact.groups.every((group) => group.amountMinor === 0)
  const sub = isAllSettled
    ? t('contactProfile_nothingOpen')
    : state.isNetOfSeveralGroups
      ? t('contactProfile_netOfGroups', { count: contact.groups.length })
      : (contact.groups[0]?.name ?? '')
  const actions = [
    state.canCharge && { key: 'charge', label: t('globalBalances_charge'), aria: t('globalBalances_chargePerson', { name }), icon: <CircleDollarSign />, isMain: true, onClick: () => presenter.charge() },
    state.canReceive && { key: 'received', label: t('globalBalances_received'), aria: t('globalBalances_receivedFrom', { name }), icon: <Download />, isMain: false, onClick: () => presenter.received() },
    state.canSettle && { key: 'settle', label: t('globalBalances_settle'), aria: t('globalBalances_settleWith', { name }), icon: <Check />, isMain: true, onClick: () => presenter.settle() },
    { key: 'add', label: t('contactProfile_addToGroup'), aria: t('contactProfile_addToGroupPerson', { name }), icon: <UserPlus />, isMain: false, onClick: () => presenter.addToGroup() },
  ].filter((action) => action !== false)
  return (
    <section
      aria-label={t('contactProfile_balance', { name })}
      className="flex flex-col gap-4 rounded-xl border bg-card p-5 @md:flex-row @md:flex-wrap @md:items-end @md:justify-between"
    >
      <div className="flex flex-col gap-0.5">
        <span className="text-[13px] text-muted-foreground">{label}</span>
        <strong className={cn('text-[28px] leading-tight font-bold tracking-tight whitespace-nowrap tabular-nums @md:text-[32px]', balanceTone(net))}>
          {formatSignedMoney(net, contact.currency, contact.fractionDigits)}
        </strong>
        <span className="text-[13px] text-muted-foreground">{sub}</span>
      </div>
      <div className="grid grid-cols-2 gap-2 @md:flex @md:flex-wrap">
        {actions.map((action, index) => (
          <Button
            key={action.key}
            variant={action.isMain ? 'default' : 'outline'}
            aria-label={action.aria}
            onClick={action.onClick}
            // Why: in a narrow column the actions sit two by two; the last one alone takes the whole row.
            className={cn((actions.length < 3 || index === 2) && '@max-md:col-span-2')}
          >
            {action.icon}
            {action.label}
          </Button>
        ))}
      </div>
    </section>
  )
}

/** "Grupos em comum": a table from the tablet up, rows with the word under the name on a phone, then the net. */
function GroupsInCommon({ contact, isNetOfSeveralGroups }: { contact: JsContact; isNetOfSeveralGroups: boolean }) {
  const columns = '@md:grid @md:grid-cols-[minmax(0,1fr)_120px_120px]'
  return (
    <section aria-label={t('contactProfile_groups', { count: contact.groups.length })} className="flex flex-col gap-2.5">
      <h3 className="text-sm font-semibold">{t('contactProfile_groups', { count: contact.groups.length })}</h3>
      <div className="overflow-hidden rounded-lg border">
        <div aria-hidden className={cn('hidden h-10 items-center border-b px-4 text-[13px] font-medium text-muted-foreground', columns)}>
          <span>{t('globalBalances_detailGroup')}</span>
          <span>{t('globalBalances_detailStatus')}</span>
          <span className="text-right">{t('globalBalances_detailAmount')}</span>
        </div>
        <ul>
          {contact.groups.map((group) => (
            <li key={group.groupId} className={cn('flex min-h-13 items-center gap-3 border-b px-3.5 py-2 text-sm @md:h-12 @md:min-h-0 @md:px-4 @md:py-0', columns)}>
              <span className="flex min-w-0 flex-1 items-center gap-2.5">
                <GroupAvatar initials={group.initials} color={group.color} className="size-6 rounded-md text-[10px]" />
                <span className="flex min-w-0 flex-col">
                  <span className="truncate">{group.name}</span>
                  <span className="text-xs text-muted-foreground @md:hidden">{netWord(group.amountMinor)}</span>
                </span>
              </span>
              <span className="hidden text-muted-foreground @md:block">{netWord(group.amountMinor)}</span>
              <span className={cn('text-right font-semibold whitespace-nowrap tabular-nums', balanceTone(group.amountMinor))}>
                {formatSignedMoney(group.amountMinor, group.currency, group.fractionDigits)}
              </span>
            </li>
          ))}
        </ul>
        <div className={cn('flex h-11 items-center justify-between gap-3 bg-muted px-3.5 text-sm font-semibold @md:px-4', columns)}>
          <span>{t(isNetOfSeveralGroups ? 'globalBalances_net' : 'globalBalances_total')}</span>
          <span className="hidden font-medium text-muted-foreground @md:block">{netWord(contact.netMinor)}</span>
          <span className={cn('text-right whitespace-nowrap tabular-nums', balanceTone(contact.netMinor))}>
            {formatSignedMoney(contact.netMinor, contact.currency, contact.fractionDigits)}
          </span>
        </div>
      </div>
    </section>
  )
}

/** A bill with what it moved between you, or a payment one of you made the other. */
function HistoryRow({ item, name }: { item: JsContactHistoryItem; name: string }) {
  const money = (minor: number) => formatMoney(minor, item.currency, item.fractionDigits)
  const date = formatShortDate(item.at)
  const isPayment = item.type === 'payment'
  let title: string
  let meta: string
  let amount: string
  let word: string
  let tone: string
  if (isPayment) {
    title = item.isFromMe ? t('contactProfile_paymentTo', { name }) : t('contactProfile_paymentFrom', { name })
    meta = t('contactProfile_paymentMeta', { group: item.groupName, date, method: paymentMethodLabel(item.method) })
    amount = money(item.amountMinor)
    word = t(item.isFromMe ? 'contactProfile_paid' : 'contactProfile_received')
    tone = 'text-muted-foreground'
  } else {
    const total = money(item.totalMinor)
    const payer = item.isPaidByMe
      ? t('contactProfile_paidByMe', { amount: total })
      : item.payerName
        ? t('contactProfile_paidBy', { name: item.payerName, amount: total })
        : t('contactProfile_paidBySeveral', { count: item.payerCount, amount: total })
    title = item.description
    meta = t('contactProfile_billMeta', { group: item.groupName, date, payer })
    amount = formatSignedMoney(item.effectMinor, item.currency, item.fractionDigits)
    word = item.effectMinor === 0 ? t('contactProfile_noEffect') : netWord(item.effectMinor)
    tone = balanceTone(item.effectMinor)
  }
  return (
    <li className="flex min-h-14 items-center gap-3 border-b px-3.5 py-2.5 last:border-b-0 @md:px-4">
      <span aria-hidden className={cn('flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted', isPayment ? 'text-primary' : 'text-muted-foreground')}>
        {isPayment ? <ArrowRight className="size-4" /> : <ReceiptText className="size-4" />}
      </span>
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="truncate text-sm font-medium">{title}</span>
        <span className="truncate text-xs text-muted-foreground">{meta}</span>
      </span>
      <span className="flex shrink-0 flex-col items-end">
        <span className={cn('text-sm font-semibold whitespace-nowrap tabular-nums', tone)}>{amount}</span>
        <span className="text-xs text-muted-foreground">{word}</span>
      </span>
    </li>
  )
}
