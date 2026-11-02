package com.quare.blitzsplit.core.expenses.testing

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.repository.BillAttachmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// In-memory BillAttachmentRepository: the state is public so a test can set and read it, and the failure knobs are public vars.
class FakeBillAttachmentRepository(
    initialAttachments: List<BillAttachment> = emptyList(),
) : BillAttachmentRepository {
    val attachments = MutableStateFlow(initialAttachments)
    val removedIds = mutableListOf<String>()

    // Why: each removal with the time it was stamped at, for the Last-Write-Wins of its tombstone.
    val removals = mutableListOf<Pair<String, Long>>()
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    val urlRequests = mutableListOf<List<String>>()
    var imageUrls: Map<String, String> = emptyMap()
    var addError: Throwable? = null
    var hasRoom = true
    var removeError: Throwable? = null
    var urlsError: Throwable? = null
    var pushError: Throwable? = null
    var pullError: Throwable? = null

    override fun observeAttachments(expenseId: String): Flow<List<BillAttachment>> =
        attachments.map { list -> list.filter { it.expenseId == expenseId } }

    override fun observeHasPendingChanges(): Flow<Boolean> = attachments.map { list -> list.any { it.isPending } }

    override fun observePendingVersions(): Flow<List<String>> =
        attachments.map { list -> list.filter { it.isPending }.map { "${it.id}:${it.updatedAt}" } }

    override suspend fun addAttachment(attachment: BillAttachment): Result<Boolean> {
        addError?.let { return Result.failure(it) }
        if (!hasRoom) return Result.success(false)
        attachments.update { it + attachment }
        return Result.success(true)
    }

    override suspend fun removeAttachment(
        id: String,
        updatedAt: Long,
    ): Result<Unit> {
        removeError?.let { return Result.failure(it) }
        removedIds += id
        removals += id to updatedAt
        attachments.update { list -> list.filterNot { it.id == id } }
        return Result.success(Unit)
    }

    override suspend fun uploadPending(userId: String): Result<Unit> {
        uploadedUserIds += userId
        pushError?.let { return Result.failure(it) }
        attachments.update { list ->
            list.map {
                it.copy(
                    isPending = false,
                    pendingImage = null,
                )
            }
        }
        return Result.success(Unit)
    }

    override suspend fun download(userId: String): Result<Unit> {
        downloadedUserIds += userId
        pullError?.let { return Result.failure(it) }
        return Result.success(Unit)
    }

    override suspend fun getImageUrls(attachments: List<BillAttachment>): Result<Map<String, String>> {
        urlRequests += attachments.map { it.id }
        urlsError?.let { return Result.failure(it) }
        return Result.success(imageUrls.filterKeys { id -> attachments.any { it.id == id && it.pendingImage == null } })
    }
}
