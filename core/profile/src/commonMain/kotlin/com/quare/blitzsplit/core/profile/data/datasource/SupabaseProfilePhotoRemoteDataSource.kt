package com.quare.blitzsplit.core.profile.data.datasource

import io.github.jan.supabase.storage.Storage

// Why: one object per user at a fixed path, overwritten by each new photo (see the avatars bucket migration).
class SupabaseProfilePhotoRemoteDataSource(
    private val storage: Storage,
) : ProfilePhotoRemoteDataSource {
    override suspend fun upload(
        userId: String,
        jpeg: ByteArray,
    ): String {
        val bucket = storage.from(BUCKET)
        val path = "$userId/$FILE_NAME"
        bucket.upload(path = path, data = jpeg) { upsert = true }
        return bucket.publicUrl(path)
    }

    private companion object {
        const val BUCKET = "avatars"
        const val FILE_NAME = "avatar.jpg"
    }
}
