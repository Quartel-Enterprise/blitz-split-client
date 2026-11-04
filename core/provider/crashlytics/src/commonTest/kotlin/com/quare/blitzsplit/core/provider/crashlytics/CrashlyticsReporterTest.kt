package com.quare.blitzsplit.core.provider.crashlytics

import co.touchlab.crashkios.crashlytics.CrashlyticsCalls
import co.touchlab.crashkios.crashlytics.CrashlyticsKotlin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CrashlyticsReporterTest {
    private val handled = mutableListOf<Throwable>()
    private val previous = CrashlyticsKotlin.implementation

    @AfterTest
    fun restore() {
        CrashlyticsKotlin.implementation = previous
    }

    @Test
    fun `GIVEN a failure WHEN recording it THEN sends only its classes cause by cause`() {
        // Given
        CrashlyticsKotlin.implementation = RecordingCalls(handled)
        val failure = IllegalStateException("Failing row contains (Jantar, 31200)", IllegalArgumentException("Ana"))

        // When
        CrashlyticsReporter().recordException(failure)

        // Then
        assertEquals(listOf("IllegalStateException ← IllegalArgumentException"), handled.map { it.message })
    }

    @Test
    fun `GIVEN a failure written to be reported WHEN recording it THEN sends it as it is`() {
        // Given
        CrashlyticsKotlin.implementation = RecordingCalls(handled)
        val failure = WrittenFailure()

        // When
        CrashlyticsReporter().recordException(failure)

        // Then
        assertEquals(listOf<Throwable>(failure), handled)
    }

    private class WrittenFailure :
        Exception("PostgrestRestException 400 23514"),
        ReportableFailure

    private class RecordingCalls(
        private val handled: MutableList<Throwable>,
    ) : CrashlyticsCalls {
        override fun logMessage(message: String) {}

        override fun sendHandledException(throwable: Throwable) {
            handled += throwable
        }

        override fun sendFatalException(throwable: Throwable) {}

        override fun setCustomValue(
            key: String,
            value: Any,
        ) {}

        override fun setUserId(identifier: String) {}

        override fun setCollectionEnabled(enabled: Boolean) {}
    }
}
