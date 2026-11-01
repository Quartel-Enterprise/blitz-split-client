import { House, ReceiptText, Search, UserRound, Users, Wallet } from 'lucide-react'
import type { ReactNode } from 'react'
import { Avatar, BrandMark } from '@/components/common'
import { t, type MessageKey } from '@/i18n'
import type { AccountProvider } from '@/lib/platform'
import { cn } from '@/lib/utils'

/** The home's five tabs, in the canvas' order; Início, Grupos and Saldos are built, the others answer "Em breve". */
export type HomeTab = 'home' | 'groups' | 'balances' | 'contacts' | 'activity'

const tabs: { tab: HomeTab; label: MessageKey; icon: ReactNode }[] = [
  { tab: 'home', label: 'home_tab', icon: <House className="size-4" /> },
  { tab: 'groups', label: 'groups_tabGroups', icon: <Users className="size-4" /> },
  { tab: 'balances', label: 'groups_tabBalances', icon: <Wallet className="size-4" /> },
  { tab: 'contacts', label: 'groups_tabContacts', icon: <UserRound className="size-4" /> },
  { tab: 'activity', label: 'groups_tabActivity', icon: <ReceiptText className="size-4" /> },
]

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

/**
 * The home's frame around Início, Grupos and Saldos: desktop (1024+) has the full sidebar, tablet (768–1023) an icon rail and
 * the phone a top bar with a bottom navigation, as the canvas draws them. The selected tab names the page.
 */
export function HomeShell({
  selected,
  profile,
  onTab,
  onProfile,
  search,
  children,
  overlays,
}: {
  selected: HomeTab
  profile: HomeProfile
  onTab: (tab: HomeTab) => void
  onProfile: () => void
  // Why: "Buscar" only on the groups, and once there is a group to look in.
  search?: { shortcut: string; keyShortcuts: string; onOpen: () => void }
  children: ReactNode
  overlays?: ReactNode
}) {
  const account = accountLabels[profile.accountProvider as AccountProvider] ?? 'groups_noAccount'
  const select = (tab: HomeTab) => {
    if (tab !== selected) onTab(tab)
  }
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
              aria-current={tab === selected ? 'page' : undefined}
              onClick={() => select(tab)}
              className={cn(
                'flex flex-col items-center justify-center gap-1 rounded-md px-1 py-1.5 text-[11px] font-medium outline-none hover:bg-sidebar-accent focus-visible:ring-[3px] focus-visible:ring-ring lg:h-9 lg:flex-row lg:justify-start lg:gap-2.5 lg:px-2.5 lg:py-0 lg:text-sm',
                tab === selected ? 'bg-sidebar-accent text-sidebar-accent-foreground' : 'text-muted-foreground',
              )}
            >
              {icon}
              {/* Why: the page has no visible title, so the selected tab's label names it at every width. */}
              <span>{t(label)}</span>
            </button>
          ))}
        </nav>
        <button
          onClick={onProfile}
          className="mt-auto flex h-12 items-center justify-center gap-2.5 rounded-lg px-1.5 text-left outline-none hover:bg-sidebar-accent focus-visible:ring-[3px] focus-visible:ring-ring lg:justify-start"
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

        {children}

        <nav aria-label={t('groups_mainNavigation')} className="sticky bottom-0 grid h-16 grid-cols-5 border-t bg-card md:hidden">
          {tabs.map(({ tab, label, icon }) => (
            <button
              key={tab}
              aria-current={tab === selected ? 'page' : undefined}
              onClick={() => select(tab)}
              className={cn('flex flex-col items-center justify-center gap-1 text-[11px] font-medium', tab === selected ? 'text-foreground' : 'text-muted-foreground')}
            >
              {icon}
              {t(label)}
            </button>
          ))}
        </nav>
      </div>

      {overlays}
    </div>
  )
}
