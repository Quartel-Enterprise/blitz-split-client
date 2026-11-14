package com.quare.blitzsplit.shared.app

import com.quare.blitzsplit.core.legal.domain.usecase.PrefetchLegalDocuments
import com.quare.blitzsplit.core.releasenotes.domain.usecase.PrefetchReleaseNotes

/*
 * Why: what the app reads from blitzsplit.com and keeps for offline, the legal texts and the release notes, brought
 * up to date together; one dependency of AppViewModel, whose constructor is at the size Koin's viewModelOf takes.
 */
class PrefetchSiteTexts(
    private val prefetchLegalDocuments: PrefetchLegalDocuments,
    private val prefetchReleaseNotes: PrefetchReleaseNotes,
) {
    suspend operator fun invoke() {
        prefetchLegalDocuments()
        prefetchReleaseNotes()
    }
}
