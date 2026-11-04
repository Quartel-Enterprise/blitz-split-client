package com.quare.blitzsplit.core.provider.crashlytics.di

import com.quare.blitzsplit.core.provider.crashlytics.CrashReporter
import com.quare.blitzsplit.core.provider.crashlytics.CrashlyticsReporter
import org.koin.dsl.module

val crashlyticsModule = module {
    single<CrashReporter> { CrashlyticsReporter() }
}
