package com.quare.blitzsplit.core.inappupdate.di

import com.quare.blitzsplit.core.inappupdate.data.AppStoreStorefront
import com.quare.blitzsplit.core.inappupdate.data.repository.AppStoreUpdateRepository
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateMechanism
import com.quare.blitzsplit.core.inappupdate.domain.repository.AppUpdateRepository
import com.quare.blitzsplit.core.inappupdate.generated.InAppUpdateConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

private val appStoreClient = named("appStoreClient")

// Why: the App Store has no in-app update; the sheet the shared code opens sends there instead.
internal fun createAppStoreUpdateModule(storefront: AppStoreStorefront): Module = module {
    single { UpdateMechanism.APP_STORE }
    single(appStoreClient) { HttpClient(Darwin) }
    single<AppUpdateRepository> {
        AppStoreUpdateRepository(
            httpClient = get(appStoreClient),
            storefront = storefront,
            installedVersion = InAppUpdateConfig.APP_VERSION,
            getRegionCode = { NSLocale.currentLocale.countryCode },
        )
    }
}
