import { HomeSidebarPresenter, type JsHomeSidebarState } from 'blitzsplit-shared'
import { createContext, useContext, type ReactNode } from 'react'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { HomeShell, type HomeTab } from '@/components/HomeShell'

interface HomeSidebar {
  presenter: HomeSidebarPresenter
  state: JsHomeSidebarState
}

const HomeSidebarContext = createContext<HomeSidebar | null>(null)

/**
 * The sidebar of the pages outside the home's tabs (the group's pages, Estatísticas and Perfil): one presenter for
 * the whole visit, so going from a group to another keeps the sidebar as it was instead of reading it again.
 */
export function HomeSidebarProvider({ children }: { children: ReactNode }) {
  const presenter = usePresenter(() => new HomeSidebarPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsHomeSidebarState) => void) => p.observeState(onChange))
  const sidebar = presenter && state ? { presenter, state } : null
  return <HomeSidebarContext.Provider value={sidebar}>{children}</HomeSidebarContext.Provider>
}

/** Opens a home tab from a page outside the tabs (its breadcrumb's first crumb), as the sidebar's tab does. */
export function useOpenHomeTab(): (tab: HomeTab) => void {
  const sidebar = useContext(HomeSidebarContext)
  return (tab) => sidebar?.presenter.openTab(tab)
}

/**
 * The home's frame around a page outside its tabs: the home tab it was opened from selected (none on Perfil, whose
 * footer is), and the open groups on the desktop's sidebar while `groups` says which one the page is about. The
 * group's pages draw their own bars on the phone; Perfil keeps the bottom navigation.
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
  phoneBars: 'nav' | 'none'
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
      overlays={overlays}
    >
      {children}
    </HomeShell>
  )
}
