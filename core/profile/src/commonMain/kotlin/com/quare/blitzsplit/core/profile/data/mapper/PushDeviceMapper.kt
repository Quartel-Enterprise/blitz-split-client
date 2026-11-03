package com.quare.blitzsplit.core.profile.data.mapper

import com.quare.blitzsplit.core.profile.data.dto.RegisterPushDeviceParamsDto
import com.quare.blitzsplit.core.profile.domain.model.PushDevice

fun interface PushDeviceMapper {
    fun toParams(device: PushDevice): RegisterPushDeviceParamsDto
}
