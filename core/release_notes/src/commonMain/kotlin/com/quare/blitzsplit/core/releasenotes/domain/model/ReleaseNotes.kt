package com.quare.blitzsplit.core.releasenotes.domain.model

// Why: what this app shows of a version: only the notes that are for the platform it runs on.
data class ReleaseNotes(
    val version: AppVersion,
    val date: ReleaseDate,
    // Why: the version this app is, which the screen marks.
    val isInstalled: Boolean,
    val notes: List<String>,
)
