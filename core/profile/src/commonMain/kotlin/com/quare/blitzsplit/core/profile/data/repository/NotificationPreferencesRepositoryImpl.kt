package com.quare.blitzsplit.core.profile.data.repository

import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.profile.data.datasource.NotificationPreferencesLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.NotificationPreferencesRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.mapper.NotificationPreferencesMapper
import com.quare.blitzsplit.core.profile.domain.repository.NotificationPreferencesRepository
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotificationPreferencesRepositoryImpl(
    private val local: NotificationPreferencesLocalDataSource,
    private val remote: NotificationPreferencesRemoteDataSource,
    private val profileLocal: ProfileLocalDataSource,
    private val mapper: NotificationPreferencesMapper,
    private val currentTimeMillis: CurrentTimeMillis,
) : NotificationPreferencesRepository {
    // Why: each toggle reads the row and writes it back whole, so two quick taps in a row would lose the first.
    private val writeLock = Mutex()

    override fun observePreferences(): Flow<NotificationPreferences> = local
        .observe()
        .map(mapper::toDomain)
        .distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local
        .observe()
        .map { entity -> entity?.pendingSync == true }
        .distinctUntilChanged()

    /*
     * Why: new preferences belong to whoever the profile belongs to, so a switch drops them with the profile's
     * copy; changed ones keep their owner, so a change to another user's still gives way.
     */
    override suspend fun updatePreference(
        type: NotificationType,
        isEnabled: Boolean,
    ) {
        writeLock.withLock { updateLocked(type, isEnabled) }
    }

    private suspend fun updateLocked(
        type: NotificationType,
        isEnabled: Boolean,
    ) {
        val stored = local.get()
        val current = mapper.toDomain(stored).enabledTypes
        val enabledTypes = if (isEnabled) current + type else current - type
        if (enabledTypes == current) return
        local.upsert(
            mapper.toEntity(
                preferences = NotificationPreferences(enabledTypes),
                /*
                 * Why: newer than the row it changes, even when this device's clock is behind the one that wrote
                 * it or two taps land in the same millisecond; otherwise the next pull or markSynced would undo it.
                 */
                updatedAt = maxOf(currentTimeMillis(), (stored?.updatedAt ?: 0L) + 1),
                ownerUserId = stored?.ownerUserId ?: profileLocal.get()?.ownerUserId,
            ),
        )
    }

    override suspend fun uploadPendingPreferences(userId: String): Result<Unit> = suspendRunCatching {
        val pending = local.get()?.takeIf { it.pendingSync && it.ownerUserId == userId } ?: return@suspendRunCatching
        remote.upsert(
            mapper.toDto(
                entity = pending,
                userId = userId,
            ),
        )
        local.markSynced(pending.updatedAt)
    }

    // Why: like the keys, another user's preferences never go up as this account's, which keeps its own or the defaults.
    override suspend fun downloadPreferences(userId: String): Result<Unit> = suspendRunCatching {
        val remotePreferences = remote.fetch(userId)
        local.deleteOtherUsers(userId)
        local.claimUnowned(userId)
        remotePreferences?.let {
            local.applyRemote(
                mapper.toEntity(
                    dto = it,
                    ownerUserId = userId,
                ),
            )
        }
    }

    override suspend fun clear() {
        local.clear()
    }
}
