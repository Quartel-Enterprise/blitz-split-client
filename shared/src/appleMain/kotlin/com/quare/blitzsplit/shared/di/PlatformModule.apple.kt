package com.quare.blitzsplit.shared.di

import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.settings.data.datasource.AppLanguageLocalDataSource
import com.quare.blitzsplit.core.settings.domain.model.DynamicColorSupport
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.core.utils.DeviceLanguage
import com.quare.blitzsplit.core.utils.DeviceRegion
import com.quare.blitzsplit.shared.settings.AppleAppLanguageLocalDataSource
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.decimalSeparator
import platform.Foundation.preferredLanguages

internal actual val platformModule: Module = module {
    single { NativeSignIn(providers = setOf(AccountProvider.APPLE)) }
    single { DeviceRegion { NSLocale.currentLocale.countryCode } }
    single { DeviceLanguage { NSLocale.preferredLanguages.firstOrNull() as? String } }
    single { DeviceDecimalSeparator { NSLocale.currentLocale.decimalSeparator.firstOrNull() ?: '.' } }
    single<AppLanguageLocalDataSource> { AppleAppLanguageLocalDataSource() }
    single { DynamicColorSupport { false } }
}
