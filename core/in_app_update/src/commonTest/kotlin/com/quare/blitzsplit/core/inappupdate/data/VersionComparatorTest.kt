package com.quare.blitzsplit.core.inappupdate.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionComparatorTest {
    @Test
    fun `GIVEN a two digit minor version WHEN comparing with a one digit one THEN the two digit one is newer`() {
        // Given
        val store = "2.10.0"
        val installed = "2.9.1"

        // When
        val comparison = VersionComparator.compare(store, installed)

        // Then
        assertTrue(comparison > 0)
    }

    @Test
    fun `GIVEN an older version WHEN comparing with a newer one THEN the older one comes first`() {
        // Given
        val store = "1.9.9"
        val installed = "2.0.0"

        // When
        val comparison = VersionComparator.compare(store, installed)

        // Then
        assertTrue(comparison < 0)
    }

    @Test
    fun `GIVEN a two digit major version WHEN comparing with a one digit one THEN the two digit one is newer`() {
        // Given
        val store = "10.0"
        val installed = "9.0"

        // When
        val comparison = VersionComparator.compare(store, installed)

        // Then
        assertTrue(comparison > 0)
    }

    @Test
    fun `GIVEN the same version WHEN comparing THEN they are equal`() {
        // Given
        val store = "2.1.0"
        val installed = "2.1.0"

        // When
        val comparison = VersionComparator.compare(store, installed)

        // Then
        assertEquals(0, comparison)
    }

    @Test
    fun `GIVEN a version missing its patch WHEN comparing with the same version with patch 0 THEN they are equal`() {
        // Given
        val store = "2.1"
        val installed = "2.1.0"

        // When
        val comparison = VersionComparator.compare(store, installed)

        // Then
        assertEquals(0, comparison)
    }

    @Test
    fun `GIVEN a version with a patch WHEN comparing with it missing THEN the patched one is newer`() {
        // Given
        val store = "2.1.1"
        val installed = "2.1"

        // When
        val comparison = VersionComparator.compare(store, installed)

        // Then
        assertTrue(comparison > 0)
    }

    @Test
    fun `GIVEN a version with a non numeric part WHEN comparing THEN that part counts as 0`() {
        // Given
        val store = "2.beta.1"
        val installed = "2.0.1"

        // When
        val comparison = VersionComparator.compare(store, installed)

        // Then
        assertEquals(0, comparison)
    }

    @Test
    fun `GIVEN a non numeric part against a numeric one WHEN comparing THEN the numeric one is newer`() {
        // Given
        val store = "2.x"
        val installed = "2.1"

        // When
        val comparison = VersionComparator.compare(store, installed)

        // Then
        assertTrue(comparison < 0)
    }

    @Test
    fun `GIVEN a beta of a newer patch WHEN comparing with an older store version THEN the beta is newer`() {
        // Given
        val store = "3.0.1"
        val installed = "3.0.2-beta_1"

        // When
        val comparison = VersionComparator.compare(store, installed)

        // Then
        assertTrue(comparison < 0)
    }

    @Test
    fun `GIVEN a release WHEN comparing with a beta of the same version THEN the release is newer`() {
        // Given
        val store = "3.0.2"
        val installed = "3.0.2-beta_3"

        // When
        val comparison = VersionComparator.compare(store, installed)

        // Then
        assertTrue(comparison > 0)
    }
}
