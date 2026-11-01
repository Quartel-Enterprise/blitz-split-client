package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.account.domain.usecase.HandleAppPaused
import com.quare.blitzsplit.core.account.domain.usecase.HandleAppResumed
import com.quare.blitzsplit.core.account.domain.usecase.HandleBrowserCallback
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.activity.presentation.viewmodel.ActivityViewModel
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.AgendaViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactInviteViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactProfileViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactsViewModel
import com.quare.blitzsplit.feature.creategroup.presentation.viewmodel.CreateGroupViewModel
import com.quare.blitzsplit.feature.deleteaccount.presentation.viewmodel.DeleteAccountViewModel
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.DefaultSplitViewModel
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.EditGroupViewModel
import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.EditProfileViewModel
import com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel.GlobalBalancesViewModel
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.BalancesViewModel
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.HomeViewModel
import com.quare.blitzsplit.feature.groupstats.presentation.viewmodel.GroupStatsViewModel
import com.quare.blitzsplit.feature.invite.presentation.viewmodel.InviteViewModel
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinInviteViewModel
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinPickViewModel
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinWithLinkViewModel
import com.quare.blitzsplit.feature.members.presentation.viewmodel.MembersViewModel
import com.quare.blitzsplit.feature.notificationsettings.presentation.viewmodel.NotificationSettingsViewModel
import com.quare.blitzsplit.feature.paymentkey.presentation.viewmodel.PaymentKeyViewModel
import com.quare.blitzsplit.feature.profile.presentation.viewmodel.ProfileViewModel
import com.quare.blitzsplit.feature.profilesetup.presentation.viewmodel.ProfileSetupViewModel
import com.quare.blitzsplit.feature.recoverycode.presentation.viewmodel.RecoveryCodeViewModel
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.PickPaymentViewModel
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.RegisterPaymentViewModel
import com.quare.blitzsplit.feature.restore.presentation.viewmodel.RestoreViewModel
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import com.quare.blitzsplit.feature.welcome.presentation.viewmodel.WelcomeViewModel
import com.quare.blitzsplit.shared.app.AppViewModel
import com.quare.blitzsplit.shared.app.LinkInbox
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.posix.memcpy

/*
 * Why: the single entry point SwiftUI calls. Swift can't call reified Koin functions, so each
 * screen's ViewModel comes from a plain function here, wrapped in the holder that clears it.
 */
object BlitzSplitApple : KoinComponent {
    /*
     * Why: the main thread, since a second browser trip opens Safari through UIApplication, and at
     * once, so the callback closes the browser trip before handleAppResumed runs.
     */
    private val scope = MainScope() + Dispatchers.Main.immediate

    fun start() {
        startBlitzSplitKoin()
    }

    fun createAppViewModel(): ViewModelHolder<AppViewModel> = ViewModelHolder(AppViewModel::class) { get() }

    fun createWelcomeViewModel(): ViewModelHolder<WelcomeViewModel> = ViewModelHolder(WelcomeViewModel::class) { get() }

    fun createProfileSetupViewModel(): ViewModelHolder<ProfileSetupViewModel> =
        ViewModelHolder(ProfileSetupViewModel::class) { get() }

    fun createRestoreViewModel(): ViewModelHolder<RestoreViewModel> = ViewModelHolder(RestoreViewModel::class) { get() }

    fun createRecoveryCodeViewModel(): ViewModelHolder<RecoveryCodeViewModel> =
        ViewModelHolder(RecoveryCodeViewModel::class) { get() }

    // Why: Início, the home's first tab and the root of the back stack.
    fun createHomeViewModel(): ViewModelHolder<HomeViewModel> = ViewModelHolder(HomeViewModel::class) { get() }

    fun createGroupsViewModel(): ViewModelHolder<GroupsViewModel> = ViewModelHolder(GroupsViewModel::class) { get() }

    fun createCreateGroupViewModel(): ViewModelHolder<CreateGroupViewModel> =
        ViewModelHolder(CreateGroupViewModel::class) { get() }

    fun createProfileViewModel(): ViewModelHolder<ProfileViewModel> = ViewModelHolder(ProfileViewModel::class) { get() }

    fun createEditProfileViewModel(): ViewModelHolder<EditProfileViewModel> =
        ViewModelHolder(EditProfileViewModel::class) { get() }

    fun createDeleteAccountViewModel(): ViewModelHolder<DeleteAccountViewModel> =
        ViewModelHolder(DeleteAccountViewModel::class) { get() }

    fun createNotificationSettingsViewModel(): ViewModelHolder<NotificationSettingsViewModel> =
        ViewModelHolder(NotificationSettingsViewModel::class) { get() }

    // Why: each group opens its own ViewModel, which reads the group from its route.
    fun createInviteViewModel(route: Route.Invite): ViewModelHolder<InviteViewModel> =
        ViewModelHolder(InviteViewModel::class) { get { parametersOf(route) } }

    fun createMembersViewModel(route: Route.Members): ViewModelHolder<MembersViewModel> =
        ViewModelHolder(MembersViewModel::class) { get { parametersOf(route) } }

    fun createGroupDetailViewModel(route: Route.GroupDetail): ViewModelHolder<GroupDetailViewModel> =
        ViewModelHolder(GroupDetailViewModel::class) { get { parametersOf(route) } }

    fun createBalancesViewModel(route: Route.Balances): ViewModelHolder<BalancesViewModel> =
        ViewModelHolder(BalancesViewModel::class) { get { parametersOf(route) } }

    fun createRegisterPaymentViewModel(route: Route.RegisterPayment): ViewModelHolder<RegisterPaymentViewModel> =
        ViewModelHolder(RegisterPaymentViewModel::class) { get { parametersOf(route) } }

    // Why: Início's "Registrar pagamento" asks "Quem pagou a quem?" first, across every group.
    fun createPickPaymentViewModel(): ViewModelHolder<PickPaymentViewModel> =
        ViewModelHolder(PickPaymentViewModel::class) { get() }

    // Why: "Acertar" or "Recebi" of the global Saldos open Registrar pagamento across the person's groups.
    fun createSettleUpViewModel(route: Route.SettleUp): ViewModelHolder<RegisterPaymentViewModel> =
        ViewModelHolder(RegisterPaymentViewModel::class) { get { parametersOf(route) } }

    fun createGlobalBalancesViewModel(route: Route.GlobalBalances): ViewModelHolder<GlobalBalancesViewModel> =
        ViewModelHolder(GlobalBalancesViewModel::class) { get { parametersOf(route) } }

    // Why: the home's Atividade tab, on one person when a contact's history opens it.
    fun createActivityViewModel(route: Route.Activity): ViewModelHolder<ActivityViewModel> =
        ViewModelHolder(ActivityViewModel::class) { get { parametersOf(route) } }

    fun createContactsViewModel(): ViewModelHolder<ContactsViewModel> =
        ViewModelHolder(ContactsViewModel::class) { get() }

    // Why: each person opens their own profile, which reads them from its route (beside Contatos too).
    fun createContactProfileViewModel(route: Route.ContactProfile): ViewModelHolder<ContactProfileViewModel> =
        ViewModelHolder(ContactProfileViewModel::class) { get { parametersOf(route) } }

    fun createAgendaViewModel(): ViewModelHolder<AgendaViewModel> = ViewModelHolder(AgendaViewModel::class) { get() }

    // Why: the invite reads whom it is for (an agenda's name, a guest's personal link) from its route.
    fun createContactInviteViewModel(route: Route.ContactInvite): ViewModelHolder<ContactInviteViewModel> =
        ViewModelHolder(ContactInviteViewModel::class) { get { parametersOf(route) } }

    fun createSplitBillViewModel(route: Route.SplitBill): ViewModelHolder<SplitBillViewModel> =
        ViewModelHolder(SplitBillViewModel::class) { get { parametersOf(route) } }

    fun createBillDetailViewModel(route: Route.BillDetail): ViewModelHolder<BillDetailViewModel> =
        ViewModelHolder(BillDetailViewModel::class) { get { parametersOf(route) } }

    fun createGroupStatsViewModel(route: Route.GroupStats): ViewModelHolder<GroupStatsViewModel> =
        ViewModelHolder(GroupStatsViewModel::class) { get { parametersOf(route) } }

    fun createEditGroupViewModel(route: Route.EditGroup): ViewModelHolder<EditGroupViewModel> =
        ViewModelHolder(EditGroupViewModel::class) { get { parametersOf(route) } }

    fun createDefaultSplitViewModel(route: Route.DefaultSplit): ViewModelHolder<DefaultSplitViewModel> =
        ViewModelHolder(DefaultSplitViewModel::class) { get { parametersOf(route) } }

    // Why: each invite link opens its own ViewModels, which read the code from their route.
    fun createJoinInviteViewModel(route: Route.JoinInvite): ViewModelHolder<JoinInviteViewModel> =
        ViewModelHolder(JoinInviteViewModel::class) { get { parametersOf(route) } }

    fun createJoinPickViewModel(route: Route.JoinPick): ViewModelHolder<JoinPickViewModel> =
        ViewModelHolder(JoinPickViewModel::class) { get { parametersOf(route) } }

    fun createJoinWithLinkViewModel(route: Route.JoinWithLink): ViewModelHolder<JoinWithLinkViewModel> =
        ViewModelHolder(JoinWithLinkViewModel::class) { get { parametersOf(route) } }

    // Why: each key opens its own ViewModel, which reads the method and the key to edit from its route.
    fun createPaymentKeyViewModel(route: Route.PaymentKey): ViewModelHolder<PaymentKeyViewModel> =
        ViewModelHolder(PaymentKeyViewModel::class) { get { parametersOf(route) } }

    /*
     * Why: Google signs in through the browser on Apple platforms, and its OAuth callback comes back
     * as borarachar://auth-callback, which SwiftUI receives in onOpenURL, like a group's invite link
     * (blitzsplit.com/g/<code>, a universal link). Each handler takes only the links it knows.
     */
    fun handleDeepLink(url: NSURL) {
        get<SupabaseClient>().handleDeeplinks(url)
        val link = url.absoluteString ?: return
        val handleBrowserCallback = get<HandleBrowserCallback>()
        scope.launch { handleBrowserCallback(link) }
        get<LinkInbox>().receive(link)
    }

    // Why: the browser sign in opens Safari, or the browser on macOS, over the app.
    fun handleAppPaused() {
        get<HandleAppPaused>().invoke()
    }

    // Why: after any handleDeepLink the return brought, so a browser closed without finishing ends its sign in.
    fun handleAppResumed() {
        get<HandleAppResumed>().invoke()
    }

    // Why: a picked photo is megabytes, and filling a KotlinByteArray from Swift goes one byte per call.
    @OptIn(ExperimentalForeignApi::class)
    fun toByteArray(data: NSData): ByteArray = ByteArray(data.length.toInt()).apply {
        if (isNotEmpty()) usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
    }
}
