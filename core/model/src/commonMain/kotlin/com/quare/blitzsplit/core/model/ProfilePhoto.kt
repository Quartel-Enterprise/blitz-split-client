package com.quare.blitzsplit.core.model

sealed interface ProfilePhoto {
    // Why: the photo of Google or Apple, or one this user uploaded before.
    data class Remote(
        val url: String,
    ) : ProfilePhoto

    // Why: picked on this device and not uploaded yet, already a 512x512 JPEG.
    class Pending(
        val jpeg: ByteArray,
    ) : ProfilePhoto {
        // Why: the UIs draw it on every render until the upload, and each state copy shares this instance.
        val dataUri: String by lazy { jpeg.toJpegDataUri() }

        override fun equals(other: Any?): Boolean = other is Pending && jpeg.contentEquals(other.jpeg)

        override fun hashCode(): Int = jpeg.contentHashCode()
    }
}
