package com.quare.blitzsplit.core.profile.testing

import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.repository.PushDeviceRepository
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// In-memory PushDeviceRepository: the device is public so a test can set and read it, every call is recorded, and
// the failure knobs are public vars.
class FakePushDeviceRepository(
    initialDevice: PushDevice? = null,
    isEnabled: Boolean = true,
) : PushDeviceRepository {
    val device = MutableStateFlow(initialDevice)
    val isEnabled = MutableStateFlow(isEnabled)
    val enabledUpdates = mutableListOf<Boolean>()
    val registeredUserIds = mutableListOf<String>()
    var unregisterCount = 0
    var registerError: Throwable? = null
    var unregisterError: Throwable? = null

    // Records the switch's writes without showing them, like a Room write that hasn't answered yet.
    var holdsEnabledWrites = false

    // Never answers, like a call stuck on a slow network.
    var unregisterHangs = false

    override fun observeDevice(): Flow<PushDevice?> = device

    override fun setDevice(device: PushDevice) {
        this.device.value = device
    }

    override fun observeIsEnabled(): Flow<Boolean> = isEnabled

    override suspend fun setEnabled(isEnabled: Boolean) {
        enabledUpdates += isEnabled
        if (!holdsEnabledWrites) this.isEnabled.value = isEnabled
    }

    override suspend fun register(userId: String): Result<Unit> {
        registeredUserIds += userId
        registerError?.let { return Result.failure(it) }
        return Result.success(Unit)
    }

    override suspend fun unregister(): Result<Unit> {
        unregisterCount++
        if (unregisterHangs) awaitCancellation()
        unregisterError?.let { return Result.failure(it) }
        return Result.success(Unit)
    }
}
