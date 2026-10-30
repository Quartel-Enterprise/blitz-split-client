package com.quare.blitzsplit.shared.di

import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.core.utils.DeviceRegion
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.decimalSeparator

internal actual val platformModule: Module = module {
    single { NativeSignIn(providers = setOf(AccountProvider.APPLE)) }
    single { DeviceRegion { NSLocale.currentLocale.countryCode } }
    single { DeviceDecimalSeparator { NSLocale.currentLocale.decimalSeparator.firstOrNull() ?: '.' } }
}
