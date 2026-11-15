package com.quare.blitzsplit.shared.app

import com.quare.blitzsplit.core.legal.domain.usecase.PrefetchLegalDocuments
import com.quare.blitzsplit.core.releasenotes.domain.usecase.PrefetchReleaseNotes
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/*
 * Why: what the app reads from blitzsplit.com and keeps for offline, the legal texts and the release notes, brought
 * up to date together; one dependency of AppViewModel, whose constructor is at the size Koin's viewModelOf takes.
 */
class PrefetchSiteTexts(
    private val prefetchLegalDocuments: PrefetchLegalDocuments,
    private val prefetchReleaseNotes: PrefetchReleaseNotes,
) {
    // Why: side by side, so a legal text stalling offline until its timeout doesn't hold the release notes back.
    suspend operator fun invoke() {
        coroutineScope {
            launch { prefetchLegalDocuments() }
            launch { prefetchReleaseNotes() }
        }
    }
}
