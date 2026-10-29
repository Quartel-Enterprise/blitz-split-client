package com.quare.blitzsplit.feature.notificationsettings.di

import com.quare.blitzsplit.feature.notificationsettings.presentation.viewmodel.NotificationSettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val notificationSettingsModule = module {
    viewModelOf(::NotificationSettingsViewModel)
}
