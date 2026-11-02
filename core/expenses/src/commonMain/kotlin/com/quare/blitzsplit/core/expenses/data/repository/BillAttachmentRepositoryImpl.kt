package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.data.datasource.BillAttachmentLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.BillAttachmentRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.mapper.BillAttachmentMapper
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.repository.BillAttachmentRepository
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentEntity
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class BillAttachmentRepositoryImpl(
    private val local: BillAttachmentLocalDataSource,
    private val remote: BillAttachmentRemoteDataSource,
    private val mapper: BillAttachmentMapper,
    private val currentTimeMillis: CurrentTimeMillis,
) : BillAttachmentRepository {
    private val urlsMutex = Mutex()
    private val urlLifetime = 2.hours

    // Why: a URL this close to expiring is signed again, so a viewer left open keeps loading.
    private val urlMargin = 30.minutes

    // Why: by path, so a Conta opened again, or the viewer, reuses the URL its image library already cached.
    private val signedUrls = mutableMapOf<String, SignedUrl>()

    override fun observeAttachments(expenseId: String): Flow<List<BillAttachment>> = local
        .observeAttachments(expenseId)
        .map { entities -> entities.mapNotNull(mapper::toDomain) }
        .distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local.observeHasPending().distinctUntilChanged()

    override fun observePendingVersions(): Flow<List<String>> = local.observePendingVersions().distinctUntilChanged()

    override suspend fun addAttachment(attachment: BillAttachment): Result<Boolean> = suspendRunCatching {
        local.insertWithinLimit(mapper.toEntity(attachment), attachment.kind.limit)
    }

    override suspend fun removeAttachment(
        id: String,
        updatedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        local.remove(id, updatedAt)
    }

    /*
     * Why: only the attachments of this user's groups, after their bills went up in the same sync. One the server
     * drops (past the limit, its bill deleted meanwhile) counts as synced and leaves the device on the next download.
     * A tombstone's image goes too, but only once the row says so, so no member is left with a broken photo.
     */
    override suspend fun uploadPending(userId: String): Result<Unit> = suspendRunCatching {
        // Why: one at a time, so an image the bucket refuses leaves the others going up and fails the sync after them.
        val failures = local.getPendingAttachments(userId).mapNotNull { entity ->
            suspendRunCatching { upload(entity, userId) }.exceptionOrNull()
        }
        failures.firstOrNull()?.let { throw it }
    }

    private suspend fun upload(
        entity: BillAttachmentEntity,
        userId: String,
    ) {
        val path = "${entity.groupId}/${entity.expenseId}/${entity.id}.jpg"
        entity.pendingJpeg?.let { jpeg -> remote.uploadImage(path, jpeg) }
        remote.upsertAttachment(mapper.toDto(entity, userId))
        // Why: best effort; an image left behind is never shown, since its row says it is gone.
        if (entity.isDeleted) suspendRunCatching { remote.removeImage(path) }
        local.markSynced(entity.id, entity.updatedAt)
    }

    override suspend fun download(userId: String): Result<Unit> = suspendRunCatching {
        val attachments = remote.fetchAttachments()
        local.applyRemote(attachments.map(mapper::toEntity))
        local.deleteGoneFromServer(
            ownerUserId = userId,
            serverAttachmentIds = attachments.filterNot { it.isDeleted }.map { it.id }.toSet(),
        )
    }

    override suspend fun getImageUrls(attachments: List<BillAttachment>): Result<Map<String, String>> =
        suspendRunCatching {
            val paths = attachments.filter { it.pendingImage == null }.associate { it.id to it.storagePath }
            urlsMutex.withLock {
                val now = currentTimeMillis()
                val missing = paths.values.filter { path ->
                    signedUrls[path]?.let { it.expiresAt - now > urlMargin.inWholeMilliseconds } != true
                }
                if (missing.isNotEmpty()) {
                    val expiresAt = now + urlLifetime.inWholeMilliseconds
                    remote.createSignedUrls(missing, urlLifetime).forEach { (path, url) ->
                        signedUrls[path] = SignedUrl(url, expiresAt)
                    }
                }
                paths.mapNotNull { (id, path) -> signedUrls[path]?.let { id to it.url } }.toMap()
            }
        }

    private class SignedUrl(
        val url: String,
        val expiresAt: Long,
    )
}
