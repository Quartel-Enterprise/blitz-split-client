package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillCommentRules
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BillCommentRulesTest {
    @Test
    fun `GIVEN an empty comment WHEN checking it THEN it is invalid`() {
        // Given
        val body = ""

        // When
        val isValid = BillCommentRules.isBodyValid(body)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN a comment of only spaces and line breaks WHEN checking it THEN it is invalid`() {
        // Given
        val body = "  \n\t  "

        // When
        val isValid = BillCommentRules.isBodyValid(body)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN a comment of one letter WHEN checking it THEN it is valid`() {
        // Given
        val body = "a"

        // When
        val isValid = BillCommentRules.isBodyValid(body)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `GIVEN a comment of 1000 letters WHEN checking it THEN it is valid`() {
        // Given
        val body = "a".repeat(1_000)

        // When
        val isValid = BillCommentRules.isBodyValid(body)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `GIVEN a comment of 1001 letters WHEN checking it THEN it is invalid`() {
        // Given
        val body = "a".repeat(1_001)

        // When
        val isValid = BillCommentRules.isBodyValid(body)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN a comment of 1000 letters between spaces WHEN checking it THEN the spaces do not count`() {
        // Given
        val body = "   " + "a".repeat(1_000) + "\n  "

        // When
        val isValid = BillCommentRules.isBodyValid(body)

        // Then
        assertTrue(isValid)
    }
}
