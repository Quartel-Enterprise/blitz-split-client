package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.repository.PushDeviceRepository
import com.quare.blitzsplit.core.profile.domain.usecase.SetPushDevice

class SetPushDeviceUseCase(
    private val repository: PushDeviceRepository,
) : SetPushDevice {
    override fun invoke(device: PushDevice) {
        repository.setDevice(device)
    }
}
