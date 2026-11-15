/// <reference types="vite/client" />

/** The app's versionName, from version.properties. */
declare const __APP_VERSION__: string

interface ImportMetaEnv {
  /** The VAPID public key of Atividade's Web Push (base64url); unset, the browser never subscribes. */
  readonly VITE_VAPID_PUBLIC_KEY?: string
  /** The Firebase web app's config, for Firebase Analytics; any unset, the web tracks nothing. */
  readonly VITE_FIREBASE_API_KEY?: string
  readonly VITE_FIREBASE_APP_ID?: string
  readonly VITE_FIREBASE_PROJECT_ID?: string
  readonly VITE_FIREBASE_MEASUREMENT_ID?: string
  /** The Sentry project's DSN, for crash reporting; unset, the web reports nothing. */
  readonly VITE_SENTRY_DSN?: string
  /** Which deploy reports to Sentry: `beta` for the betas; unset, `production`. */
  readonly VITE_SENTRY_ENVIRONMENT?: string
}
