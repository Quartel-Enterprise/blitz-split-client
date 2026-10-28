import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from '@/app/App'
import { OpenInAnotherTabScreen } from '@/app/OpenInAnotherTabScreen'
import { startShared } from '@/bridge/shared'
import { applyDocumentLocale } from '@/i18n'
import './index.css'

applyDocumentLocale()

const root = createRoot(document.getElementById('root')!)
void startShared(() => root.render(<OpenInAnotherTabScreen />)).then(() =>
  root.render(
    <StrictMode>
      <App />
    </StrictMode>,
  ),
)
