package com.quare.blitzsplit.core.groups.data.datasource

import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.storage.Storage

// Why: the URL of a new photo is the same path, so the version makes every cache fetch it again, as a profile's photo.
class SupabaseGroupPhotoRemoteDataSource(
    private val storage: Storage,
) : GroupPhotoRemoteDataSource {
    private val clientErrors = 400..499
    private val retryableErrors = setOf(408, 429)

    override fun urlOf(
        groupId: String,
        version: Long,
    ): String = "${storage.from(BUCKET).publicUrl(pathOf(groupId))}?v=$version"

    // Why: a 4xx other than a timeout or a rate limit (the bucket's rules, its size cap) answers the same next time.
    override suspend fun upload(
        groupId: String,
        jpeg: ByteArray,
    ): Boolean {
        try {
            storage.from(BUCKET).upload(
                path = pathOf(groupId),
                data = jpeg,
            ) { upsert = true }
        } catch (exception: RestException) {
            if (exception.statusCode in clientErrors && exception.statusCode !in retryableErrors) return false
            throw exception
        }
        return true
    }

    private fun pathOf(groupId: String): String = "$groupId/$FILE_NAME"

    private companion object {
        const val BUCKET = "group-photos"
        const val FILE_NAME = "photo.jpg"
    }
}
