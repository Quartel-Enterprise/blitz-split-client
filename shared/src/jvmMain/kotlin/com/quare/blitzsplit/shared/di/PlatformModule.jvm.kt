package com.quare.blitzsplit.shared.di

import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.core.utils.DeviceRegion
import org.koin.core.module.Module
import org.koin.dsl.module
import java.text.DecimalFormatSymbols
import java.util.Locale

internal actual val platformModule: Module = module {
    single { NativeSignIn(providers = emptySet()) }
    single { DeviceRegion { Locale.getDefault().country.ifEmpty { null } } }
    single { DeviceDecimalSeparator { DecimalFormatSymbols.getInstance().decimalSeparator } }
}
