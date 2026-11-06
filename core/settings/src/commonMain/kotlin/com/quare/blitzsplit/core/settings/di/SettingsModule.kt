package com.quare.blitzsplit.core.settings.di

import com.quare.blitzsplit.core.settings.data.datasource.DeviceSettingsLocalDataSource
import com.quare.blitzsplit.core.settings.data.datasource.RoomDeviceSettingsLocalDataSource
import com.quare.blitzsplit.core.settings.data.repository.AppLanguageRepositoryImpl
import com.quare.blitzsplit.core.settings.data.repository.DeviceSettingsRepositoryImpl
import com.quare.blitzsplit.core.settings.domain.repository.AppLanguageRepository
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.DismissAppBanner
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveAppBannerDismissed
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveAppLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveBitcoinInSats
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveDynamicColors
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveGroupsLayout
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveLastNotificationPromptAt
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveLastUpdatePromptAt
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveStatsChartPreferences
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveThemePreference
import com.quare.blitzsplit.core.settings.domain.usecase.RefreshAppLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateAppLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateBitcoinInSats
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateDynamicColors
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateGroupsLayout
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateLastNotificationPromptAt
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateLastUpdatePromptAt
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateStatsChartPreferences
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateThemePreference
import com.quare.blitzsplit.core.settings.domain.usecase.impl.DismissAppBannerUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveAppBannerDismissedUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveAppLanguageUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveBitcoinInSatsUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveDynamicColorsUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveGroupsLayoutUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveLastNotificationPromptAtUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveLastUpdatePromptAtUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveStatsChartPreferencesUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveThemePreferenceUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.RefreshAppLanguageUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateAppLanguageUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateBitcoinInSatsUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateDynamicColorsUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateGroupsLayoutUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateLastNotificationPromptAtUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateLastUpdatePromptAtUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateStatsChartPreferencesUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateThemePreferenceUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val settingsModule = module {
    singleOf(::RoomDeviceSettingsLocalDataSource).bind<DeviceSettingsLocalDataSource>()
    singleOf(::DeviceSettingsRepositoryImpl).bind<DeviceSettingsRepository>()
    factoryOf(::ObserveThemePreferenceUseCase).bind<ObserveThemePreference>()
    factoryOf(::UpdateThemePreferenceUseCase).bind<UpdateThemePreference>()
    factoryOf(::ObserveGroupsLayoutUseCase).bind<ObserveGroupsLayout>()
    factoryOf(::UpdateGroupsLayoutUseCase).bind<UpdateGroupsLayout>()
    factoryOf(::ObserveStatsChartPreferencesUseCase).bind<ObserveStatsChartPreferences>()
    factoryOf(::UpdateStatsChartPreferencesUseCase).bind<UpdateStatsChartPreferences>()
    factoryOf(::ObserveDynamicColorsUseCase).bind<ObserveDynamicColors>()
    factoryOf(::UpdateDynamicColorsUseCase).bind<UpdateDynamicColors>()
    factoryOf(::ObserveBitcoinInSatsUseCase).bind<ObserveBitcoinInSats>()
    factoryOf(::UpdateBitcoinInSatsUseCase).bind<UpdateBitcoinInSats>()
    factoryOf(::ObserveLastUpdatePromptAtUseCase).bind<ObserveLastUpdatePromptAt>()
    factoryOf(::UpdateLastUpdatePromptAtUseCase).bind<UpdateLastUpdatePromptAt>()
    factoryOf(::ObserveLastNotificationPromptAtUseCase).bind<ObserveLastNotificationPromptAt>()
    factoryOf(::UpdateLastNotificationPromptAtUseCase).bind<UpdateLastNotificationPromptAt>()
    factoryOf(::ObserveAppBannerDismissedUseCase).bind<ObserveAppBannerDismissed>()
    factoryOf(::DismissAppBannerUseCase).bind<DismissAppBanner>()
    // Why: one copy of the language for the whole app, so Profile's pick redraws the Apple app's root at once.
    singleOf(::AppLanguageRepositoryImpl).bind<AppLanguageRepository>()
    factoryOf(::ObserveAppLanguageUseCase).bind<ObserveAppLanguage>()
    factoryOf(::UpdateAppLanguageUseCase).bind<UpdateAppLanguage>()
    factoryOf(::RefreshAppLanguageUseCase).bind<RefreshAppLanguage>()
}
