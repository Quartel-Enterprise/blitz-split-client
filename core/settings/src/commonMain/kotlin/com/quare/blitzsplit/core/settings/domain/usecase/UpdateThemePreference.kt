package com.quare.blitzsplit.core.settings.domain.usecase

import com.quare.blitzsplit.core.model.ThemePreference

fun interface UpdateThemePreference {
    suspend operator fun invoke(theme: ThemePreference)
}
