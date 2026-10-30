import { AppPresenter, type JsRoute } from 'blitzsplit-shared'
import { useEffect, useRef } from 'react'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { Toaster } from '@/components/ui/sonner'
import { CreateGroupDialog } from '@/features/create-group/CreateGroupDialog'
import { DeleteAccountDialog } from '@/features/delete-account/DeleteAccountDialog'
import { GroupsScreen } from '@/features/groups/GroupsScreen'
import { InviteDialog } from '@/features/invite/InviteDialog'
import { JoinInviteScreen } from '@/features/join/JoinInviteScreen'
import { JoinPickScreen } from '@/features/join/JoinPickScreen'
import { MembersScreen } from '@/features/members/MembersScreen'
import { PaymentKeyDialog } from '@/features/payment-key/PaymentKeyDialog'
import { ProfileScreen } from '@/features/profile/ProfileScreen'
import { ProfileSetupScreen } from '@/features/profile-setup/ProfileSetupScreen'
import { RestoreScreen } from '@/features/restore/RestoreScreen'
import { WelcomeScreen } from '@/features/welcome/WelcomeScreen'
import { isBrowserBackBlocked } from '@/lib/browserBack'
import { applyTheme } from '@/lib/theme'

/**
 * The back stack lives in Kotlin and the ViewModels move it; React renders its last entry and keeps
 * the browser history in step, so the browser's back button walks the same flow.
 */
export function App() {
  const app = usePresenter(() => new AppPresenter())
  const isReady = useWatch(app, (presenter, onChange: (ready: boolean) => void) => presenter.observeIsReady(onChange))
  const backStack = useWatch(app, (presenter, onChange: (routes: JsRoute[]) => void) => presenter.observeBackStack(onChange))
  const theme = useWatch(app, (presenter, onChange: (theme: string) => void) => presenter.observeTheme(onChange))
  // The theme is the database's once the app is ready; until then the last visit's stays painted.
  useEffect(() => {
    if (isReady && theme) applyTheme(theme)
  }, [isReady, theme])
  useBrowserHistory(app, backStack)

  if (!isReady || !backStack) return null
  const top = backStack[backStack.length - 1]
  // Why: a payment key and the account deletion open as dialogs over the profile, and a new group and its invite
  // over the home or the group's members, which stay mounted under them.
  const isPaymentKey = top.name === 'payment-key'
  const isDeleteAccount = top.name === 'delete-account'
  const isCreateGroup = top.name === 'create-group'
  const isInvite = top.name === 'invite'
  const isDialog = isCreateGroup || isInvite || isPaymentKey || isDeleteAccount
  const page = isDialog ? backStack.at(-2) : top
  const pageName = page?.name ?? (isPaymentKey || isDeleteAccount ? 'profile' : 'groups')
  return (
    <>
      <Screen name={pageName} route={page} />
      {isPaymentKey && <PaymentKeyDialog key={`${top.paymentMethod}:${top.paymentKeyId}`} route={top} />}
      {isDeleteAccount && <DeleteAccountDialog />}
      {isCreateGroup && <CreateGroupDialog />}
      {isInvite && <InviteDialog key={top.groupId} route={top} />}
      <Toaster position="top-center" theme={theme === 'light' || theme === 'dark' ? theme : 'system'} />
    </>
  )
}

function Screen({ name, route }: { name: string; route: JsRoute | undefined }) {
  switch (name) {
    case 'profile-setup':
      return <ProfileSetupScreen />
    case 'restore':
      return <RestoreScreen />
    case 'groups':
      return <GroupsScreen />
    case 'members':
      return route ? <MembersScreen key={route.groupId} route={route} /> : null
    case 'join-invite':
      // Why: the group's link and each member's personal link of the same group are different invites.
      return route ? <JoinInviteScreen key={`${route.inviteCode}/${route.inviteMemberCode ?? ''}`} route={route} /> : null
    case 'join-pick':
      return route ? <JoinPickScreen key={route.inviteCode} route={route} /> : null
    // Why: the web edits name, photo and color inside the profile, so it never pushes the editor.
    case 'profile':
    case 'edit-profile':
      return <ProfileScreen />
    default:
      return <WelcomeScreen />
  }
}

// Each push of the shared back stack becomes a browser history entry; the back button pops it.
function useBrowserHistory(app: AppPresenter | null, backStack: JsRoute[] | null) {
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
      // Why: a deletion under way waits for the welcome, as the screen's own Back does, so the entry comes back.
      if (isBrowserBackBlocked()) {
        window.history.pushState({ size: size.current }, '')
        return
      }
      if (target < size.current) app.popTo(target)
    }
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [app])
}
