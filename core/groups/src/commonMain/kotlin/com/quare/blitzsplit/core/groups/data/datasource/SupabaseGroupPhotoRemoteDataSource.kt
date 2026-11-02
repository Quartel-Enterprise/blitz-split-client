package com.quare.blitzsplit.core.groups.data.datasource

import io.github.jan.supabase.storage.Storage

// Why: the URL of a new photo is the same path, so the version makes every cache fetch it again, as a profile's photo.
class SupabaseGroupPhotoRemoteDataSource(
    private val storage: Storage,
) : GroupPhotoRemoteDataSource {
    override fun urlOf(
        groupId: String,
        version: Long,
    ): String = "${storage.from(BUCKET).publicUrl(pathOf(groupId))}?v=$version"

    override suspend fun upload(
        groupId: String,
        jpeg: ByteArray,
    ) {
        storage.from(BUCKET).upload(
            path = pathOf(groupId),
            data = jpeg,
        ) { upsert = true }
    }

    private fun pathOf(groupId: String): String = "$groupId/$FILE_NAME"

    private companion object {
        const val BUCKET = "group-photos"
        const val FILE_NAME = "photo.jpg"
    }
}
