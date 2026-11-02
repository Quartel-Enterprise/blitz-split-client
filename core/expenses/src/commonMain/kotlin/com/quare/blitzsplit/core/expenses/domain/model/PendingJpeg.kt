package com.quare.blitzsplit.core.expenses.domain.model

import kotlin.io.encoding.Base64

class PendingJpeg(
    val jpeg: ByteArray,
) {
    // Why: the UIs draw it on every render until the upload, and each state copy shares this instance.
    val dataUri: String by lazy { "data:image/jpeg;base64,${Base64.encode(jpeg)}" }

    override fun equals(other: Any?): Boolean = other is PendingJpeg && jpeg.contentEquals(other.jpeg)

    override fun hashCode(): Int = jpeg.contentHashCode()
}
