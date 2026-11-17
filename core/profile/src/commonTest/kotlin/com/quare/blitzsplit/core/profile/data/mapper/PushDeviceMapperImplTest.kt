package com.quare.blitzsplit.core.profile.data.mapper

import com.quare.blitzsplit.core.profile.data.dto.RegisterPushDeviceParamsDto
import com.quare.blitzsplit.core.profile.domain.model.ApnsEnvironment
import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.model.PushPlatform
import kotlin.test.Test
import kotlin.test.assertEquals

class PushDeviceMapperImplTest {
    private val mapper = PushDeviceMapperImpl(timeZoneId = { "Asia/Tokyo" })

    @Test
    fun `GIVEN an Android token WHEN mapping to the params THEN sends android with no APNs environment nor web keys`() {
        // Given
        val device = PushDevice(
            token = "fcm-1",
            platform = PushPlatform.ANDROID,
            apnsEnvironment = null,
            webP256dh = null,
            webAuth = null,
        )

        // When
        val params = mapper.toParams(device)

        // Then
        assertEquals(
            RegisterPushDeviceParamsDto(
                token = "fcm-1",
                platform = "android",
                apnsEnvironment = null,
                webP256dh = null,
                webAuth = null,
                timeZone = "Asia/Tokyo",
            ),
            params,
        )
    }

    @Test
    fun `GIVEN a debug iPhone token WHEN mapping to the params THEN sends ios in the development environment`() {
        // Given
        val device = PushDevice(
            token = "apns-1",
            platform = PushPlatform.IOS,
            apnsEnvironment = ApnsEnvironment.DEVELOPMENT,
            webP256dh = null,
            webAuth = null,
        )

        // When
        val params = mapper.toParams(device)

        // Then
        assertEquals(
            RegisterPushDeviceParamsDto(
                token = "apns-1",
                platform = "ios",
                apnsEnvironment = "development",
                webP256dh = null,
                webAuth = null,
                timeZone = "Asia/Tokyo",
            ),
            params,
        )
    }

    @Test
    fun `GIVEN a release Mac token WHEN mapping to the params THEN sends macos in the production environment`() {
        // Given
        val device = PushDevice(
            token = "apns-2",
            platform = PushPlatform.MACOS,
            apnsEnvironment = ApnsEnvironment.PRODUCTION,
            webP256dh = null,
            webAuth = null,
        )

        // When
        val params = mapper.toParams(device)

        // Then
        assertEquals(
            RegisterPushDeviceParamsDto(
                token = "apns-2",
                platform = "macos",
                apnsEnvironment = "production",
                webP256dh = null,
                webAuth = null,
                timeZone = "Asia/Tokyo",
            ),
            params,
        )
    }

    @Test
    fun `GIVEN a browser subscription WHEN mapping to the params THEN sends web with its keys as they are`() {
        // Given
        val device = PushDevice(
            token = "https://push.example/endpoint",
            platform = PushPlatform.WEB,
            apnsEnvironment = null,
            webP256dh = "BNcRd-p256dh",
            webAuth = "tBHI-auth",
        )

        // When
        val params = mapper.toParams(device)

        // Then
        assertEquals(
            RegisterPushDeviceParamsDto(
                token = "https://push.example/endpoint",
                platform = "web",
                apnsEnvironment = null,
                webP256dh = "BNcRd-p256dh",
                webAuth = "tBHI-auth",
                timeZone = "Asia/Tokyo",
            ),
            params,
        )
    }

    @Test
    fun `GIVEN the device clock changes zone WHEN mapping twice THEN each registration sends the zone it has then`() {
        // Given
        var zone = "America/Sao_Paulo"
        val travelling = PushDeviceMapperImpl(timeZoneId = { zone })
        val device = PushDevice(
            token = "fcm-1",
            platform = PushPlatform.ANDROID,
            apnsEnvironment = null,
            webP256dh = null,
            webAuth = null,
        )

        // When
        val before = travelling.toParams(device)
        zone = "Europe/Lisbon"
        val after = travelling.toParams(device)

        // Then
        assertEquals("America/Sao_Paulo", before.timeZone)
        assertEquals("Europe/Lisbon", after.timeZone)
    }
}
