package com.quare.blitzsplit.feature.releasenotes.presentation.model

import com.quare.blitzsplit.core.releasenotes.domain.model.ReleaseNotes

data class ReleaseNotesUiState(
    /*
     * Why: every version this app may show, stable and beta, newest first, already narrowed to its platform's notes;
     * null until the device downloaded them.
     */
    val releases: List<ReleaseNotes>?,
    // Why: "Show beta versions"; null until the device's choice was read, so the list never flashes the other way.
    val showsBeta: Boolean?,
    // Why: the download failed; it only shows while there are no notes, since saved ones are drawn instead.
    val hasLoadFailed: Boolean,
) {
    // Why: the versions on screen: the stable ones, and the beta ones with "Show beta versions" on.
    val visibleReleases: List<ReleaseNotes>
        get() = releases.orEmpty().filter { release -> showsBeta == true || !release.version.isBeta }

    private val isLoaded: Boolean
        get() = releases != null && showsBeta != null

    val showsSkeleton: Boolean
        get() = !isLoaded && !showsLoadFailed

    // Why: "Couldn't load", with "Try again": offline (or the site down) before the device ever saved the notes.
    val showsLoadFailed: Boolean
        get() = releases == null && hasLoadFailed

    // Why: "Show beta versions" comes with the list, whichever way it stands.
    val showsBetaSwitch: Boolean
        get() = isLoaded

    // Why: nothing to list with the switch as it stands.
    val showsEmpty: Boolean
        get() = isLoaded && visibleReleases.isEmpty()

    // Why: the empty list says to turn the switch on when that would show something.
    val hasOnlyBetaReleases: Boolean
        get() = showsEmpty && releases.orEmpty().isNotEmpty()
}
