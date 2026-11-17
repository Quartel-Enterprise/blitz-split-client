import { Direction } from 'radix-ui'
import { StrictMode, type ReactNode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from '@/app/App'
import { startAnalytics } from '@/analytics/firebaseAnalytics'
import { startCrashReporting } from '@/crash/sentry'
import { AppSkeleton } from '@/components/PageSkeletons'
import { OpenInAnotherTabScreen } from '@/app/OpenInAnotherTabScreen'
import { startShared } from '@/bridge/shared'
import { applyDocumentLocale, direction } from '@/i18n'
import { applyTheme, lastTheme } from '@/lib/theme'
import { listenToNotificationTaps, startWebPush } from '@/push/webPush'
import './index.css'

// Why: started first; what the page throws before Sentry's own script arrives still goes unreported.
void startCrashReporting()
applyDocumentLocale()
applyTheme(lastTheme())

// Why: Radix's menus, tabs, radio groups and toggles follow the page's direction (their arrow keys and sides).
const directed = (node: ReactNode) => <Direction.Provider dir={direction}>{node}</Direction.Provider>

const root = createRoot(document.getElementById('root')!)
// Why: opening the browser's database takes a moment; the page is never blank meanwhile.
root.render(<AppSkeleton />)
void startShared(() => root.render(directed(<OpenInAnotherTabScreen />))).then(() => {
  root.render(<StrictMode>{directed(<App />)}</StrictMode>)
  listenToNotificationTaps()
  void startWebPush()
  void startAnalytics()
})
