package com.quare.blitzsplit.core.profile.data.repository

import com.quare.blitzsplit.core.profile.data.datasource.AppInviteLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.AppInviteRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.dto.AppInviteDto
import com.quare.blitzsplit.core.profile.data.dto.PlatformSeenDto
import com.quare.blitzsplit.core.profile.domain.model.AppInviteStatus
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.profile.domain.model.PhoneUse
import com.quare.blitzsplit.core.profile.domain.repository.AppInviteRepository
import com.quare.blitzsplit.core.provider.room.entity.AppInviteEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppInviteRepositoryImpl(
    private val local: AppInviteLocalDataSource,
    private val remote: AppInviteRemoteDataSource,
    private val profileLocal: ProfileLocalDataSource,
    private val platform: ClientPlatform,
    private val currentTimeMillis: CurrentTimeMillis,
) : AppInviteRepository {
    // Why: a dismissal and a sync each read the row and write it back whole.
    private val writeLock = Mutex()

    // Why: the platform only needs recording once per user while the app runs; each sync still reads the phone use.
    private var recordedUserId: String? = null

    override fun observeStatus(): Flow<AppInviteStatus> = local
        .observe()
        .map(::statusOf)
        .distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local
        .observe()
        .map { entity -> entity?.pendingSync == true }
        .distinctUntilChanged()

    // Why: a new row belongs to whoever the profile belongs to, as the notification preferences do.
    override suspend fun dismiss() {
        writeLock.withLock {
            val stored = local.get()
            if (stored?.dismissedAt != null) return
            local.upsert(
                (stored ?: createEmptyRow(profileLocal.get()?.ownerUserId)).copy(
                    dismissedAt = currentTimeMillis(),
                    pendingSync = true,
                ),
            )
        }
    }

    override suspend fun sync(userId: String): Result<Unit> = suspendRunCatching {
        if (recordedUserId != userId) {
            remote.recordPlatformSeen(platform.code)
            recordedUserId = userId
        }
        val remoteDismissal = remote.fetchDismissal(userId)
        val platformsSeen = remote.fetchPlatformsSeen(userId)
        val pendingDismissedAt = writeLock.withLock {
            local.deleteOtherUsers(userId)
            local.claimUnowned(userId)
            val stored = local.get()
            // Why: a dismissal is final, so either side's wins; the phone use is the server's alone.
            val merged = (stored ?: createEmptyRow(userId)).copy(
                dismissedAt = stored?.dismissedAt ?: remoteDismissal?.dismissedAt,
                pendingSync = stored?.pendingSync == true && remoteDismissal == null,
                androidSeenAt = platformsSeen.findFirstSeenOn(ClientPlatform.ANDROID),
                iosSeenAt = platformsSeen.findFirstSeenOn(ClientPlatform.IOS),
                ownerUserId = userId,
            )
            if (merged != stored) local.upsert(merged)
            merged.dismissedAt?.takeIf { merged.pendingSync }
        }
        pendingDismissedAt?.let { dismissedAt ->
            remote.insertDismissal(
                AppInviteDto(
                    userId = userId,
                    dismissedAt = dismissedAt,
                ),
            )
            local.markSynced(dismissedAt)
        }
    }

    override suspend fun clear() {
        local.clear()
    }

    private fun statusOf(entity: AppInviteEntity?): AppInviteStatus {
        val seen = listOfNotNull(
            entity?.androidSeenAt?.let { ClientPlatform.ANDROID to it },
            entity?.iosSeenAt?.let { ClientPlatform.IOS to it },
        )
        return AppInviteStatus(
            isDismissed = entity?.dismissedAt != null,
            phoneUse = seen.takeIf { it.isNotEmpty() }?.let { pairs ->
                PhoneUse(
                    platforms = pairs.mapTo(mutableSetOf()) { it.first },
                    sinceMillis = pairs.minOf { it.second },
                )
            },
        )
    }

    private fun createEmptyRow(ownerUserId: String?): AppInviteEntity = AppInviteEntity(
        id = SINGLE_ROW_ID,
        dismissedAt = null,
        pendingSync = false,
        androidSeenAt = null,
        iosSeenAt = null,
        ownerUserId = ownerUserId,
    )

    private fun List<PlatformSeenDto>.findFirstSeenOn(platform: ClientPlatform): Long? =
        filter { it.platform == platform.code }.minOfOrNull { it.firstSeenAt }

    // Why: the server's names, as `platforms_seen.platform` checks them.
    private val ClientPlatform.code: String
        get() = name.lowercase()
}
