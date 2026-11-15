package com.quare.blitzsplit.core.releasenotes.data.datasource

fun interface ReleaseNotesRemoteDataSource {
    suspend fun fetchFeed(
        language: String,
        etag: String?,
    ): ReleaseNotesRemoteAnswer
}
