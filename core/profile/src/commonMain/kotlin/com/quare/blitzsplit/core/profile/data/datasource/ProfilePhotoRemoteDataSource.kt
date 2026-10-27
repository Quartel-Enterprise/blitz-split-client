package com.quare.blitzsplit.core.profile.data.datasource

// Why: it returns the public URL, which the profile row keeps as avatar_url.
fun interface ProfilePhotoRemoteDataSource {
    suspend fun upload(
        userId: String,
        jpeg: ByteArray,
    ): String
}
