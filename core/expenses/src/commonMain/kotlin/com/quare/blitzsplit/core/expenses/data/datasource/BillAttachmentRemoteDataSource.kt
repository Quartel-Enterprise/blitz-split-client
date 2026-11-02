package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.BillAttachmentDto
import kotlin.time.Duration

interface BillAttachmentRemoteDataSource {
    suspend fun uploadImage(
        path: String,
        jpeg: ByteArray,
    )

    suspend fun removeImage(path: String)

    suspend fun upsertAttachment(attachment: BillAttachmentDto)

    // Why: every attachment of the user's groups, tombstones included, page by page.
    suspend fun fetchAttachments(): List<BillAttachmentDto>

    // Why: by path, leaving out any the bucket couldn't sign (an image not up yet).
    suspend fun createSignedUrls(
        paths: List<String>,
        expiresIn: Duration,
    ): Map<String, String>
}
