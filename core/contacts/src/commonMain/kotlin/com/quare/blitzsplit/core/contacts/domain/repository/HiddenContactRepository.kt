package com.quare.blitzsplit.core.contacts.domain.repository

import com.quare.blitzsplit.core.contacts.domain.model.HiddenContact
import kotlinx.coroutines.flow.Flow

interface HiddenContactRepository {
    fun observeHidden(): Flow<List<HiddenContact>>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: "Ocultar" (true) or "Mostrar" (false), on the device first, offline too; the sync takes it up.
    suspend fun setHidden(
        personKey: String,
        isHidden: Boolean,
    )

    suspend fun uploadPending(userId: String): Result<Unit>

    // Why: after a switch, another user's list gives way to this user's.
    suspend fun download(userId: String): Result<Unit>

    // Why: a sign out leaves nothing of the list on the device.
    suspend fun clear()
}
