package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.account.domain.usecase.HandleAppPaused
import com.quare.blitzsplit.core.account.domain.usecase.HandleAppResumed
import com.quare.blitzsplit.core.account.domain.usecase.HandleBrowserCallback
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.creategroup.presentation.viewmodel.CreateGroupViewModel
import com.quare.blitzsplit.feature.deleteaccount.presentation.viewmodel.DeleteAccountViewModel
import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.EditProfileViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import com.quare.blitzsplit.feature.invite.presentation.viewmodel.InviteViewModel
import com.quare.blitzsplit.feature.notificationsettings.presentation.viewmodel.NotificationSettingsViewModel
import com.quare.blitzsplit.feature.paymentkey.presentation.viewmodel.PaymentKeyViewModel
import com.quare.blitzsplit.feature.profile.presentation.viewmodel.ProfileViewModel
import com.quare.blitzsplit.feature.profilesetup.presentation.viewmodel.ProfileSetupViewModel
import com.quare.blitzsplit.feature.recoverycode.presentation.viewmodel.RecoveryCodeViewModel
import com.quare.blitzsplit.feature.restore.presentation.viewmodel.RestoreViewModel
import com.quare.blitzsplit.feature.welcome.presentation.viewmodel.WelcomeViewModel
import com.quare.blitzsplit.shared.app.AppViewModel
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

    // Why: each key opens its own ViewModel, which reads the method and the key to edit from its route.
    fun createPaymentKeyViewModel(route: Route.PaymentKey): ViewModelHolder<PaymentKeyViewModel> =
        ViewModelHolder(PaymentKeyViewModel::class) { get { parametersOf(route) } }

    /*
     * Why: Google signs in through the browser on Apple platforms, and its OAuth callback comes back
     * as borarachar://auth-callback, which SwiftUI receives in onOpenURL.
     */
    fun handleDeepLink(url: NSURL) {
        get<SupabaseClient>().handleDeeplinks(url)
        val callbackUrl = url.absoluteString ?: return
        val handleBrowserCallback = get<HandleBrowserCallback>()
        scope.launch { handleBrowserCallback(callbackUrl) }
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
