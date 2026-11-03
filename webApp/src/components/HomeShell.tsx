import { House, ReceiptText, Search, UserRound, Users, Wallet } from 'lucide-react'
import type { JsGroupRow } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { Avatar, BrandMark } from '@/components/common'
import { balanceTone } from '@/features/balances/BalanceCard'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { t, type MessageKey } from '@/i18n'
import { formatSignedPlainMoney } from '@/lib/money'
import type { AccountProvider } from '@/lib/platform'
import { cn } from '@/lib/utils'

/** The home's five tabs, in the canvas' order, all built: Início, Grupos, Saldos, Contatos and Atividade. */
export type HomeTab = 'home' | 'groups' | 'balances' | 'contacts' | 'activity'

const tabs: { tab: HomeTab; label: MessageKey; icon: ReactNode }[] = [
  { tab: 'home', label: 'home_tab', icon: <House className="size-4" /> },
  { tab: 'groups', label: 'groups_tabGroups', icon: <Users className="size-4" /> },
  { tab: 'balances', label: 'groups_tabBalances', icon: <Wallet className="size-4" /> },
  { tab: 'contacts', label: 'groups_tabContacts', icon: <UserRound className="size-4" /> },
  { tab: 'activity', label: 'groups_tabActivity', icon: <ReceiptText className="size-4" /> },
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

/** Who is signed in, under the sidebar and in the phone's top bar. */
export interface HomeProfile {
  displayName: string
  avatarInitial: string
  avatarColor: string
  photoUrl: string | null | undefined
  accountProvider: string | null | undefined
}

/** "Em aberto" under the desktop's tabs, around the group's pages: the open groups, the page's group selected. */
export interface ShellOpenGroups {
  groups: JsGroupRow[]
  selectedId: string
  onOpen: (groupId: string) => void
}

/**
 * The home's frame around Início, Grupos, Saldos, Contatos and Atividade, and around the pages they open (the group's
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
  search,
  activityUnreadCount = 0,
  openGroups,
  phoneBars = 'home',
  isOutsideTabs = phoneBars !== 'home',
  children,
  overlays,
}: {
  // Why: undefined on Perfil, which no tab opened: its footer is selected instead.
  selected?: HomeTab
  isProfileSelected?: boolean
  profile: HomeProfile
  onTab: (tab: HomeTab) => void
  onProfile: () => void
  // Why: "Buscar" only on the groups, and once there is a group to look in.
  search?: { shortcut: string; keyShortcuts: string; onOpen: () => void }
  // Why: Atividade's badge, in the primary color (never the destructive red), on every width.
  activityUnreadCount?: number
  openGroups?: ShellOpenGroups
  // Why: the home's tabs have the brand's bar and the bottom navigation; Perfil its own bar over the navigation, and
  // the group's pages their own bars with neither.
  phoneBars?: 'home' | 'nav' | 'none'
  // Why: a page outside the tabs goes back to the tab it sits in, which a tab's own page never needs; a contact's
  // profile keeps the brand's bar outside them.
  isOutsideTabs?: boolean
  children: ReactNode
  overlays?: ReactNode
}) {
  const account = accountLabels[profile.accountProvider as AccountProvider] ?? 'groups_noAccount'
  const select = (tab: HomeTab) => {
    if (tab !== selected || isOutsideTabs) onTab(tab)
  }
  const current = (tab: HomeTab) => (tab === selected ? (isOutsideTabs ? 'true' : 'page') : undefined)
  const unreadOf = (tab: HomeTab) => (tab === 'activity' ? activityUnreadCount : 0)
  // Why: "Atividade, 3 não lidas": the badge is hidden from screen readers, so the tab's name says it.
  const unreadLabel = (tab: HomeTab) => (unreadOf(tab) > 0 ? t('activity_tabUnread', { count: unreadOf(tab) }) : undefined)
  return (
    <div className="flex min-h-dvh">
      <aside className="sticky top-0 hidden h-dvh w-20 shrink-0 flex-col border-r bg-sidebar p-2 md:flex lg:w-60 lg:p-3">
        <BrandMark showsName={false} className="h-10 px-1 lg:hidden" />
        <BrandMark className="hidden h-10 px-1.5 lg:flex" />
        {search && (
          <button
            aria-label={t('groups_search')}
            aria-keyshortcuts={search.keyShortcuts}
            onClick={search.onOpen}
            className="mt-3 flex h-9 items-center justify-center gap-2 rounded-md border bg-card text-sm text-muted-foreground outline-none hover:bg-sidebar-accent focus-visible:ring-[3px] focus-visible:ring-ring lg:justify-start lg:px-2.5"
          >
            <Search aria-hidden className="size-4 shrink-0" />
            <span className="hidden flex-1 text-left lg:inline">{t('groups_searchButton')}</span>
            <kbd className="hidden h-5 items-center rounded border bg-muted px-1.5 font-sans text-[11px] lg:flex">{search.shortcut}</kbd>
          </button>
        )}
        <nav aria-label={t('groups_mainNavigation')} className="mt-3 flex flex-col gap-0.5">
          {tabs.map(({ tab, label, icon }) => (
            <button
              key={tab}
              aria-current={current(tab)}
              aria-label={unreadLabel(tab)}
              onClick={() => select(tab)}
              className={cn(
                'flex flex-col items-center justify-center gap-1 rounded-md px-1 py-1.5 text-[11px] font-medium outline-none hover:bg-sidebar-accent focus-visible:ring-[3px] focus-visible:ring-ring lg:h-9 lg:flex-row lg:justify-start lg:gap-2.5 lg:px-2.5 lg:py-0 lg:text-sm',
                tab === selected ? 'bg-sidebar-accent text-sidebar-accent-foreground' : 'text-muted-foreground',
              )}
            >
              {/* Why: the rail has no room for the count, so it shows a dot on the icon. */}
              <span className="relative">
                {icon}
                {unreadOf(tab) > 0 && <span aria-hidden className="absolute -top-0.5 -right-0.5 size-2 rounded-full bg-primary lg:hidden" />}
              </span>
              {/* Why: the page has no visible title, so the selected tab's label names it at every width. */}
              <span>{t(label)}</span>
              {unreadOf(tab) > 0 && (
                <span aria-hidden className="ml-auto hidden h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1.5 text-[11px] font-semibold text-primary-foreground tabular-nums lg:flex">
                  {unreadOf(tab)}
                </span>
              )}
            </button>
          ))}
        </nav>
        {openGroups && <SidebarGroups openGroups={openGroups} />}
        <button
          onClick={onProfile}
          aria-current={isProfileSelected ? 'page' : undefined}
          className={cn(
            'mt-auto flex h-12 shrink-0 items-center justify-center gap-2.5 rounded-lg px-1.5 text-left outline-none hover:bg-sidebar-accent focus-visible:ring-[3px] focus-visible:ring-ring lg:justify-start',
            isProfileSelected && 'bg-sidebar-accent text-sidebar-accent-foreground',
          )}
        >
          <Avatar initial={profile.avatarInitial} color={profile.avatarColor} photoUrl={profile.photoUrl} className="size-7 text-xs" />
          {/* Why: the rail hides the name and account only visually, so they stay the button's accessible name. */}
          {/* Why: the name and account say whose profile it is; this says what the button opens. */}
          {profile.displayName && <span className="sr-only">{t('groups_profile')}</span>}
          <span className="sr-only min-w-0 flex-col lg:not-sr-only lg:flex">
            <span className="truncate text-sm font-medium">{profile.displayName || t('groups_profile')}</span>
            <span className="text-xs text-muted-foreground">{t(account)}</span>
          </span>
        </button>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        {phoneBars === 'home' && (
          <header className="flex h-14 items-center gap-2.5 border-b px-4 md:hidden">
            <BrandMark className="mr-auto" />
            {search && (
              <button
                aria-label={t('groups_search')}
                onClick={search.onOpen}
                className="flex size-10 items-center justify-center rounded-md outline-none hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring"
              >
                <Search aria-hidden className="size-[18px]" />
              </button>
            )}
            <button aria-label={t('groups_profile')} onClick={onProfile} className="rounded-full outline-none focus-visible:ring-[3px] focus-visible:ring-ring">
              <Avatar initial={profile.avatarInitial} color={profile.avatarColor} photoUrl={profile.photoUrl} className="size-9 text-sm" />
            </button>
          </header>
        )}

        {children}

        {phoneBars !== 'none' && (
          <nav aria-label={t('groups_mainNavigation')} className="sticky bottom-0 grid h-16 grid-cols-5 border-t bg-card md:hidden">
            {tabs.map(({ tab, label, icon }) => (
              <button
                key={tab}
                aria-current={current(tab)}
                aria-label={unreadLabel(tab)}
                onClick={() => select(tab)}
                className={cn('flex flex-col items-center justify-center gap-1 text-[11px] font-medium', tab === selected ? 'text-foreground' : 'text-muted-foreground')}
              >
                <span className="relative">
                  {icon}
                  {unreadOf(tab) > 0 && (
                    <span aria-hidden className="absolute -top-1.5 left-2.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-semibold text-primary-foreground tabular-nums">
                      {unreadOf(tab)}
                    </span>
                  )}
                </span>
                {t(label)}
              </button>
            ))}
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
            <span className={cn('text-xs font-medium tabular-nums', balanceTone(group.netMinor))}>{formatSignedPlainMoney(group.netMinor, group.fractionDigits)}</span>
          </button>
        ))}
      </nav>
    </div>
  )
}
