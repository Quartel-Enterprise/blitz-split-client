import { initializeApp } from 'firebase/app'
import { initializeAnalytics, isSupported, logEvent } from 'firebase/analytics'
import { attachAnalytics } from 'blitzsplit-shared'

/**
 * Firebase Analytics on the web (docs/analytics/README.md), the same project as the apps. It starts only in a
 * production build with the web app's config (`VITE_FIREBASE_*`) and in a browser that supports it: without any of
 * them, the events the shared code tracks go nowhere. A blocked gtag.js (an ad blocker) leaves it off the same way.
 * Each screen is tracked by the shared code from the back stack, so gtag sends no page_view of its own (nor does GA4's
 * enhanced measurement, whose "page changes based on browser history events" stays off in the web stream's settings).
 */
const config = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY ?? '',
  appId: import.meta.env.VITE_FIREBASE_APP_ID ?? '',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID ?? '',
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID ?? '',
}

export async function startAnalytics(): Promise<void> {
  if (import.meta.env.DEV || Object.values(config).some((value) => value === '')) return
  try {
    if (!(await isSupported())) return
    const app = initializeApp(config)
    const analytics = initializeAnalytics(app, {
      config: { send_page_view: false, allow_google_signals: false, allow_ad_personalization_signals: false },
    })
    attachAnalytics((name, params) => logEvent(analytics, name, params))
  } catch {
    // Analytics only counts what people do; a page that can't load it works as before
  }
}
