import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
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
