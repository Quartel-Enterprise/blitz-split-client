package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.domain.repository.AppLanguageRepository
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateAppLanguage

class UpdateAppLanguageUseCase(
    private val repository: AppLanguageRepository,
) : UpdateAppLanguage {
    override fun invoke(language: AppLanguage) {
        repository.updateLanguage(language)
    }
}
