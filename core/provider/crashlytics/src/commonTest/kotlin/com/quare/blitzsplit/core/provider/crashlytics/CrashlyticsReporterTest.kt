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
    fun `GIVEN crash reporting started WHEN recording an exception THEN sends it as handled`() {
        // Given
        CrashlyticsKotlin.implementation = RecordingCalls(handled)
        val failure = IllegalStateException("server down")

        // When
        CrashlyticsReporter().recordException(failure)

        // Then
        assertEquals(listOf<Throwable>(failure), handled)
    }

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
