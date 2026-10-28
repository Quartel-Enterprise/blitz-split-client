import { startBlitzSplit } from 'blitzsplit-shared'

const DATABASE_LOCK = 'borarachar-database'

// Held until the tab closes or navigates away; the browser then releases the lock on its own
const untilTheTabCloses = new Promise<never>(() => {})

let started = false

/**
 * Starts the shared Kotlin code once, handing it the SQLite Web Worker that keeps Room's database
 * in the browser (OPFS). Vite bundles the worker because the app creates it here.
 *
 * Why the lock: the database's OPFS files take exclusive handles, so only one tab of the app can
 * open it. The first tab keeps the lock while it's open; any other calls `onWaiting` and starts as
 * soon as that tab closes.
 */
export async function startShared(onWaiting: () => void) {
  if (started) return
  started = true
  // Browsers without Web Locks also predate the OPFS handles the worker needs; let the worker report it
  if (navigator.locks && !(await takeLockIfFree())) {
    onWaiting()
    await waitForLockWhileVisible()
  }
  const worker = new Worker(new URL('../sqlite/worker.js', import.meta.url), { type: 'module' })
  startBlitzSplit(worker)
}

function takeLockIfFree(): Promise<boolean> {
  return new Promise((resolve) => {
    void navigator.locks.request(DATABASE_LOCK, { ifAvailable: true }, (lock) => {
      resolve(lock !== null)
      return lock ? untilTheTabCloses : undefined
    })
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
        // An AbortError when the tab was hidden before its turn; it queues again once visible
        .catch(() => {})
    }
    document.addEventListener('visibilitychange', onVisibilityChange)
    onVisibilityChange()
  })
}
