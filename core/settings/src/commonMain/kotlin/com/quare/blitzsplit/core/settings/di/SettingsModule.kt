package com.quare.blitzsplit.core.settings.di

import com.quare.blitzsplit.core.settings.data.datasource.DeviceSettingsLocalDataSource
import com.quare.blitzsplit.core.settings.data.datasource.RoomDeviceSettingsLocalDataSource
import com.quare.blitzsplit.core.settings.data.repository.DeviceSettingsRepositoryImpl
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveThemePreference
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateThemePreference
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveThemePreferenceUseCase
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
}
