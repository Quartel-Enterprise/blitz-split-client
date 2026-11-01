package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatsChartCodesTest {
    @Test
    fun `GIVEN a stored grain code WHEN reading it THEN is its grain`() {
        // Given
        val code = "week"

        // When
        val grain = StatsGrain.fromCodeOrNull(code)

        // Then
        assertEquals(StatsGrain.WEEK, grain)
    }

    @Test
    fun `GIVEN a grain code this version does not know WHEN reading it THEN is null`() {
        // Given
        val code = "quarter"

        // When
        val grain = StatsGrain.fromCodeOrNull(code)

        // Then
        assertNull(grain)
    }

    @Test
    fun `GIVEN a stored chart code WHEN reading it THEN is its chart`() {
        // Given
        val code = "line"

        // When
        val chartType = StatsChartType.fromCodeOrNull(code)

        // Then
        assertEquals(StatsChartType.LINE, chartType)
    }

    @Test
    fun `GIVEN a chart code this version does not know WHEN reading it THEN is null`() {
        // Given
        val code = "pie"

        // When
        val chartType = StatsChartType.fromCodeOrNull(code)

        // Then
        assertNull(chartType)
    }
}
