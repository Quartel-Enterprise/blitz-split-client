package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceSettingsLastSyncRecorderTest {
    private val recordedTimes = mutableListOf<Long>()
    private lateinit var recorder: DeviceSettingsLastSyncRecorder

    @Test
    fun `GIVEN the account profile on the watch WHEN a sync goes through THEN keeps its time`() = runTest {
        // Given
        prepareScenario(profile = UserProfile("Pierre", AvatarColor.BLUE, null))

        // When
        recorder.record()

        // Then
        assertEquals(listOf(NOW), recordedTimes)
    }

    @Test
    fun `GIVEN a watch with no profile yet WHEN a sync goes through THEN keeps no time`() = runTest {
        // Given
        prepareScenario(profile = null)

        // When
        recorder.record()

        // Then
        assertEquals(emptyList(), recordedTimes)
    }

    private fun prepareScenario(profile: UserProfile?) {
        recorder = DeviceSettingsLastSyncRecorder(
            updateLastSyncedAt = { recordedTimes += it },
            profileRepository = FakeProfileRepository(initialProfile = profile),
            currentTimeMillis = { NOW },
        )
    }

    private companion object {
        const val NOW = 1_795_176_000_000L
    }
}
