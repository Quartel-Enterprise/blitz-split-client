package com.quare.blitzsplit.core.releasenotes.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppVersionTest {
    @Test
    fun `GIVEN the name of a stable version WHEN reading it THEN gives its three numbers and no beta`() {
        // Given
        val name = "3.1.0"

        // When
        val version = AppVersion.parseOrNull(name)

        // Then
        assertEquals(
            AppVersion(
                major = 3,
                minor = 1,
                patch = 0,
                beta = null,
            ),
            version,
        )
    }

    @Test
    fun `GIVEN the name of a beta WHEN reading it THEN gives its three numbers and the number of the beta`() {
        // Given
        val name = "3.1.0-beta_2"

        // When
        val version = AppVersion.parseOrNull(name)

        // Then
        assertEquals(
            AppVersion(
                major = 3,
                minor = 1,
                patch = 0,
                beta = 2,
            ),
            version,
        )
    }

    @Test
    fun `GIVEN a version with numbers of several digits WHEN reading it THEN gives each number whole`() {
        // Given
        val name = "12.34.56-beta_78"

        // When
        val version = AppVersion.parseOrNull(name)

        // Then
        assertEquals(
            AppVersion(
                major = 12,
                minor = 34,
                patch = 56,
                beta = 78,
            ),
            version,
        )
    }

    @Test
    fun `GIVEN names written any other way WHEN reading them THEN none is a version`() {
        // Given
        val names = listOf(
            "",
            " ",
            "3",
            "3.1",
            "3.1.0.0",
            "3.1.0-rc1",
            "3.1.0-beta",
            "3.1.0-beta_",
            "3.1.0-beta2",
            "3.1.0-beta_2x",
            "3.1.0-BETA_2",
            "3.1.0_beta_2",
            "v3.1.0",
            " 3.1.0",
            "3.1.0 ",
            "3.1.0\n",
            "3.1.x",
            "3..0",
            "-3.1.0",
            "3,1,0",
            "local",
        )

        // When
        val read = names.associateWith(AppVersion::parseOrNull)

        // Then
        assertEquals(emptyMap(), read.filterValues { it != null })
    }

    @Test
    fun `GIVEN a name whose number is too long to count WHEN reading it THEN it is not a version`() {
        // Given
        val names = listOf("3.1.99999999999999999999", "99999999999.1.0", "3.1.0-beta_99999999999")

        // When
        val read = names.associateWith(AppVersion::parseOrNull)

        // Then
        assertEquals(emptyMap(), read.filterValues { it != null })
    }

    @Test
    fun `GIVEN versions and betas out of order WHEN sorting them THEN each beta comes before its version`() {
        // Given
        val shuffled = listOf(
            "3.1.0",
            "3.0.1-beta_1",
            "2.9.9",
            "3.0.0-beta_10",
            "3.0.0",
            "3.1.0-beta_1",
            "3.0.0-beta_9",
            "10.0.0",
            "3.0.1",
            "3.0.0-beta_1",
        ).map { checkNotNull(AppVersion.parseOrNull(it)) }

        // When
        val sorted = shuffled.sorted()

        // Then
        assertEquals(
            listOf(
                "2.9.9",
                "3.0.0-beta_1",
                "3.0.0-beta_9",
                "3.0.0-beta_10",
                "3.0.0",
                "3.0.1-beta_1",
                "3.0.1",
                "3.1.0-beta_1",
                "3.1.0",
                "10.0.0",
            ),
            sorted.map(AppVersion::fullName),
        )
    }

    @Test
    fun `GIVEN the ninth and the tenth beta of a version WHEN comparing them THEN the ninth is older`() {
        // Given
        val ninth = AppVersion(3, 0, 0, beta = 9)
        val tenth = AppVersion(3, 0, 0, beta = 10)

        // When
        val isOlder = ninth < tenth

        // Then
        assertTrue(isOlder)
    }

    @Test
    fun `GIVEN a stable version and its highest beta WHEN comparing them THEN the stable one is newer`() {
        // Given
        val stable = AppVersion(3, 0, 0, beta = null)
        val beta = AppVersion(3, 0, 0, beta = 999_999_999)

        // When
        val isNewer = stable > beta

        // Then
        assertTrue(isNewer)
    }

    @Test
    fun `GIVEN a stable version and the first beta of the next patch WHEN comparing them THEN the stable is older`() {
        // Given
        val stable = AppVersion(3, 0, 0, beta = null)
        val nextBeta = AppVersion(3, 0, 1, beta = 1)

        // When
        val isOlder = stable < nextBeta

        // Then
        assertTrue(isOlder)
    }

    @Test
    fun `GIVEN the same beta read twice WHEN comparing them THEN neither is newer`() {
        // Given
        val first = AppVersion.parseOrNull("3.1.0-beta_2")
        val second = AppVersion(3, 1, 0, beta = 2)

        // When
        val comparison = first?.compareTo(second)

        // Then
        assertEquals(0, comparison)
    }

    @Test
    fun `GIVEN a stable version WHEN asking its names THEN both are the name as written and it is not a beta`() {
        // Given
        val version = AppVersion(3, 1, 0, beta = null)

        // When
        val names = version.name to version.fullName

        // Then
        assertEquals("3.1.0" to "3.1.0", names)
        assertFalse(version.isBeta)
    }

    @Test
    fun `GIVEN a beta WHEN asking its names THEN its name is its version and its full name the one written`() {
        // Given
        val version = AppVersion(3, 1, 0, beta = 2)

        // When
        val names = version.name to version.fullName

        // Then
        assertEquals("3.1.0" to "3.1.0-beta_2", names)
        assertTrue(version.isBeta)
    }
}
