package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.CustomPalette
import com.quare.blitzsplit.core.expenses.domain.model.CustomPaletteFullException
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.AddCustomColorUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCustomColorRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AddCustomColorUseCaseTest {
    private lateinit var repository: FakeCustomColorRepository
    private lateinit var useCase: AddCustomColorUseCase

    private val blue = CustomColor(0x3A7BD5)

    @Test
    fun `GIVEN a palette with room WHEN adding a new color THEN saves it on the device at the current time`() =
        runTest {
            // Given
            prepareScenario(
                palette = listOf(CustomColor(0x1A2340)),
                now = 7_000L,
            )

            // When
            val result = useCase(blue)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf(blue to 7_000L), repository.addedAt)
            assertEquals(listOf(CustomColor(0x1A2340), blue), repository.colors.value)
        }

    @Test
    fun `GIVEN the color already in the palette WHEN adding it THEN succeeds without writing`() = runTest {
        // Given
        prepareScenario(palette = listOf(blue))

        // When
        val result = useCase(blue)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(repository.addedAt.isEmpty())
    }

    @Test
    fun `GIVEN a full palette that has the color WHEN adding it THEN succeeds without writing`() = runTest {
        // Given
        prepareScenario(palette = fullPalette().dropLast(1) + blue)

        // When
        val result = useCase(blue)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(repository.addedAt.isEmpty())
    }

    @Test
    fun `GIVEN a full palette WHEN adding a new color THEN fails as full and writes nothing`() = runTest {
        // Given
        prepareScenario(palette = fullPalette())

        // When
        val result = useCase(blue)

        // Then
        assertIs<CustomPaletteFullException>(result.exceptionOrNull())
        assertTrue(repository.addedAt.isEmpty())
    }

    @Test
    fun `GIVEN the device fails to save WHEN adding a color THEN fails`() = runTest {
        // Given
        prepareScenario(saveError = IllegalStateException("disk full"))

        // When
        val result = useCase(blue)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun fullPalette(): List<CustomColor> = List(CustomPalette.MAX_COLORS) { CustomColor(it + 1) }

    private fun prepareScenario(
        palette: List<CustomColor> = emptyList(),
        now: Long = 1_000L,
        saveError: Throwable? = null,
    ) {
        repository = FakeCustomColorRepository(initialColors = palette).also { it.saveError = saveError }
        useCase = AddCustomColorUseCase(repository) { now }
    }
}
