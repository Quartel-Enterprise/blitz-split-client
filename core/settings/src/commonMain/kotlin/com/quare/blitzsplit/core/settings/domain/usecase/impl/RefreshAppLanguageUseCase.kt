package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.AppLanguageRepository
import com.quare.blitzsplit.core.settings.domain.usecase.RefreshAppLanguage

class RefreshAppLanguageUseCase(
    private val repository: AppLanguageRepository,
) : RefreshAppLanguage {
    override fun invoke() {
        repository.refresh()
    }
}
