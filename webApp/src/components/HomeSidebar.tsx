import { HomeSidebarPresenter, type JsHomeSidebarState } from 'blitzsplit-shared'
import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { HomeShell, HomeShellChromeContext, HomeShellView, loadedProfile, type HomeShellChrome, type HomeTab } from '@/components/HomeShell'
import { homeSearchKeys } from '@/features/groups/SearchDialog'

interface HomeSidebar {
  presenter: HomeSidebarPresenter
  state: JsHomeSidebarState
}

const HomeSidebarContext = createContext<HomeSidebar | null>(null)

/**
 * The sidebar of every page in the home's frame (its tabs, the group's pages, Estatísticas and Perfil): one presenter
 * while any of them is in the back stack, so going from a page to another keeps who is signed in, Search and the open
 * groups as they were instead of reading them again.
 */
export function HomeSidebarProvider({ isActive, children }: { isActive: boolean; children: ReactNode }) {
  const [sidebar, setSidebar] = useState<HomeSidebar | null>(null)
  return (
    <HomeSidebarContext.Provider value={isActive ? sidebar : null}>
      {/* Why: a sibling, so starting and stopping the presenter never remounts the pages. */}
      {isActive && <HomeSidebarSource onChange={setSidebar} />}
      {children}
    </HomeSidebarContext.Provider>
  )
}

function HomeSidebarSource({ onChange }: { onChange: (sidebar: HomeSidebar | null) => void }) {
  const presenter = usePresenter(() => new HomeSidebarPresenter())
  const state = useWatch(presenter, (p, onState: (state: JsHomeSidebarState) => void) => p.observeState(onState))
  useEffect(() => {
    onChange(presenter && state ? { presenter, state } : null)
  }, [presenter, state])
  useEffect(() => () => onChange(null), [])
  return null
}

interface FramedPage {
  chrome: HomeShellChrome
  // Why: false once the page left, until the next one hands its chrome: its clicks would reach a cleared presenter.
  isShown: boolean
}

const nothing = () => {}

// Why: the Search of a page outside the tabs, which answers to no shortcut, as ⌘K there could close a dialog over it.
const pageSearchKeys = { shortcut: '', keyShortcuts: '' }

const startingChrome: HomeShellChrome = { profile: null, onTab: nothing, onProfile: nothing }

/**
 * The home's frame (`HomeShellView`) around one page of it at a time, mounted for as long as pages of the frame follow
 * each other in the same place of the back stack: a tab, a group or Perfil swaps the content alone, while the sidebar,
 * the rail and the phone's bars stay as they are. Each page (`HomeShell`) hands it its tab, bars and clicks once its
 * presenter is up; until then the frame keeps the previous page's, its clicks through the sidebar. Who is signed in
 * and whether Search shows come from the sidebar while the page still reads its own, so neither blinks as the page
 * comes in. Before the first page, it draws nothing, as the page did.
 */
export function HomeFrame({ children }: { children: ReactNode }) {
  const sidebar = useContext(HomeSidebarContext)
  const [page, setPage] = useState<FramedPage | null>(null)
  const handChrome = useCallback((chrome: HomeShellChrome | null) => {
    setPage((previous) => (chrome ? { chrome, isShown: true } : previous && { ...previous, isShown: false }))
  }, [])
  const sidebarState = sidebar?.state.isLoaded ? sidebar.state : null
  const chrome = !page ? startingChrome : page.isShown ? page.chrome : sidebarChrome(page.chrome, sidebar?.presenter)
  // Why: the tabs show Search's shortcut (⌘K), the pages outside them none, as each draws its own Search.
  const isOnTab = !(chrome.isOutsideTabs ?? (chrome.phoneBars ?? 'home') !== 'home')
  const searchKeys = isOnTab ? homeSearchKeys : pageSearchKeys
  return (
    <HomeShellChromeContext.Provider value={handChrome}>
      <HomeShellView
        {...chrome}
        profile={chrome.profile ?? (sidebarState && loadedProfile(sidebarState))}
        search={chrome.search ?? (sidebarState?.canSearch ? { ...searchKeys, onOpen: nothing } : undefined)}
        className={page ? undefined : 'invisible'}
      >
        {children}
      </HomeShellView>
    </HomeShellChromeContext.Provider>
  )
}

/**
 * A page's chrome as it was drawn once the page left, its clicks through the sidebar's presenter, which opens a tab,
 * Perfil, Home and a group from any page; Search and the bell wait for the next page's own.
 */
function sidebarChrome(chrome: HomeShellChrome, presenter: HomeSidebarPresenter | undefined): HomeShellChrome {
  return {
    ...chrome,
    onTab: (tab) => presenter?.openTab(tab),
    onProfile: () => presenter?.openProfile(),
    onBrand: () => presenter?.openBrand(),
    search: chrome.search && { ...chrome.search, onOpen: nothing },
    bell: chrome.bell && { ...chrome.bell, onOpen: nothing },
    openGroups:
      chrome.openGroups && chrome.openGroups !== 'loading' ? { ...chrome.openGroups, onOpen: (id) => presenter?.openGroup(id) } : chrome.openGroups,
  }
}

/** Opens a home tab from a page outside the tabs (its breadcrumb's first crumb), as the sidebar's tab does. */
export function useOpenHomeTab(): (tab: HomeTab) => void {
  const sidebar = useContext(HomeSidebarContext)
  return (tab) => sidebar?.presenter.openTab(tab)
}

/**
 * The home's frame around a page outside its tabs: the home tab it was opened from selected (none on Perfil, whose
 * footer is), and the open groups on the desktop's sidebar while `groups` says which one the page is about. The
 * group's pages draw their own bars on the phone; Perfil keeps the bottom navigation, and a contact's profile the home's
 * bars too.
 */
export function PageShell({
  selected,
  groups,
  phoneBars,
  children,
  overlays,
}: {
  selected?: HomeTab
  groups?: { selectedId: string }
  phoneBars: 'home' | 'nav' | 'none'
  children: ReactNode
  overlays?: ReactNode
}) {
  const sidebar = useContext(HomeSidebarContext)
  // Why: the frame draws at once, the profile and the open groups shimmering until the sidebar is read.
  const presenter = sidebar?.presenter
  const state = sidebar?.state.isLoaded ? sidebar.state : null
  return (
    <HomeShell
      selected={selected}
      isProfileSelected={selected === undefined}
      profile={state && loadedProfile(state)}
      onTab={(tab) => presenter?.openTab(tab)}
      onProfile={() => presenter?.openProfile()}
      onBrand={() => presenter?.openBrand()}
      // Why: the same bars as the tabs', Search included, which goes back to the page's tab to search over it. No ⌘K
      // here, so a shortcut never closes a dialog over the page.
      search={pageSearchOf(state, presenter)}
      openGroups={groups && (state && presenter ? { groups: state.openGroups, selectedId: groups.selectedId, onOpen: (id) => presenter.openGroup(id) } : 'loading')}
      phoneBars={phoneBars}
      isOutsideTabs
      overlays={overlays}
    >
      {children}
    </HomeShell>
  )
}

/**
 * A page's Search, with no shortcut. While the sidebar is still read it holds its place (a click does nothing yet), so
 * the phone's bottom navigation never lays out four items and then five; once read it shows only with a group.
 */
function pageSearchOf(state: JsHomeSidebarState | null, presenter: HomeSidebarPresenter | undefined) {
  if (!state) return { ...pageSearchKeys, onOpen: nothing }
  if (!state.canSearch || !presenter) return undefined
  return { ...pageSearchKeys, onOpen: () => presenter.openSearch() }
}
