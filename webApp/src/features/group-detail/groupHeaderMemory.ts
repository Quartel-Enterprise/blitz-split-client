import type { JsGroupExit, JsGroupHeader, JsRoute } from 'blitzsplit-shared'

/** What the group's page drew around Despesas or Saldos, for the next tab to draw while it reads the group. */
export interface RememberedGroupPage {
  header: JsGroupHeader
  hasExpenses: boolean
  showsSplitBillButton: boolean
  exit: JsGroupExit
}

// Why: the tabs that draw the group's page; Membros draws its own, so a switch from it has no page to carry over.
const tabsWithPage = new Set(['group-detail', 'balances'])
const groupTabs = new Set([...tabsWithPage, 'members'])

const pages = new Map<string, RememberedGroupPage>()
let lastTop: JsRoute | undefined
let switchedGroupId: string | null = null

/**
 * Switching between a group's tabs only changes what is under them, so the tab opened draws the page the one left
 * drew at once, instead of nothing until it has read the group. Only right after a switch from Despesas or Saldos,
 * and only while a tab of the group is on the back stack, so a group opened later never shows an older header than
 * the device holds.
 */
export function noteBackStack(routes: JsRoute[]) {
  const top = routes[routes.length - 1]
  const isSwitchFromPage = lastTop && top && tabsWithPage.has(lastTop.name) && lastTop.isGroupTabSwitchTo(top)
  switchedGroupId = isSwitchFromPage ? (top.groupId ?? null) : null
  lastTop = top
  const groupIds = new Set(routes.filter((route) => groupTabs.has(route.name)).map((route) => route.groupId))
  for (const groupId of pages.keys()) {
    if (!groupIds.has(groupId)) pages.delete(groupId)
  }
}

export function rememberGroupPage(groupId: string, page: RememberedGroupPage) {
  pages.set(groupId, page)
}

export function rememberedGroupPage(groupId: string): RememberedGroupPage | undefined {
  return switchedGroupId === groupId ? pages.get(groupId) : undefined
}
