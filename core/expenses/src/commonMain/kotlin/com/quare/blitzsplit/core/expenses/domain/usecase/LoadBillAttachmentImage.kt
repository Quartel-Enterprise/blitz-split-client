package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface LoadBillAttachmentImage {
    // Why: the JPEG of a photo added on this device and not up yet, offline too; null once it is up.
    suspend operator fun invoke(id: String): Result<ByteArray?>
}
