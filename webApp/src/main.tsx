import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from '@/app/App'
import { OpenInAnotherTabScreen } from '@/app/OpenInAnotherTabScreen'
import { startShared } from '@/bridge/shared'
import { applyDocumentLocale } from '@/i18n'
import { applyTheme } from '@/lib/theme'
import './index.css'

applyDocumentLocale()
applyTheme('system')

const root = createRoot(document.getElementById('root')!)
void startShared(() => root.render(<OpenInAnotherTabScreen />)).then(() =>
  root.render(
    <StrictMode>
      <App />
    </StrictMode>,
  ),
)
