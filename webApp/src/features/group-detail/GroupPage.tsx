import { ArrowLeft, BarChart3, ChevronRight, Ellipsis, LogOut, Pencil, Pin, PinOff, Plus, Trash2, UserPlus } from 'lucide-react'
import type { JsGroupExit, JsGroupHeader } from 'blitzsplit-shared'
import { useEffect, type ReactNode } from 'react'
import { Avatar, GroupTabs, type GroupTab } from '@/components/common'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { BalanceCard } from '@/features/balances/BalanceCard'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { t } from '@/i18n'
import { formatMoney } from '@/lib/money'
import { cn } from '@/lib/utils'
import { GroupExitDialog, type GroupExitEvents } from './GroupExitDialog'
import { rememberGroupPage, rememberedGroupPage, type RememberedGroupPage } from './groupHeaderMemory'

/** What the group's page asks of its ViewModel, the same on Despesas and Saldos. */
export interface GroupPageEvents extends GroupExitEvents {
  back(): void
  invite(): void
  stats(): void
  members(): void
  togglePin(): void
  editGroup(): void
  leaveGroup(): void
  deleteGroup(): void
  splitBill(): void
  settle(): void
}

// Why: the page drawn while a tab reads the group takes no clicks, its ViewModel may not even exist yet.
const ignoredEvents = new Proxy({}, { get: () => () => {} }) as GroupPageEvents

/**
 * What Despesas and Saldos draw until they have read the group: the page the tab they replaced drew, with this tab
 * picked and nothing under it, or nothing when they were opened any other way (`noteBackStack`).
 */
export function GroupPageLoading({ groupId, tab }: { groupId: string; tab: GroupTab }) {
  const page = rememberedGroupPage(groupId)
  if (!page) return null
  return (
    <GroupPage {...page} tab={tab} events={ignoredEvents} onSelectTab={() => {}}>
      {null}
    </GroupPage>
  )
}

/** Keeps what this tab's page draws, once it has read the group, for the next tab of the group to draw at once. */
export function useRememberGroupPage(groupId: string, page: RememberedGroupPage | null) {
  useEffect(() => {
    if (page) rememberGroupPage(groupId, page)
  })
}

/**
 * The group's page around a tab, Despesas or Saldos: the top bar with the ⋯ menu, the group's name, total and faces,
 * "Estatísticas", "Convidar" and "Rachar conta" beside them (at the bottom on the phone), "Seu saldo neste grupo" and the tabs.
 */
export function GroupPage({
  header,
  tab,
  hasExpenses,
  showsSplitBillButton,
  exit,
  events,
  onSelectTab,
  children,
}: {
  header: JsGroupHeader
  tab: GroupTab
  hasExpenses: boolean
  showsSplitBillButton: boolean
  // Why: "Sair do grupo" and "Apagar grupo", which need the internet, their dialogs and "Grupo apagado".
  exit: JsGroupExit
  events: GroupPageEvents
  onSelectTab: (tab: GroupTab) => void
  children: ReactNode
}) {
  const members = t(header.memberCount === 1 ? 'members_countOne' : 'members_countOther', { count: header.memberCount })
  const meta = hasExpenses
    ? t('groupDetail_metaTotal', { members, total: formatMoney(header.totalSpentMinor, header.currency, header.fractionDigits) })
    : t('groupDetail_metaEmpty', { members })
  const notJoined =
    header.notJoinedCount > 0
      ? t(header.notJoinedCount === 1 ? 'groupDetail_notJoinedOne' : 'groupDetail_notJoinedOther', { count: header.notJoinedCount })
      : null

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="sticky top-0 z-10 flex h-14 items-center gap-1 border-b bg-background/95 px-2 backdrop-blur sm:px-4">
        <Button variant="ghost" size="icon" aria-label={t('common_back')} onClick={() => events.back()}>
          <ArrowLeft />
        </Button>
        {/* Why: the page names the group in its heading from the tablet up; the phone's bar names it instead. */}
        <span className="mr-auto truncate font-semibold md:hidden">{header.groupName}</span>
        <nav aria-label={t('groupDetail_breadcrumb')} className="mr-auto hidden items-center gap-1.5 text-sm md:flex">
          <button type="button" onClick={() => events.back()} className="rounded-sm text-muted-foreground outline-none hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring">
            {t('groups_title')}
          </button>
          <ChevronRight aria-hidden className="size-3.5 text-muted-foreground" />
          <span aria-current="page" className="truncate font-medium">
            {header.groupName}
          </span>
        </nav>
        <Button variant="ghost" size="icon" aria-label={t('stats_title')} onClick={() => events.stats()} className="md:hidden">
          <BarChart3 />
        </Button>
        <Button variant="ghost" size="icon" aria-label={t('groupDetail_invitePeople')} onClick={() => events.invite()} className="md:hidden">
          <UserPlus />
        </Button>
        <GroupMenu isOnline={exit.isOnline} isPinned={header.isPinned} events={events} />
      </header>

      <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-4 p-4 sm:gap-5 md:px-8 md:py-7 lg:px-10 lg:py-8">
        <div className="flex items-start gap-4">
          <GroupAvatar initials={header.groupInitials} color={header.groupColor} className="size-12 rounded-xl text-base md:size-14 md:text-lg" />
          <div className="flex min-w-0 flex-1 flex-col gap-1.5">
            <h1 className="sr-only text-2xl leading-[30px] font-semibold tracking-tight md:not-sr-only md:truncate">{header.groupName}</h1>
            <span className="text-sm text-muted-foreground">{meta}</span>
            <button
              type="button"
              onClick={() => events.members()}
              aria-label={[members, notJoined, t('groupDetail_seeMembers')].filter(Boolean).join(' · ')}
              className="flex items-center gap-2 self-start rounded-md text-[13px] text-muted-foreground outline-none hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring"
            >
              <span aria-hidden className="flex">
                {header.faces.map((face, index) => (
                  <Avatar key={index} initial={face.initials} color={face.color} className={cn('size-7 border-2 border-background text-[11px]', index > 0 && '-ml-1.5')} />
                ))}
                {header.extraMemberCount > 0 && (
                  <span className="-ml-1.5 flex h-7 min-w-7 items-center justify-center rounded-full border-2 border-background bg-muted px-1.5 text-[11px] font-semibold">
                    +{header.extraMemberCount}
                  </span>
                )}
              </span>
              {notJoined}
            </button>
          </div>
          <div className="hidden items-center gap-2 md:flex">
            <Button variant="outline" onClick={() => events.stats()}>
              <BarChart3 />
              {t('stats_title')}
            </Button>
            <Button variant="outline" onClick={() => events.invite()}>
              <UserPlus />
              {t('groupDetail_invite')}
            </Button>
            {showsSplitBillButton && (
              <Button onClick={() => events.splitBill()}>
                <Plus />
                {t('groupDetail_splitBill')}
              </Button>
            )}
          </div>
        </div>

        {header.myBalance && (
          <BalanceCard
            balance={header.myBalance}
            isEveryoneSettled={header.isEveryoneSettled}
            currency={header.currency}
            fractionDigits={header.fractionDigits}
            onSettle={() => events.settle()}
          />
        )}

        <GroupTabs selected={tab} onSelect={onSelectTab} />

        {children}
      </main>

      {showsSplitBillButton && (
        <div className="sticky bottom-0 border-t bg-card px-4 py-3 md:hidden">
          <Button onClick={() => events.splitBill()} className="h-11 w-full text-[15px]">
            <Plus />
            {t('groupDetail_splitBill')}
          </Button>
        </div>
      )}
      <GroupExitDialog exit={exit} currency={header.currency} fractionDigits={header.fractionDigits} events={events} />
    </div>
  )
}

/**
 * The ⋯ menu keeps only what changes the group itself (6.1i); offline, Sair and Apagar need the internet (6.1j). "Fixar
 * na Início" comes first and works offline: it puts the group first in Início's "Seus grupos".
 */
function GroupMenu({ isOnline, isPinned, events }: { isOnline: boolean; isPinned: boolean; events: GroupPageEvents }) {
  const needsInternet = !isOnline && <span className="ml-auto text-xs text-muted-foreground">{t('groupExit_needsInternet')}</span>
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" aria-label={t('groupDetail_moreOptions')}>
          <Ellipsis />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-64">
        <DropdownMenuItem onSelect={() => events.togglePin()}>
          {isPinned ? <PinOff /> : <Pin />}
          {t(isPinned ? 'groupDetail_unpin' : 'groupDetail_pin')}
        </DropdownMenuItem>
        <DropdownMenuItem onSelect={() => events.editGroup()}>
          <Pencil />
          {t('groupDetail_editGroup')}
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem disabled={!isOnline} onSelect={() => events.leaveGroup()}>
          <LogOut />
          {t('groupDetail_leaveGroup')}
          {needsInternet}
        </DropdownMenuItem>
        <DropdownMenuItem variant="destructive" disabled={!isOnline} onSelect={() => events.deleteGroup()}>
          <Trash2 />
          {t('groupDetail_deleteGroup')}
          {needsInternet}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
