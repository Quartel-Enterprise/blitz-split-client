package com.quare.blitzsplit.core.releasenotes.domain.usecase

// Why: the language the release notes are read in, the one the app is speaking, as the code the site names its feeds by.
fun interface GetReleaseNotesLanguage {
    suspend operator fun invoke(): String
}
