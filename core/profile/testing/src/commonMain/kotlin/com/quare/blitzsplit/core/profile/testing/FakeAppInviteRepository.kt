package com.quare.blitzsplit.core.profile.testing

import com.quare.blitzsplit.core.profile.domain.model.AppInviteStatus
import com.quare.blitzsplit.core.profile.domain.repository.AppInviteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// In-memory AppInviteRepository: the state is public so a test can set and read it, every call is recorded, and the
// failure knob is a public var.
class FakeAppInviteRepository(
    initialStatus: AppInviteStatus = AppInviteStatus(
        isDismissed = false,
        phoneUse = null,
    ),
) : AppInviteRepository {
    val status = MutableStateFlow(initialStatus)
    val hasPendingChanges = MutableStateFlow(false)
    val syncedUserIds = mutableListOf<String>()
    var syncError: Throwable? = null
    var dismissCount = 0
    var clearCount = 0

    override fun observeStatus(): Flow<AppInviteStatus> = status

    override fun observeHasPendingChanges(): Flow<Boolean> = hasPendingChanges

    override suspend fun dismiss() {
        dismissCount++
        status.value = status.value.copy(isDismissed = true)
        hasPendingChanges.value = true
    }

    override suspend fun sync(userId: String): Result<Unit> {
        syncedUserIds += userId
        syncError?.let { return Result.failure(it) }
        hasPendingChanges.value = false
        return Result.success(Unit)
    }

    override suspend fun clear() {
        clearCount++
        status.value = AppInviteStatus(
            isDismissed = false,
            phoneUse = null,
        )
        hasPendingChanges.value = false
    }
}
