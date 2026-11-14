package com.quare.blitzsplit.core.settings.domain

import com.quare.blitzsplit.core.provider.analytics.AnalyticsHub
import com.quare.blitzsplit.core.provider.analytics.AnalyticsService
import com.quare.blitzsplit.core.provider.analytics.di.analyticsModule
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.settings.domain.usecase.impl.FollowUsageDataSharingUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FollowUsageDataSharingUseCaseTest {
    private lateinit var koin: KoinApplication
    private lateinit var repository: FakeDeviceSettingsRepository
    private lateinit var hub: AnalyticsHub
    private lateinit var trackEvent: TrackEvent
    private lateinit var service: RecordingAnalyticsService
    private lateinit var followUsageDataSharing: FollowUsageDataSharingUseCase

    @AfterTest
    fun closeKoin() {
        koin.close()
    }

    @Test
    fun `GIVEN a device that never touched usage data sharing WHEN following it THEN Analytics is told to collect`() =
        runTest {
            // Given
            prepareScenario(storedOn = true)

            // When
            follow()

            // Then
            assertEquals(listOf(true), service.collectionChoices)
        }

    @Test
    fun `GIVEN usage data sharing turned off on this device WHEN following it THEN Analytics is told not to collect`() =
        runTest {
            // Given
            prepareScenario(storedOn = false)

            // When
            follow()

            // Then
            assertEquals(listOf(false), service.collectionChoices)
        }

    @Test
    fun `GIVEN usage data sharing followed while on WHEN it is turned off THEN Analytics is told not to collect`() =
        runTest {
            // Given
            prepareScenario(storedOn = true)
            follow()

            // When
            repository.updateUsageDataSharing(false)

            // Then
            assertEquals(listOf(true, false), service.collectionChoices)
        }

    @Test
    fun `GIVEN usage data sharing followed while off WHEN it is turned back on THEN Analytics is told to collect`() =
        runTest {
            // Given
            prepareScenario(storedOn = false)
            follow()

            // When
            repository.updateUsageDataSharing(true)

            // Then
            assertEquals(listOf(false, true), service.collectionChoices)
        }

    @Test
    fun `GIVEN usage data sharing followed and turned off WHEN tracking an event THEN nothing is sent`() = runTest {
        // Given
        prepareScenario(storedOn = true)
        follow()
        repository.updateUsageDataSharing(false)

        // When
        trackEvent("bill_saved", emptyMap())

        // Then
        assertTrue(service.events.isEmpty())
    }

    @Test
    fun `GIVEN usage data sharing followed while on WHEN tracking an event THEN it is sent`() = runTest {
        // Given
        prepareScenario(storedOn = true)
        follow()

        // When
        trackEvent("bill_saved", emptyMap())

        // Then
        assertEquals(listOf("bill_saved"), service.events)
    }

    @Test
    fun `GIVEN usage data sharing on WHEN following it THEN keeps following after the current value`() = runTest {
        // Given
        prepareScenario(storedOn = true)

        // When
        val following = follow()

        // Then
        assertTrue(following.isActive)
    }

    private fun TestScope.follow(): Job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        followUsageDataSharing()
    }

    private fun prepareScenario(storedOn: Boolean) {
        koin = koinApplication { modules(analyticsModule) }
        hub = koin.koin.get()
        trackEvent = koin.koin.get()
        service = RecordingAnalyticsService()
        hub.attach(service)
        repository = FakeDeviceSettingsRepository().also { it.usageDataSharing.value = storedOn }
        followUsageDataSharing = FollowUsageDataSharingUseCase(
            repository = repository,
            hub = hub,
        )
    }
}

private class RecordingAnalyticsService : AnalyticsService {
    val events = mutableListOf<String>()
    val collectionChoices = mutableListOf<Boolean>()
    val userIds = mutableListOf<String?>()

    override fun logEvent(
        name: String,
        params: Map<String, Any>,
    ) {
        events += name
    }

    override fun setCollectionEnabled(isEnabled: Boolean) {
        collectionChoices += isEnabled
    }

    override fun setUserId(userId: String?) {
        userIds += userId
    }
}
