package com.quare.blitzsplit.core.profile.data.repository

import com.quare.blitzsplit.core.profile.data.datasource.PushDeviceRemoteDataSource
import com.quare.blitzsplit.core.profile.data.mapper.PushDeviceMapper
import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.repository.PushDeviceRepository
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/*
 * Why: in memory, since each platform hands the token over again on every launch, and registering it again once per
 * launch is what keeps the server's copy current when the push service changed it while the app was closed.
 */
class PushDeviceRepositoryImpl(
    private val remoteDataSource: PushDeviceRemoteDataSource,
    private val mapper: PushDeviceMapper,
) : PushDeviceRepository {
    private val device = MutableStateFlow<PushDevice?>(null)
    private val lock = Mutex()
    private var registered: Pair<PushDevice, String>? = null

    override fun observeDevice(): Flow<PushDevice?> = device

    override fun setDevice(device: PushDevice) {
        this.device.value = device
    }

    override suspend fun register(userId: String): Result<Unit> = lock.withLock {
        val current = device.value ?: return@withLock Result.success(Unit)
        if (registered == current to userId) return@withLock Result.success(Unit)
        suspendRunCatching { remoteDataSource.register(mapper.toParams(current)) }
            .onSuccess { registered = current to userId }
    }

    /*
     * Why: also a token an earlier launch left registered, which this one didn't register again. The server only
     * removes it from the caller, so it is harmless when another user holds it now.
     */
    override suspend fun unregister(): Result<Unit> = lock.withLock {
        val current = device.value ?: return@withLock Result.success(Unit)
        suspendRunCatching { remoteDataSource.unregister(current.token) }
            .onSuccess { registered = null }
    }
}
