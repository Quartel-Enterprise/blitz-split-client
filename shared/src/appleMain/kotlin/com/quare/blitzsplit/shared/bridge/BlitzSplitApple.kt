package com.quare.blitzsplit.shared.bridge

import androidx.room3.RoomDatabase
import com.quare.blitzsplit.core.account.domain.usecase.HandleAppPaused
import com.quare.blitzsplit.core.account.domain.usecase.HandleAppResumed
import com.quare.blitzsplit.core.account.domain.usecase.HandleBrowserCallback
import com.quare.blitzsplit.core.expenses.domain.model.BalanceSnapshot
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveBalanceSnapshot
import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.core.profile.domain.model.ApnsEnvironment
import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.model.PushPlatform
import com.quare.blitzsplit.core.profile.domain.usecase.SetPushDevice
import com.quare.blitzsplit.core.provider.analytics.AnalyticsHub
import com.quare.blitzsplit.core.provider.analytics.AnalyticsService
import com.quare.blitzsplit.core.provider.room.db.AppDatabase
import com.quare.blitzsplit.core.sync.domain.usecase.ReportAppForeground
import com.quare.blitzsplit.core.watch.domain.usecase.ApproveNearbyWatch
import com.quare.blitzsplit.core.watch.domain.usecase.ObserveAccountSignOuts
import com.quare.blitzsplit.feature.activity.presentation.viewmodel.ActivityViewModel
import com.quare.blitzsplit.feature.appdownload.presentation.viewmodel.AppDownloadViewModel
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.ShareBillViewModel
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.SharedBillViewModel
import com.quare.blitzsplit.feature.categorymap.presentation.viewmodel.CategoryMapViewModel
import com.quare.blitzsplit.feature.connectwatch.presentation.viewmodel.ConnectWatchViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.AgendaViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactInviteViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactProfileViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactsViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ProfileLinkViewModel
import com.quare.blitzsplit.feature.contactsupport.presentation.viewmodel.ContactSupportViewModel
import com.quare.blitzsplit.feature.creategroup.presentation.viewmodel.CreateGroupViewModel
import com.quare.blitzsplit.feature.deleteaccount.presentation.viewmodel.DeleteAccountViewModel
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.DefaultSplitViewModel
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.EditGroupViewModel
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.GroupPhotoViewModel
import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.EditProfileViewModel
import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.ProfilePhotoViewModel
import com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel.ChargeViewModel
import com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel.GlobalBalancesViewModel
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.AllExpensesViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.HomeSidebarViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.HomeViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.SearchViewModel
import com.quare.blitzsplit.feature.groupstats.presentation.viewmodel.GroupStatsViewModel
import com.quare.blitzsplit.feature.inappupdate.presentation.viewmodel.InAppUpdateViewModel
import com.quare.blitzsplit.feature.invite.presentation.viewmodel.InviteViewModel
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinInviteViewModel
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinPickViewModel
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinWithLinkViewModel
import com.quare.blitzsplit.feature.legal.presentation.viewmodel.LegalDocumentViewModel
import com.quare.blitzsplit.feature.members.presentation.viewmodel.MembersViewModel
import com.quare.blitzsplit.feature.notificationsettings.presentation.viewmodel.NotificationSettingsViewModel
import com.quare.blitzsplit.feature.payingmethod.presentation.viewmodel.PayingMethodViewModel
import com.quare.blitzsplit.feature.paymentkey.presentation.viewmodel.PaymentKeyViewModel
import com.quare.blitzsplit.feature.profile.presentation.viewmodel.ProfileViewModel
import com.quare.blitzsplit.feature.profilesetup.presentation.viewmodel.ProfileSetupViewModel
import com.quare.blitzsplit.feature.recoverycode.presentation.viewmodel.RecoveryCodeViewModel
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.PickPaymentViewModel
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.RegisterPaymentViewModel
import com.quare.blitzsplit.feature.releasenotes.presentation.viewmodel.ReleaseNotesViewModel
import com.quare.blitzsplit.feature.restore.presentation.viewmodel.RestoreViewModel
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import com.quare.blitzsplit.feature.welcome.presentation.viewmodel.WelcomeViewModel
import com.quare.blitzsplit.shared.app.AppViewModel
import com.quare.blitzsplit.shared.app.LinkInbox
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import com.quare.blitzsplit.shared.storescreenshots.DirectoryAssets
import com.quare.blitzsplit.shared.storescreenshots.SampleTrips
import com.quare.blitzsplit.shared.storescreenshots.StoreScreenshotsScreen
import com.quare.blitzsplit.shared.storescreenshots.StoreScreenshotsScreen.Companion.openOverHome
import com.quare.blitzsplit.shared.storescreenshots.StoreScreenshotsSeed
import com.quare.blitzsplit.shared.storescreenshots.createStoreScreenshotsDatabaseBuilder
import com.quare.blitzsplit.shared.storescreenshots.deleteStoreScreenshotsDatabase
import com.quare.blitzsplit.shared.storescreenshots.storeScreenshotsModuleOf
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.posix.memcpy
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import com.quare.blitzsplit.core.provider.crashlytics.startCrashReporting as startCrashlytics

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

    /*
     * Why: the store screenshots' "Bill items" launch, or the listing art's "Dividir", with the split still to open.
     * The bill's screen opens it by itself once the bill is loaded, as its Split row does: nothing taps on the Mac's
     * capture, and on a phone the row's place changes with the screen's height.
     */
    private var opensSplitOf: StoreScreenshotsScreen? = null

    fun start() {
        startBlitzSplitKoin()
    }

    /*
     * Why: the App Store screenshots (docs/store-listing-screenshots.md): the captures launch a debug build with the
     * sample trip seeded into a fresh Room, in their language and theme, before the first screen reads it, then open
     * the screen named by screenCode over Início. A release framework never seeds: it starts as usual, so the hook does
     * nothing even if something calls it.
     */
    @OptIn(ExperimentalNativeApi::class)
    fun startForStoreScreenshots(
        language: String,
        isDark: Boolean,
        assetsDirectory: String?,
        screenCode: String?,
    ) {
        if (!Platform.isDebugBinary) {
            start()
            return
        }
        deleteStoreScreenshotsDatabase()
        val trip = SampleTrips.tripOf(language)
        startBlitzSplitKoin(
            extraModules = listOf(
                storeScreenshotsModuleOf(trip),
                module { single<RoomDatabase.Builder<AppDatabase>> { createStoreScreenshotsDatabaseBuilder() } },
            ),
        )
        /*
         * Why: blocks the launch until the trip is there, off the main thread, so a seed step that hops to Main can't
         * wait on it.
         */
        val seeded = runBlocking(Dispatchers.Default) {
            StoreScreenshotsSeed(trip, assetsDirectory?.let(::DirectoryAssets)).createTrip(
                language = AppLanguage.fromTag(language),
                theme = if (isDark) ThemePreference.DARK else ThemePreference.LIGHT,
            )
        }
        val screen = StoreScreenshotsScreen.fromCodeOrNull(screenCode)
        opensSplitOf = screen?.takeIf { it == StoreScreenshotsScreen.ITEMS || it == StoreScreenshotsScreen.SPLIT_MODES }
        val routes = screen?.routesOf(seeded).orEmpty()
        val navigator = get<Navigator>()
        scope.launch { navigator.openOverHome(routes) }
    }

    // Why: right after FirebaseApp.configure(), which only a build with its GoogleService-Info.plist calls.
    fun startCrashReporting(isDebug: Boolean) {
        startCrashlytics(isDebug = isDebug)
    }

    // Why: Firebase Analytics in Swift, attached only by a release build with its GoogleService-Info.plist.
    fun attachAnalytics(service: AnalyticsService) {
        get<AnalyticsHub>().attach(service)
    }

    /*
     * Why: the iPhone's balance widget reads a snapshot the app writes to the App Group, since the widget's process has
     * no Room; the app writes it again on each change (docs/product/balances-and-contacts.md). It runs for the app's
     * whole life, whatever screen is open, so the balances are worked out off the main thread.
     */
    fun createBalanceSnapshotWatcher(): FlowWatcher<BalanceSnapshot> =
        FlowWatcher(get<ObserveBalanceSnapshot>()().flowOn(Dispatchers.Default))

    fun createAppViewModel(): ViewModelHolder<AppViewModel> = ViewModelHolder(AppViewModel::class) { get() }

    fun createWelcomeViewModel(): ViewModelHolder<WelcomeViewModel> = ViewModelHolder(WelcomeViewModel::class) { get() }

    fun createProfileSetupViewModel(): ViewModelHolder<ProfileSetupViewModel> =
        ViewModelHolder(ProfileSetupViewModel::class) { get() }

    fun createRestoreViewModel(): ViewModelHolder<RestoreViewModel> = ViewModelHolder(RestoreViewModel::class) { get() }

    fun createRecoveryCodeViewModel(): ViewModelHolder<RecoveryCodeViewModel> =
        ViewModelHolder(RecoveryCodeViewModel::class) { get() }

    // Why: Início, the home's first tab and the root of the back stack.
    fun createHomeViewModel(): ViewModelHolder<HomeViewModel> = ViewModelHolder(HomeViewModel::class) { get() }

    // Why: "Todas as despesas", which Início's "Ver tudo" opens.
    fun createAllExpensesViewModel(route: Route.AllExpenses): ViewModelHolder<AllExpensesViewModel> =
        ViewModelHolder(AllExpensesViewModel::class) { get { parametersOf(route) } }

    fun createGroupsViewModel(): ViewModelHolder<GroupsViewModel> = ViewModelHolder(GroupsViewModel::class) { get() }

    fun createHomeSidebarViewModel(): ViewModelHolder<HomeSidebarViewModel> =
        ViewModelHolder(HomeSidebarViewModel::class) { get() }

    fun createSearchViewModel(): ViewModelHolder<SearchViewModel> = ViewModelHolder(SearchViewModel::class) { get() }

    fun createCreateGroupViewModel(): ViewModelHolder<CreateGroupViewModel> =
        ViewModelHolder(CreateGroupViewModel::class) { get() }

    fun createProfileViewModel(): ViewModelHolder<ProfileViewModel> = ViewModelHolder(ProfileViewModel::class) { get() }

    fun createLegalDocumentViewModel(route: Route.LegalDocument): ViewModelHolder<LegalDocumentViewModel> =
        ViewModelHolder(LegalDocumentViewModel::class) { get { parametersOf(route) } }

    // Why: "Atualização disponível" over wherever the app is, the App Store's newer version.
    fun createInAppUpdateViewModel(route: Route.InAppUpdate): ViewModelHolder<InAppUpdateViewModel> =
        ViewModelHolder(InAppUpdateViewModel::class) { get { parametersOf(route) } }

    // Why: the Mac's "Bora Rachar on iPhone" sheet (14.2), from Home's card and Settings' iPhone row.
    fun createAppDownloadViewModel(route: Route.AppDownload): ViewModelHolder<AppDownloadViewModel> =
        ViewModelHolder(AppDownloadViewModel::class) { get { parametersOf(route) } }

    fun createEditProfileViewModel(): ViewModelHolder<EditProfileViewModel> =
        ViewModelHolder(EditProfileViewModel::class) { get() }

    fun createProfilePhotoViewModel(): ViewModelHolder<ProfilePhotoViewModel> =
        ViewModelHolder(ProfilePhotoViewModel::class) { get() }

    fun createDeleteAccountViewModel(): ViewModelHolder<DeleteAccountViewModel> =
        ViewModelHolder(DeleteAccountViewModel::class) { get() }

    fun createContactSupportViewModel(): ViewModelHolder<ContactSupportViewModel> =
        ViewModelHolder(ContactSupportViewModel::class) { get() }

    // Why: Profile › Connect watch (15.14), Settings' sheet on the Mac.
    fun createConnectWatchViewModel(): ViewModelHolder<ConnectWatchViewModel> =
        ViewModelHolder(ConnectWatchViewModel::class) { get() }

    /*
     * Why: the iPhone's side of nearby pairing (15.10): the paired watch hands its code over WatchConnectivity and the
     * iPhone approves it with its own session, with no confirmation, as Android's WatchPairingListenerService does.
     * onReply gets one of WatchLink's REPLY_ values, on the main thread.
     */
    fun approveNearbyWatch(
        code: String,
        onReply: (String) -> Unit,
    ) {
        val approve = get<ApproveNearbyWatch>()
        scope.launch { onReply(approve(code)) }
    }

    // Why: the user id of each sign out (or deleted account) on the iPhone, which it tells its watch (15.13).
    fun createAccountSignOutsWatcher(): FlowWatcher<String> = FlowWatcher(get<ObserveAccountSignOuts>()())

    fun createNotificationSettingsViewModel(): ViewModelHolder<NotificationSettingsViewModel> =
        ViewModelHolder(NotificationSettingsViewModel::class) { get() }

    fun createCategoryMapViewModel(): ViewModelHolder<CategoryMapViewModel> =
        ViewModelHolder(CategoryMapViewModel::class) { get() }

    fun createReleaseNotesViewModel(): ViewModelHolder<ReleaseNotesViewModel> =
        ViewModelHolder(ReleaseNotesViewModel::class) { get() }

    // Why: each group opens its own ViewModel, which reads the group from its route.
    fun createInviteViewModel(route: Route.Invite): ViewModelHolder<InviteViewModel> =
        ViewModelHolder(InviteViewModel::class) { get { parametersOf(route) } }

    // Why: Membros' content under the group's header, opened with the group's route.
    fun createMembersViewModel(route: Route.GroupDetail): ViewModelHolder<MembersViewModel> =
        ViewModelHolder(MembersViewModel::class) { get { parametersOf(route) } }

    fun createGroupDetailViewModel(route: Route.GroupDetail): ViewModelHolder<GroupDetailViewModel> =
        ViewModelHolder(GroupDetailViewModel::class) { get { parametersOf(route) } }

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

    // Why: "Cobrar", over Saldos or a contact's profile, for that person in the currency shown.
    fun createChargeViewModel(route: Route.Charge): ViewModelHolder<ChargeViewModel> =
        ViewModelHolder(ChargeViewModel::class) { get { parametersOf(route) } }

    // Why: Atividade, which Início's bell opens, on one person when a contact's history opens it.
    fun createActivityViewModel(route: Route.Activity): ViewModelHolder<ActivityViewModel> =
        ViewModelHolder(ActivityViewModel::class) { get { parametersOf(route) } }

    fun createContactsViewModel(): ViewModelHolder<ContactsViewModel> =
        ViewModelHolder(ContactsViewModel::class) { get() }

    // Why: each person opens their own profile, which reads them from its route (beside Contatos too).
    fun createContactProfileViewModel(route: Route.ContactProfile): ViewModelHolder<ContactProfileViewModel> =
        ViewModelHolder(ContactProfileViewModel::class) { get { parametersOf(route) } }

    // Why: a profile link's page reads its code from its route.
    fun createProfileLinkViewModel(route: Route.ProfileLink): ViewModelHolder<ProfileLinkViewModel> =
        ViewModelHolder(ProfileLinkViewModel::class) { get { parametersOf(route) } }

    fun createAgendaViewModel(): ViewModelHolder<AgendaViewModel> = ViewModelHolder(AgendaViewModel::class) { get() }

    // Why: the invite reads whom it is for (an agenda's name, a guest's personal link) from its route.
    fun createContactInviteViewModel(route: Route.ContactInvite): ViewModelHolder<ContactInviteViewModel> =
        ViewModelHolder(ContactInviteViewModel::class) { get { parametersOf(route) } }

    fun createSplitBillViewModel(route: Route.SplitBill): ViewModelHolder<SplitBillViewModel> =
        ViewModelHolder(SplitBillViewModel::class) { get { parametersOf(route) } }.also(::openSplitOnce)

    /*
     * Why: only the first bill of the launch, the one the screenshot opens: a bill opened after it stays as it is.
     * "Bill items" waits for the dinner's items; the art's "Dividir" for the bill the receipt filled in.
     */
    private fun openSplitOnce(holder: ViewModelHolder<SplitBillViewModel>) {
        val screen = opensSplitOf ?: return
        opensSplitOf = null
        val viewModel = holder.viewModel
        scope.launch {
            viewModel.uiState.first { state ->
                if (screen == StoreScreenshotsScreen.ITEMS) {
                    state.split.mode == SplitMode.ITEMS
                } else {
                    state.description.isNotBlank() && state.amountMinor != null
                }
            }
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        }
    }

    fun createBillDetailViewModel(route: Route.BillDetail): ViewModelHolder<BillDetailViewModel> =
        ViewModelHolder(BillDetailViewModel::class) { get { parametersOf(route) } }

    // Why: "Compartilhar conta" over Conta; a bill's link page is the web's only, as the apps don't claim /c/.
    fun createShareBillViewModel(route: Route.ShareBill): ViewModelHolder<ShareBillViewModel> =
        ViewModelHolder(ShareBillViewModel::class) { get { parametersOf(route) } }

    // Why: a bill's link page reads its code, and the comment it was shared at, from its route.
    fun createSharedBillViewModel(route: Route.SharedBill): ViewModelHolder<SharedBillViewModel> =
        ViewModelHolder(SharedBillViewModel::class) { get { parametersOf(route) } }

    fun createGroupStatsViewModel(route: Route.GroupStats): ViewModelHolder<GroupStatsViewModel> =
        ViewModelHolder(GroupStatsViewModel::class) { get { parametersOf(route) } }

    fun createGroupPhotoViewModel(route: Route.GroupPhoto): ViewModelHolder<GroupPhotoViewModel> =
        ViewModelHolder(GroupPhotoViewModel::class) { get { parametersOf(route) } }

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

    fun createPayingMethodViewModel(route: Route.PayingMethod): ViewModelHolder<PayingMethodViewModel> =
        ViewModelHolder(PayingMethodViewModel::class) { get { parametersOf(route) } }

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

    /*
     * Why: APNs' token for Atividade's push notifications, as hex, on every launch and whenever it changes. A debug
     * build's token is the sandbox's.
     */
    fun setPushToken(
        token: String,
        isMac: Boolean,
        isSandbox: Boolean,
    ) {
        get<SetPushDevice>().invoke(
            PushDevice(
                token = token,
                platform = if (isMac) PushPlatform.MACOS else PushPlatform.IOS,
                apnsEnvironment = if (isSandbox) ApnsEnvironment.DEVELOPMENT else ApnsEnvironment.PRODUCTION,
                webP256dh = null,
                webAuth = null,
            ),
        )
    }

    // Why: the browser sign in opens Safari, or the browser on macOS, over the app.
    fun handleAppPaused() {
        get<HandleAppPaused>().invoke()
    }

    // Why: after any handleDeepLink the return brought, so a browser closed without finishing ends its sign in.
    fun handleAppResumed() {
        get<HandleAppResumed>().invoke()
    }

    // Why: real time listens only while the app is in front (its scene phase isn't background).
    fun setAppForeground(isForeground: Boolean) {
        get<ReportAppForeground>().invoke(isForeground)
    }

    /*
     * Why: a picked photo is megabytes, and filling a KotlinByteArray from Swift goes one byte per call. NSUInteger and
     * size_t are 32 bits on the Apple Watch's arm64_32, the same width as each other on every target.
     */
    @OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
    fun toByteArray(data: NSData): ByteArray = ByteArray(data.length.toInt()).apply {
        if (isNotEmpty()) usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
    }

    /*
     * Why: a photo added offline reaches Swift as a KotlinByteArray, and reading it from Swift goes one byte per call.
     * NSUInteger is 32 bits on the Apple Watch's arm64_32, so the length converts to each target's width.
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class, UnsafeNumber::class)
    fun toData(bytes: ByteArray): NSData = if (bytes.isEmpty()) {
        NSData()
    } else {
        bytes.usePinned { pinned ->
            NSData.create(
                bytes = pinned.addressOf(0),
                length = bytes.size.convert(),
            )
        }
    }
}
