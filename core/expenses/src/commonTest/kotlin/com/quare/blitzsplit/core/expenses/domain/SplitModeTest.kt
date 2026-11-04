package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.DefaultSplitMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SplitModeTest {
    @Test
    fun `GIVEN Igual Partes and Porcentagem WHEN asking the group default they make THEN each makes its own`() {
        // Given
        val modes = listOf(SplitMode.EQUAL, SplitMode.SHARES, SplitMode.PERCENT)

        // When
        val defaults = modes.map { it.defaultMode }

        // Then
        assertEquals(listOf(DefaultSplitMode.EQUAL, DefaultSplitMode.SHARES, DefaultSplitMode.PERCENT), defaults)
    }

    @Test
    fun `GIVEN Valores and Itens WHEN asking the group default they make THEN they make none`() {
        // Given
        val modes = listOf(SplitMode.AMOUNT, SplitMode.ITEMS)

        // When
        val defaults = modes.map { it.defaultMode }

        // Then
        defaults.forEach { assertNull(it) }
    }
}
