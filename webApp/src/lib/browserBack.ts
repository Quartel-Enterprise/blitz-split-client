/**
 * A screen that can't be left midway (an account deletion under way) holds the browser's back button
 * while it is set. Why: the history entry is the browser's, so App re-pushes it instead of popping the stack.
 */
let blockers = 0

export function isBrowserBackBlocked(): boolean {
  return blockers > 0
}

export function blockBrowserBack(): () => void {
  blockers += 1
  return () => {
    blockers -= 1
  }
}
