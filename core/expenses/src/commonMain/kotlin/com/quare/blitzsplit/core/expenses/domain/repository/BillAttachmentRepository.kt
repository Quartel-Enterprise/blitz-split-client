package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import kotlinx.coroutines.flow.Flow

interface BillAttachmentRepository {
    // Why: a bill's attachments still on it, the receipt first, then the photos in the order they were added.
    fun observeAttachments(expenseId: String): Flow<List<BillAttachment>>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: each change waiting to go up, so a new one triggers a sync after a failed push.
    fun observePendingVersions(): Flow<List<String>>

    // Why: on the device first, offline included; false when the bill has no room left for its kind.
    suspend fun addAttachment(attachment: BillAttachment): Result<Boolean>

    // Why: any member takes one off, offline too, as a tombstone the sync takes up.
    suspend fun removeAttachment(
        id: String,
        updatedAt: Long,
    ): Result<Unit>

    // Why: the image first, then its row, after the bills they are on went up in the same sync.
    suspend fun uploadPending(userId: String): Result<Unit>

    suspend fun download(userId: String): Result<Unit>

    // Why: a signed URL for each attachment already in the bucket, by its id; needs the internet.
    suspend fun getImageUrls(attachments: List<BillAttachment>): Result<Map<String, String>>
}
