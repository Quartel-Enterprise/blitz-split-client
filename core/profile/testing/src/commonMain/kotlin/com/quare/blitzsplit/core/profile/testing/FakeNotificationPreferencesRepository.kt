package com.quare.blitzsplit.core.profile.testing

import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.profile.domain.repository.NotificationPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// In-memory NotificationPreferencesRepository: the state is public so a test can set and read it, every call is
// recorded, and the failure knobs are public vars.
class FakeNotificationPreferencesRepository(
    initialPreferences: NotificationPreferences = NotificationPreferences.DEFAULT,
) : NotificationPreferencesRepository {
    val preferences = MutableStateFlow(initialPreferences)
    val hasPendingChanges = MutableStateFlow(false)
    val updates = mutableListOf<Pair<NotificationType, Boolean>>()
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var pushError: Throwable? = null
    var pullError: Throwable? = null
    var clearError: Throwable? = null
    var clearCount = 0

    override fun observePreferences(): Flow<NotificationPreferences> = preferences

    override fun observeHasPendingChanges(): Flow<Boolean> = hasPendingChanges

    override suspend fun updatePreference(
        type: NotificationType,
        isEnabled: Boolean,
    ) {
        updates += type to isEnabled
        val current = preferences.value.enabledTypes
        preferences.value = NotificationPreferences(if (isEnabled) current + type else current - type)
        hasPendingChanges.value = true
    }

    override suspend fun uploadPendingPreferences(userId: String): Result<Unit> {
        uploadedUserIds += userId
        pushError?.let { return Result.failure(it) }
        hasPendingChanges.value = false
        return Result.success(Unit)
    }

    override suspend fun downloadPreferences(userId: String): Result<Unit> {
        downloadedUserIds += userId
        return pullError?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    override suspend fun clear() {
        clearCount++
        clearError?.let { throw it }
        preferences.value = NotificationPreferences.DEFAULT
        hasPendingChanges.value = false
    }
}
