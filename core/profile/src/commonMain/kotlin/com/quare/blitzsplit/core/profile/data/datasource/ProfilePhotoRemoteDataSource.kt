package com.quare.blitzsplit.core.profile.data.datasource

// Why: it returns the public URL, which the profile row keeps as avatar_url.
interface ProfilePhotoRemoteDataSource {
    suspend fun upload(
        userId: String,
        jpeg: ByteArray,
    ): String

    // Why: an uploaded photo is the object of the user who uploaded it, unlike a Google or Apple picture.
    fun isUploaded(url: String): Boolean
}
