package com.quare.blitzsplit.core.settings.domain

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.data.repository.AppLanguageRepositoryImpl
import com.quare.blitzsplit.core.settings.domain.usecase.impl.ObserveAppLanguageUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.RefreshAppLanguageUseCase
import com.quare.blitzsplit.core.settings.domain.usecase.impl.UpdateAppLanguageUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageUseCasesTest {
    private lateinit var store: FakeAppLanguageStore
    private lateinit var observeAppLanguage: ObserveAppLanguageUseCase
    private lateinit var updateAppLanguage: UpdateAppLanguageUseCase
    private lateinit var refreshAppLanguage: RefreshAppLanguageUseCase
    private val observed = mutableListOf<AppLanguage>()

    @Test
    fun `GIVEN Portuguese kept by the platform WHEN observing the app language THEN emits Portuguese`() = runTest {
        // Given
        prepareScenario(stored = AppLanguage.PORTUGUESE)

        // When
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeAppLanguage().collect(observed::add)
        }

        // Then
        assertEquals(listOf(AppLanguage.PORTUGUESE), observed)
    }

    @Test
    fun `GIVEN the app language observed WHEN picking English THEN the platform keeps it and observers see it`() =
        runTest {
            // Given
            prepareScenario(stored = AppLanguage.SYSTEM)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                observeAppLanguage().collect(observed::add)
            }

            // When
            updateAppLanguage(AppLanguage.ENGLISH)

            // Then
            assertEquals(listOf(AppLanguage.ENGLISH), store.writes)
            assertEquals(listOf(AppLanguage.SYSTEM, AppLanguage.ENGLISH), observed)
        }

    @Test
    fun `GIVEN the platform settings switched to Portuguese WHEN refreshing THEN observers see Portuguese`() = runTest {
        // Given
        prepareScenario(stored = AppLanguage.ENGLISH)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeAppLanguage().collect(observed::add)
        }
        store.stored = AppLanguage.PORTUGUESE

        // When
        refreshAppLanguage()

        // Then
        assertEquals(listOf(AppLanguage.ENGLISH, AppLanguage.PORTUGUESE), observed)
        assertEquals(emptyList(), store.writes)
    }

    private fun prepareScenario(stored: AppLanguage) {
        store = FakeAppLanguageStore(stored)
        val repository = AppLanguageRepositoryImpl(store)
        observeAppLanguage = ObserveAppLanguageUseCase(repository)
        updateAppLanguage = UpdateAppLanguageUseCase(repository)
        refreshAppLanguage = RefreshAppLanguageUseCase(repository)
    }
}
