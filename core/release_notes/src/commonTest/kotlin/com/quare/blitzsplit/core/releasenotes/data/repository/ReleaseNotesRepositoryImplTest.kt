package com.quare.blitzsplit.core.releasenotes.data.repository

import com.quare.blitzsplit.core.releasenotes.data.datasource.ReleaseNotesLocalDataSource
import com.quare.blitzsplit.core.releasenotes.data.datasource.ReleaseNotesRemoteAnswer
import com.quare.blitzsplit.core.releasenotes.data.datasource.ReleaseNotesRemoteDataSource
import com.quare.blitzsplit.core.releasenotes.data.datasource.SavedReleaseNotesFeed
import com.quare.blitzsplit.core.releasenotes.data.parser.ReleaseNotesFeedParserImpl
import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedRelease
import com.quare.blitzsplit.core.releasenotes.feedJson
import com.quare.blitzsplit.core.releasenotes.published
import com.quare.blitzsplit.core.releasenotes.releaseJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class ReleaseNotesRepositoryImplTest {
    private lateinit var local: FakeReleaseNotesLocalDataSource
    private lateinit var remote: FakeReleaseNotesRemoteDataSource
    private lateinit var repository: ReleaseNotesRepositoryImpl

    private val oldFeed = feedJson(releaseJson("3.0.0"))
    private val newFeed = feedJson(releaseJson("3.1.0"), releaseJson("3.0.0"))
    private val englishFeed = feedJson(releaseJson("3.1.0"))
    private val errorPage = "<html>Sign in to the hotel wifi</html>"

    @Test
    fun `GIVEN nothing was ever downloaded WHEN observing the versions THEN there is none yet`() = runTest {
        // Given
        prepareScenario(site = mapOf("pt" to newFeed))

        // When
        val releases = repository.observeReleases("pt").first()

        // Then
        assertNull(releases)
        assertTrue(remote.requests.isEmpty())
    }

    @Test
    fun `GIVEN a feed was saved in Portuguese WHEN observing it offline THEN gives its versions newest first`() =
        runTest {
            // Given
            prepareScenario(
                saved = mapOf("pt" to savedFeed(json = feedJson(releaseJson("3.0.0"), releaseJson("3.1.0")))),
                siteError = IllegalStateException("offline"),
            )

            // When
            val releases = repository.observeReleases("pt").first()

            // Then
            assertEquals(listOf(published("3.1.0"), published("3.0.0")), releases)
            assertTrue(remote.requests.isEmpty())
        }

    @Test
    fun `GIVEN only the Portuguese feed was saved WHEN observing the Spanish one THEN there is none yet`() = runTest {
        // Given
        prepareScenario(saved = mapOf("pt" to savedFeed(json = oldFeed)))

        // When
        val releases = repository.observeReleases("es").first()

        // Then
        assertNull(releases)
    }

    @Test
    fun `GIVEN the screen is open with nothing saved WHEN a refresh downloads the feed THEN the screen gets it`() =
        runTest {
            // Given
            prepareScenario(site = mapOf("pt" to newFeed))
            val seen = mutableListOf<List<PublishedRelease>?>()
            backgroundScope.launch { repository.observeReleases("pt").collect(seen::add) }
            runCurrent()

            // When
            repository.refreshReleases("pt")
            runCurrent()

            // Then
            assertEquals(listOf(null, listOf(published("3.1.0"), published("3.0.0"))), seen)
        }

    @Test
    fun `GIVEN the screen is open with an old feed WHEN a refresh downloads a new one THEN the screen gets it`() =
        runTest {
            // Given
            prepareScenario(
                saved = mapOf(
                    "pt" to savedFeed(
                        json = oldFeed,
                        checkedAt = NOW - 2.days.ms,
                    ),
                ),
                site = mapOf("pt" to newFeed),
            )
            val seen = mutableListOf<List<String>?>()
            backgroundScope.launch {
                repository.observeReleases("pt").collect { releases -> seen += releases?.map { it.version.fullName } }
            }
            runCurrent()

            // When
            repository.refreshReleases("pt")
            runCurrent()

            // Then
            assertEquals(listOf<List<String>?>(listOf("3.0.0"), listOf("3.1.0", "3.0.0")), seen)
        }

    @Test
    fun `GIVEN nothing saved WHEN refreshing THEN downloads the feed and saves it with its ETag and the time`() =
        runTest {
            // Given
            prepareScenario(
                site = mapOf("pt" to newFeed),
                siteEtags = mapOf("pt" to "\"new\""),
            )

            // When
            val result = repository.refreshReleases("pt")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(mapOf("pt" to SavedRow(newFeed, "\"new\"", NOW)), local.rows.value)
            assertEquals(listOf("pt"), remote.requests)
            assertEquals(listOf<String?>(null), remote.sentEtags)
        }

    @Test
    fun `GIVEN nothing saved and a site that sends no ETag WHEN refreshing THEN saves the feed without one`() =
        runTest {
            // Given
            prepareScenario(site = mapOf("pt" to newFeed))

            // When
            val result = repository.refreshReleases("pt")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(mapOf("pt" to SavedRow(newFeed, null, NOW)), local.rows.value)
        }

    @Test
    fun `GIVEN the feed was confirmed an hour ago WHEN refreshing THEN succeeds without asking the site`() = runTest {
        // Given
        val saved = mapOf(
            "pt" to savedFeed(
                json = oldFeed,
                etag = "\"old\"",
                checkedAt = NOW - 1.hours.ms,
            ),
        )
        prepareScenario(
            saved = saved,
            site = mapOf("pt" to newFeed),
        )

        // When
        val result = repository.refreshReleases("pt")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.requests.isEmpty())
        assertEquals(saved, local.rows.value)
    }

    @Test
    fun `GIVEN the feed was confirmed a millisecond less than a day ago WHEN refreshing THEN asks nothing`() = runTest {
        // Given
        prepareScenario(
            saved = mapOf(
                "pt" to savedFeed(
                    json = oldFeed,
                    checkedAt = NOW - 1.days.ms + 1,
                ),
            ),
            site = mapOf("pt" to newFeed),
        )

        // When
        val result = repository.refreshReleases("pt")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.requests.isEmpty())
    }

    @Test
    fun `GIVEN the feed was confirmed exactly a day ago WHEN refreshing THEN asks again with the saved ETag`() =
        runTest {
            // Given
            prepareScenario(
                saved = mapOf(
                    "pt" to savedFeed(
                        json = oldFeed,
                        etag = "\"old\"",
                        checkedAt = NOW - 1.days.ms,
                    ),
                ),
                site = mapOf("pt" to newFeed),
                siteEtags = mapOf("pt" to "\"new\""),
            )

            // When
            val result = repository.refreshReleases("pt")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("pt"), remote.requests)
            assertEquals(listOf<String?>("\"old\""), remote.sentEtags)
            assertEquals(mapOf("pt" to SavedRow(newFeed, "\"new\"", NOW)), local.rows.value)
        }

    @Test
    fun `GIVEN the feed was confirmed a week ago WHEN refreshing THEN asks the site again`() = runTest {
        // Given
        prepareScenario(
            saved = mapOf(
                "pt" to savedFeed(
                    json = oldFeed,
                    checkedAt = NOW - 7.days.ms,
                ),
            ),
            site = mapOf("pt" to newFeed),
        )

        // When
        val result = repository.refreshReleases("pt")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("pt"), remote.requests)
    }

    @Test
    fun `GIVEN the clock was set back before the last check WHEN refreshing THEN asks the site again`() = runTest {
        // Given
        prepareScenario(
            saved = mapOf(
                "pt" to savedFeed(
                    json = oldFeed,
                    etag = "\"old\"",
                    checkedAt = NOW + 1.hours.ms,
                ),
            ),
            site = mapOf("pt" to newFeed),
        )

        // When
        val result = repository.refreshReleases("pt")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("pt"), remote.requests)
        assertEquals(mapOf("pt" to SavedRow(newFeed, null, NOW)), local.rows.value)
    }

    @Test
    fun `GIVEN a fresh Portuguese feed and no Spanish one WHEN refreshing Spanish THEN downloads it`() = runTest {
        // Given
        val portuguese = savedFeed(
            json = oldFeed,
            checkedAt = NOW - 1.hours.ms,
        )
        prepareScenario(
            saved = mapOf("pt" to portuguese),
            site = mapOf("es" to newFeed),
        )

        // When
        val result = repository.refreshReleases("es")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("es"), remote.requests)
        assertEquals(mapOf("pt" to portuguese, "es" to SavedRow(newFeed, null, NOW)), local.rows.value)
    }

    @Test
    fun `GIVEN the saved feed is still the current one WHEN the site answers 304 THEN only moves the check time`() =
        runTest {
            // Given
            prepareScenario(
                saved = mapOf(
                    "pt" to savedFeed(
                        json = oldFeed,
                        etag = "\"old\"",
                        checkedAt = NOW - 2.days.ms,
                    ),
                ),
                site = mapOf("pt" to newFeed),
                siteEtags = mapOf("pt" to "\"old\""),
            )

            // When
            val result = repository.refreshReleases("pt")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf<String?>("\"old\""), remote.sentEtags)
            assertEquals(mapOf("pt" to SavedRow(oldFeed, "\"old\"", NOW)), local.rows.value)
            assertEquals(0, local.saveCount)
        }

    @Test
    fun `GIVEN a 304 moved the check time WHEN refreshing again right after THEN the site is not asked again`() =
        runTest {
            // Given
            prepareScenario(
                saved = mapOf(
                    "pt" to savedFeed(
                        json = oldFeed,
                        etag = "\"old\"",
                        checkedAt = NOW - 2.days.ms,
                    ),
                ),
                site = mapOf("pt" to newFeed),
                siteEtags = mapOf("pt" to "\"old\""),
            )
            repository.refreshReleases("pt")

            // When
            val result = repository.refreshReleases("pt")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("pt"), remote.requests)
        }

    @Test
    fun `GIVEN the site has no Turkish feed WHEN refreshing in Turkish THEN keeps the English feed under Turkish`() =
        runTest {
            // Given
            prepareScenario(
                site = mapOf("en" to englishFeed),
                siteEtags = mapOf("en" to "\"en\""),
            )

            // When
            val result = repository.refreshReleases("tr")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("tr", "en"), remote.requests)
            assertEquals(mapOf("tr" to SavedRow(englishFeed, "\"en\"", NOW)), local.rows.value)
        }

    @Test
    fun `GIVEN the English feed kept under Turkish is still current WHEN refreshing THEN only moves the check time`() =
        runTest {
            // Given
            prepareScenario(
                saved = mapOf(
                    "tr" to savedFeed(
                        json = englishFeed,
                        etag = "\"en\"",
                        checkedAt = NOW - 1.days.ms,
                    ),
                ),
                site = mapOf("en" to englishFeed),
                siteEtags = mapOf("en" to "\"en\""),
            )

            // When
            val result = repository.refreshReleases("tr")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("tr", "en"), remote.requests)
            assertEquals(listOf<String?>("\"en\"", "\"en\""), remote.sentEtags)
            assertEquals(mapOf("tr" to SavedRow(englishFeed, "\"en\"", NOW)), local.rows.value)
            assertEquals(0, local.saveCount)
        }

    @Test
    fun `GIVEN the site has no English feed WHEN refreshing in English THEN fails after asking once`() = runTest {
        // Given
        prepareScenario(site = mapOf("pt" to newFeed))

        // When
        val result = repository.refreshReleases("en")

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf("en"), remote.requests)
        assertTrue(local.rows.value.isEmpty())
    }

    @Test
    fun `GIVEN the site has neither the Turkish nor the English feed WHEN refreshing in Turkish THEN fails`() =
        runTest {
            // Given
            prepareScenario(site = emptyMap())

            // When
            val result = repository.refreshReleases("tr")

            // Then
            assertTrue(result.isFailure)
            assertEquals(listOf("tr", "en"), remote.requests)
            assertTrue(local.rows.value.isEmpty())
        }

    @Test
    fun `GIVEN the site answers an error page for Portuguese WHEN refreshing THEN keeps the English feed instead`() =
        runTest {
            // Given
            prepareScenario(
                site = mapOf("pt" to errorPage, "en" to englishFeed),
                siteEtags = mapOf("pt" to "\"page\"", "en" to "\"en\""),
            )

            // When
            val result = repository.refreshReleases("pt")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("pt", "en"), remote.requests)
            assertEquals(mapOf("pt" to SavedRow(englishFeed, "\"en\"", NOW)), local.rows.value)
        }

    @Test
    fun `GIVEN error pages for Portuguese and English WHEN refreshing THEN fails and keeps the saved feed as it was`() =
        runTest {
            // Given
            val saved = mapOf(
                "pt" to savedFeed(
                    json = oldFeed,
                    etag = "\"old\"",
                    checkedAt = NOW - 2.days.ms,
                ),
            )
            prepareScenario(
                saved = saved,
                site = mapOf("pt" to errorPage, "en" to errorPage),
            )

            // When
            val result = repository.refreshReleases("pt")

            // Then
            assertTrue(result.isFailure)
            assertEquals(listOf("pt", "en"), remote.requests)
            assertEquals(saved, local.rows.value)
            assertEquals(0, local.saveCount)
        }

    @Test
    fun `GIVEN the site answers an error page for English WHEN refreshing in English THEN fails after asking once`() =
        runTest {
            // Given
            prepareScenario(site = mapOf("en" to errorPage))

            // When
            val result = repository.refreshReleases("en")

            // Then
            assertTrue(result.isFailure)
            assertEquals(listOf("en"), remote.requests)
            assertTrue(local.rows.value.isEmpty())
        }

    @Test
    fun `GIVEN a feed confirmed two days ago WHEN refreshing offline THEN fails and leaves the saved feed as it was`() =
        runTest {
            // Given
            val saved = mapOf(
                "pt" to savedFeed(
                    json = oldFeed,
                    etag = "\"old\"",
                    checkedAt = NOW - 2.days.ms,
                ),
            )
            prepareScenario(
                saved = saved,
                siteError = IllegalStateException("offline"),
            )

            // When
            val result = repository.refreshReleases("pt")

            // Then
            assertEquals("offline", result.exceptionOrNull()?.message)
            assertEquals(saved, local.rows.value)
            assertEquals(0, local.saveCount)
        }

    @Test
    fun `GIVEN nothing saved WHEN the site answers a server error THEN fails without trying English`() = runTest {
        // Given
        prepareScenario(
            site = mapOf("en" to englishFeed),
            siteError = IllegalStateException("The release notes answered 500"),
        )

        // When
        val result = repository.refreshReleases("pt")

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf("pt"), remote.requests)
        assertTrue(local.rows.value.isEmpty())
    }

    @Test
    fun `GIVEN a refresh failed offline WHEN refreshing again THEN asks the site again`() = runTest {
        // Given
        prepareScenario(siteError = IllegalStateException("offline"))
        repository.refreshReleases("pt")

        // When
        val result = repository.refreshReleases("pt")

        // Then
        assertTrue(result.isFailure)
        assertEquals(listOf("pt", "pt"), remote.requests)
    }

    @Test
    fun `GIVEN the person leaves while the feed downloads WHEN refreshing THEN the cancellation goes on`() = runTest {
        // Given
        prepareScenario(siteError = CancellationException("left the screen"))

        // When
        val failure = assertFailsWith<CancellationException> { repository.refreshReleases("pt") }

        // Then
        assertEquals("left the screen", failure.message)
        assertEquals(0, local.saveCount)
    }

    @Test
    fun `GIVEN nothing saved WHEN the launch and the screen refresh at once THEN the site is asked once`() = runTest {
        // Given
        prepareScenario(site = mapOf("pt" to newFeed))

        // When
        val results = listOf(
            async { repository.refreshReleases("pt") },
            async { repository.refreshReleases("pt") },
        ).awaitAll()

        // Then
        assertTrue(results.all { it.isSuccess })
        assertEquals(listOf("pt"), remote.requests)
        assertEquals(1, local.saveCount)
    }

    @Test
    fun `GIVEN the feed was just downloaded WHEN refreshing again THEN the site is asked once`() = runTest {
        // Given
        prepareScenario(site = mapOf("pt" to newFeed))
        repository.refreshReleases("pt")

        // When
        val result = repository.refreshReleases("pt")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("pt"), remote.requests)
    }

    @Test
    fun `GIVEN the feed saved today is one the app cannot read WHEN refreshing THEN downloads it again`() = runTest {
        // Given
        prepareScenario(
            saved = mapOf("pt" to savedFeed(json = errorPage)),
            site = mapOf("pt" to newFeed),
        )

        // When
        val result = repository.refreshReleases("pt")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(published("3.1.0"), published("3.0.0")), repository.observeReleases("pt").first())
    }

    @Test
    fun `GIVEN an old saved feed the app cannot read WHEN refreshing THEN asks without its ETag and saves it`() =
        runTest {
            // Given
            prepareScenario(
                saved = mapOf(
                    "pt" to savedFeed(
                        json = errorPage,
                        etag = "\"abc\"",
                        checkedAt = NOW - 2.days.ms,
                    ),
                ),
                site = mapOf("pt" to newFeed),
                siteEtags = mapOf("pt" to "\"abc\""),
            )

            // When
            val result = repository.refreshReleases("pt")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf<String?>(null), remote.sentEtags)
            assertEquals(listOf(published("3.1.0"), published("3.0.0")), repository.observeReleases("pt").first())
        }

    private fun savedFeed(
        json: String,
        etag: String? = null,
        checkedAt: Long = NOW,
    ): SavedRow = SavedRow(
        json = json,
        etag = etag,
        checkedAt = checkedAt,
    )

    private fun prepareScenario(
        saved: Map<String, SavedRow> = emptyMap(),
        site: Map<String, String> = emptyMap(),
        siteEtags: Map<String, String> = emptyMap(),
        siteError: Throwable? = null,
    ) {
        local = FakeReleaseNotesLocalDataSource(saved)
        remote = FakeReleaseNotesRemoteDataSource(
            site = site,
            etags = siteEtags,
            error = siteError,
        )
        repository = ReleaseNotesRepositoryImpl(
            local = local,
            remote = remote,
            parser = ReleaseNotesFeedParserImpl(),
            currentTimeMillis = { NOW },
        )
    }

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}

private val Duration.ms: Long
    get() = inWholeMilliseconds

// Why: rows holds, by the language asked for, the feed's JSON with its ETag and check time, as the table does.
private class FakeReleaseNotesLocalDataSource(
    initial: Map<String, SavedRow>,
) : ReleaseNotesLocalDataSource {
    val rows = MutableStateFlow(initial)
    var saveCount = 0

    override fun observeFeed(language: String): Flow<String?> = rows.map { it[language]?.json }

    override suspend fun getSavedFeed(language: String): SavedReleaseNotesFeed? = rows.value[language]?.let { row ->
        SavedReleaseNotesFeed(
            json = row.json,
            etag = row.etag,
            checkedAt = row.checkedAt,
        )
    }

    override suspend fun saveFeed(
        language: String,
        json: String,
        etag: String?,
        checkedAt: Long,
    ) {
        saveCount++
        rows.update { it + (language to SavedRow(json, etag, checkedAt)) }
    }

    override suspend fun markChecked(
        language: String,
        checkedAt: Long,
    ) {
        rows.update { all ->
            all[language]?.let { row -> all + (language to row.copy(checkedAt = checkedAt)) } ?: all
        }
    }
}

/*
 * Why: site holds what the site answers 200 with for each language, any other answering 404; etags their ETags, a
 * matching one answering 304. The answer takes a moment, so a second refresh may start while the first one waits.
 */
private class FakeReleaseNotesRemoteDataSource(
    private val site: Map<String, String>,
    private val etags: Map<String, String>,
    private val error: Throwable?,
) : ReleaseNotesRemoteDataSource {
    val requests = mutableListOf<String>()
    val sentEtags = mutableListOf<String?>()

    override suspend fun fetchFeed(
        language: String,
        etag: String?,
    ): ReleaseNotesRemoteAnswer {
        requests += language
        sentEtags += etag
        yield()
        error?.let { throw it }
        val json = site[language] ?: return ReleaseNotesRemoteAnswer.Missing
        if (etag != null && etag == etags[language]) return ReleaseNotesRemoteAnswer.Unchanged
        return ReleaseNotesRemoteAnswer.Feed(
            json = json,
            etag = etags[language],
        )
    }
}
