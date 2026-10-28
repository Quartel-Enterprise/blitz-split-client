import { readFileSync } from 'node:fs'
import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// version.properties is the single source of truth for the app version, as on Android and Apple.
const versionName = /^versionName\s*=\s*(.+)$/m.exec(readFileSync(path.resolve(import.meta.dirname, '../version.properties'), 'utf8'))?.[1].trim() ?? ''

export default defineConfig({
  plugins: [react(), tailwindcss()],
  define: {
    __APP_VERSION__: JSON.stringify(versionName),
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      ws: path.resolve(__dirname, './src/bridge/ws-browser.ts'),
    },
  },
  // SQLite WASM ships its own worker and wasm files; pre-bundling would break their relative URLs.
  optimizeDeps: {
    exclude: ['@sqlite.org/sqlite-wasm', 'blitzsplit-shared'],
  },
  worker: {
    format: 'es',
  },
})
