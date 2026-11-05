import { AppPresenter, type JsBillLinkNotice, type JsRoute } from 'blitzsplit-shared'
import { useEffect, useRef } from 'react'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { PageKeyContext } from '@/components/HomeShell'
import { HomeSidebarProvider } from '@/components/HomeSidebar'
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
import { GlobalBalancesScreen } from '@/features/global-balances/GlobalBalancesScreen'
import { GroupDetailScreen } from '@/features/group-detail/GroupDetailScreen'
import { GroupStatsScreen } from '@/features/group-stats/GroupStatsScreen'
import { GroupsScreen } from '@/features/groups/GroupsScreen'
import { SearchRouteDialog } from '@/features/groups/SearchDialog'
import { HomeScreen } from '@/features/home/HomeScreen'
import { InviteDialog } from '@/features/invite/InviteDialog'
import { JoinInviteScreen } from '@/features/join/JoinInviteScreen'
import { JoinPickScreen } from '@/features/join/JoinPickScreen'
import { JoinWithLinkDialog } from '@/features/join/JoinWithLinkDialog'
import { PaymentKeyDialog } from '@/features/payment-key/PaymentKeyDialog'
import { ProfilePhotoDialog } from '@/features/profile/ProfilePhotoDialog'
import { ProfileScreen } from '@/features/profile/ProfileScreen'
import { CategoryMapScreen } from '@/features/category-map/CategoryMapScreen'
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

  if (!isReady || !backStack) return <AppSkeleton />
  return (
    <>
      <HomeSidebarProvider isActive={backStack.some((route) => SHELL_PAGES.has(route.name))}>
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

// Why: the pages that draw inside the home's frame (`PageShell`), which read the sidebar.
const SHELL_PAGES = new Set(['activity', 'group-detail', 'group-stats', 'contact-profile', 'profile', 'edit-profile'])

/**
 * Draws a back stack: its page, with the panel and the dialog over it. "Mapa de categorias" from Categoria (9.0)
 * covers Rachar conta, and so does whatever the map opens (Abrir Perfil): those draw as a stack of their own over
 * the bill, which stays mounted with what was typed while it is anywhere under them.
 */
function StackLayers({ backStack, offset = 0 }: { backStack: JsRoute[]; offset?: number }) {
  const mapIndex = backStack.findIndex((route, index) => route.name === 'category-map' && backStack[index - 1]?.name === 'split-bill')
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
  // Why: Acertar and Recebi of Saldos settle up across groups in the same dialog, over Saldos.
  const isRegisterPayment = top.name === 'register-payment' || top.name === 'settle-up'
  // Why: Início's "Registrar pagamento" asks who paid whom first, over Início, then gives Registrar pagamento its place.
  const isPickPayment = top.name === 'pick-payment'
  const isDialog =
    isCreateGroup || isInvite || isContactInvite || isJoinWithLink || isSearch || isSplitBill || isShareBill || isEditGroup || isDefaultSplit || isSimplifyDebtsInfo || isRegisterPayment || isPickPayment || isPaymentKey || isDeleteAccount || isProfilePhoto
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
        <Screen name={pageName} route={page} />
      </PageKeyContext.Provider>
      {panelBill && (
        <BillDetailSheet key={`bill:${panelBill.groupId}/${panelBill.expenseId}`} route={panelBill} isCovered={panelBill !== top && !isShareBill} />
      )}
      {isShareBill && <ShareBillDialog key={`${top.groupId}/${top.expenseId}`} route={top} />}
      {isPaymentKey && <PaymentKeyDialog key={`${top.paymentMethod}:${top.paymentKeyId}`} route={top} />}
      {isDeleteAccount && <DeleteAccountDialog />}
      {isProfilePhoto && <ProfilePhotoDialog />}
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
