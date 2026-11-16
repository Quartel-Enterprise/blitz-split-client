package com.quare.blitzsplit.core.settings.domain.usecase

import com.quare.blitzsplit.core.model.TimeFormatPreference

fun interface UpdateTimeFormatPreference {
    suspend operator fun invoke(timeFormat: TimeFormatPreference)
}
