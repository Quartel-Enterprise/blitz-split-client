package com.quare.blitzsplit.core.releasenotes.data.repository

import com.quare.blitzsplit.core.releasenotes.data.datasource.ReleaseNotesLocalDataSource
import com.quare.blitzsplit.core.releasenotes.data.datasource.ReleaseNotesRemoteAnswer
import com.quare.blitzsplit.core.releasenotes.data.datasource.ReleaseNotesRemoteDataSource
import com.quare.blitzsplit.core.releasenotes.data.datasource.SavedReleaseNotesFeed
import com.quare.blitzsplit.core.releasenotes.data.datasource.SeenInstalledVersion
import com.quare.blitzsplit.core.releasenotes.data.parser.ReleaseNotesFeedParser
import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion
import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedRelease
import com.quare.blitzsplit.core.releasenotes.domain.repository.ReleaseNotesRepository
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class ReleaseNotesRepositoryImpl(
    private val local: ReleaseNotesLocalDataSource,
    private val remote: ReleaseNotesRemoteDataSource,
    private val parser: ReleaseNotesFeedParser,
    private val currentTimeMillis: CurrentTimeMillis,
) : ReleaseNotesRepository {
    /*
     * Why: the launch's download and the screen's may ask for the same feed at once; the second waits and finds it
     * fresh, so the site is asked once.
     */
    private val refreshMutex = Mutex()

    // Why: a version comes out every few days at most; a day keeps its notes from waiting long without asking at every launch.
    private val freshFor = 24.hours

    /*
     * Why: a feed without the installed version is asked again sooner, since the site gets a version's notes only
     * once its release ends, after the testers already have the build.
     */
    private val freshWhileMissingInstalledFor = 15.minutes

    // Why: a version released without notes never reaches the site, so the quicker checks stop after its first hours.
    private val missingInstalledChecksFor = 6.hours

    override fun observeReleases(language: String): Flow<List<PublishedRelease>?> = local
        .observeFeed(language)
        .distinctUntilChanged()
        .map { json -> json?.let(parser::parse) }

    override suspend fun refreshReleases(
        language: String,
        installedVersion: AppVersion?,
    ): Result<Unit> = refreshMutex.withLock {
        suspendRunCatching {
            /*
             * Why: a saved feed this version of the app can't read counts as none: it is downloaded again now, and
             * without its ETag, since an "unchanged" answer would leave the screen with nothing to draw.
             */
            val saved = local.getSavedFeed(language)
            val savedReleases = saved?.let { parser.parse(it.json) }
            val seenVersion = installedVersion?.let { findSeenVersion(it, saved) }
            if (saved == null || savedReleases == null) {
                download(
                    language = language,
                    savedEtag = null,
                    seenVersion = seenVersion,
                )
            } else if (!saved.isFresh(savedReleases, installedVersion, seenVersion)) {
                download(
                    language = language,
                    savedEtag = saved.etag,
                    seenVersion = seenVersion,
                )
            }
        }
    }

    /*
     * Why: the site is asked with the saved feed's ETag, so an unchanged one answers 304 with no body and only its
     * check time moves.
     */
    private suspend fun download(
        language: String,
        savedEtag: String?,
        seenVersion: SeenInstalledVersion?,
    ) {
        val answer = fetchReadable(
            language = language,
            etag = savedEtag,
        ) ?: fetchFallback(
            language = language,
            etag = savedEtag,
        )
        checkNotNull(answer) { "The site has no release notes to read" }
        val now = currentTimeMillis()
        // Why: a readable answer is a new feed or Unchanged; Missing was already left for the fallback.
        if (answer is ReleaseNotesRemoteAnswer.Feed) {
            local.saveFeed(
                language = language,
                json = answer.json,
                etag = answer.etag,
                checkedAt = now,
                installedVersion = seenVersion,
            )
        } else {
            local.markChecked(
                language = language,
                checkedAt = now,
                installedVersion = seenVersion,
            )
        }
    }

    /*
     * Why: only a feed the app can read replaces the saved one, never an error page a proxy answered with; a language
     * answered with anything else still tries the English one.
     */
    private suspend fun fetchReadable(
        language: String,
        etag: String?,
    ): ReleaseNotesRemoteAnswer? = when (
        val answer = remote.fetchFeed(
            language = language,
            etag = etag,
        )
    ) {
        is ReleaseNotesRemoteAnswer.Feed -> answer.takeIf { parser.parse(it.json) != null }
        ReleaseNotesRemoteAnswer.Unchanged -> answer
        ReleaseNotesRemoteAnswer.Missing -> null
    }

    /*
     * Why: a language the site doesn't write the notes in keeps the English ones under its own name, so the saved ETag
     * is English's and an unchanged English feed answers 304 too.
     */
    private suspend fun fetchFallback(
        language: String,
        etag: String?,
    ): ReleaseNotesRemoteAnswer? = if (language == FALLBACK_LANGUAGE) {
        null
    } else {
        fetchReadable(
            language = FALLBACK_LANGUAGE,
            etag = etag,
        )
    }

    // Why: the time the saved feed was first checked on this version, or now when it never was.
    private fun findSeenVersion(
        installedVersion: AppVersion,
        saved: SavedReleaseNotesFeed?,
    ): SeenInstalledVersion = saved?.installedVersion?.takeIf { it.name == installedVersion.fullName }
        ?: SeenInstalledVersion(
            name = installedVersion.fullName,
            seenAt = currentTimeMillis(),
        )

    // Why: a clock set back since the last check never keeps a feed from being checked again.
    private fun SavedReleaseNotesFeed.isFresh(
        releases: List<PublishedRelease>,
        installedVersion: AppVersion?,
        seenVersion: SeenInstalledVersion?,
    ): Boolean {
        val now = currentTimeMillis()
        val elapsed = (now - checkedAt).milliseconds
        val isMissingInstalled = installedVersion != null && releases.none { it.version >= installedVersion }
        val isInFirstHours = seenVersion != null && (now - seenVersion.seenAt).milliseconds < missingInstalledChecksFor
        val freshness = if (isMissingInstalled && isInFirstHours) freshWhileMissingInstalledFor else freshFor
        return elapsed >= Duration.ZERO && elapsed < freshness
    }

    private companion object {
        // Why: a language the site doesn't write the release notes in reads them in English, as the app's own strings.
        const val FALLBACK_LANGUAGE = "en"
    }
}
