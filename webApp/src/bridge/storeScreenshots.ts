/**
 * The sample trip of the screenshots (scripts/readme-hero): the capture opens the dev server in a browser profile of
 * its own and sets `window.__storeScreenshots` before the page runs. The shared code then seeds the trip into that
 * profile's empty Room and opens the screen, as the Apple apps' store screenshots do. A production build never looks.
 */
export type StoreScreenshotsRun = {
  /** The app language's code the trip is told in: "en", "pt", "pt-PT"… */
  language: string
  isDark: boolean
  /** A code of scripts/store-screenshots/screens.tsv: "group", "balances"… */
  screen: string | null
  /** The photos by their path under scripts/store-screenshots/ ("faces/person0.jpg"), as base64 */
  photos: Record<string, string>
}

declare global {
  interface Window {
    __storeScreenshots?: StoreScreenshotsRun
  }
}

export function storeScreenshotsRun(): StoreScreenshotsRun | null {
  if (!import.meta.env.DEV) return null
  return window.__storeScreenshots ?? null
}
