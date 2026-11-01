package com.quare.blitzsplit.shared.di

import com.quare.blitzsplit.core.account.di.accountModule
import com.quare.blitzsplit.core.expenses.di.expenseModule
import com.quare.blitzsplit.core.groups.di.groupModule
import com.quare.blitzsplit.core.navigation.di.navigationModule
import com.quare.blitzsplit.core.profile.di.profileModule
import com.quare.blitzsplit.core.provider.image.di.imageModule
import com.quare.blitzsplit.core.provider.room.di.roomModule
import com.quare.blitzsplit.core.provider.supabase.di.supabaseModule
import com.quare.blitzsplit.core.settings.di.settingsModule
import com.quare.blitzsplit.core.sync.di.syncModule
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.core.utils.SystemCurrentTimeMillis
import com.quare.blitzsplit.core.utils.SystemMinuteTicks
import com.quare.blitzsplit.feature.billdetail.di.billDetailModule
import com.quare.blitzsplit.feature.creategroup.di.createGroupModule
import com.quare.blitzsplit.feature.deleteaccount.di.deleteAccountModule
import com.quare.blitzsplit.feature.editgroup.di.editGroupModule
import com.quare.blitzsplit.feature.editprofile.di.editProfileModule
import com.quare.blitzsplit.feature.globalbalances.di.globalBalancesModule
import com.quare.blitzsplit.feature.groupdetail.di.groupDetailModule
import com.quare.blitzsplit.feature.groups.di.groupsModule
import com.quare.blitzsplit.feature.invite.di.inviteModule
import com.quare.blitzsplit.feature.join.di.joinModule
import com.quare.blitzsplit.feature.members.di.membersModule
import com.quare.blitzsplit.feature.notificationsettings.di.notificationSettingsModule
import com.quare.blitzsplit.feature.paymentkey.di.paymentKeyModule
import com.quare.blitzsplit.feature.profilesetup.di.profileSetupModule
import com.quare.blitzsplit.feature.recoverycode.di.recoveryCodeModule
import com.quare.blitzsplit.feature.registerpayment.di.registerPaymentModule
import com.quare.blitzsplit.feature.restore.di.restoreModule
import com.quare.blitzsplit.feature.splitbill.di.splitBillModule
import com.quare.blitzsplit.feature.welcome.di.welcomeModule
import com.quare.blitzsplit.shared.app.AppViewModel
import com.quare.blitzsplit.shared.app.LinkInbox
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import software.quare.connectivity.connectivityModule
import com.quare.blitzsplit.feature.profile.di.profileModule as profileScreenModule

private val appModule = module {
    single<CurrentTimeMillis> { SystemCurrentTimeMillis() }
    single<MinuteTicks> { SystemMinuteTicks(get()) }
    single { LinkInbox() }
    viewModelOf(::AppViewModel)
}

val sharedModules: List<Module> = listOf(
    appModule,
    platformModule,
    connectivityModule,
    navigationModule,
    roomModule,
    supabaseModule,
    imageModule,
    profileModule,
    accountModule,
    groupModule,
    expenseModule,
    syncModule,
    settingsModule,
    welcomeModule,
    profileSetupModule,
    restoreModule,
    recoveryCodeModule,
    groupsModule,
    profileScreenModule,
    editProfileModule,
    paymentKeyModule,
    deleteAccountModule,
    notificationSettingsModule,
    createGroupModule,
    inviteModule,
    membersModule,
    joinModule,
    groupDetailModule,
    billDetailModule,
    splitBillModule,
    editGroupModule,
    registerPaymentModule,
    globalBalancesModule,
)

// Why: each app adds what only it can provide (the Android Context, the browser's SQLite Worker).
fun startBlitzSplitKoin(
    extraModules: List<Module> = emptyList(),
    configure: KoinApplication.() -> Unit = {},
): KoinApplication = startKoin {
    configure()
    modules(sharedModules + extraModules)
}
