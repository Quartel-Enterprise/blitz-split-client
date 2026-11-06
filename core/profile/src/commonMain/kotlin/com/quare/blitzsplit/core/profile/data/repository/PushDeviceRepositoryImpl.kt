package com.quare.blitzsplit.core.profile.data.repository

import com.quare.blitzsplit.core.profile.data.datasource.PushDeviceRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.PushSettingsLocalDataSource
import com.quare.blitzsplit.core.profile.data.mapper.PushDeviceMapper
import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.repository.PushDeviceRepository
import com.quare.blitzsplit.core.provider.room.entity.PushSettingsEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/*
 * Why: in memory, since each platform hands the token over again on every launch, and registering it again once per
 * launch is what keeps the server's copy current when the push service changed it while the app was closed. The
 * switch that lets this device take pushes is kept in Room, as it outlives the launch.
 */
class PushDeviceRepositoryImpl(
    private val remoteDataSource: PushDeviceRemoteDataSource,
    private val mapper: PushDeviceMapper,
    private val localDataSource: PushSettingsLocalDataSource,
) : PushDeviceRepository {
    private val device = MutableStateFlow<PushDevice?>(null)
    private val lock = Mutex()
    private var registered: Pair<PushDevice, String>? = null

    override fun observeDevice(): Flow<PushDevice?> = device

    override fun setDevice(device: PushDevice) {
        this.device.value = device
    }

    // Why: no row yet is on, as every device took pushes before the switch.
    override fun observeIsEnabled(): Flow<Boolean> = localDataSource
        .observe()
        .map { entity -> entity?.isEnabled ?: true }
        .distinctUntilChanged()

    override suspend fun setEnabled(isEnabled: Boolean) {
        localDataSource.upsert(
            PushSettingsEntity(
                id = SINGLE_ROW_ID,
                isEnabled = isEnabled,
            ),
        )
    }

    override suspend fun register(userId: String): Result<Unit> = lock.withLock {
        val current = device.value ?: return@withLock Result.success(Unit)
        if (registered == current to userId) return@withLock Result.success(Unit)
        suspendRunCatching { remoteDataSource.register(mapper.toParams(current)) }
            .onSuccess { registered = current to userId }
    }

    /*
     * Why: also a token an earlier launch left registered, which this one didn't register again. The server removes
     * it whoever holds it, since only this device knows it, so no session is needed. Forgotten here even when the
     * call fails, since the server may have removed it anyway (an answer lost on the way), so the next sign in
     * registers it again rather than trusting a registration that may be gone.
     */
    override suspend fun unregister(): Result<Unit> = lock.withLock {
        registered = null
        val current = device.value ?: return@withLock Result.success(Unit)
        suspendRunCatching { remoteDataSource.unregister(current.token) }
    }
}
