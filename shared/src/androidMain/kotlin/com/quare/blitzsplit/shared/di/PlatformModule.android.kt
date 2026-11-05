package com.quare.blitzsplit.shared.di

import android.icu.util.ULocale
import android.os.Build
import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.settings.domain.model.AppLanguageStore
import com.quare.blitzsplit.core.settings.domain.model.DynamicColorSupport
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.core.utils.DeviceRegion
import com.quare.blitzsplit.shared.settings.AndroidAppLanguageStore
import org.koin.core.module.Module
import org.koin.dsl.module
import java.text.DecimalFormatSymbols
import java.util.Locale

internal actual val platformModule: Module = module {
    single { NativeSignIn(providers = setOf(AccountProvider.GOOGLE)) }
    // Why: a locale with no region ("pt") takes its likely one (BR), as the web does.
    single {
        DeviceRegion {
            ULocale
                .addLikelySubtags(
                    ULocale.forLocale(Locale.getDefault()),
                ).country
                .ifEmpty { null }
        }
    }
    single { DeviceDecimalSeparator { DecimalFormatSymbols.getInstance().decimalSeparator } }
    single<AppLanguageStore> { AndroidAppLanguageStore(get()) }
    single { DynamicColorSupport { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S } }
}
