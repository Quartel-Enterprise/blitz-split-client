import { AppPresenter, type JsBillLinkNotice, type JsRoute } from 'blitzsplit-shared'
import { useEffect, useRef } from 'react'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { PageKeyContext } from '@/components/HomeShell'
import { HomeFrame, HomeSidebarProvider } from '@/components/HomeSidebar'
import { AppSkeleton } from '@/components/PageSkeletons'
import { AlertDialog, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { Toaster } from '@/components/ui/sonner'
import { ActivityScreen } from '@/features/activity/ActivityScreen'
import { BillDetailSheet } from '@/features/bill-detail/BillDetailSheet'
import { ShareBillDialog } from '@/features/bill-detail/ShareBillDialog'
import { ContactInviteDialog } from '@/features/contacts/ContactInviteDialog'
import { ContactsScreen } from '@/features/contacts/ContactsScreen'
import { CreateGroupDialog } from '@/features/create-group/CreateGroupDialog'
import { DeleteAccountDialog } from '@/features/delete-account/DeleteAccountDialog'
import { DefaultSplitDialog } from '@/features/edit-group/DefaultSplitDialog'
import { EditGroupDialog } from '@/features/edit-group/EditGroupDialog'
import { ChargeDialog } from '@/features/global-balances/ChargeDialog'
import { GlobalBalancesScreen } from '@/features/global-balances/GlobalBalancesScreen'
import { GroupDetailScreen } from '@/features/group-detail/GroupDetailScreen'
import { forgetClosedPickedTabs } from '@/features/group-detail/pickedTabs'
import { CategoryExpensesScreen } from '@/features/group-stats/CategoryExpensesScreen'
import { GroupStatsScreen } from '@/features/group-stats/GroupStatsScreen'
import { GroupsScreen } from '@/features/groups/GroupsScreen'
import { SearchRouteDialog } from '@/features/groups/SearchDialog'
import { HomeScreen } from '@/features/home/HomeScreen'
import { InviteDialog } from '@/features/invite/InviteDialog'
import { AppDownloadDialog } from '@/features/app-download/AppDownload'
import { JoinInviteScreen } from '@/features/join/JoinInviteScreen'
import { JoinPickScreen } from '@/features/join/JoinPickScreen'
import { JoinWithLinkDialog } from '@/features/join/JoinWithLinkDialog'
import { PaymentKeyDialog } from '@/features/payment-key/PaymentKeyDialog'
import { GroupPhotoDialog } from '@/features/group-detail/GroupPhotoDialog'
import { ProfilePhotoDialog } from '@/features/profile/ProfilePhotoDialog'
import { ProfileScreen } from '@/features/profile/ProfileScreen'
import { CategoryMapScreen } from '@/features/category-map/CategoryMapScreen'
import { NotificationSettingsDialog } from '@/features/notification-settings/NotificationSettingsCard'
import { ProfileSetupScreen } from '@/features/profile-setup/ProfileSetupScreen'
import { PickPaymentDialog } from '@/features/register-payment/PickPaymentDialog'
import { RegisterPaymentDialog } from '@/features/register-payment/RegisterPaymentDialog'
import { RestoreScreen } from '@/features/restore/RestoreScreen'
import { SharedBillScreen } from '@/features/shared-bill/SharedBillScreen'
import { SimplifyDebtsInfoDialog } from '@/features/simplify-debts/SimplifyDebtsInfoDialog'
import { SplitBillDialog } from '@/features/split-bill/SplitBillDialog'
import { WelcomeScreen } from '@/features/welcome/WelcomeScreen'
import { t } from '@/i18n'
import { isBrowserBackBlocked } from '@/lib/browserBack'
import { setShowsBitcoinInSats } from '@/lib/money'
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
  const billLinkNotice = useWatch(app, (presenter, onChange: (notice: JsBillLinkNotice) => void) => presenter.observeBillLinkNotice(onChange))?.notice
  const showsBitcoinInSats = useWatch(app, (presenter, onChange: (isOn: boolean) => void) => presenter.observeBitcoinInSats(onChange))
  // Why: set before the screens under it draw, so this render's amounts already follow it; a change redraws them all.
  setShowsBitcoinInSats(showsBitcoinInSats ?? false)
  // The theme is the database's once the app is ready; until then the last visit's stays painted.
  useEffect(() => {
    if (isReady && theme) applyTheme(theme)
  }, [isReady, theme])
  useBrowserHistory(app, backStack)
  useEffect(() => {
    if (backStack) forgetClosedPickedTabs(backStack)
  }, [backStack])

  if (!isReady || !backStack) return <AppSkeleton />
  return (
    <>
      <HomeSidebarProvider isActive={backStack.some((route) => FRAME_PAGES.has(route.name))}>
        <StackLayers backStack={backStack} />
      </HomeSidebarProvider>
      {app && billLinkNotice && <BillLinkNoticeDialog notice={billLinkNotice} onDismiss={() => app.dismissBillLinkNotice()} />}
      <Toaster position="top-center" theme={theme === 'light' || theme === 'dark' ? theme : 'system'} />
    </>
  )
}

/**
 * Why a comment's link opened nothing, over wherever the app is: "Você não está neste grupo" ("not-in-group") or "Sem
 * internet" ("offline"). "Ir para o início", or closing it any way, goes back to Início.
 */
function BillLinkNoticeDialog({ notice, onDismiss }: { notice: string; onDismiss: () => void }) {
  const isOffline = notice === 'offline'
  return (
    <AlertDialog
      open
      onOpenChange={(open) => {
        if (!open) onDismiss()
      }}
    >
      <AlertDialogContent className="sm:max-w-md">
        <AlertDialogHeader>
          <AlertDialogTitle>{t(isOffline ? 'billLinkNotice_offlineTitle' : 'billLinkNotice_notInGroupTitle')}</AlertDialogTitle>
          <AlertDialogDescription>{t(isOffline ? 'billLinkNotice_offlineBody' : 'billLinkNotice_notInGroupBody')}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <Button onClick={onDismiss}>{t('billLinkNotice_goHome')}</Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}

// Why: the pages that draw inside the home's frame (`HomeFrame`): the four tabs and the pages they open.
const FRAME_PAGES = new Set([
  'home',
  'groups',
  'global-balances',
  'contacts',
  'activity',
  'group-detail',
  'group-stats',
  'category-expenses',
  'contact-profile',
  'profile',
  'edit-profile',
])

/**
 * Draws a back stack: its page, with the panel and the dialog over it. "Mapa de categorias" from Categoria (9.0)
 * covers Rachar conta, and so does the bill's group opened from its "Grupo" (beta feedback) and whatever either
 * opens (Abrir Perfil, a bill of the group): those draw as a stack of their own over the bill, which stays mounted
 * with what was typed while it is anywhere under them.
 */
function StackLayers({ backStack, offset = 0 }: { backStack: JsRoute[]; offset?: number }) {
  const mapIndex = backStack.findIndex(
    (route, index) => (route.name === 'category-map' || route.name === 'group-detail') && backStack[index - 1]?.name === 'split-bill',
  )
  const isMapOverBill = mapIndex > 0
  const stack = isMapOverBill ? backStack.slice(0, mapIndex) : backStack
  const top = stack[stack.length - 1]
  // Why: a payment key and the account deletion open as dialogs over the profile, and a new group, its invite and
  // the reader of an invite's link or QR over the home or the group's members, which stay mounted under them. A new
  // bill and the group's editor open over the group's expenses (a bill from the home over the home) the same way, and a payment over its balances,
  // expenses or members.
  const isPaymentKey = top.name === 'payment-key'
  const isDeleteAccount = top.name === 'delete-account'
  // Why: the profile's photo big, over the profile, whose avatar opened it.
  const isProfilePhoto = top.name === 'profile-photo'
  // Why: the group's photo big, over the group, whose avatar opened it.
  const isGroupPhoto = top.name === 'group-photo'
  const isCreateGroup = top.name === 'create-group'
  const isInvite = top.name === 'invite'
  // Why: Contatos' "Convidar com link ou QR" (and a guest's "Convidar Diego") opens over Contatos or the profile.
  const isContactInvite = top.name === 'contact-invite'
  const isJoinWithLink = top.name === 'join-with-link'
  // Why: "Buscar" (⌘K) over Início, Saldos or Contatos, which stay mounted under it.
  const isSearch = top.name === 'search'
  const isSplitBill = top.name === 'split-bill'
  // Why: "Compartilhar conta" opens over Conta, which stays open under it.
  const isShareBill = top.name === 'share-bill'
  const isEditGroup = top.name === 'edit-group'
  // Why: "Divisão padrão" takes Editar grupo's place, which stays mounted under it with what was typed.
  const isDefaultSplit = top.name === 'default-split'
  // Why: "Como funciona" of "Simplificar dívidas" takes Editar grupo's place the same way, or opens over the group's Saldos.
  const isSimplifyDebtsInfo = top.name === 'simplify-debts-info'
  const isOverEditGroup = (isDefaultSplit || isSimplifyDebtsInfo) && stack[stack.length - 2]?.name === 'edit-group'
  const editGroup = isEditGroup ? top : isOverEditGroup ? stack[stack.length - 2] : undefined
  // Why: Cobrar over the page that opened it, Saldos or a contact's profile (in Contatos' pane too).
  const isCharge = top.name === 'charge'
  // Why: Acertar and Recebi of Saldos settle up across groups in the same dialog, over Saldos.
  const isRegisterPayment = top.name === 'register-payment' || top.name === 'settle-up'
  // Why: Início's "Registrar pagamento" asks who paid whom first, over Início, then gives Registrar pagamento its place.
  const isPickPayment = top.name === 'pick-payment'
  // Why: Atividade's "Notification settings" over Atividade; the profile shows the same settings inline.
  const isNotificationSettings = top.name === 'notification-settings'
  // Why: "Blitz Split on your phone" (14.2) over Início or Perfil, whichever opened it.
  const isAppDownload = top.name === 'app-download'
  const isDialog =
    isNotificationSettings ||
    isAppDownload ||
    isCreateGroup || isInvite || isContactInvite || isJoinWithLink || isSearch || isSplitBill || isShareBill || isEditGroup || isDefaultSplit || isSimplifyDebtsInfo || isRegisterPayment || isCharge || isPickPayment || isPaymentKey || isDeleteAccount || isProfilePhoto || isGroupPhoto
  let pageIndex = stack.length - (isDialog ? 2 : 1) - (isOverEditGroup ? 1 : 0)
  // Why: a bill opens as a panel over the group's expenses, which keep watching their group under it. Editar conta
  // opens over the panel, which stays mounted (closed) so the bill comes back as it was.
  const bill = stack[pageIndex]?.name === 'bill-detail' ? stack[pageIndex] : undefined
  if (bill) pageIndex -= 1
  const page = stack[pageIndex]
  // Why: a person's profile (or Perfil) opened from Conta takes the page, and the panel stays mounted (closed) under
  // it, as under Editar conta, so back finds the bill with what was typed.
  const isProfilePage = page?.name === 'contact-profile' || page?.name === 'profile'
  const billUnderPage = !bill && isProfilePage && stack[pageIndex - 1]?.name === 'bill-detail' ? stack[pageIndex - 1] : undefined
  const panelBill = bill ?? billUnderPage
  // Why: a person's profile opened from a payment's face takes the page too, and the payment stays mounted (closed)
  // under it, so back finds the form with what was typed.
  const paymentUnderPage =
    isProfilePage && (stack[pageIndex - 1]?.name === 'register-payment' || stack[pageIndex - 1]?.name === 'settle-up')
      ? stack[pageIndex - 1]
      : undefined
  const payment = isRegisterPayment ? top : paymentUnderPage
  const pageName = page?.name ?? (isPaymentKey || isDeleteAccount || isProfilePhoto ? 'profile' : 'home')
  // Why: another tab or group in the same place of the stack is a new page, which fades in (HomeShell). The place
  // counts from the whole stack, so a page over the map never shares its key with one under it.
  const pageKey = `${offset + pageIndex}/${pageName}/${page?.groupId ?? ''}/${page?.groupTab ?? ''}`
  return (
    <>
      <PageKeyContext.Provider value={pageKey}>
        {/* Why: one frame while its pages follow each other here, so a tab or a group swaps the content alone. */}
        {FRAME_PAGES.has(pageName) ? (
          <HomeFrame>
            <Screen name={pageName} route={page} />
          </HomeFrame>
        ) : (
          <Screen name={pageName} route={page} />
        )}
      </PageKeyContext.Provider>
      {panelBill && (
        <BillDetailSheet key={`bill:${panelBill.groupId}/${panelBill.expenseId}`} route={panelBill} isCovered={panelBill !== top && !isShareBill} />
      )}
      {isShareBill && <ShareBillDialog key={`${top.groupId}/${top.expenseId}`} route={top} />}
      {isPaymentKey && <PaymentKeyDialog key={`${top.paymentMethod}:${top.paymentKeyId}`} route={top} />}
      {isDeleteAccount && <DeleteAccountDialog />}
      {isNotificationSettings && <NotificationSettingsDialog />}
      {isAppDownload && <AppDownloadDialog route={top} />}
      {isProfilePhoto && <ProfilePhotoDialog />}
      {isGroupPhoto && <GroupPhotoDialog key={top.groupId} route={top} />}
      {isCreateGroup && <CreateGroupDialog />}
      {isInvite && <InviteDialog key={top.groupId} route={top} />}
      {isContactInvite && <ContactInviteDialog key={top.personKey ?? ''} route={top} />}
      {isJoinWithLink && <JoinWithLinkDialog route={top} />}
      {isSearch && <SearchRouteDialog />}
      {isSplitBill && <SplitBillDialog key={`${top.groupId}/${top.expenseId ?? ''}`} route={top} isCovered={isMapOverBill} />}
      {isMapOverBill && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-background">
          <StackLayers backStack={backStack.slice(mapIndex)} offset={offset + mapIndex} />
        </div>
      )}
      {editGroup && <EditGroupDialog key={editGroup.groupId} route={editGroup} isCovered={isOverEditGroup} />}
      {isDefaultSplit && <DefaultSplitDialog key={top.groupId} route={top} />}
      {isSimplifyDebtsInfo && <SimplifyDebtsInfoDialog key={top.currency ?? ''} route={top} />}
      {isPickPayment && <PickPaymentDialog />}
      {isCharge && <ChargeDialog key={`${top.personKey}/${top.currency}`} route={top} />}
      {payment && (
        <RegisterPaymentDialog
          key={`${payment.groupId ?? payment.personKey}/${payment.fromMemberId ?? payment.currency}/${payment.toMemberId ?? ''}`}
          route={payment}
          isCovered={payment !== top}
        />
      )}
    </>
  )
}

function Screen({ name, route }: { name: string; route: JsRoute | undefined }) {
  switch (name) {
    case 'profile-setup':
      return <ProfileSetupScreen />
    case 'restore':
      return <RestoreScreen />
    case 'home':
      return <HomeScreen />
    case 'groups':
      return <GroupsScreen />
    // Why: a route opening the same group on another tab (Convidar's "Ver quem já entrou") is a new page.
    case 'group-detail':
      return route ? <GroupDetailScreen key={`${route.groupId}/${route.groupTab}`} route={route} /> : null
    case 'group-stats':
      return route ? <GroupStatsScreen key={route.groupId} route={route} /> : null
    case 'category-expenses':
      return route ? <CategoryExpensesScreen key={route.groupId} route={route} /> : null
    case 'global-balances':
      return route ? <GlobalBalancesScreen route={route} /> : null
    // Why: a push notification replaces a contact's Atividade with everyone's, which is a new page.
    case 'activity':
      return route ? <ActivityScreen key={route.personKey ?? ''} route={route} /> : null
    // Why: on a tablet and a phone the profile takes the list's place inside Contatos, which stays mounted under it;
    // one opened from Saldos draws the same page.
    case 'contacts':
      return <ContactsScreen />
    case 'contact-profile':
      return <ContactsScreen profileRoute={route} />
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
    // Why: a bill's link (7.3h), a page of its own outside the home's frame.
    case 'shared-bill':
      return route ? <SharedBillScreen key={route.sharedBillCode} route={route} /> : null
    default:
      return <WelcomeScreen />
  }
}

/*
 * Each push of the shared back stack becomes a browser history entry; the back button pops it. A screen the app
 * closes itself (a dialog's ✕, a tab of the sidebar, a deletion) goes back in the history too, so the entries above
 * the stack never linger as back presses that do nothing.
 */
function useBrowserHistory(app: AppPresenter | null, backStack: JsRoute[] | null) {
  // The size of the stack the current history entry stands for.
  const size = useRef(0)
  // Why: the first entry of this page; going back below it would leave the app, so the stack replaces it instead.
  const floor = useRef(0)
  // Why: a history.go of ours under way; the popstate it ends with catches the history up with the stack.
  const isGoing = useRef(false)
  const latestSize = useRef(0)
  latestSize.current = backStack?.length ?? 0
  const sync = useRef((length: number) => {
    if (size.current === 0) {
      window.history.replaceState({ size: length }, '')
      size.current = length
      floor.current = length
    } else if (length > size.current) {
      // Why: one entry per screen pushed, so a bill opened over its group at once still goes back to the group.
      for (let next = size.current + 1; next <= length; next++) window.history.pushState({ size: next }, '')
      size.current = length
    } else if (length < size.current && size.current > floor.current) {
      const landing = Math.max(length, floor.current)
      goBy.current(landing - size.current, size.current)
      size.current = landing
    } else {
      window.history.replaceState({ size: length }, '')
      size.current = length
      floor.current = Math.min(floor.current, length)
    }
  })
  /*
   * Why: a traversal the browser drops (another one under way) ends with no popstate. A moment later, if the history
   * still stands on the entry it left from, it catches up from there; a traversal that is only slow has moved off it
   * by then, and its popstate still ends it.
   */
  const goings = useRef(0)
  const goBy = useRef((delta: number, from: number) => {
    isGoing.current = true
    const going = ++goings.current
    window.history.go(delta)
    window.setTimeout(() => {
      if (!isGoing.current || going !== goings.current) return
      if ((window.history.state as { size?: number } | null)?.size !== from) return
      isGoing.current = false
      size.current = from
      sync.current(latestSize.current)
    }, 1000)
  })
  useEffect(() => {
    if (!app || !backStack) return
    if (!isGoing.current) sync.current(backStack.length)
  }, [app, backStack])
  useEffect(() => {
    if (!app) return
    const onPopState = (event: PopStateEvent) => {
      if (isGoing.current) {
        isGoing.current = false
        sync.current(latestSize.current)
        return
      }
      const target = (event.state as { size?: number } | null)?.size ?? 1
      // Why: a deletion under way waits for the welcome, as the screen's own Back does, so the entry comes back.
      if (isBrowserBackBlocked()) {
        window.history.pushState({ size: size.current }, '')
        return
      }
      if (target < size.current) {
        size.current = target
        app.popTo(target)
      } else if (target > size.current) {
        // Why: the forward button names screens the stack no longer has.
        goBy.current(size.current - target, target)
      }
    }
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [app])
}
