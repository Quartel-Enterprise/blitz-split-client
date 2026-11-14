package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import kotlinx.coroutines.flow.Flow

// Why: the person's palette of custom colors, on the device and offline first, synced to their other devices.
interface CustomColorRepository {
    // Why: the colors in the palette, in the order they were added; a deleted one isn't among them.
    fun observeColors(): Flow<List<CustomColor>>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: when each color was last changed, so a new change triggers a sync even right after a failed push.
    fun observeVersions(): Flow<List<Long>>

    // Why: a color already in the palette stays where it is; one deleted before comes back.
    suspend fun add(
        color: CustomColor,
        updatedAt: Long,
    ): Result<Unit>

    suspend fun delete(
        color: CustomColor,
        updatedAt: Long,
    ): Result<Unit>

    suspend fun uploadPending(userId: String): Result<Unit>

    suspend fun download(userId: String): Result<Unit>

    suspend fun clear()
}
