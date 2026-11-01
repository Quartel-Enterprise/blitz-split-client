import { AppPresenter, type JsRoute } from 'blitzsplit-shared'
import { useEffect, useRef } from 'react'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { Toaster } from '@/components/ui/sonner'
import { BalancesScreen } from '@/features/balances/BalancesScreen'
import { BillDetailSheet } from '@/features/bill-detail/BillDetailSheet'
import { CreateGroupDialog } from '@/features/create-group/CreateGroupDialog'
import { DeleteAccountDialog } from '@/features/delete-account/DeleteAccountDialog'
import { DefaultSplitDialog } from '@/features/edit-group/DefaultSplitDialog'
import { EditGroupDialog } from '@/features/edit-group/EditGroupDialog'
import { GlobalBalancesScreen } from '@/features/global-balances/GlobalBalancesScreen'
import { GroupDetailScreen } from '@/features/group-detail/GroupDetailScreen'
import { GroupStatsScreen } from '@/features/group-stats/GroupStatsScreen'
import { GroupsScreen } from '@/features/groups/GroupsScreen'
import { InviteDialog } from '@/features/invite/InviteDialog'
import { JoinInviteScreen } from '@/features/join/JoinInviteScreen'
import { JoinPickScreen } from '@/features/join/JoinPickScreen'
import { JoinWithLinkDialog } from '@/features/join/JoinWithLinkDialog'
import { MembersScreen } from '@/features/members/MembersScreen'
import { PaymentKeyDialog } from '@/features/payment-key/PaymentKeyDialog'
import { ProfileScreen } from '@/features/profile/ProfileScreen'
import { CategoryMapScreen } from '@/features/category-map/CategoryMapScreen'
import { ProfileSetupScreen } from '@/features/profile-setup/ProfileSetupScreen'
import { RegisterPaymentDialog } from '@/features/register-payment/RegisterPaymentDialog'
import { RestoreScreen } from '@/features/restore/RestoreScreen'
import { SplitBillDialog } from '@/features/split-bill/SplitBillDialog'
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
  // Why: "Mapa de categorias" from Categoria (9.0) covers Rachar conta, which stays mounted with what was typed.
  const isMapOverBill = backStack[backStack.length - 1].name === 'category-map' && backStack[backStack.length - 2]?.name === 'split-bill'
  const stack = isMapOverBill ? backStack.slice(0, -1) : backStack
  const top = stack[stack.length - 1]
  // Why: a payment key and the account deletion open as dialogs over the profile, and a new group, its invite and
  // the reader of an invite's link or QR over the home or the group's members, which stay mounted under them. A new
  // bill and the group's editor open over the group's expenses (a bill from the home over the home) the same way, and a payment over its balances,
  // expenses or members.
  const isPaymentKey = top.name === 'payment-key'
  const isDeleteAccount = top.name === 'delete-account'
  const isCreateGroup = top.name === 'create-group'
  const isInvite = top.name === 'invite'
  const isJoinWithLink = top.name === 'join-with-link'
  const isSplitBill = top.name === 'split-bill'
  const isEditGroup = top.name === 'edit-group'
  // Why: "Divisão padrão" takes Editar grupo's place, which stays mounted under it with what was typed.
  const isDefaultSplit = top.name === 'default-split'
  const editGroup = isEditGroup ? top : isDefaultSplit && stack[stack.length - 2]?.name === 'edit-group' ? stack[stack.length - 2] : undefined
  // Why: Acertar and Recebi of Saldos settle up across groups in the same dialog, over Saldos.
  const isRegisterPayment = top.name === 'register-payment' || top.name === 'settle-up'
  const isDialog =
    isCreateGroup || isInvite || isJoinWithLink || isSplitBill || isEditGroup || isDefaultSplit || isRegisterPayment || isPaymentKey || isDeleteAccount
  let pageIndex = stack.length - (isDialog ? 2 : 1) - (isDefaultSplit && editGroup ? 1 : 0)
  // Why: a bill opens as a panel over the group's expenses, which keep watching their group under it. Editar conta
  // opens over the panel, which stays mounted (closed) so the bill comes back as it was.
  const bill = stack[pageIndex]?.name === 'bill-detail' ? stack[pageIndex] : undefined
  if (bill) pageIndex -= 1
  const page = stack[pageIndex]
  const pageName = page?.name ?? (isPaymentKey || isDeleteAccount ? 'profile' : 'groups')
  return (
    <>
      <Screen name={pageName} route={page} />
      {bill && <BillDetailSheet key={`${bill.groupId}/${bill.expenseId}`} route={bill} isCovered={bill !== top} />}
      {isPaymentKey && <PaymentKeyDialog key={`${top.paymentMethod}:${top.paymentKeyId}`} route={top} />}
      {isDeleteAccount && <DeleteAccountDialog />}
      {isCreateGroup && <CreateGroupDialog />}
      {isInvite && <InviteDialog key={top.groupId} route={top} />}
      {isJoinWithLink && <JoinWithLinkDialog route={top} />}
      {isSplitBill && <SplitBillDialog key={`${top.groupId}/${top.expenseId ?? ''}`} route={top} isCovered={isMapOverBill} />}
      {isMapOverBill && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-background">
          <CategoryMapScreen />
        </div>
      )}
      {editGroup && <EditGroupDialog key={editGroup.groupId} route={editGroup} isCovered={isDefaultSplit} />}
      {isDefaultSplit && <DefaultSplitDialog key={top.groupId} route={top} />}
      {isRegisterPayment && (
        <RegisterPaymentDialog key={`${top.groupId ?? top.personKey}/${top.fromMemberId ?? top.currency}/${top.toMemberId ?? ''}`} route={top} />
      )}
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
    case 'group-detail':
      return route ? <GroupDetailScreen key={route.groupId} route={route} /> : null
    case 'group-stats':
      return route ? <GroupStatsScreen key={route.groupId} route={route} /> : null
    case 'balances':
      return route ? <BalancesScreen key={route.groupId} route={route} /> : null
    case 'global-balances':
      return route ? <GlobalBalancesScreen route={route} /> : null
    case 'join-invite':
      // Why: the group's link and each member's personal link of the same group are different invites.
      return route ? <JoinInviteScreen key={`${route.inviteCode}/${route.inviteMemberCode ?? ''}`} route={route} /> : null
    case 'join-pick':
      return route ? <JoinPickScreen key={route.inviteCode} route={route} /> : null
    // Why: the web edits name, photo and color inside the profile, so it never pushes the editor.
    case 'profile':
    case 'edit-profile':
      return <ProfileScreen />
    case 'category-map':
      return <CategoryMapScreen />
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
      // Why: one entry per screen pushed, so a bill opened over its group at once still goes back to the group.
      for (let next = size.current + 1; next <= backStack.length; next++) window.history.pushState({ size: next }, '')
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
