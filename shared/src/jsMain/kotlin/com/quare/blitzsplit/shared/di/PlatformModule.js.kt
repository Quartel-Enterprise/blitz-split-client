package com.quare.blitzsplit.shared.di

import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.core.utils.DeviceRegion
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single { NativeSignIn(providers = emptySet()) }
    single { DeviceRegion(::getBrowserRegion) }
    single { DeviceDecimalSeparator(::getBrowserDecimalSeparator) }
}

// Why: a language without a region takes its likely one ("pt" → BR), as the brand badge of the web does.
private fun getBrowserRegion(): String? = try {
    js("new Intl.Locale(navigator.language).maximize().region") as? String
} catch (unsupported: Throwable) {
    null
}

// Why: the separator Intl writes in 1.5 for the browser's language.
private fun getBrowserDecimalSeparator(): Char = try {
    (js("new Intl.NumberFormat(navigator.language).format(1.5).charAt(1)") as? String)?.firstOrNull() ?: '.'
} catch (unsupported: Throwable) {
    '.'
}
