import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from '@/app/App'
import { startShared } from '@/bridge/shared'
import { applyDocumentLocale } from '@/i18n'
import './index.css'

applyDocumentLocale()
startShared()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
