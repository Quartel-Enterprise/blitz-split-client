package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BillLinkVisibilityTest {
    @Test
    fun `GIVEN the code members WHEN reading it THEN only who is in the bill opens the link`() {
        // Given
        val code = "members"

        // When
        val visibility = BillLinkVisibility.fromCodeOrNull(code)

        // Then
        assertEquals(BillLinkVisibility.MEMBERS, visibility)
    }

    @Test
    fun `GIVEN the code anyone WHEN reading it THEN anyone with the link opens it`() {
        // Given
        val code = "anyone"

        // When
        val visibility = BillLinkVisibility.fromCodeOrNull(code)

        // Then
        assertEquals(BillLinkVisibility.ANYONE, visibility)
    }

    @Test
    fun `GIVEN no code WHEN reading it THEN there is no visibility`() {
        // Given
        val code: String? = null

        // When
        val visibility = BillLinkVisibility.fromCodeOrNull(code)

        // Then
        assertNull(visibility)
    }

    @Test
    fun `GIVEN a code this version does not know WHEN reading it THEN there is no visibility`() {
        // Given
        val code = "public"

        // When
        val visibility = BillLinkVisibility.fromCodeOrNull(code)

        // Then
        assertNull(visibility)
    }

    @Test
    fun `GIVEN a known code in upper case WHEN reading it THEN there is no visibility`() {
        // Given
        val code = "MEMBERS"

        // When
        val visibility = BillLinkVisibility.fromCodeOrNull(code)

        // Then
        assertNull(visibility)
    }
}
