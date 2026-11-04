import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from '@/app/App'
import { startAnalytics } from '@/analytics/firebaseAnalytics'
import { AppSkeleton } from '@/components/PageSkeletons'
import { OpenInAnotherTabScreen } from '@/app/OpenInAnotherTabScreen'
import { startShared } from '@/bridge/shared'
import { applyDocumentLocale } from '@/i18n'
import { applyTheme, lastTheme } from '@/lib/theme'
import { listenToNotificationTaps, startWebPush } from '@/push/webPush'
import './index.css'

applyDocumentLocale()
applyTheme(lastTheme())

const root = createRoot(document.getElementById('root')!)
// Why: opening the browser's database takes a moment; the page is never blank meanwhile.
root.render(<AppSkeleton />)
void startShared(() => root.render(<OpenInAnotherTabScreen />)).then(() => {
  root.render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
  listenToNotificationTaps()
  void startWebPush()
  void startAnalytics()
})
