package com.quare.blitzsplit.core.settings.domain.usecase

import com.quare.blitzsplit.core.model.ThemePreference
import kotlinx.coroutines.flow.Flow

fun interface ObserveThemePreference {
    operator fun invoke(): Flow<ThemePreference>
}
