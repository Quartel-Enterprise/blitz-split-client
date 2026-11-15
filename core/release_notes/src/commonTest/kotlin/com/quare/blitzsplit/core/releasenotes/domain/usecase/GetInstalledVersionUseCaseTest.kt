package com.quare.blitzsplit.core.releasenotes.domain.usecase

import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion
import com.quare.blitzsplit.core.releasenotes.domain.usecase.impl.GetInstalledVersionUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetInstalledVersionUseCaseTest {
    private lateinit var getInstalledVersion: GetInstalledVersionUseCase

    @Test
    fun `GIVEN a stable build WHEN asking the installed version THEN reads its version name`() {
        // Given
        prepareScenario(versionName = "3.1.0")

        // When
        val installed = getInstalledVersion()

        // Then
        assertEquals(AppVersion(3, 1, 0, beta = null), installed)
    }

    @Test
    fun `GIVEN a beta build WHEN asking the installed version THEN reads its version name with the beta`() {
        // Given
        prepareScenario(versionName = "3.1.0-beta_2")

        // When
        val installed = getInstalledVersion()

        // Then
        assertEquals(AppVersion(3, 1, 0, beta = 2), installed)
    }

    @Test
    fun `GIVEN a local build named another way WHEN asking the installed version THEN there is none`() {
        // Given
        prepareScenario(versionName = "3.1.0-SNAPSHOT")

        // When
        val installed = getInstalledVersion()

        // Then
        assertNull(installed)
    }

    @Test
    fun `GIVEN a build without a version name WHEN asking the installed version THEN there is none`() {
        // Given
        prepareScenario(versionName = "")

        // When
        val installed = getInstalledVersion()

        // Then
        assertNull(installed)
    }

    private fun prepareScenario(versionName: String) {
        getInstalledVersion = GetInstalledVersionUseCase(versionName = versionName)
    }
}
