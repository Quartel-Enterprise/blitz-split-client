package com.quare.blitzsplit.core.settings.data

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.settings.data.repository.AppLanguageRepositoryImpl
import com.quare.blitzsplit.core.settings.domain.FakeAppLanguageLocalDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageRepositoryImplTest {
    private lateinit var store: FakeAppLanguageLocalDataSource
    private lateinit var repository: AppLanguageRepositoryImpl
    private val observed = mutableListOf<AppLanguage>()

    @Test
    fun `GIVEN the repository just built WHEN nobody observes or refreshes THEN the platform language is not read`() =
        runTest {
            // Given
            prepareScenario(
                stored = AppLanguage.PORTUGUESE,
                observing = false,
            )

            // When
            val reads = store.readCount

            // Then
            assertEquals(0, reads)
        }

    @Test
    fun `GIVEN Portuguese kept by the platform WHEN observing the language THEN emits Portuguese`() = runTest {
        // Given
        prepareScenario(stored = AppLanguage.PORTUGUESE)

        // When
        val languages = observed.toList()

        // Then
        assertEquals(listOf(AppLanguage.PORTUGUESE), languages)
    }

    @Test
    fun `GIVEN no language picked on this device WHEN observing the language THEN follows the device`() = runTest {
        // Given
        prepareScenario(stored = AppLanguage.SYSTEM)

        // When
        val languages = observed.toList()

        // Then
        assertEquals(listOf(AppLanguage.SYSTEM), languages)
    }

    @Test
    fun `GIVEN the language being observed WHEN picking English THEN writes English to the platform`() = runTest {
        // Given
        prepareScenario(stored = AppLanguage.SYSTEM)

        // When
        repository.updateLanguage(AppLanguage.ENGLISH)

        // Then
        assertEquals(listOf(AppLanguage.ENGLISH), store.writes)
        assertEquals(AppLanguage.ENGLISH, store.stored)
    }

    @Test
    fun `GIVEN the language being observed WHEN picking English THEN observers see English`() = runTest {
        // Given
        prepareScenario(stored = AppLanguage.SYSTEM)

        // When
        repository.updateLanguage(AppLanguage.ENGLISH)

        // Then
        assertEquals(listOf(AppLanguage.SYSTEM, AppLanguage.ENGLISH), observed)
    }

    @Test
    fun `GIVEN English picked WHEN going back to the device language THEN writes it and observers follow the device`() =
        runTest {
            // Given
            prepareScenario(stored = AppLanguage.ENGLISH)

            // When
            repository.updateLanguage(AppLanguage.SYSTEM)

            // Then
            assertEquals(listOf(AppLanguage.SYSTEM), store.writes)
            assertEquals(listOf(AppLanguage.ENGLISH, AppLanguage.SYSTEM), observed)
        }

    @Test
    fun `GIVEN nobody observing yet WHEN picking Portuguese THEN writes it and the first observer sees Portuguese`() =
        runTest {
            // Given
            prepareScenario(
                stored = AppLanguage.SYSTEM,
                observing = false,
            )

            // When
            repository.updateLanguage(AppLanguage.PORTUGUESE)

            // Then
            assertEquals(listOf(AppLanguage.PORTUGUESE), store.writes)
            assertEquals(AppLanguage.PORTUGUESE, repository.observeLanguage().first())
        }

    @Test
    fun `GIVEN Portuguese being observed WHEN picking Portuguese again THEN observers are not notified again`() =
        runTest {
            // Given
            prepareScenario(stored = AppLanguage.PORTUGUESE)

            // When
            repository.updateLanguage(AppLanguage.PORTUGUESE)

            // Then
            assertEquals(listOf(AppLanguage.PORTUGUESE), observed)
        }

    @Test
    fun `GIVEN Portuguese observed WHEN the platform switches to English unrefreshed THEN observers keep Portuguese`() =
        runTest {
            // Given
            prepareScenario(stored = AppLanguage.PORTUGUESE)

            // When
            store.stored = AppLanguage.ENGLISH

            // Then
            assertEquals(listOf(AppLanguage.PORTUGUESE), observed)
        }

    @Test
    fun `GIVEN the platform settings switched Portuguese to English WHEN refreshing THEN observers see English`() =
        runTest {
            // Given
            prepareScenario(stored = AppLanguage.PORTUGUESE)
            store.stored = AppLanguage.ENGLISH

            // When
            repository.refresh()

            // Then
            assertEquals(listOf(AppLanguage.PORTUGUESE, AppLanguage.ENGLISH), observed)
        }

    @Test
    fun `GIVEN the platform still on Portuguese WHEN refreshing THEN observers are not notified again`() = runTest {
        // Given
        prepareScenario(stored = AppLanguage.PORTUGUESE)

        // When
        repository.refresh()

        // Then
        assertEquals(listOf(AppLanguage.PORTUGUESE), observed)
        assertEquals(emptyList(), store.writes)
    }

    @Test
    fun `GIVEN nobody observing and the platform on Portuguese WHEN refreshing THEN a new observer sees Portuguese`() =
        runTest {
            // Given
            prepareScenario(
                stored = AppLanguage.SYSTEM,
                observing = false,
            )
            store.stored = AppLanguage.PORTUGUESE

            // When
            repository.refresh()

            // Then
            assertEquals(AppLanguage.PORTUGUESE, repository.observeLanguage().first())
            assertEquals(emptyList(), store.writes)
        }

    private fun TestScope.prepareScenario(
        stored: AppLanguage,
        observing: Boolean = true,
    ) {
        store = FakeAppLanguageLocalDataSource(stored)
        repository = AppLanguageRepositoryImpl(store)
        if (observing) {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeLanguage().collect(observed::add)
            }
        }
    }
}
