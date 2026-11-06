package com.quare.blitzsplit.core.settings.data.repository

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.data.datasource.AppLanguageLocalDataSource
import com.quare.blitzsplit.core.settings.domain.repository.AppLanguageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class AppLanguageRepositoryImpl(
    private val localDataSource: AppLanguageLocalDataSource,
) : AppLanguageRepository {
    // Why: read from the platform once asked for, so building the graph never touches its storage.
    private val language: MutableStateFlow<AppLanguage> by lazy { MutableStateFlow(localDataSource.read()) }

    override fun observeLanguage(): Flow<AppLanguage> = language

    override fun updateLanguage(language: AppLanguage) {
        localDataSource.write(language)
        this.language.value = language
    }

    override fun refresh() {
        language.value = localDataSource.read()
    }
}
