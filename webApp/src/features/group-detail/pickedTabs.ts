import type { JsRoute } from 'blitzsplit-shared'

/**
 * The tab picked on each group's page of the back stack, by its page key (`<place in the stack>/group-detail/<group>/
 * <route's tab>`). Why: only the top page is mounted, so a page the group opens (Estatísticas, a person's profile)
 * drops the group's, whose ViewModel starts again on the tab the route opened on; back finds the tab that was picked.
 */
const pickedTabs = new Map<string, string>()

export function rememberPickedTab(pageKey: string, tab: string) {
  pickedTabs.set(pageKey, tab)
}

export function pickedTabOf(pageKey: string): string | undefined {
  return pickedTabs.get(pageKey)
}

/*
 * Why: a page closed, or another group taking its place (the sidebar's), forgets its tab, so a group opened again in
 * that place starts on its route's tab.
 */
export function forgetClosedPickedTabs(backStack: JsRoute[]) {
  for (const key of pickedTabs.keys()) {
    const [place, , groupId, groupTab] = key.split('/')
    const route = backStack[Number(place)]
    const isStillThere = route?.name === 'group-detail' && (route.groupId ?? '') === groupId && (route.groupTab ?? '') === groupTab
    if (!isStillThere) pickedTabs.delete(key)
  }
}
