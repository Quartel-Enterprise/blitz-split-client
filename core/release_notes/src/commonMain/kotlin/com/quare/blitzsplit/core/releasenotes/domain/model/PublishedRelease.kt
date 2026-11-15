package com.quare.blitzsplit.core.releasenotes.domain.model

// Why: a version as the site publishes it, with the notes of every platform; ReleaseNotes is what one app shows of it.
data class PublishedRelease(
    val version: AppVersion,
    val date: ReleaseDate,
    val notes: List<PublishedNote>,
)
