package com.quare.blitzsplit.core.profile.domain

import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.model.PushPlatform
import com.quare.blitzsplit.core.profile.domain.usecase.impl.SetPushDeviceUseCase
import com.quare.blitzsplit.core.profile.testing.FakePushDeviceRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class SetPushDeviceUseCaseTest {
    @Test
    fun `GIVEN no token yet WHEN the platform hands one over THEN the repository keeps that device`() {
        // Given
        val repository = FakePushDeviceRepository()
        val useCase = SetPushDeviceUseCase(repository)
        val device = PushDevice(
            token = "fcm-1",
            platform = PushPlatform.ANDROID,
            apnsEnvironment = null,
            webP256dh = null,
            webAuth = null,
        )

        // When
        useCase(device)

        // Then
        assertEquals(device, repository.device.value)
    }
}
