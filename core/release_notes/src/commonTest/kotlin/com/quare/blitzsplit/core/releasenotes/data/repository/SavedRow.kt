package com.quare.blitzsplit.core.releasenotes.data.repository

// Why: a row of the fake table of ReleaseNotesRepositoryImplTest: the feed kept for a language, as the entity holds it.
internal data class SavedRow(
    val json: String,
    val etag: String?,
    val checkedAt: Long,
)
