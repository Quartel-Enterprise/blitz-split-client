package com.quare.blitzsplit.shared.di

import android.content.res.Resources
import android.icu.util.ULocale
import android.os.Build
import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.settings.data.datasource.AppLanguageLocalDataSource
import com.quare.blitzsplit.core.settings.domain.model.DynamicColorSupport
import com.quare.blitzsplit.core.utils.DeviceDecimalSeparator
import com.quare.blitzsplit.core.utils.DeviceRegion
import com.quare.blitzsplit.shared.settings.AndroidAppLanguageLocalDataSource
import org.koin.core.module.Module
import org.koin.dsl.module
import java.text.DecimalFormatSymbols

internal actual val platformModule: Module = module {
    single { NativeSignIn(providers = setOf(AccountProvider.GOOGLE)) }
    /*
     * Why: a locale with no region ("pt") takes its likely one (BR), as the web does. The system's own locale, never
     * the default one, which follows Profile › Language: picking Português on a phone in the US keeps it in the US.
     */
    single {
        DeviceRegion {
            ULocale
                .addLikelySubtags(
                    ULocale.forLocale(Resources.getSystem().configuration.locales[0]),
                ).country
                .ifEmpty { null }
        }
    }
    single { DeviceDecimalSeparator { DecimalFormatSymbols.getInstance().decimalSeparator } }
    single<AppLanguageLocalDataSource> { AndroidAppLanguageLocalDataSource(get()) }
    single { DynamicColorSupport { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S } }
}
