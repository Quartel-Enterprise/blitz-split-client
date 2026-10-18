package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import com.quare.blitzsplit.feature.profilesetup.presentation.viewmodel.ProfileSetupViewModel
import com.quare.blitzsplit.feature.recoverycode.presentation.viewmodel.RecoveryCodeViewModel
import com.quare.blitzsplit.feature.restore.presentation.viewmodel.RestoreViewModel
import com.quare.blitzsplit.feature.welcome.presentation.viewmodel.WelcomeViewModel
import com.quare.blitzsplit.shared.app.AppViewModel
import com.quare.blitzsplit.shared.di.startBlitzSplitKoin
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import platform.Foundation.NSURL

/*
 * Why: the single entry point SwiftUI calls. Swift can't call reified Koin functions, so each
 * screen's ViewModel comes from a plain function here, wrapped in the holder that clears it.
 */
object BlitzSplitApple : KoinComponent {
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

    /*
     * Why: Google signs in through the browser on Apple platforms, and its OAuth callback comes back
     * as borarachar://auth-callback, which SwiftUI receives in onOpenURL.
     */
    fun handleDeepLink(url: NSURL) {
        get<SupabaseClient>().handleDeeplinks(url)
    }
}
