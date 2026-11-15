package com.quare.blitzsplit.shared.app

import com.quare.blitzsplit.core.legal.domain.usecase.PrefetchLegalDocuments
import com.quare.blitzsplit.core.releasenotes.domain.usecase.PrefetchReleaseNotes

/*
 * Why: the use cases for what the app reads from blitzsplit.com and keeps for offline, the legal texts and the release
 * notes, as one dependency of AppViewModel, whose constructor is at the size Koin's viewModelOf takes.
 */
data class SiteTextUseCases(
    val prefetchLegalDocuments: PrefetchLegalDocuments,
    val prefetchReleaseNotes: PrefetchReleaseNotes,
)
