import { startBlitzSplit } from 'blitzsplit-shared'

const DATABASE_LOCK = 'borarachar-database'

/*
 * Held until the tab closes or navigates away; the browser then releases the lock on its own. A page
 * kept in the back/forward cache isn't gone, though, so it lets go itself (see releaseOnPageCache).
 */
let releaseLock = () => {}
const untilTheTabCloses = new Promise<void>((resolve) => {
  releaseLock = resolve
})

let starting: Promise<void> | null = null

/**
 * Starts the shared Kotlin code once, handing it the SQLite Web Worker that keeps Room's database
 * in the browser (OPFS). Vite bundles the worker because the app creates it here. Every call
 * resolves once the shared code has started.
 *
 * Why the lock: the database's OPFS files take exclusive handles, so only one tab of the app can
 * open it. The first tab keeps the lock while it's open; any other calls `onWaiting` and starts as
 * soon as that tab closes.
 */
export function startShared(onWaiting: () => void): Promise<void> {
  starting ??= start(onWaiting)
  return starting
}

async function start(onWaiting: () => void) {
  // Browsers without Web Locks also predate the OPFS handles the worker needs; let the worker report it
  if (navigator.locks) {
    // A tab opening in the background (session restore, a link opened in a new tab) would otherwise
    // take the lock in the moment the tab in use reloads or comes back from a sign in
    await untilVisible()
    if (!(await takeLockIfFree())) {
      onWaiting()
      await waitForLockWhileVisible()
    }
  }
  const worker = new Worker(new URL('../sqlite/worker.js', import.meta.url), { type: 'module' })
  releaseOnPageCache(worker)
  startBlitzSplit(worker)
}

function untilVisible(): Promise<void> {
  if (document.visibilityState === 'visible') return Promise.resolve()
  return new Promise((resolve) => {
    const onVisibilityChange = () => {
      if (document.visibilityState !== 'visible') return
      document.removeEventListener('visibilitychange', onVisibilityChange)
      resolve()
    }
    document.addEventListener('visibilitychange', onVisibilityChange)
  })
}

/**
 * Why: a browser that keeps this page in its back/forward cache (leaving for a sign in, say) would
 * keep the lock and the worker's file handles too, and the page the sign in returns to would wait
 * for itself. So the page ends the worker and frees the lock when cached, and starts over if it's
 * brought back.
 */
function releaseOnPageCache(worker: Worker) {
  window.addEventListener('pagehide', (event) => {
    if (!event.persisted) return
    worker.terminate()
    releaseLock()
  })
  window.addEventListener('pageshow', (event) => {
    if (event.persisted) window.location.reload()
  })
}

function takeLockIfFree(): Promise<boolean> {
  return new Promise((resolve) => {
    navigator.locks
      .request(DATABASE_LOCK, { ifAvailable: true }, (lock) => {
        resolve(lock !== null)
        return lock ? untilTheTabCloses : undefined
      })
      // Web Locks refused (a sandboxed or opaque origin): start without it, as when the browser has none
      .catch(() => resolve(true))
  })
}

/**
 * Why only while visible: a reload, or a sign in through the browser, releases the lock of the tab
 * in use for a moment. A tab queued in the background would take it then, and the tab the person is
 * looking at would come back to the waiting screen.
 */
function waitForLockWhileVisible(): Promise<void> {
  return new Promise((resolve) => {
    let request: AbortController | null = null
    const onVisibilityChange = () => {
      if (document.visibilityState === 'hidden') {
        request?.abort()
        request = null
        return
      }
      if (request) return
      request = new AbortController()
      navigator.locks
        .request(DATABASE_LOCK, { signal: request.signal }, () => {
          document.removeEventListener('visibilitychange', onVisibilityChange)
          resolve()
          return untilTheTabCloses
        })
        .catch((error: unknown) => {
          // An AbortError when the tab was hidden before its turn; it queues again once visible
          if (error instanceof DOMException && error.name === 'AbortError') return
          // Web Locks refused: start without it rather than wait forever
          document.removeEventListener('visibilitychange', onVisibilityChange)
          resolve()
        })
    }
    document.addEventListener('visibilitychange', onVisibilityChange)
    onVisibilityChange()
  })
}
