/// <reference types="vite/client" />

/** The app's versionName, from version.properties. */
declare const __APP_VERSION__: string

interface ImportMetaEnv {
  /** The VAPID public key of Atividade's Web Push (base64url); unset, the browser never subscribes. */
  readonly VITE_VAPID_PUBLIC_KEY?: string
}
