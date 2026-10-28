package com.quare.blitzsplit.core.model

// Why: every UI already loads an image from a URL, and a data URI lets the picked photo show before it is uploaded.
fun ProfilePhoto.toImageUrl(): String = when (this) {
    is ProfilePhoto.Remote -> url
    is ProfilePhoto.Pending -> dataUri
}
