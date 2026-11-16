package com.quare.blitzsplit.core.settings.domain.usecase

import com.quare.blitzsplit.core.model.TimeFormatPreference
import kotlinx.coroutines.flow.Flow

fun interface ObserveTimeFormatPreference {
    operator fun invoke(): Flow<TimeFormatPreference>
}
