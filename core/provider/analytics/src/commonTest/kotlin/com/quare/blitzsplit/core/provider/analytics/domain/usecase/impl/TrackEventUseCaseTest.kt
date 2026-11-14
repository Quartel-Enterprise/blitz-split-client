package com.quare.blitzsplit.core.provider.analytics.domain.usecase.impl

import com.quare.blitzsplit.core.provider.analytics.AnalyticsHub
import com.quare.blitzsplit.core.provider.analytics.AnalyticsService
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackEventUseCaseTest {
    private val logged = mutableListOf<Pair<String, Map<String, Any>>>()
    private val hub = AnalyticsHub().apply { setCollectionEnabled(true) }
    private val trackEvent = TrackEventUseCase(hub)

    @Test
    fun `GIVEN events tracked before a service attaches WHEN it attaches THEN sends them in order`() {
        // Given
        trackEvent("screen_viewed", mapOf("screen" to "home"))
        trackEvent("bill_saved", emptyMap())

        // When
        hub.attach(RecordingService(logged))

        // Then
        assertEquals(listOf("screen_viewed", "bill_saved"), logged.map { it.first })
    }

    @Test
    fun `GIVEN a service never attaches WHEN tracking many events THEN keeps only the first 50`() {
        // Given
        repeat(60) { index -> trackEvent("event_$index", emptyMap()) }

        // When
        hub.attach(RecordingService(logged))

        // Then
        assertEquals((0 until 50).map { "event_$it" }, logged.map { it.first })
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
    fun `GIVEN a service attached WHEN tracking a Float and an enum THEN sends a Double and the enum name`() {
        // Given
        hub.attach(RecordingService(logged))

        // When
        trackEvent("stats_opened", mapOf("ratio" to 0.5f, "grain" to Grain.WEEK))

        // Then
        assertEquals(listOf("stats_opened" to mapOf<String, Any>("ratio" to 0.5, "grain" to "week")), logged)
    }

    @Test
    fun `GIVEN a service attached WHEN tracking a domain object THEN leaves that param out`() {
        // Given
        hub.attach(RecordingService(logged))

        // When
        trackEvent("payment_recorded", mapOf("payer" to Person("Ana"), "method" to "pix"))

        // Then
        assertEquals(listOf("payment_recorded" to mapOf<String, Any>("method" to "pix")), logged)
    }

    private enum class Grain { WEEK }

    private data class Person(
        val name: String,
    )

    private class RecordingService(
        private val logged: MutableList<Pair<String, Map<String, Any>>>,
    ) : AnalyticsService {
        override fun logEvent(
            name: String,
            params: Map<String, Any>,
        ) {
            logged += name to params
        }

        override fun setCollectionEnabled(isEnabled: Boolean) {
            // Why: these tests only look at the events.
        }

        override fun setUserId(userId: String?) {
            // Why: these tests only look at the events.
        }
    }
}
