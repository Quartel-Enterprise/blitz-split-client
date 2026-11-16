package com.quare.blitzsplit.shared.app

import com.quare.blitzsplit.core.settings.domain.usecase.ObserveBitcoinInSats
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveDynamicColors
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveThemePreference
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveTimeFormatPreference

/*
 * Why: the device's choices on how the whole app draws (the theme, dynamic colors, bitcoin's unit, the time format),
 * as one dependency of AppViewModel, whose constructor is at the size Koin's viewModelOf takes.
 */
data class DisplayPreferenceUseCases(
    val observeThemePreference: ObserveThemePreference,
    val observeDynamicColors: ObserveDynamicColors,
    val observeBitcoinInSats: ObserveBitcoinInSats,
    val observeTimeFormat: ObserveTimeFormatPreference,
)
