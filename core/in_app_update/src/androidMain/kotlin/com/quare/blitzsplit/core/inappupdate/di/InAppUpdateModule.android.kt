package com.quare.blitzsplit.core.inappupdate.di

import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.quare.blitzsplit.core.inappupdate.CurrentActivityProvider
import com.quare.blitzsplit.core.inappupdate.data.repository.GooglePlayUpdateRepository
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateMechanism
import com.quare.blitzsplit.core.inappupdate.domain.repository.AppUpdateRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal actual val platformInAppUpdateModule: Module = module {
    single { UpdateMechanism.GOOGLE_PLAY }
    single<AppUpdateManager> { AppUpdateManagerFactory.create(androidContext()) }
    singleOf(::CurrentActivityProvider)
    // Why: a single, since it listens to Google Play's download from its creation on.
    singleOf(::GooglePlayUpdateRepository).bind<AppUpdateRepository>()
}
