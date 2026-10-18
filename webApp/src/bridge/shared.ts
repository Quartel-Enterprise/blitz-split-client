import { startBlitzSplit } from 'blitzsplit-shared'

let started = false

/**
 * Starts the shared Kotlin code once, handing it the SQLite Web Worker that keeps Room's database
 * in the browser (OPFS). Vite bundles the worker because the app creates it here.
 */
export function startShared() {
  if (started) return
  started = true
  const worker = new Worker(new URL('../sqlite/worker.js', import.meta.url), { type: 'module' })
  startBlitzSplit(worker)
}
