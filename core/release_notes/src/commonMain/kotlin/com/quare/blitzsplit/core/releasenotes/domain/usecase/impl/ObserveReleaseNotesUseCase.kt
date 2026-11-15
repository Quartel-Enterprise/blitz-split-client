package com.quare.blitzsplit.core.releasenotes.domain.usecase.impl

import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion
import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedNote
import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedRelease
import com.quare.blitzsplit.core.releasenotes.domain.model.ReleaseNotes
import com.quare.blitzsplit.core.releasenotes.domain.repository.ReleaseNotesRepository
import com.quare.blitzsplit.core.releasenotes.domain.usecase.GetInstalledVersion
import com.quare.blitzsplit.core.releasenotes.domain.usecase.ObserveReleaseNotes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveReleaseNotesUseCase(
    private val repository: ReleaseNotesRepository,
    private val platform: ClientPlatform,
    private val getInstalledVersion: GetInstalledVersion,
) : ObserveReleaseNotes {
    override fun invoke(language: String): Flow<List<ReleaseNotes>?> {
        val installed = getInstalledVersion()
        return repository.observeReleases(language).map { releases ->
            releases?.mapNotNull { release -> release.toShownOrNull(installed) }
        }
    }

    // Why: an app whose own version can't be read (a local build) hides nothing by version.
    private fun PublishedRelease.toShownOrNull(installed: AppVersion?): ReleaseNotes? {
        if (installed != null && version > installed) return null
        val ownNotes = notes.filter { it.isFor(platform) }.map(PublishedNote::text)
        // Why: a version that changed only other platforms has nothing to say here.
        if (ownNotes.isEmpty()) return null
        return ReleaseNotes(
            version = version,
            date = date,
            isInstalled = version == installed,
            notes = ownNotes,
        )
    }

    private fun PublishedNote.isFor(platform: ClientPlatform): Boolean =
        EVERY_PLATFORM in platforms || platform.feedName in platforms

    // Why: the names the site's feed gives the platforms; iOS is the iPhone's and the iPad's.
    private val ClientPlatform.feedName: String
        get() = when (this) {
            ClientPlatform.ANDROID -> "android"
            ClientPlatform.IOS -> "ios"
            ClientPlatform.MACOS -> "mac"
            ClientPlatform.WEB -> "web"
        }

    private companion object {
        const val EVERY_PLATFORM = "all"
    }
}
