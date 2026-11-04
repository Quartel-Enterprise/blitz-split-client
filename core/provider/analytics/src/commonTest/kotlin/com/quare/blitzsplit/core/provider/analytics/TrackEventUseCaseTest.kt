package com.quare.blitzsplit.core.provider.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackEventUseCaseTest {
    private val logged = mutableListOf<Pair<String, Map<String, Any>>>()
    private val hub = AnalyticsHub()
    private val trackEvent = TrackEventUseCase(hub)

    @Test
    fun `GIVEN no service attached WHEN tracking THEN the event goes nowhere`() {
        // Given
        AnalyticsHub().attach(RecordingService(logged))

        // When
        trackEvent("bill_saved", mapOf("is_new" to true))

        // Then
        assertTrue(logged.isEmpty())
    }

    @Test
    fun `GIVEN a service attached WHEN tracking THEN sends booleans as text and whole numbers as Long`() {
        // Given
        hub.attach(RecordingService(logged))

        // When
        trackEvent("bill_saved", mapOf("is_new" to true, "people" to 3, "split" to "equal"))

        // Then
        assertEquals(
            listOf("bill_saved" to mapOf<String, Any>("is_new" to "true", "people" to 3L, "split" to "equal")),
            logged,
        )
    }

    @Test
    fun `GIVEN a service attached WHEN tracking a Float THEN sends it as a Double`() {
        // Given
        hub.attach(RecordingService(logged))

        // When
        trackEvent("stats_opened", mapOf("ratio" to 0.5f))

        // Then
        assertEquals(listOf("stats_opened" to mapOf<String, Any>("ratio" to 0.5)), logged)
    }

    private class RecordingService(
        private val logged: MutableList<Pair<String, Map<String, Any>>>,
    ) : AnalyticsService {
        override fun logEvent(
            name: String,
            params: Map<String, Any>,
        ) {
            logged += name to params
        }
    }
}
