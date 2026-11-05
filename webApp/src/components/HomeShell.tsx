import { Bell, ChevronRight, House, Search, UserRound, Users, Wallet } from 'lucide-react'
import type { JsGroupRow } from 'blitzsplit-shared'
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { Avatar, BrandMark } from '@/components/common'
import { Skeleton, SkeletonCircle, SkeletonLine } from '@/components/ui/skeleton'
import { balanceTone } from '@/features/balances/BalanceCard'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { t, type MessageKey } from '@/i18n'
import { formatSignedPlainMoney } from '@/lib/money'
import type { AccountProvider } from '@/lib/platform'
import { cn } from '@/lib/utils'

/** The home's four tabs, in the canvas' order: Início, Grupos, Saldos and Contatos. Atividade is Início's bell. */
export type HomeTab = 'home' | 'groups' | 'balances' | 'contacts'

const tabs: { tab: HomeTab; label: MessageKey; icon: ReactNode }[] = [
  { tab: 'home', label: 'home_tab', icon: <House className="size-4" /> },
  { tab: 'groups', label: 'groups_tabGroups', icon: <Users className="size-4" /> },
  { tab: 'balances', label: 'groups_tabBalances', icon: <Wallet className="size-4" /> },
  { tab: 'contacts', label: 'groups_tabContacts', icon: <UserRound className="size-4" /> },
]

/** The tab's name, which a page outside the tabs puts first in its breadcrumb ("Início › Viagem Floripa"). */
export function homeTabLabel(tab: HomeTab): string {
  return t(tabs.find((entry) => entry.tab === tab)?.label ?? 'groups_tabGroups')
}

// The canvas names the account under the profile; "No account" covers both local only and anonymous.
const accountLabels: Record<AccountProvider, MessageKey> = {
  google: 'groups_googleAccount',
  apple: 'groups_appleAccount',
}

/**
 * The content of every page in the home's frame: centered and capped at the same width on a wide window, so a tab, a
 * group and the profile line up as the sidebar opens one after the other.
 */
const pageWidthClass = 'mx-auto flex w-full max-w-6xl flex-1 flex-col'
export const pageContentClass = `${pageWidthClass} p-4 sm:p-8 lg:px-10`

/** A group's pages and Statistics: the same width, under their own bar on a phone. */
export const groupPageContentClass = `${pageWidthClass} p-4 md:px-8 md:py-7 lg:px-10 lg:py-8`

/** The first crumb of a page outside the tabs, "Grupos ›": the tab it was opened from, which its click opens. */
export function HomeTabCrumb({ tab, onOpen }: { tab: HomeTab; onOpen: (tab: HomeTab) => void }) {
  return (
    <>
      <button
        type="button"
        onClick={() => onOpen(tab)}
        className="rounded-sm text-muted-foreground outline-none hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring"
      >
        {homeTabLabel(tab)}
      </button>
      <ChevronRight aria-hidden className="size-3.5 text-muted-foreground" />
    </>
  )
}

/**
 * Which page the back stack shows (App's StackLayers), so a page fades in once as it opens: a screen that swaps its
 * skeleton for its page mounts a new frame, which must not fade in again.
 */
export const PageKeyContext = createContext('')

// Why: the last page that came in, read at a frame's mount; module state, since each page mounts its own frame.
let enteredPageKey: string | null = null

function useEntersAnimated(): boolean {
  const pageKey = useContext(PageKeyContext)
  // Why: decided once at mount, so a page that changes its key without remounting (Contatos' profile) never fades.
  const [entersAnimated] = useState(() => pageKey !== enteredPageKey)
  useEffect(() => {
    enteredPageKey = pageKey
  }, [pageKey])
  return entersAnimated
}

/** Who is signed in, under the sidebar and in the phone's top bar. */
export interface HomeProfile {
  displayName: string
  avatarInitial: string
  avatarColor: string
  photoUrl: string | null | undefined
  accountProvider: string | null | undefined
}

/**
 * The profile once the frame's screen read it, null before: a read profile always has its initial (the home exists only
 * with a profile), so an empty one is the empty start of the screen's state, which the frame shimmers instead of
 * drawing an empty avatar in the default color.
 */
export function loadedProfile(profile: HomeProfile): HomeProfile | null {
  return profile.avatarInitial ? profile : null
}

/**
 * Início's bell, which opens Atividade over it (no tab since Pierre's call), its unread count in the primary color
 * (never the destructive red). The count is hidden from screen readers, so the button's name says it.
 */
export function ActivityBell({ unreadCount, onOpen, className }: { unreadCount: number; onOpen: () => void; className?: string }) {
  return (
    <button
      aria-label={unreadCount > 0 ? t('activity_bellUnread', { count: unreadCount }) : t('activity_title')}
      title={t('activity_title')}
      onClick={onOpen}
      className={cn(
        'relative flex size-10 shrink-0 items-center justify-center rounded-md outline-none hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring md:size-9',
        className,
      )}
    >
      <Bell aria-hidden className="size-[18px]" />
      {unreadCount > 0 && (
        <span aria-hidden className="absolute top-1 right-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-semibold text-primary-foreground tabular-nums">
          {unreadCount > 99 ? '99+' : unreadCount}
        </span>
      )}
    </button>
  )
}

/** "Em aberto" under the desktop's tabs, around the group's pages: the open groups, the page's group selected. */
export interface ShellOpenGroups {
  groups: JsGroupRow[]
  selectedId: string
  onOpen: (groupId: string) => void
}

/**
 * The home's frame around Início, Grupos, Saldos and Contatos, and around the pages they open (Atividade, the group's
 * pages, Estatísticas, Perfil): desktop (1024+) has the full sidebar, tablet (768–1023) an icon rail and the phone a
 * top bar with a bottom navigation, as the canvas draws them. The selected tab names the page; outside the tabs it
 * says where the page was opened from, and its click goes back there.
 */
export function HomeShell({
  selected,
  isProfileSelected = false,
  profile,
  onTab,
  onProfile,
  onBrand,
  search,
  bell,
  openGroups,
  phoneBars = 'home',
  isOutsideTabs = phoneBars !== 'home',
  children,
  overlays,
}: {
  // Why: undefined on Perfil, which no tab opened: its footer is selected instead.
  selected?: HomeTab
  isProfileSelected?: boolean
  // Why: null until it was read, a shimmer in the avatar's and the name's place.
  profile: HomeProfile | null
  onTab: (tab: HomeTab) => void
  onProfile: () => void
  /** The logo: opens Home, the tracked way where the screen has a presenter that counts it. */
  onBrand?: () => void
  // Why: "Buscar" on the four tabs and the pages outside them, once there is a group to look in; a page's has no
  // shortcut (an empty one), as ⌘K there could close a dialog over it.
  search?: { shortcut: string; keyShortcuts: string; onOpen: () => void }
  // Why: Início's bell in the phone's top bar; from the tablet up Início draws it in its title row.
  bell?: { unreadCount: number; onOpen: () => void }
  // Why: 'loading' while the sidebar is still read, a shimmer in the open groups' place.
  openGroups?: ShellOpenGroups | 'loading'
  // Why: the home's tabs have the brand's bar and the bottom navigation; Perfil its own bar over the navigation, and
  // the group's pages their own bars with neither.
  phoneBars?: 'home' | 'nav' | 'none'
  // Why: a page outside the tabs goes back to the tab it sits in, which a tab's own page never needs; a contact's
  // profile keeps the brand's bar outside them.
  isOutsideTabs?: boolean
  children: ReactNode
  overlays?: ReactNode
}) {
  const account = accountLabels[profile?.accountProvider as AccountProvider] ?? 'groups_noAccount'
  const select = (tab: HomeTab) => {
    if (tab !== selected || isOutsideTabs) onTab(tab)
  }
  // Why: a screen whose presenter can't count the logo yet opens Home as its tab does.
  const openBrand = onBrand ?? (() => select('home'))
  const entersAnimated = useEntersAnimated()
  const current = (tab: HomeTab) => (tab === selected ? (isOutsideTabs ? 'true' : 'page') : undefined)
  return (
    <div className="flex min-h-dvh">
      <aside className="sticky top-0 hidden h-dvh w-20 shrink-0 flex-col border-r bg-sidebar p-2 md:flex lg:w-60 lg:p-3">
        <button
          aria-label={t('appName')}
          onClick={openBrand}
          className="rounded-md text-left outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
        >
          <BrandMark showsName={false} className="h-10 px-1 lg:hidden" />
          <BrandMark className="hidden h-10 px-1.5 lg:flex" />
        </button>
        {/* Why: the rail's first item and the desktop's field atop the tabs, where a keyboard-first window looks for
            search (⌘K); the phone puts it last in its bottom navigation instead, as the apps do (Pierre's call). */}
        {search && (
          <button
            aria-label={t('groups_search')}
            aria-keyshortcuts={search.keyShortcuts || undefined}
            onClick={search.onOpen}
            className="mt-3 flex flex-col items-center justify-center gap-1 rounded-md px-1 py-1.5 text-[11px] font-medium text-muted-foreground outline-none hover:bg-sidebar-accent focus-visible:ring-[3px] focus-visible:ring-ring lg:h-9 lg:flex-row lg:justify-start lg:gap-2 lg:border lg:bg-card lg:px-2.5 lg:py-0 lg:text-sm lg:font-normal"
          >
            <Search aria-hidden className="size-4 shrink-0" />
            <span aria-hidden className="lg:hidden">{t('groups_search')}</span>
            <span className="hidden flex-1 text-left lg:inline">{t('groups_searchButton')}</span>
            {search.shortcut && <kbd className="hidden h-5 items-center rounded border bg-muted px-1.5 font-sans text-[11px] lg:flex">{search.shortcut}</kbd>}
          </button>
        )}
        <nav aria-label={t('groups_mainNavigation')} className="mt-3 flex flex-col gap-0.5">
          {tabs.map(({ tab, label, icon }) => (
            <button
              key={tab}
              aria-current={current(tab)}
              onClick={() => select(tab)}
              className={cn(
                'flex flex-col items-center justify-center gap-1 rounded-md px-1 py-1.5 text-[11px] font-medium transition-colors outline-none hover:bg-sidebar-accent focus-visible:ring-[3px] focus-visible:ring-ring lg:h-9 lg:flex-row lg:justify-start lg:gap-2.5 lg:px-2.5 lg:py-0 lg:text-sm',
                tab === selected ? 'bg-sidebar-accent text-sidebar-accent-foreground' : 'text-muted-foreground',
              )}
            >
              {icon}
              {/* Why: the label names the page wherever the page shows no title of its own (a phone). */}
              <span>{t(label)}</span>
            </button>
          ))}
        </nav>
        {openGroups === 'loading' ? <SidebarGroupsSkeleton /> : openGroups && <SidebarGroups openGroups={openGroups} />}
        <button
          onClick={onProfile}
          aria-current={isProfileSelected ? 'page' : undefined}
          className={cn(
            'mt-auto flex h-12 shrink-0 items-center justify-center gap-2.5 rounded-lg px-1.5 text-left outline-none hover:bg-sidebar-accent focus-visible:ring-[3px] focus-visible:ring-ring lg:justify-start',
            isProfileSelected && 'bg-sidebar-accent text-sidebar-accent-foreground',
          )}
        >
          {profile ? (
            <>
              <Avatar initial={profile.avatarInitial} color={profile.avatarColor} photoUrl={profile.photoUrl} className="size-7 text-xs" />
              {/* Why: the rail hides the name and account only visually, so they stay the button's accessible name. */}
              {/* Why: the name and account say whose profile it is; this says what the button opens. */}
              {profile.displayName && <span className="sr-only">{t('groups_profile')}</span>}
              <span className="sr-only min-w-0 flex-col lg:not-sr-only lg:flex">
                <span className="truncate text-sm font-medium">{profile.displayName || t('groups_profile')}</span>
                <span className="text-xs text-muted-foreground">{t(account)}</span>
              </span>
            </>
          ) : (
            <>
              <SkeletonCircle className="size-7" />
              <span className="sr-only">{t('groups_profile')}</span>
              <span aria-hidden className="hidden min-w-0 flex-1 flex-col gap-1.5 lg:flex">
                <SkeletonLine className="w-24" />
                <SkeletonLine className="h-2.5 w-16" />
              </span>
            </>
          )}
        </button>
      </aside>

      {/* Why: clips the page's 6 px rise as it comes in, which would otherwise flash a scrollbar on a short page. */}
      <div className="flex min-w-0 flex-1 flex-col overflow-y-clip">
        {phoneBars === 'home' && (
          <header className="flex h-14 items-center gap-2.5 border-b px-4 md:hidden">
            <button
              aria-label={t('appName')}
              onClick={openBrand}
              className="mr-auto rounded-md outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
            >
              <BrandMark />
            </button>
            {bell && <ActivityBell unreadCount={bell.unreadCount} onOpen={bell.onOpen} />}
            <button aria-label={t('groups_profile')} onClick={onProfile} className="rounded-full outline-none focus-visible:ring-[3px] focus-visible:ring-ring">
              {profile ? (
                <Avatar initial={profile.avatarInitial} color={profile.avatarColor} photoUrl={profile.photoUrl} className="size-9 text-sm" />
              ) : (
                <SkeletonCircle className="size-9" />
              )}
            </button>
          </header>
        )}

        {/* Why: a new page fades in under the frame, which stays put (each page mounts its own shell). */}
        <div className={cn('flex min-w-0 flex-1 flex-col', entersAnimated && 'page-enter')}>{children}</div>

        {phoneBars !== 'none' && (
          <nav aria-label={t('groups_mainNavigation')} className={cn('sticky bottom-0 grid h-16 border-t bg-card md:hidden', search ? 'grid-cols-5' : 'grid-cols-4')}>
            {tabs.map(({ tab, label, icon }) => (
              <button
                key={tab}
                aria-current={current(tab)}
                onClick={() => select(tab)}
                className={cn('flex flex-col items-center justify-center gap-1 text-[11px] font-medium', tab === selected ? 'text-foreground' : 'text-muted-foreground')}
              >
                {icon}
                {t(label)}
              </button>
            ))}
            {/* Why: last, under the thumb and where iOS keeps its search tab; it opens the search over this tab. */}
            {search && (
              <button onClick={search.onOpen} className="flex flex-col items-center justify-center gap-1 text-[11px] font-medium text-muted-foreground">
                <Search aria-hidden className="size-4" />
                {t('groups_search')}
              </button>
            )}
          </nav>
        )}
      </div>

      {overlays}
    </div>
  )
}

/**
 * The open groups under the desktop's tabs (the rail has no room for them), with your net in each, signed and in the
 * group's colors of a balance, scrolling on their own once they outgrow the sidebar.
 */
function SidebarGroups({ openGroups }: { openGroups: ShellOpenGroups }) {
  if (openGroups.groups.length === 0) return null
  return (
    <div className="mt-5 hidden min-h-0 flex-col lg:flex">
      <h2 id="sidebarOpenGroups" className="px-2.5 pb-1.5 text-xs font-medium text-muted-foreground">
        {t('groups_openTitle')}
      </h2>
      <nav aria-labelledby="sidebarOpenGroups" className="-mx-1 flex min-h-0 flex-col gap-0.5 overflow-y-auto px-1 pb-1">
        {openGroups.groups.map((group) => (
          <button
            key={group.id}
            aria-current={group.id === openGroups.selectedId ? 'page' : undefined}
            onClick={() => openGroups.onOpen(group.id)}
            className={cn(
              'flex h-9 shrink-0 items-center gap-2.5 rounded-md px-2.5 text-left text-sm outline-none hover:bg-sidebar-accent focus-visible:ring-[3px] focus-visible:ring-ring',
              group.id === openGroups.selectedId && 'bg-sidebar-accent font-medium text-sidebar-accent-foreground',
            )}
          >
            <GroupAvatar initials={group.initials} color={group.color} photoUrl={group.photoUrl} className="size-[22px] rounded-md text-[9px]" />
            <span className="min-w-0 flex-1 truncate">{group.name}</span>
            <span className={cn('text-xs font-medium tabular-nums', balanceTone(group.netMinor))}>{formatSignedPlainMoney(group.netMinor, group.fractionDigits, group.currency)}</span>
          </button>
        ))}
      </nav>
    </div>
  )
}

/** "Em aberto" while the sidebar is still read: its title and a few groups' places, on the desktop only as the list. */
function SidebarGroupsSkeleton() {
  return (
    <div aria-hidden className="mt-5 hidden flex-col gap-2.5 px-2.5 lg:flex">
      <SkeletonLine className="h-2.5 w-16" />
      {[0, 1, 2].map((index) => (
        <span key={index} className="flex items-center gap-2.5">
          <Skeleton className="size-[22px] rounded-md" />
          <SkeletonLine className="flex-1" />
          <SkeletonLine className="w-10" />
        </span>
      ))}
    </div>
  )
}
