package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.sync.domain.usecase.impl.ReportAppForegroundUseCase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppForegroundTest {
    private lateinit var appForeground: AppForeground
    private lateinit var reportAppForeground: ReportAppForegroundUseCase

    @BeforeTest
    fun setUp() {
        appForeground = AppForeground()
        reportAppForeground = ReportAppForegroundUseCase(appForeground)
    }

    @Test
    fun `GIVEN the platform said nothing yet WHEN reading THEN the app is out of front`() {
        // When
        val isForeground = appForeground.observe().value

        // Then
        assertFalse(isForeground)
    }

    @Test
    fun `GIVEN the app out of front WHEN the platform says it came to the front THEN it is in front`() {
        // When
        appForeground.update(isForeground = true)

        // Then
        assertTrue(appForeground.observe().value)
    }

    @Test
    fun `GIVEN the app in front WHEN the platform says it left THEN it is out of front`() {
        // Given
        appForeground.update(isForeground = true)

        // When
        appForeground.update(isForeground = false)

        // Then
        assertFalse(appForeground.observe().value)
    }

    @Test
    fun `GIVEN the app out of front WHEN reporting it in front THEN the app is in front`() {
        // When
        reportAppForeground(isForeground = true)

        // Then
        assertTrue(appForeground.observe().value)
    }

    @Test
    fun `GIVEN the app in front WHEN reporting it in the background THEN the app is out of front`() {
        // Given
        reportAppForeground(isForeground = true)

        // When
        reportAppForeground(isForeground = false)

        // Then
        assertFalse(appForeground.observe().value)
    }
}
