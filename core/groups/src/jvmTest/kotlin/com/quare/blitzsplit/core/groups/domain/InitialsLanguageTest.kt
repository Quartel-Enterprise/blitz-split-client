package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.Initials
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InitialsLanguageTest {
    private val deviceLocale = Locale.getDefault()

    @BeforeTest
    fun setUp() {
        Locale.setDefault(Locale.forLanguageTag("tr"))
    }

    @AfterTest
    fun tearDown() {
        Locale.setDefault(deviceLocale)
    }

    @Test
    fun `GIVEN the app in Turkish WHEN taking the initials of a name in lower case THEN capitalizes i with its dot`() {
        // Given
        val name = "ilker ırmak"

        // When
        val initials = Initials.of(name)

        // Then
        assertEquals("İI", initials)
    }

    @Test
    fun `GIVEN the app in Turkish WHEN taking the first letter of a lower case name THEN capitalizes i with its dot`() {
        // Given
        val name = "ilker"

        // When
        val initial = Initials.firstOf(name)

        // Then
        assertEquals("İ", initial)
    }
}
