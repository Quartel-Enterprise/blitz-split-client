package com.quare.blitzsplit.core.provider.crashlytics

import co.touchlab.crashkios.crashlytics.CrashlyticsKotlin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebCrashReporterTest {
    private val reported = mutableListOf<Throwable>()
    private val previous = CrashlyticsKotlin.implementation

    @AfterTest
    fun restore() {
        CrashlyticsKotlin.implementation = previous
    }

    @Test
    fun `GIVEN a failure with a message WHEN recording it THEN the stack handed over has frames and no message`() {
        // Given
        attachWebCrashReporter { reported += it }
        val failure = IllegalStateException("Failing row contains (Jantar, 31200)")

        // When
        CrashlyticsReporter().recordException(failure)

        // Then
        val stack = reported.single().asDynamic().stack as String
        assertFalse("Jantar" in stack)
        assertTrue(stack.isNotBlank())
    }

    @Test
    fun `GIVEN a stack that starts with the message of the failure WHEN adopting it THEN keeps only the frames`() {
        // Given
        val original = IllegalStateException("Failing row")
        original.asDynamic().stack = "IllegalStateException: Failing row\ncontains (Jantar, 31200)\n" +
            "    at sync (app.js:1:2)\n    at run (app.js:3:4)"
        val adopter = Exception("adopter")

        // When
        adopter.adoptStackTrace(from = original)

        // Then
        assertEquals("    at sync (app.js:1:2)\n    at run (app.js:3:4)", adopter.asDynamic().stack)
    }

    @Test
    fun `GIVEN a stack of frames only WHEN adopting it THEN takes it as it is`() {
        // Given
        val original = IllegalStateException("Failing row")
        original.asDynamic().stack = "sync@app.js:1:2\nrun@app.js:3:4"
        val adopter = Exception("adopter")

        // When
        adopter.adoptStackTrace(from = original)

        // Then
        assertEquals("sync@app.js:1:2\nrun@app.js:3:4", adopter.asDynamic().stack)
    }

    @Test
    fun `GIVEN a stack that is only the name and the message WHEN adopting it THEN takes nothing`() {
        // Given
        val original = IllegalStateException("Failing row contains (Jantar, 31200)")
        original.asDynamic().stack = "IllegalStateException: Failing row contains (Jantar, 31200)"
        val adopter = Exception("adopter")

        // When
        adopter.adoptStackTrace(from = original)

        // Then
        assertEquals("", adopter.asDynamic().stack)
    }

    @Test
    fun `GIVEN a message with a line that starts like a frame WHEN adopting the stack THEN keeps only the frames`() {
        // Given
        val original = IllegalStateException("Failing row\nat least one member (Ana)")
        original.asDynamic().stack = "IllegalStateException: Failing row\nat least one member (Ana)\n" +
            "    at sync (app.js:1:2)"
        val adopter = Exception("adopter")

        // When
        adopter.adoptStackTrace(from = original)

        // Then
        assertEquals("    at sync (app.js:1:2)", adopter.asDynamic().stack)
    }

    @Test
    fun `GIVEN a Kotlin failure WHEN asking for its class name THEN gives the name of its class`() {
        // Given
        val failure = IllegalStateException("Failing row")

        // When
        val name = findKotlinClassNameOf(failure)

        // Then
        assertEquals("IllegalStateException", name)
    }

    @Test
    fun `GIVEN an error Kotlin did not define WHEN asking for its class name THEN gives none`() {
        // Given
        val error = js("new TypeError('x')").unsafeCast<Throwable>()

        // When
        val name = findKotlinClassNameOf(error)

        // Then
        assertNull(name)
    }
}
