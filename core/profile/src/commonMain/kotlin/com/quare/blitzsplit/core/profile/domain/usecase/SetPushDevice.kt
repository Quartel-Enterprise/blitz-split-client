package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.profile.domain.model.PushDevice

// Why: called by each platform once its push service gives the device a token, and again whenever it changes.
fun interface SetPushDevice {
    operator fun invoke(device: PushDevice)
}
