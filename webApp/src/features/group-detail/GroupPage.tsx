import { ArrowLeft, BarChart3, Ellipsis, LogOut, Pencil, Pin, PinOff, Trash2, UserPlus } from 'lucide-react'
import type { JsGroupExit, JsGroupHeader } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { Avatar, GroupTabs, SplitBillIcon, type GroupTab } from '@/components/common'
import { groupPageContentClass, HomeTabCrumb, type HomeTab } from '@/components/HomeShell'
import { PageShell, useOpenHomeTab } from '@/components/HomeSidebar'
import { Button } from '@/components/ui/button'
import { LoadingRegion, Skeleton, SkeletonCircle, SkeletonLine, SkeletonRows } from '@/components/ui/skeleton'
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

/** What the group's page asks of its ViewModel, whatever tab is picked. */
export interface GroupPageEvents extends GroupExitEvents {
  back(): void
  invite(): void
  stats(): void
  togglePin(): void
  editGroup(): void
  leaveGroup(): void
  deleteGroup(): void
  splitBill(): void
  settle(): void
}

/**
 * The group's page around the tab picked, in the home's frame with the group selected among the open ones: the
 * breadcrumb, the group's name, total and faces (which pick Membros), "Estatísticas", "Convidar", "Rachar conta" (the
 * main button on every tab, Membros included) and the ⋯ menu beside them, "Seu saldo neste grupo" and the
 * tabs. The phone has no frame: back, the name and the icons on top, the main button at the bottom.
 */
export function GroupPage({
  groupId,
  homeTab,
  header,
  tab,
  hasExpenses,
  isBalanceLoading,
  showsSplitBillButton,
  exit,
  events,
  onSelectTab,
  children,
}: {
  groupId: string
  // Why: the home tab the group was opened from, which the sidebar selects and the breadcrumb starts with.
  homeTab: HomeTab
  header: JsGroupHeader
  tab: GroupTab
  hasExpenses: boolean
  // Why: "Seu saldo neste grupo" shimmers while a restored account's copy isn't whole, instead of hiding.
  isBalanceLoading: boolean
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
  const openHomeTab = useOpenHomeTab()

  return (
    <PageShell selected={homeTab} groups={{ selectedId: groupId }} phoneBars="none">
      <div className="flex min-h-dvh flex-col">
        {/* Why: the page names the group in its heading from the tablet up; the phone's bar names it instead. */}
        <header className="sticky top-0 z-10 flex h-14 items-center gap-1 border-b bg-background/95 px-2 backdrop-blur md:hidden">
          <Button variant="ghost" size="icon" aria-label={t('common_back')} onClick={() => events.back()}>
            <ArrowLeft />
          </Button>
          <span className="mr-auto truncate font-semibold">{header.groupName}</span>
          <Button variant="ghost" size="icon" aria-label={t('stats_title')} onClick={() => events.stats()}>
            <BarChart3 />
          </Button>
          <Button variant="ghost" size="icon" aria-label={t('groupDetail_invitePeople')} onClick={() => events.invite()}>
            <UserPlus />
          </Button>
          <GroupMenu isOnline={exit.isOnline} isPinned={header.isPinned} events={events} />
        </header>

        <main className={cn(groupPageContentClass, 'gap-4 sm:gap-5')}>
          <nav aria-label={t('groupDetail_breadcrumb')} className="-mb-1 hidden items-center gap-1.5 text-[13px] md:flex">
            <HomeTabCrumb tab={homeTab} onOpen={openHomeTab} />
            <span aria-current="page" className="truncate">
              {header.groupName}
            </span>
          </nav>
          <div className="flex items-start gap-4">
            <GroupAvatar initials={header.groupInitials} color={header.groupColor} photoUrl={header.groupPhotoUrl} className="size-12 rounded-xl text-base md:size-14 md:text-lg" />
            <div className="flex min-w-0 flex-1 flex-col gap-1.5">
              <h1 className="sr-only text-2xl leading-[30px] font-semibold tracking-tight md:not-sr-only md:truncate">{header.groupName}</h1>
              <span className="text-sm text-muted-foreground">{meta}</span>
              <button
                type="button"
                onClick={() => onSelectTab('members')}
                aria-label={[members, notJoined, t('groupDetail_seeMembers')].filter(Boolean).join(' · ')}
                className="flex items-center gap-2 self-start rounded-md text-[13px] text-muted-foreground outline-none hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring"
              >
                <span aria-hidden className="flex">
                  {header.faces.map((face, index) => (
                    <Avatar key={index} initial={face.initials} color={face.color} photoUrl={face.photoUrl} className={cn('size-7 border-2 border-background text-[11px]', index > 0 && '-ml-1.5')} />
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
                  <SplitBillIcon />
                  {t('groupDetail_splitBill')}
                </Button>
              )}
              <GroupMenu isOnline={exit.isOnline} isPinned={header.isPinned} events={events} isOutlined />
            </div>
          </div>

          {!header.myBalance && isBalanceLoading && <BalanceCardSkeleton />}
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
              <SplitBillIcon />
              {t('groupDetail_splitBill')}
            </Button>
          </div>
        )}
        <GroupExitDialog exit={exit} currency={header.currency} fractionDigits={header.fractionDigits} events={events} />
      </div>
    </PageShell>
  )
}

/** "Seu saldo neste grupo" while it can't be told yet: its title, the amount and Acertar's place. */
function BalanceCardSkeleton() {
  return (
    <LoadingRegion isDelayed={false} className="flex items-center gap-4 rounded-xl border bg-card p-4">
      <span className="flex flex-1 flex-col gap-2.5">
        <SkeletonLine className="w-32" />
        <Skeleton className="h-6 w-28" />
      </span>
      <Skeleton className="h-9 w-24" />
    </LoadingRegion>
  )
}

/**
 * The group's page until the group was read, in the same frame: back on the phone's bar, the breadcrumb, the group's
 * avatar and lines, the balance card, the tabs and a few rows, after 150 ms. Estatísticas gives its own `children`.
 */
export function GroupPageSkeleton({ groupId, homeTab, onBack, children }: { groupId: string; homeTab: HomeTab; onBack: () => void; children?: ReactNode }) {
  return (
    <PageShell selected={homeTab} groups={{ selectedId: groupId }} phoneBars="none">
      <div className="flex min-h-dvh flex-col">
        <header className="sticky top-0 z-10 flex h-14 items-center gap-1 border-b bg-background/95 px-2 backdrop-blur md:hidden">
          <Button variant="ghost" size="icon" aria-label={t('common_back')} onClick={onBack}>
            <ArrowLeft />
          </Button>
        </header>
        <main className={groupPageContentClass}>
          <LoadingRegion className="flex flex-col gap-4 sm:gap-5">
            <SkeletonLine className="hidden w-40 md:block" />
            {children ?? <GroupHeaderSkeleton />}
          </LoadingRegion>
        </main>
      </div>
    </PageShell>
  )
}

/** The group's avatar, name, meta and faces, the balance card, the tabs and a few bills. */
function GroupHeaderSkeleton() {
  return (
    <>
      <div className="flex items-start gap-4">
        <Skeleton className="size-12 rounded-xl md:size-14" />
        <span className="flex flex-1 flex-col gap-2.5">
          <SkeletonLine className="h-5 w-48" />
          <SkeletonLine className="w-36" />
          <span className="flex">
            {[0, 1, 2].map((index) => (
              <SkeletonCircle key={index} className={cn('size-7 border-2 border-background', index > 0 && '-ml-1.5')} />
            ))}
          </span>
        </span>
      </div>
      <Skeleton className="h-[72px] w-full rounded-xl" />
      <Skeleton className="h-9 w-full max-w-sm rounded-lg" />
      <SkeletonRows count={4} leading="square" className="rounded-xl border bg-card" />
    </>
  )
}

/**
 * The ⋯ menu keeps only what changes the group itself (6.1i); offline, Sair and Apagar need the internet (6.1j). "Fixar
 * na Início" comes first and works offline: it puts the group first in Início's "Seus grupos".
 */
function GroupMenu({ isOnline, isPinned, events, isOutlined = false }: { isOnline: boolean; isPinned: boolean; events: GroupPageEvents; isOutlined?: boolean }) {
  const needsInternet = !isOnline && <span className="ml-auto text-xs text-muted-foreground">{t('groupExit_needsInternet')}</span>
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant={isOutlined ? 'outline' : 'ghost'} size="icon" aria-label={t('groupDetail_moreOptions')}>
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
