import type { JsRoute } from 'blitzsplit-shared'
import { flushSync } from 'react-dom'

/*
 * The group's photo and name travel from the row that opened it into the group's header, and back to the row, as
 * Android's shared elements do: the View Transitions API morphs the two snapshots, the page crossfading under them.
 *
 * The marks: a row (Groups' list, table and grid, Home's "Your groups") carries `data-group-link={id}`, the group's
 * header `data-group-header={id}`, and each holds a `data-group-avatar` and a `data-group-name` element. The members'
 * faces fly too (beta feedback of 2026-11-16): a row's `data-group-faces` (the table and the grid, which show them) and
 * the group's own, under its header and outside it, `data-group-faces={id}`. The names
 * (`view-transition-name`) must be unique on a page, so they are set only for a transition, on the one row clicked
 * (or found again on the way back) and the header, and cleared once it ends.
 *
 * A person's face flies the same way into the profile it opens (Profile or a contact's) and back into it (beta feedback
 * of 2026-11-15): the clickable face, name or row carries `data-profile-link={key}` around the face (`.avatar` or
 * `data-profile-face`), the profile's photo `data-profile-photo`. Only the face clicked in the last second flies, so a
 * bill showing its payer twice, or a profile opened from a search hit, never flies out of another face; the key finds
 * the face again on the way back, once its page drew it again. A row that shows the person's name and balance (Contacts,
 * Balances) marks them `data-profile-piece="name"`, `"amount"` and `"word"`, in the link or beside it, and they fly into
 * the pieces so marked in the profile's page (`data-profile-page`) (beta feedback of 2026-11-16); a side without one
 * leaves that piece to crossfade with the page. The amount and the word carry their currency (`data-profile-balance`):
 * Balances lists a person once per currency, and only the row in the profile's currency flies into its balance.
 *
 * Browsers without `document.startViewTransition`, reduced motion and the browser's own back animation (a swipe
 * back, `hasUAVisualTransition`) switch at once.
 */

// Why: how long a click on a row stays the source of the group the stack opens next.
const CLICK_WINDOW_MS = 1000
// Why: the page coming in reads its state from the database first; past this, the page crossfades alone.
const WAIT_FOR_TARGET_MS = 250

let clickedLink: { element: HTMLElement; at: number } | null = null
let clickedFace: { element: HTMLElement; at: number } | null = null
// Why: the face each open profile flew out of, by the depth of its route in the stack, which back flies into again.
const facesByDepth = new Map<number, string>()
const PROFILE_ROUTES = new Set(['contact-profile', 'profile'])
// Why: a swipe back that left the stack as it was must not keep the next change from its transition.
const SKIP_WINDOW_MS = 1000
let skipsUntil = 0
let isUpdating = false

if (typeof document !== 'undefined') {
  // Why: capture, so the row is known before its own click handler moves the back stack.
  document.addEventListener(
    'click',
    (event) => {
      const target = event.target instanceof Element ? event.target : null
      const link = target?.closest<HTMLElement>('[data-group-link]')
      clickedLink = link ? { element: link, at: performance.now() } : null
      const face = target?.closest<HTMLElement>('[data-profile-link]')
      clickedFace = face ? { element: face, at: performance.now() } : null
    },
    { capture: true },
  )
}

/**
 * Whether the browser animated this history step itself (a swipe back), so the change of the stack it makes, within a
 * second, only switches; false again when the step didn't move the stack after all (held for "Discard changes?").
 */
export function setSkipsNextGroupTransition(skips: boolean) {
  skipsUntil = skips ? performance.now() + SKIP_WINDOW_MS : 0
}

/** Whether the pages mounting now come in through a shared transition, which replaces their own fade (`page-enter`). */
export function isGroupTransitionUpdating(): boolean {
  return isUpdating
}

// Why: what flies: the element named on the page leaving, the selector of its counterpart on the page coming in, and how
// a holder (a row, a header, a face's link) names its pieces for the transition.
type Plan = { from: HTMLElement; to: string; mark: (holder: HTMLElement) => HTMLElement[] }

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
    if (isPending) {
      forgetFacesFrom(routes.length + 1)
      return
    }
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
  const skips = performance.now() < skipsUntil
  skipsUntil = 0
  const click = clickedLink
  clickedLink = null
  const face = clickedFace
  clickedFace = null
  // Why: read before the profile leaves the stack, whatever then flies; the depths it left forget their faces, so a
  // later profile there (a search hit, a link) never flies into a face nobody tapped.
  const leftFace = facesByDepth.get(previous.length)
  forgetFacesFrom(next.length > previous.length ? next.length : next.length + 1)
  if (skips || !canTransition()) return null
  return groupPlanOf(previous, next, click) ?? profilePlanOf(previous, next, face, leftFace)
}

function forgetFacesFrom(depth: number) {
  for (const stored of facesByDepth.keys()) if (stored >= depth) facesByDepth.delete(stored)
}

function groupPlanOf(previous: JsRoute[], next: JsRoute[], click: { element: HTMLElement; at: number } | null): Plan | null {
  const before = previous[previous.length - 1]
  const after = next[next.length - 1]
  if (after?.name === 'group-detail' && next.length > previous.length && after.groupId) {
    // Why: only the row just clicked, still on the page, for the group the stack opened.
    if (!isRecent(click) || click.element.dataset.groupLink !== after.groupId) return null
    return groupPlan(after.groupId, click.element, `[data-group-header="${CSS.escape(after.groupId)}"]`)
  }
  if (before?.name === 'group-detail' && next.length < previous.length && before.groupId && (after?.name === 'home' || after?.name === 'groups')) {
    const header = document.querySelector<HTMLElement>(`[data-group-header="${CSS.escape(before.groupId)}"]`)
    return header ? groupPlan(before.groupId, header, `[data-group-link="${CSS.escape(before.groupId)}"]`) : null
  }
  return null
}

function groupPlan(groupId: string, from: HTMLElement, to: string): Plan {
  return {
    from,
    to,
    mark: (holder) => {
      const avatar = holder.querySelector<HTMLElement>('[data-group-avatar]')
      const title = holder.querySelector<HTMLElement>('[data-group-name]')
      const faces = holder.matches('[data-group-header]')
        ? topmostInView(`[data-group-faces="${CSS.escape(groupId)}"]`)
        : holder.querySelector<HTMLElement>('[data-group-faces]')
      if (avatar) avatar.style.viewTransitionName = `group-avatar-${cssIdent(groupId)}`
      if (title) title.style.viewTransitionName = `group-name-${cssIdent(groupId)}`
      if (faces) faces.style.viewTransitionName = `group-faces-${cssIdent(groupId)}`
      return [avatar, title, faces].filter((element): element is HTMLElement => element !== null)
    },
  }
}

function profilePlanOf(
  previous: JsRoute[],
  next: JsRoute[],
  face: { element: HTMLElement; at: number } | null,
  leftFace: string | undefined,
): Plan | null {
  const before = previous[previous.length - 1]
  const after = next[next.length - 1]
  if (after && PROFILE_ROUTES.has(after.name) && next.length > previous.length) {
    if (!isRecent(face)) return null
    const key = face.element.dataset.profileLink
    if (key) facesByDepth.set(next.length, key)
    return { from: face.element, to: '[data-profile-photo]', mark: markFace }
  }
  if (before && PROFILE_ROUTES.has(before.name) && next.length < previous.length) {
    const photo = topmostInView('[data-profile-photo]')
    return leftFace && photo ? { from: photo, to: `[data-profile-link="${CSS.escape(leftFace)}"]`, mark: markFace } : null
  }
  return null
}

const PROFILE_PIECES = ['name', 'amount', 'word']

/*
 * Why: the profile's photo, or the face inside the link clicked, carries the one name both sides share. A link with no
 * face inside (a name, a round button laid over the face) takes the face beside it, and the row's name and balance are
 * found the same way; the profile's are found in its page.
 */
function markFace(holder: HTMLElement): HTMLElement[] {
  const isPhoto = holder.matches('[data-profile-photo]')
  const scopes = isPhoto ? [holder.closest<HTMLElement>('[data-profile-page]')] : [holder, holder.parentElement]
  const firstIn = (selector: string) => scopes.reduce<HTMLElement | null>((found, scope) => found ?? scope?.querySelector<HTMLElement>(selector) ?? null, null)
  const face = isPhoto ? holder : firstIn('[data-profile-face], .avatar')
  if (!face) return []
  face.style.viewTransitionName = 'profile-face'
  const pieces = PROFILE_PIECES.flatMap((piece) => {
    const element = firstIn(`[data-profile-piece="${piece}"]`)
    // Why: a piece scrolled out of sight (the profile's balance above the top) crossfades with the page, as a face does.
    if (!element || !isInView(element)) return []
    const balance = element.dataset.profileBalance
    element.style.viewTransitionName = balance ? `profile-${piece}-${cssIdent(balance)}` : `profile-${piece}`
    // Why: its snapshots keep their proportions (index.css), since the two texts differ in size and wording.
    element.style.setProperty('view-transition-class', 'profile-piece')
    return [element]
  })
  return [face, ...pieces]
}

function isRecent(click: { element: HTMLElement; at: number } | null): click is { element: HTMLElement; at: number } {
  return click !== null && performance.now() - click.at <= CLICK_WINDOW_MS && click.element.isConnected
}

function canTransition(): boolean {
  return typeof document.startViewTransition === 'function' && !window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function run(plan: Plan, apply: () => void) {
  const named: HTMLElement[] = plan.mark(plan.from)
  try {
    const transition = document.startViewTransition(async () => {
      isUpdating = true
      try {
        flushSync(apply)
        // Why: the old page's marks are captured already; one still on the page would share the target's names, which aborts the transition.
        named.splice(0).forEach((element) => (element.style.viewTransitionName = ''))
        const holder = await waitFor(plan.to)
        if (holder) named.push(...plan.mark(holder))
      } finally {
        isUpdating = false
      }
    })
    // Why: cleared whether it ended or failed; `finally` would pass a failed update on as an unhandled rejection.
    const clear = () => named.forEach((element) => (element.style.viewTransitionName = ''))
    transition.finished.then(clear, clear)
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
  const shown = () => topmostInView(selector)
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

/*
 * Why: the last match in sight, the page drawn on top: a stack over a bill (a group, a member, their profile) keeps the
 * stack under it mounted, whose own profile or row would otherwise be found first.
 */
function topmostInView(selector: string): HTMLElement | null {
  return Array.from(document.querySelectorAll<HTMLElement>(selector)).filter(isInView).pop() ?? null
}

function isInView(element: HTMLElement): boolean {
  const rect = element.getBoundingClientRect()
  return rect.width > 0 && rect.bottom > 0 && rect.top < window.innerHeight
}

// Why: a group's id is a UUID, which may start with a digit; a custom ident may not.
function cssIdent(id: string): string {
  return id.replace(/[^a-zA-Z0-9_-]/g, '_')
}
