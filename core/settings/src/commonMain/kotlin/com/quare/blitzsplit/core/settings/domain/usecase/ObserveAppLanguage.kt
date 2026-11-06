package com.quare.blitzsplit.core.settings.domain.usecase

import com.quare.blitzsplit.core.model.AppLanguage
import kotlinx.coroutines.flow.Flow

fun interface ObserveAppLanguage {
    operator fun invoke(): Flow<AppLanguage>
}
