package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteCustomColorUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCustomColorRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteCustomColorUseCaseTest {
    private val blue = CustomColor(0x3A7BD5)
    private val yellow = CustomColor(0xFFF59D)
    private val repository = FakeCustomColorRepository(initialColors = listOf(blue, yellow))
    private val useCase = DeleteCustomColorUseCase(repository) { 9_000L }

    @Test
    fun `GIVEN a color in the palette WHEN deleting it THEN it leaves the palette at the current time`() = runTest {
        // Given
        val color = blue

        // When
        val result = useCase(color)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(blue to 9_000L), repository.deletedAt)
        assertEquals(listOf(yellow), repository.colors.value)
    }
}
