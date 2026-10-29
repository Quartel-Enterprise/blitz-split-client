package com.quare.blitzsplit.shared.di

import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.utils.DeviceRegion
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single { NativeSignIn(providers = emptySet()) }
    single { DeviceRegion(::getBrowserRegion) }
}

// Why: a language without a region takes its likely one ("pt" → BR), as the brand badge of the web does.
private fun getBrowserRegion(): String? = try {
    js("new Intl.Locale(navigator.language).maximize().region") as? String
} catch (unsupported: Throwable) {
    null
}
