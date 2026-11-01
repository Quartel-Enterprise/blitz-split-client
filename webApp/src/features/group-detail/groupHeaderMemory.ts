import type { JsGroupExit, JsGroupHeader, JsRoute } from 'blitzsplit-shared'

/** What the group's page drew around Despesas or Saldos, for the next tab to draw while it reads the group. */
export interface RememberedGroupPage {
  header: JsGroupHeader
  hasExpenses: boolean
  showsSplitBillButton: boolean
  exit: JsGroupExit
}

const pages = new Map<string, RememberedGroupPage>()
let lastTop: JsRoute | undefined
let switchedGroupId: string | null = null

/**
 * Switching between a group's tabs only changes what is under them, so the tab opened draws the page the one left
 * drew at once, instead of nothing until it has read the group. Only right after a tab switch: any other move of the
 * back stack forgets it, so a group opened later never shows an older header than the device holds.
 */
export function noteBackStack(routes: JsRoute[]) {
  const top = routes[routes.length - 1]
  switchedGroupId = lastTop && top && lastTop.isGroupTabSwitchTo(top) ? (top.groupId ?? null) : null
  lastTop = top
}

export function rememberGroupPage(groupId: string, page: RememberedGroupPage) {
  pages.set(groupId, page)
}

export function rememberedGroupPage(groupId: string): RememberedGroupPage | undefined {
  return switchedGroupId === groupId ? pages.get(groupId) : undefined
}
