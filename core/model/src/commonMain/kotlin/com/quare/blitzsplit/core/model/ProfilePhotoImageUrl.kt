package com.quare.blitzsplit.core.model

import kotlin.io.encoding.Base64

// Why: every UI already loads an image from a URL, and a data URI lets the picked photo show before it is uploaded.
fun ProfilePhoto.toImageUrl(): String = when (this) {
    is ProfilePhoto.Remote -> url
    is ProfilePhoto.Pending -> dataUri
}

// Why: a pending photo and the photo editor's are both JPEGs drawn from their bytes.
internal fun ByteArray.toJpegDataUri(): String = "data:image/jpeg;base64,${Base64.encode(this)}"
