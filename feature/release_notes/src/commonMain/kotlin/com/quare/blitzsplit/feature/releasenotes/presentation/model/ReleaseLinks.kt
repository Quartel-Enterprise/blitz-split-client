package com.quare.blitzsplit.feature.releasenotes.presentation.model

import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion

/*
 * Why: the GitHub pages of the releases. The web draws them as links, which a browser opens at once from the click,
 * while the other platforms open what the ViewModel hands them.
 */
object ReleaseLinks {
    const val ALL_RELEASES_URL = "https://github.com/Quartel-Enterprise/blitz-split-client/releases"

    // Why: the release workflows tag each version by its whole name, "3.1.0" or "3.1.0-beta_2".
    fun toReleaseUrl(version: AppVersion): String = "$ALL_RELEASES_URL/tag/${version.fullName}"
}
