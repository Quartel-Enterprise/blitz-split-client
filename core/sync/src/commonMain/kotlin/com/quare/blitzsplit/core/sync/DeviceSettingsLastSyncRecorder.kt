package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateLastSyncedAt
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlinx.coroutines.flow.first

/*
 * Why: the watch's "Updated at 14:32". Only a sync of an account's data counts: one that found no profile (a watch
 * not connected yet) synced nothing.
 */
class DeviceSettingsLastSyncRecorder(
    private val updateLastSyncedAt: UpdateLastSyncedAt,
    private val profileRepository: ProfileRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : LastSyncRecorder {
    override suspend fun record() {
        if (profileRepository.observeProfile().first() == null) return
        updateLastSyncedAt(currentTimeMillis())
    }
}
