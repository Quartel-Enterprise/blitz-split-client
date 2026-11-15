package com.quare.blitzsplit.core.releasenotes.domain.usecase

/*
 * Why: at launch and when Profile › Language changes, the release notes are brought up to date in the language the
 * app speaks, so "Release notes" opens offline too. A failure is left for the screen, which tries again.
 */
fun interface PrefetchReleaseNotes {
    suspend operator fun invoke()
}
