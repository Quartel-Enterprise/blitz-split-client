package com.quare.blitzsplit.core.provider.analytics

import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.impl.TrackEventUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsHubTest {
    private lateinit var hub: AnalyticsHub
    private lateinit var service: RecordingService
    private lateinit var trackEvent: TrackEvent

    @Test
    fun `GIVEN the choice is not known yet and a service attached WHEN tracking THEN sends nothing`() {
        // Given
        prepareScenario()
        hub.attach(service)

        // When
        trackEvent("screen_viewed", emptyMap())

        // Then
        assertTrue(service.events.isEmpty())
    }

    @Test
    fun `GIVEN events waiting and a service attached WHEN the choice arrives as on THEN sends them in order`() {
        // Given
        prepareScenario()
        trackEvent("screen_viewed", emptyMap())
        hub.attach(service)
        trackEvent("bill_saved", emptyMap())

        // When
        hub.setCollectionEnabled(true)

        // Then
        assertEquals(listOf("screen_viewed", "bill_saved"), service.events)
    }

    @Test
    fun `GIVEN events tracked before the choice arrived as on WHEN a service attaches THEN sends them in order`() {
        // Given
        prepareScenario()
        trackEvent("screen_viewed", emptyMap())
        trackEvent("bill_saved", emptyMap())
        hub.setCollectionEnabled(true)

        // When
        hub.attach(service)

        // Then
        assertEquals(listOf("screen_viewed", "bill_saved"), service.events)
    }

    @Test
    fun `GIVEN events waiting with no service WHEN the choice arrives as off and later on THEN never sends them`() {
        // Given
        prepareScenario()
        trackEvent("screen_viewed", emptyMap())
        hub.setCollectionEnabled(false)
        hub.attach(service)

        // When
        hub.setCollectionEnabled(true)

        // Then
        assertTrue(service.events.isEmpty())
    }

    @Test
    fun `GIVEN events waiting and a service WHEN the choice arrives as off and later on THEN never sends them`() {
        // Given
        prepareScenario()
        trackEvent("screen_viewed", emptyMap())
        hub.attach(service)
        hub.setCollectionEnabled(false)

        // When
        hub.setCollectionEnabled(true)

        // Then
        assertTrue(service.events.isEmpty())
    }

    @Test
    fun `GIVEN sharing off and a service attached WHEN tracking THEN sends nothing`() {
        // Given
        prepareScenario()
        hub.setCollectionEnabled(false)
        hub.attach(service)

        // When
        trackEvent("bill_saved", emptyMap())

        // Then
        assertTrue(service.events.isEmpty())
    }

    @Test
    fun `GIVEN an event tracked while sharing was off WHEN it turns on and a service attaches THEN skips it`() {
        // Given
        prepareScenario()
        hub.setCollectionEnabled(false)
        trackEvent("bill_saved", emptyMap())
        hub.setCollectionEnabled(true)

        // When
        hub.attach(service)

        // Then
        assertTrue(service.events.isEmpty())
    }

    @Test
    fun `GIVEN sharing turned off after it was on WHEN tracking THEN sends nothing`() {
        // Given
        prepareScenario()
        hub.setCollectionEnabled(true)
        hub.attach(service)
        trackEvent("screen_viewed", emptyMap())
        hub.setCollectionEnabled(false)

        // When
        trackEvent("bill_saved", emptyMap())

        // Then
        assertEquals(listOf("screen_viewed"), service.events)
    }

    @Test
    fun `GIVEN sharing turned off and then on again WHEN tracking THEN sends the event`() {
        // Given
        prepareScenario()
        hub.setCollectionEnabled(true)
        hub.attach(service)
        hub.setCollectionEnabled(false)
        hub.setCollectionEnabled(true)

        // When
        trackEvent("bill_saved", emptyMap())

        // Then
        assertEquals(listOf("bill_saved"), service.events)
    }

    @Test
    fun `GIVEN a service attached WHEN the choice changes THEN tells the service every change`() {
        // Given
        prepareScenario()
        hub.attach(service)
        val choices = listOf(true, false, true)

        // When
        choices.forEach(hub::setCollectionEnabled)

        // Then
        assertEquals(choices, service.collectionChoices)
    }

    @Test
    fun `GIVEN the choice known as off WHEN a service attaches THEN tells it collection is off`() {
        // Given
        prepareScenario()
        hub.setCollectionEnabled(false)

        // When
        hub.attach(service)

        // Then
        assertEquals(listOf(false), service.collectionChoices)
    }

    @Test
    fun `GIVEN the choice known as on WHEN a service attaches THEN tells it collection is on`() {
        // Given
        prepareScenario()
        hub.setCollectionEnabled(true)

        // When
        hub.attach(service)

        // Then
        assertEquals(listOf(true), service.collectionChoices)
    }

    @Test
    fun `GIVEN the choice not known yet WHEN a service attaches THEN tells it nothing about collection`() {
        // Given
        prepareScenario()

        // When
        hub.attach(service)

        // Then
        assertTrue(service.collectionChoices.isEmpty())
    }

    @Test
    fun `GIVEN a service attached WHEN setting the user id THEN forwards it`() {
        // Given
        prepareScenario()
        hub.attach(service)

        // When
        hub.setUserId(USER_ID)

        // Then
        assertEquals(listOf<String?>(null, USER_ID), service.userIds)
    }

    @Test
    fun `GIVEN a user id set before any service WHEN a service attaches THEN applies it`() {
        // Given
        prepareScenario()
        hub.setUserId(USER_ID)

        // When
        hub.attach(service)

        // Then
        assertEquals(listOf<String?>(USER_ID), service.userIds)
    }

    @Test
    fun `GIVEN no user id ever set WHEN a service attaches THEN sets no user id on it`() {
        // Given
        prepareScenario()

        // When
        hub.attach(service)

        // Then
        assertEquals(listOf<String?>(null), service.userIds)
    }

    @Test
    fun `GIVEN a user id cleared before any service WHEN a service attaches THEN sets no user id on it`() {
        // Given
        prepareScenario()
        hub.setUserId(USER_ID)
        hub.setUserId(null)

        // When
        hub.attach(service)

        // Then
        assertEquals(listOf<String?>(null), service.userIds)
    }

    @Test
    fun `GIVEN a service attached with a user id WHEN clearing it THEN forwards the cleared id`() {
        // Given
        prepareScenario()
        hub.setUserId(USER_ID)
        hub.attach(service)

        // When
        hub.setUserId(null)

        // Then
        assertEquals(listOf(USER_ID, null), service.userIds)
    }

    @Test
    fun `GIVEN an SDK that throws on one queued event WHEN the service attaches THEN still sends the others`() {
        // Given
        prepareScenario(failingEvents = setOf("screen_viewed"))
        hub.setCollectionEnabled(true)
        trackEvent("screen_viewed", emptyMap())
        trackEvent("bill_saved", emptyMap())

        // When
        hub.attach(service)

        // Then
        assertEquals(listOf("bill_saved"), service.events)
    }

    @Test
    fun `GIVEN an SDK that throws on the event WHEN tracking THEN the hub does not throw`() {
        // Given
        prepareScenario(failingEvents = setOf("bill_saved"))
        hub.setCollectionEnabled(true)
        hub.attach(service)

        // When
        val result = runCatching { trackEvent("bill_saved", emptyMap()) }

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `GIVEN an SDK that throws on the choice WHEN attaching and changing it THEN the hub does not throw`() {
        // Given
        prepareScenario(failsOnCollection = true)
        hub.setCollectionEnabled(true)

        // When
        val result = runCatching {
            hub.attach(service)
            hub.setCollectionEnabled(false)
            hub.setCollectionEnabled(true)
            trackEvent("bill_saved", emptyMap())
        }

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("bill_saved"), service.events)
    }

    @Test
    fun `GIVEN an SDK that throws on the user id WHEN setting it and attaching THEN the hub does not throw`() {
        // Given
        prepareScenario(failsOnUserId = true)
        hub.setCollectionEnabled(true)

        // When
        val result = runCatching {
            hub.attach(service)
            hub.setUserId(USER_ID)
            trackEvent("bill_saved", emptyMap())
        }

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("bill_saved"), service.events)
    }

    @Test
    fun `GIVEN sharing on WHEN tracking while a service attaches THEN the SDK gets the choice first`() {
        // Given
        prepareScenario()
        hub.setCollectionEnabled(true)
        val calls = mutableListOf<String>()
        val racing = object : AnalyticsService {
            override fun logEvent(
                name: String,
                params: Map<String, Any>,
            ) {
                calls += "event $name"
            }

            override fun setCollectionEnabled(isEnabled: Boolean) {
                calls += "collection $isEnabled"
            }

            override fun setUserId(userId: String?) {
                calls += "user id"
                // Why: stands for another thread tracking while attach is still configuring the SDK.
                trackEvent("screen_viewed", emptyMap())
            }
        }

        // When
        hub.attach(racing)

        // Then
        assertEquals(listOf("user id", "collection true", "event screen_viewed"), calls)
    }

    private fun prepareScenario(
        failingEvents: Set<String> = emptySet(),
        failsOnCollection: Boolean = false,
        failsOnUserId: Boolean = false,
    ) {
        hub = AnalyticsHub()
        service = RecordingService(failingEvents, failsOnCollection, failsOnUserId)
        trackEvent = TrackEventUseCase(hub)
    }

    private companion object {
        const val USER_ID = "user-1"
    }
}

private class RecordingService(
    private val failingEvents: Set<String>,
    private val failsOnCollection: Boolean,
    private val failsOnUserId: Boolean,
) : AnalyticsService {
    val events = mutableListOf<String>()
    val collectionChoices = mutableListOf<Boolean>()
    val userIds = mutableListOf<String?>()

    override fun logEvent(
        name: String,
        params: Map<String, Any>,
    ) {
        if (name in failingEvents) error("SDK failed on $name")
        events += name
    }

    override fun setCollectionEnabled(isEnabled: Boolean) {
        if (failsOnCollection) error("SDK failed on the collection choice")
        collectionChoices += isEnabled
    }

    override fun setUserId(userId: String?) {
        if (failsOnUserId) error("SDK failed on the user id")
        userIds += userId
    }
}
