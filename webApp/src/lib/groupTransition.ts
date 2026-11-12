import type { JsRoute } from 'blitzsplit-shared'
import { flushSync } from 'react-dom'

/*
 * The group's photo and name travel from the row that opened it into the group's header, and back to the row, as
 * Android's shared elements do: the View Transitions API morphs the two snapshots, the page crossfading under them.
 *
 * The marks: a row (Groups' list, table and grid, Home's "Your groups") carries `data-group-link={id}`, the group's
 * header `data-group-header={id}`, and each holds a `data-group-avatar` and a `data-group-name` element. The names
 * (`view-transition-name`) must be unique on a page, so they are set only for a transition, on the one row clicked
 * (or found again on the way back) and the header, and cleared once it ends.
 *
 * Browsers without `document.startViewTransition`, reduced motion and the browser's own back animation (a swipe
 * back, `hasUAVisualTransition`) switch at once.
 */

// Why: how long a click on a row stays the source of the group the stack opens next.
const CLICK_WINDOW_MS = 1000
// Why: the page coming in reads its state from the database first; past this, the page crossfades alone.
const WAIT_FOR_TARGET_MS = 250

let clickedLink: { element: HTMLElement; at: number } | null = null
let skipsNext = false
let isUpdating = false

if (typeof document !== 'undefined') {
  // Why: capture, so the row is known before its own click handler moves the back stack.
  document.addEventListener(
    'click',
    (event) => {
      const link = event.target instanceof Element ? event.target.closest<HTMLElement>('[data-group-link]') : null
      clickedLink = link ? { element: link, at: performance.now() } : null
    },
    { capture: true },
  )
}

/** The browser animated this history step itself (a swipe back), so the next change of the stack only switches. */
export function skipNextGroupTransition() {
  skipsNext = true
}

/** Whether the pages mounting now come in through a shared transition, which replaces their own fade (`page-enter`). */
export function isGroupTransitionUpdating(): boolean {
  return isUpdating
}

type Plan = { kind: 'enter'; groupId: string; source: HTMLElement } | { kind: 'leave'; groupId: string; header: HTMLElement }

/**
 * Wraps the back stack's setter: a group opened from a row it was clicked on, or a group left back to the page that
 * lists it, goes through a view transition; any other change applies at once.
 */
export function withGroupTransition(apply: (routes: JsRoute[]) => void): (routes: JsRoute[]) => void {
  let previous: JsRoute[] | null = null
  let latest: JsRoute[] | null = null
  let isPending = false
  return (routes) => {
    latest = routes
    // Why: a change while a transition waits for its frame is applied by it, never before it (stale over new).
    if (isPending) return
    const plan = previous && planOf(previous, routes)
    previous = routes
    if (!plan) {
      apply(routes)
      return
    }
    isPending = true
    run(plan, () => {
      isPending = false
      if (latest) {
        previous = latest
        apply(latest)
      }
    })
  }
}

function planOf(previous: JsRoute[], next: JsRoute[]): Plan | null {
  const skips = skipsNext
  skipsNext = false
  const click = clickedLink
  clickedLink = null
  if (skips || !canTransition()) return null
  const before = previous[previous.length - 1]
  const after = next[next.length - 1]
  if (after?.name === 'group-detail' && next.length > previous.length && after.groupId) {
    // Why: only the row just clicked, still on the page, for the group the stack opened.
    if (!click || performance.now() - click.at > CLICK_WINDOW_MS || !click.element.isConnected) return null
    if (click.element.dataset.groupLink !== after.groupId) return null
    return { kind: 'enter', groupId: after.groupId, source: click.element }
  }
  if (before?.name === 'group-detail' && next.length < previous.length && before.groupId && (after?.name === 'home' || after?.name === 'groups')) {
    const header = document.querySelector<HTMLElement>(`[data-group-header="${CSS.escape(before.groupId)}"]`)
    return header ? { kind: 'leave', groupId: before.groupId, header } : null
  }
  return null
}

function canTransition(): boolean {
  return typeof document.startViewTransition === 'function' && !window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function run(plan: Plan, apply: () => void) {
  const named: HTMLElement[] = []
  const name = (holder: HTMLElement, groupId: string) => {
    const avatar = holder.querySelector<HTMLElement>('[data-group-avatar]')
    const title = holder.querySelector<HTMLElement>('[data-group-name]')
    if (avatar) avatar.style.viewTransitionName = `group-avatar-${cssIdent(groupId)}`
    if (title) title.style.viewTransitionName = `group-name-${cssIdent(groupId)}`
    named.push(...[avatar, title].filter((element): element is HTMLElement => element !== null))
  }
  name(plan.kind === 'enter' ? plan.source : plan.header, plan.groupId)
  const target = plan.kind === 'enter' ? `[data-group-header="${CSS.escape(plan.groupId)}"]` : `[data-group-link="${CSS.escape(plan.groupId)}"]`
  try {
    const transition = document.startViewTransition(async () => {
      isUpdating = true
      try {
        flushSync(apply)
        const holder = await waitFor(target)
        if (holder) name(holder, plan.groupId)
      } finally {
        isUpdating = false
      }
    })
    transition.finished.finally(() => named.forEach((element) => (element.style.viewTransitionName = '')))
  } catch {
    named.forEach((element) => (element.style.viewTransitionName = ''))
    apply()
  }
}

/**
 * The element in sight once the page coming in drew it, or null after `WAIT_FOR_TARGET_MS`. Groups draws its table and
 * its phone list, one of them hidden; a row out of sight (Home lists three groups, a long list) leaves the page to
 * crossfade alone.
 */
function waitFor(selector: string): Promise<HTMLElement | null> {
  const shown = () => Array.from(document.querySelectorAll<HTMLElement>(selector)).find(isInView) ?? null
  const found = shown()
  if (found) return Promise.resolve(found)
  return new Promise((resolve) => {
    const observer = new MutationObserver(() => {
      const element = shown()
      if (element) finish(element)
    })
    const timeout = window.setTimeout(() => finish(null), WAIT_FOR_TARGET_MS)
    const finish = (element: HTMLElement | null) => {
      observer.disconnect()
      window.clearTimeout(timeout)
      resolve(element)
    }
    observer.observe(document.body, { childList: true, subtree: true })
  })
}

function isInView(element: HTMLElement): boolean {
  const rect = element.getBoundingClientRect()
  return rect.width > 0 && rect.bottom > 0 && rect.top < window.innerHeight
}

// Why: a group's id is a UUID, which may start with a digit; a custom ident may not.
function cssIdent(id: string): string {
  return id.replace(/[^a-zA-Z0-9_-]/g, '_')
}
