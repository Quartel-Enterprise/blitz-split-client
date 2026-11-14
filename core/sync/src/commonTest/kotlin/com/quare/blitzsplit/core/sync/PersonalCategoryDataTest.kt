package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCustomColorRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersonalCategoryDataTest {
    private lateinit var terms: FakeCategoryTermRepository
    private lateinit var colors: FakeCustomColorRepository
    private lateinit var personalCategoryData: PersonalCategoryData
    private val changeEmissions = mutableListOf<List<Any>>()

    @Test
    fun `GIVEN nothing failing WHEN syncing THEN the map and the palette come down and go up under the user id`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val result = personalCategoryData.sync(USER_ID)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(USER_ID), terms.downloadedUserIds)
            assertEquals(listOf(USER_ID), terms.uploadedUserIds)
            assertEquals(listOf(USER_ID), colors.downloadedUserIds)
            assertEquals(listOf(USER_ID), colors.uploadedUserIds)
        }

    @Test
    fun `GIVEN a term and a custom color changed on the device WHEN syncing THEN neither is pending anymore`() =
        runTest {
            // Given
            prepareScenario()
            terms.saveTerm(
                term = "pizzaria",
                categoryId = "food.restaurant",
                updatedAt = 1L,
            )
            colors.add(teal, updatedAt = 1L)

            // When
            personalCategoryData.sync(USER_ID)

            // Then
            assertFalse(personalCategoryData.observeHasPendingChanges().first())
        }

    @Test
    fun `GIVEN the map fails to come down WHEN syncing THEN the palette still syncs and the sync fails`() = runTest {
        // Given
        prepareScenario(mapPullError = mapError)

        // When
        val result = personalCategoryData.sync(USER_ID)

        // Then
        assertEquals(mapError.message, result.exceptionOrNull()?.message)
        assertEquals(listOf(USER_ID), colors.downloadedUserIds)
        assertEquals(listOf(USER_ID), colors.uploadedUserIds)
    }

    @Test
    fun `GIVEN the map fails to come down WHEN syncing THEN the map does not go up`() = runTest {
        // Given
        prepareScenario(mapPullError = mapError)

        // When
        personalCategoryData.sync(USER_ID)

        // Then
        assertTrue(terms.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the map fails to go up WHEN syncing THEN the palette still syncs and the sync fails`() = runTest {
        // Given
        prepareScenario(mapPushError = mapError)

        // When
        val result = personalCategoryData.sync(USER_ID)

        // Then
        assertEquals(mapError.message, result.exceptionOrNull()?.message)
        assertEquals(listOf(USER_ID), colors.uploadedUserIds)
    }

    @Test
    fun `GIVEN the palette fails to come down WHEN syncing THEN the map still syncs and the sync fails`() = runTest {
        // Given
        prepareScenario(palettePullError = paletteError)

        // When
        val result = personalCategoryData.sync(USER_ID)

        // Then
        assertEquals(paletteError.message, result.exceptionOrNull()?.message)
        assertEquals(listOf(USER_ID), terms.downloadedUserIds)
        assertEquals(listOf(USER_ID), terms.uploadedUserIds)
        assertTrue(colors.uploadedUserIds.isEmpty())
    }

    @Test
    fun `GIVEN the palette fails to go up WHEN syncing THEN the map still syncs and the sync fails`() = runTest {
        // Given
        prepareScenario(palettePushError = paletteError)

        // When
        val result = personalCategoryData.sync(USER_ID)

        // Then
        assertEquals(paletteError.message, result.exceptionOrNull()?.message)
        assertEquals(listOf(USER_ID), terms.uploadedUserIds)
    }

    @Test
    fun `GIVEN both the map and the palette fail WHEN syncing THEN the failure of the map is reported`() = runTest {
        // Given
        prepareScenario(
            mapPushError = mapError,
            palettePushError = paletteError,
        )

        // When
        val result = personalCategoryData.sync(USER_ID)

        // Then
        assertEquals(mapError.message, result.exceptionOrNull()?.message)
    }

    @Test
    fun `GIVEN a map and a palette WHEN clearing THEN both are erased`() = runTest {
        // Given
        prepareScenario()
        terms.saveTerm(
            term = "pizzaria",
            categoryId = "food.restaurant",
            updatedAt = 1L,
        )
        colors.add(teal, updatedAt = 1L)

        // When
        personalCategoryData.clear()

        // Then
        assertTrue(terms.isCleared)
        assertTrue(terms.changes.value.isEmpty())
        assertTrue(colors.isCleared)
        assertTrue(colors.colors.value.isEmpty())
    }

    @Test
    fun `GIVEN the map fails to clear WHEN clearing THEN the palette is still erased and the failure surfaces`() =
        runTest {
            // Given
            prepareScenario(mapClearError = mapError)
            colors.add(teal, updatedAt = 1L)

            // When
            val result = runCatching { personalCategoryData.clear() }

            // Then
            assertEquals(mapError, result.exceptionOrNull())
            assertTrue(colors.isCleared)
            assertTrue(colors.colors.value.isEmpty())
        }

    @Test
    fun `GIVEN the palette fails to clear WHEN clearing THEN the map is still erased and the failure surfaces`() =
        runTest {
            // Given
            prepareScenario(paletteClearError = paletteError)
            terms.saveTerm(
                term = "pizzaria",
                categoryId = "food.restaurant",
                updatedAt = 1L,
            )

            // When
            val result = runCatching { personalCategoryData.clear() }

            // Then
            assertEquals(paletteError, result.exceptionOrNull())
            assertTrue(terms.isCleared)
            assertTrue(terms.changes.value.isEmpty())
        }

    @Test
    fun `GIVEN nothing changed on the device WHEN observing pending changes THEN there are none`() = runTest {
        // Given
        prepareScenario()

        // When
        val hasPending = personalCategoryData.observeHasPendingChanges().first()

        // Then
        assertFalse(hasPending)
    }

    @Test
    fun `GIVEN only a term waiting to go up WHEN observing pending changes THEN there are some`() = runTest {
        // Given
        prepareScenario()
        terms.saveTerm(
            term = "pizzaria",
            categoryId = "food.restaurant",
            updatedAt = 1L,
        )

        // When
        val hasPending = personalCategoryData.observeHasPendingChanges().first()

        // Then
        assertTrue(hasPending)
    }

    @Test
    fun `GIVEN only a custom color waiting to go up WHEN observing pending changes THEN there are some`() = runTest {
        // Given
        prepareScenario()
        colors.add(teal, updatedAt = 1L)

        // When
        val hasPending = personalCategoryData.observeHasPendingChanges().first()

        // Then
        assertTrue(hasPending)
    }

    @Test
    fun `GIVEN the changes observed WHEN a term changes THEN they are emitted again`() = runTest {
        // Given
        prepareScenario()
        val emissionsBefore = changeEmissions.size

        // When
        terms.saveTerm(
            term = "pizzaria",
            categoryId = "food.restaurant",
            updatedAt = 1L,
        )
        runCurrent()

        // Then
        assertEquals(emissionsBefore + 1, changeEmissions.size)
    }

    @Test
    fun `GIVEN the changes observed WHEN a custom color is added THEN they are emitted again`() = runTest {
        // Given
        prepareScenario()
        val emissionsBefore = changeEmissions.size

        // When
        colors.add(teal, updatedAt = 1L)
        runCurrent()

        // Then
        assertEquals(emissionsBefore + 1, changeEmissions.size)
    }

    private val teal = CustomColor(0x1ABC9C)
    private val mapError = IllegalStateException("the map failed")
    private val paletteError = IllegalStateException("the palette failed")

    private fun TestScope.prepareScenario(
        mapPullError: Throwable? = null,
        mapPushError: Throwable? = null,
        mapClearError: Throwable? = null,
        palettePullError: Throwable? = null,
        palettePushError: Throwable? = null,
        paletteClearError: Throwable? = null,
    ) {
        terms = FakeCategoryTermRepository().also {
            it.pullError = mapPullError
            it.pushError = mapPushError
            it.clearError = mapClearError
        }
        colors = FakeCustomColorRepository().also {
            it.pullError = palettePullError
            it.pushError = palettePushError
            it.clearError = paletteClearError
        }
        personalCategoryData = PersonalCategoryData(terms, colors)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            personalCategoryData.observeChanges().collect(changeEmissions::add)
        }
    }

    private companion object {
        const val USER_ID = "user-1"
    }
}
