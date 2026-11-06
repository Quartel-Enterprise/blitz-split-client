package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.domain.repository.AppLanguageRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveAppLanguage
import kotlinx.coroutines.flow.Flow

class ObserveAppLanguageUseCase(
    private val repository: AppLanguageRepository,
) : ObserveAppLanguage {
    override fun invoke(): Flow<AppLanguage> = repository.observeLanguage()
}
