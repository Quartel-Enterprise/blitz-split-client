package com.quare.blitzsplit.core.profile.data.mapper

import com.quare.blitzsplit.core.profile.data.dto.RegisterPushDeviceParamsDto
import com.quare.blitzsplit.core.profile.domain.model.ApnsEnvironment
import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.model.PushPlatform

class PushDeviceMapperImpl : PushDeviceMapper {
    override fun toParams(device: PushDevice): RegisterPushDeviceParamsDto = RegisterPushDeviceParamsDto(
        token = device.token,
        platform = when (device.platform) {
            PushPlatform.ANDROID -> "android"
            PushPlatform.IOS -> "ios"
            PushPlatform.MACOS -> "macos"
            PushPlatform.WEB -> "web"
        },
        apnsEnvironment = when (device.apnsEnvironment) {
            ApnsEnvironment.DEVELOPMENT -> "development"
            ApnsEnvironment.PRODUCTION -> "production"
            null -> null
        },
        webP256dh = device.webP256dh,
        webAuth = device.webAuth,
    )
}
