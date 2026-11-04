package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.data.datasource.BillAttachmentRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.BillShareLinkRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.dto.CommentOnSharedBillParamsDto
import com.quare.blitzsplit.core.expenses.data.dto.DeleteSharedBillCommentParamsDto
import com.quare.blitzsplit.core.expenses.data.dto.EditSharedBillCommentParamsDto
import com.quare.blitzsplit.core.expenses.data.dto.LikeSharedBillCommentParamsDto
import com.quare.blitzsplit.core.expenses.data.mapper.SharedBillMapper
import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.model.SharedBill
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.repository.BillShareLinkRepository
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlin.time.Duration.Companion.hours

class BillShareLinkRepositoryImpl(
    private val remote: BillShareLinkRemoteDataSource,
    private val attachmentRemote: BillAttachmentRemoteDataSource,
    private val mapper: SharedBillMapper,
) : BillShareLinkRepository {
    private val signedUrlLifetime = 2.hours

    override suspend fun fetchLink(expenseId: String): Result<BillShareLink?> = suspendRunCatching {
        remote.fetchLink(expenseId)?.let(mapper::toDomain)
    }

    override suspend fun updateLink(
        expenseId: String,
        visibility: BillLinkVisibility,
    ): Result<BillShareLink?> = suspendRunCatching {
        mapper.toDomain(remote.updateLink(expenseId, visibility.code))
    }

    override suspend fun disableLink(expenseId: String): Result<Unit> = suspendRunCatching {
        remote.disableLink(expenseId)
    }.map { }

    // Why: the images are a nicety, so a bucket that can't sign them leaves the bill without them, not unread.
    override suspend fun fetchSharedBill(code: String): Result<SharedBill> = suspendRunCatching {
        val dto = remote.fetchSharedBill(code)
        val paths = dto.attachments.orEmpty().map { it.path }
        val imageUrls = if (paths.isEmpty()) {
            emptyMap()
        } else {
            suspendRunCatching {
                attachmentRemote.createSignedUrls(
                    paths,
                    signedUrlLifetime,
                )
            }.getOrDefault(emptyMap())
        }
        mapper.toDomain(dto, imageUrls)
    }

    override suspend fun createSharedBillComment(
        code: String,
        id: String,
        body: String,
        threadId: String?,
        replyToName: String?,
        authorName: String?,
    ): Result<SharedBillComment> = suspendRunCatching {
        val params = CommentOnSharedBillParamsDto(
            code = code,
            id = id,
            body = body,
            threadId = threadId,
            replyToName = replyToName,
            authorName = authorName,
        )
        mapper.toDomain(remote.createSharedBillComment(params))
    }

    override suspend fun editSharedBillComment(
        code: String,
        id: String,
        body: String,
    ): Result<SharedBillComment> = suspendRunCatching {
        mapper.toDomain(remote.editSharedBillComment(EditSharedBillCommentParamsDto(code, id, body)))
    }

    override suspend fun deleteSharedBillComment(
        code: String,
        id: String,
    ): Result<SharedBillComment> = suspendRunCatching {
        mapper.toDomain(remote.deleteSharedBillComment(DeleteSharedBillCommentParamsDto(code, id)))
    }

    override suspend fun likeSharedBillComment(
        code: String,
        id: String,
        isLiked: Boolean,
    ): Result<SharedBillComment> = suspendRunCatching {
        mapper.toDomain(remote.likeSharedBillComment(LikeSharedBillCommentParamsDto(code, id, isLiked)))
    }
}
