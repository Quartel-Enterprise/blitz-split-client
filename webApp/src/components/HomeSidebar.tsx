import { HomeSidebarPresenter, type JsHomeSidebarState } from 'blitzsplit-shared'
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { HomeShell, type HomeTab } from '@/components/HomeShell'

interface HomeSidebar {
  presenter: HomeSidebarPresenter
  state: JsHomeSidebarState
}

const HomeSidebarContext = createContext<HomeSidebar | null>(null)

/**
 * The sidebar of the pages outside the home's tabs (the group's pages, Estatísticas and Perfil): one presenter while
 * any of them is in the back stack, so going from a group to another keeps the sidebar as it was instead of reading it
 * again, and none while only the home's tabs are, which never draw it.
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
  // Why: nothing is drawn until the sidebar is read, so it never fills in beside the page.
  if (!sidebar?.state.isLoaded) return null
  const { presenter, state } = sidebar
  return (
    <HomeShell
      selected={selected}
      isProfileSelected={selected === undefined}
      profile={state}
      activityUnreadCount={state.activityUnreadCount}
      onTab={(tab) => presenter.openTab(tab)}
      onProfile={() => presenter.openProfile()}
      openGroups={groups && { groups: state.openGroups, selectedId: groups.selectedId, onOpen: (id) => presenter.openGroup(id) }}
      phoneBars={phoneBars}
      isOutsideTabs
      overlays={overlays}
    >
      {children}
    </HomeShell>
  )
}
