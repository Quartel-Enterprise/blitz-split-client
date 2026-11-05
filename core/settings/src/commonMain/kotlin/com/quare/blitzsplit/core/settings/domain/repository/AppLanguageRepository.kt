package com.quare.blitzsplit.core.settings.domain.repository

import com.quare.blitzsplit.core.model.AppLanguage
import kotlinx.coroutines.flow.Flow

interface AppLanguageRepository {
    fun observeLanguage(): Flow<AppLanguage>

    fun updateLanguage(language: AppLanguage)

    // Why: the platform's own settings changed it while the app ran (Android's per-app language).
    fun refresh()
}
