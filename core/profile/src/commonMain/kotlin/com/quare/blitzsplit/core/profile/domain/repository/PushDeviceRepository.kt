package com.quare.blitzsplit.core.profile.domain.repository

import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import kotlinx.coroutines.flow.Flow

interface PushDeviceRepository {
    // Why: null until the platform has a token, which it never has while push isn't set up or is refused.
    fun observeDevice(): Flow<PushDevice?>

    fun setDevice(device: PushDevice)

    // Why: the server moves a token to whoever registers it last, so each user signed in here registers it once.
    suspend fun register(userId: String): Result<Unit>

    // Why: a sign out stops the account's notifications reaching a device it left.
    suspend fun unregister(): Result<Unit>
}
