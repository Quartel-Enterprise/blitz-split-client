package com.quare.blitzsplit.core.profile.domain.repository

import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import kotlinx.coroutines.flow.Flow

interface NotificationPreferencesRepository {
    fun observePreferences(): Flow<NotificationPreferences>

    fun observeHasPendingChanges(): Flow<Boolean>

    suspend fun updatePreference(
        type: NotificationType,
        isEnabled: Boolean,
    )

    suspend fun uploadPendingPreferences(userId: String): Result<Unit>

    // Why: after a switch, another user's preferences give way to this user's, even ones not pushed yet.
    suspend fun downloadPreferences(userId: String): Result<Unit>

    // Why: a sign out leaves nothing of the account on the device.
    suspend fun clear()
}
