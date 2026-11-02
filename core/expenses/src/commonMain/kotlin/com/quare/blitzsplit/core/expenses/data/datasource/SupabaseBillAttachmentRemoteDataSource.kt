package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.BillAttachmentDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlin.time.Duration

class SupabaseBillAttachmentRemoteDataSource(
    private val postgrest: Postgrest,
    private val storage: Storage,
) : BillAttachmentRemoteDataSource {
    // Why: an image that went up before its row's push failed goes up again over itself.
    override suspend fun uploadImage(
        path: String,
        jpeg: ByteArray,
    ) {
        storage.from(BUCKET).upload(
            path = path,
            data = jpeg,
        ) { upsert = true }
    }

    override suspend fun removeImage(path: String) {
        storage.from(BUCKET).delete(listOf(path))
    }

    override suspend fun upsertAttachment(attachment: BillAttachmentDto) {
        postgrest.from(BILL_ATTACHMENTS).upsert(attachment)
    }

    override suspend fun fetchAttachments(): List<BillAttachmentDto> = postgrest.fetchAllById<BillAttachmentDto>(
        table = BILL_ATTACHMENTS,
        idOf = { it.id },
    )

    override suspend fun createSignedUrls(
        paths: List<String>,
        expiresIn: Duration,
    ): Map<String, String> = storage
        .from(BUCKET)
        .createSignedUrls(
            expiresIn = expiresIn,
            paths = paths,
        ).filter { it.error == null }
        .associate { it.path to it.signedURL }

    private companion object {
        const val BUCKET = "bill-attachments"
        const val BILL_ATTACHMENTS = "bill_attachments"
    }
}
