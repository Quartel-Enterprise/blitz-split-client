package com.quare.blitzsplit.shared.di

import com.quare.blitzsplit.core.account.di.accountModule
import com.quare.blitzsplit.core.navigation.di.navigationModule
import com.quare.blitzsplit.core.profile.di.profileModule
import com.quare.blitzsplit.core.provider.image.di.imageModule
import com.quare.blitzsplit.core.provider.room.di.roomModule
import com.quare.blitzsplit.core.provider.supabase.di.supabaseModule
import com.quare.blitzsplit.core.sync.di.syncModule
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.SystemCurrentTimeMillis
import com.quare.blitzsplit.feature.groups.di.groupsModule
import com.quare.blitzsplit.feature.profilesetup.di.profileSetupModule
import com.quare.blitzsplit.feature.recoverycode.di.recoveryCodeModule
import com.quare.blitzsplit.feature.restore.di.restoreModule
import com.quare.blitzsplit.feature.welcome.di.welcomeModule
import com.quare.blitzsplit.shared.app.AppViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import software.quare.connectivity.connectivityModule

private val appModule = module {
    single<CurrentTimeMillis> { SystemCurrentTimeMillis() }
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
    syncModule,
    welcomeModule,
    profileSetupModule,
    restoreModule,
    recoveryCodeModule,
    groupsModule,
)

// Why: each app adds what only it can provide (the Android Context, the browser's SQLite Worker).
fun startBlitzSplitKoin(
    extraModules: List<Module> = emptyList(),
    configure: KoinApplication.() -> Unit = {},
): KoinApplication = startKoin {
    configure()
    modules(sharedModules + extraModules)
}
