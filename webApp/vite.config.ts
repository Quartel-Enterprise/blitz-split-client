import { readFileSync } from 'node:fs'
import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig, type Plugin } from 'vite'

// version.properties is the single source of truth for the app version, as on Android and Apple.
const versionName = /^versionName\s*=\s*(.+)$/m.exec(readFileSync(path.resolve(import.meta.dirname, '../version.properties'), 'utf8'))?.[1].trim() ?? ''

// stores.properties says which stores list the phone app, as Gradle reads it for "Get the app" (page 14).
const appStoreId = /^appStoreId[ \t]*=[ \t]*(\S*)[ \t]*$/m.exec(readFileSync(path.resolve(import.meta.dirname, '../stores.properties'), 'utf8'))?.[1] ?? ''
if (!/^\d*$/.test(appStoreId)) throw new Error("stores.properties: appStoreId is the App Store's numeric Apple ID, or empty")

// Why: Safari on an iPhone draws the App Store's own banner from this tag (14.3b), so it only goes in with a listing.
function smartAppBanner(): Plugin {
  return {
    name: 'smart-app-banner',
    transformIndexHtml: () => (appStoreId ? [{ tag: 'meta', attrs: { name: 'apple-itunes-app', content: `app-id=${appStoreId}` }, injectTo: 'head' }] : []),
  }
}

export default defineConfig({
  plugins: [react(), tailwindcss(), smartAppBanner()],
  define: {
    __APP_VERSION__: JSON.stringify(versionName),
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      ws: path.resolve(__dirname, './src/bridge/ws-browser.ts'),
    },
    // Why: kotlinx-datetime's JS imports js-joda from the shared library's folder, whose node_modules Gradle's copy
    // wipes on each build; the app installs it instead and resolves it from here.
    dedupe: ['@js-joda/core'],
  },
  // SQLite WASM ships its own worker and wasm files; pre-bundling would break their relative URLs.
  optimizeDeps: {
    exclude: ['@sqlite.org/sqlite-wasm', 'blitzsplit-shared'],
  },
  worker: {
    format: 'es',
  },
})
