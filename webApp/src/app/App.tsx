import { AppPresenter } from 'blitzsplit-shared'
import { useEffect, useRef } from 'react'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { Toaster } from '@/components/ui/sonner'
import { GroupsScreen } from '@/features/groups/GroupsScreen'
import { ProfileScreen } from '@/features/profile/ProfileScreen'
import { ProfileSetupScreen } from '@/features/profile-setup/ProfileSetupScreen'
import { RestoreScreen } from '@/features/restore/RestoreScreen'
import { WelcomeScreen } from '@/features/welcome/WelcomeScreen'
import { applyTheme } from '@/lib/theme'

/**
 * The back stack lives in Kotlin and the ViewModels move it; React renders its last entry and keeps
 * the browser history in step, so the browser's back button walks the same flow.
 */
export function App() {
  const app = usePresenter(() => new AppPresenter())
  const isReady = useWatch(app, (presenter, onChange: (ready: boolean) => void) => presenter.observeIsReady(onChange))
  const backStack = useWatch(app, (presenter, onChange: (routes: string[]) => void) => presenter.observeBackStack(onChange))
  const theme = useWatch(app, (presenter, onChange: (theme: string) => void) => presenter.observeTheme(onChange))
  useEffect(() => applyTheme(theme ?? 'system'), [theme])
  useBrowserHistory(app, backStack)

  if (!isReady || !backStack) return null
  return (
    <>
      <Screen route={backStack[backStack.length - 1]} />
      <Toaster position="top-center" />
    </>
  )
}

function Screen({ route }: { route: string }) {
  switch (route) {
    case 'profile-setup':
      return <ProfileSetupScreen />
    case 'restore':
      return <RestoreScreen />
    case 'groups':
      return <GroupsScreen />
    // Why: the web edits name, photo and color inside the profile, so it never pushes the editor.
    case 'profile':
    case 'edit-profile':
      return <ProfileScreen />
    default:
      return <WelcomeScreen />
  }
}

// Each push of the shared back stack becomes a browser history entry; the back button pops it.
function useBrowserHistory(app: AppPresenter | null, backStack: string[] | null) {
  const size = useRef(0)
  useEffect(() => {
    if (!app || !backStack) return
    if (backStack.length > size.current && size.current > 0) {
      window.history.pushState({ size: backStack.length }, '')
    } else {
      window.history.replaceState({ size: backStack.length }, '')
    }
    size.current = backStack.length
  }, [app, backStack])
  useEffect(() => {
    if (!app) return
    const onPopState = (event: PopStateEvent) => {
      const target = (event.state as { size?: number } | null)?.size ?? 1
      if (target < size.current) app.popTo(target)
    }
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [app])
}
