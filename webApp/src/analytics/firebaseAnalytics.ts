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

/**
 * Firebase Analytics collects in production builds, on by default under legitimate interest, until Profile's "Share
 * usage data" turns it off in this browser (docs/analytics/README.md#consent). false turns it off for everyone.
 */
const collectsAnalytics = true

export async function startAnalytics(): Promise<void> {
  if (!collectsAnalytics || import.meta.env.DEV || Object.values(config).some((value) => value === '')) return
  try {
    // Why: loaded only here, so a build without the config, or a page that never gets this far, downloads no Firebase
    const [{ initializeApp }, { initializeAnalytics, isSupported, logEvent, setUserId }] = await Promise.all([
      import('firebase/app'),
      import('firebase/analytics'),
    ])
    if (!(await isSupported())) return
    // Why: off until the shared code read this browser's "Share usage data", so gtag sends nothing of its own before
    disableCollection(true)
    const app = initializeApp(config)
    const analytics = initializeAnalytics(app, {
      config: { send_page_view: false, allow_google_signals: false, allow_ad_personalization_signals: false },
    })
    attachAnalytics(
      (name, params) => logEvent(analytics, name, name === 'screen_view' ? webScreenView(params) : params),
      (isEnabled) => disableCollection(!isEnabled),
      (userId) => setUserId(analytics, userId ?? null),
    )
  } catch {
    // Analytics only counts what people do; a page that can't load it works as before
  }
}

/**
 * The shared code's screen_view carries GA4's screen_name and screen_class, which Android's and Apple's SDKs read; the
 * web SDK reads the same screen from firebase_screen and firebase_screen_class instead.
 */
function webScreenView(params: Record<string, unknown>): Record<string, unknown> {
  const { screen_name: screen, screen_class: screenClass, ...rest } = params
  return { ...rest, firebase_screen: screen, firebase_screen_class: screenClass }
}

/**
 * gtag's own switch, read before anything it sends, which firebase/analytics' setAnalyticsCollectionEnabled sets the
 * same way; set here first so it holds from initializeAnalytics on.
 */
function disableCollection(isDisabled: boolean): void {
  ;(window as unknown as Record<string, boolean>)[`ga-disable-${config.measurementId}`] = isDisabled
}
