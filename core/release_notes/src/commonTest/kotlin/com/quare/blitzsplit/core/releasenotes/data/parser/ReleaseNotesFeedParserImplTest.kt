package com.quare.blitzsplit.core.releasenotes.data.parser

import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion
import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedNote
import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedRelease
import com.quare.blitzsplit.core.releasenotes.domain.model.ReleaseDate
import com.quare.blitzsplit.core.releasenotes.feedJson
import com.quare.blitzsplit.core.releasenotes.noteJson
import com.quare.blitzsplit.core.releasenotes.releaseJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReleaseNotesFeedParserImplTest {
    private val parser = ReleaseNotesFeedParserImpl()

    @Test
    fun `GIVEN a beta with a note for all and one for the Mac and the web WHEN reading the feed THEN gives it whole`() {
        // Given
        val json =
            """
            {
              "releases": [
                {
                  "version": "3.1.0-beta_2",
                  "beta": true,
                  "date": "2026-11-20",
                  "notes": [
                    { "text": "Faster sync", "platforms": ["all"] },
                    { "text": "A wider sidebar", "platforms": ["mac", "web"] }
                  ]
                }
              ]
            }
            """.trimIndent()

        // When
        val releases = parser.parse(json)

        // Then
        assertEquals(
            listOf(
                PublishedRelease(
                    version = AppVersion(
                        major = 3,
                        minor = 1,
                        patch = 0,
                        beta = 2,
                    ),
                    date = ReleaseDate(
                        year = 2026,
                        month = 11,
                        day = 20,
                    ),
                    notes = listOf(
                        PublishedNote(
                            text = "Faster sync",
                            platforms = setOf("all"),
                        ),
                        PublishedNote(
                            text = "A wider sidebar",
                            platforms = setOf("mac", "web"),
                        ),
                    ),
                ),
            ),
            releases,
        )
    }

    @Test
    fun `GIVEN a stable version dated in January WHEN reading the feed THEN gives its date and no beta`() {
        // Given
        val json = feedJson(
            releaseJson(
                version = "3.0.0",
                date = "2026-01-05",
            ),
        )

        // When
        val release = parser.parse(json)?.single()

        // Then
        assertEquals(AppVersion(3, 0, 0, beta = null), release?.version)
        assertEquals(ReleaseDate(2026, 1, 5), release?.date)
    }

    @Test
    fun `GIVEN the site wrote the versions oldest first WHEN reading the feed THEN gives them newest first`() {
        // Given
        val json = feedJson(
            releaseJson("3.0.0-beta_9"),
            releaseJson("3.0.0"),
            releaseJson("3.0.0-beta_10"),
            releaseJson("3.1.0-beta_1"),
            releaseJson("2.9.0"),
            releaseJson("3.1.0"),
        )

        // When
        val releases = parser.parse(json)

        // Then
        assertEquals(
            listOf("3.1.0", "3.1.0-beta_1", "3.0.0", "3.0.0-beta_10", "3.0.0-beta_9", "2.9.0"),
            releases?.map { it.version.fullName },
        )
    }

    @Test
    fun `GIVEN a feed with keys this app does not know WHEN reading it THEN ignores them and gives the versions`() {
        // Given
        val json =
            """
            {
              "generated": "2026-11-20T10:00:00Z",
              "releases": [
                {
                  "version": "3.1.0",
                  "beta": false,
                  "date": "2026-11-20",
                  "highlight": { "image": "hero.png" },
                  "notes": [{ "text": "Faster sync", "platforms": ["all"], "kind": "improvement" }]
                }
              ]
            }
            """.trimIndent()

        // When
        val releases = parser.parse(json)

        // Then
        assertEquals(listOf("3.1.0"), releases?.map { it.version.fullName })
        assertEquals(listOf("Faster sync"), releases?.single()?.notes?.map(PublishedNote::text))
    }

    @Test
    fun `GIVEN a version without the beta key WHEN reading the feed THEN still gives it`() {
        // Given
        val json = """{"releases":[{"version":"3.1.0","date":"2026-11-20","notes":[]}]}"""

        // When
        val releases = parser.parse(json)

        // Then
        assertEquals(listOf("3.1.0"), releases?.map { it.version.fullName })
    }

    @Test
    fun `GIVEN a version named in a way the app can not read WHEN reading the feed THEN gives only the others`() {
        // Given
        val json = feedJson(
            releaseJson("3.2.0-rc1"),
            releaseJson("3.1.0"),
            releaseJson("next"),
            releaseJson("3.0.0"),
        )

        // When
        val releases = parser.parse(json)

        // Then
        assertEquals(listOf("3.1.0", "3.0.0"), releases?.map { it.version.fullName })
    }

    @Test
    fun `GIVEN a version dated in a way the app can not read WHEN reading the feed THEN gives only the others`() {
        // Given
        val json = feedJson(
            releaseJson(
                version = "3.3.0",
                date = "November 20, 2026",
            ),
            releaseJson(
                version = "3.2.0",
                date = "2026-11-20T10:00:00Z",
            ),
            releaseJson(
                version = "3.1.0",
                date = "20/11/2026",
            ),
            releaseJson(
                version = "3.0.1",
                date = "",
            ),
            releaseJson(
                version = "3.0.0",
                date = "2026-11-20",
            ),
        )

        // When
        val releases = parser.parse(json)

        // Then
        assertEquals(listOf("3.0.0"), releases?.map { it.version.fullName })
    }

    @Test
    fun `GIVEN a version dated in month 13 WHEN reading the feed THEN leaves it out and gives the others`() {
        // Given
        val json = feedJson(
            releaseJson(
                version = "3.1.0",
                date = "2026-13-01",
            ),
            releaseJson(
                version = "3.0.0",
                date = "2026-12-01",
            ),
        )

        // When
        val releases = parser.parse(json)

        // Then
        assertEquals(listOf("3.0.0"), releases?.map { it.version.fullName })
    }

    @Test
    fun `GIVEN a version dated on day 32 WHEN reading the feed THEN leaves it out and gives the others`() {
        // Given
        val json = feedJson(
            releaseJson(
                version = "3.1.0",
                date = "2026-01-32",
            ),
            releaseJson(
                version = "3.0.0",
                date = "2026-01-31",
            ),
        )

        // When
        val releases = parser.parse(json)

        // Then
        assertEquals(listOf("3.0.0"), releases?.map { it.version.fullName })
    }

    @Test
    fun `GIVEN versions dated on a day their month does not have WHEN reading the feed THEN leaves them out`() {
        // Given
        val json = feedJson(
            releaseJson(
                version = "3.3.0",
                date = "2026-02-29",
            ),
            releaseJson(
                version = "3.2.0",
                date = "2026-04-31",
            ),
            releaseJson(
                version = "3.1.0",
                date = "2100-02-29",
            ),
            releaseJson(
                version = "3.0.0",
                date = "2028-02-29",
            ),
        )

        // When
        val releases = parser.parse(json)

        // Then
        assertEquals(listOf("3.0.0"), releases?.map { it.version.fullName })
    }

    @Test
    fun `GIVEN the same version listed twice WHEN reading the feed THEN gives it once as first listed`() {
        // Given
        val json = feedJson(
            releaseJson(
                version = "3.1.0",
                date = "2026-11-20",
            ),
            releaseJson(
                version = "3.01.0",
                date = "2026-11-21",
            ),
            releaseJson(
                version = "3.0.0",
                date = "2026-10-01",
            ),
        )

        // When
        val releases = parser.parse(json)

        // Then
        assertEquals(
            listOf("3.1.0" to ReleaseDate(2026, 11, 20), "3.0.0" to ReleaseDate(2026, 10, 1)),
            releases?.map { it.version.fullName to it.date },
        )
    }

    @Test
    fun `GIVEN versions dated in month zero and on day zero WHEN reading the feed THEN leaves them out`() {
        // Given
        val json = feedJson(
            releaseJson(
                version = "3.2.0",
                date = "2026-00-10",
            ),
            releaseJson(
                version = "3.1.0",
                date = "2026-10-00",
            ),
            releaseJson(
                version = "3.0.0",
                date = "2026-10-01",
            ),
        )

        // When
        val releases = parser.parse(json)

        // Then
        assertEquals(listOf("3.0.0"), releases?.map { it.version.fullName })
    }

    @Test
    fun `GIVEN a note that names Android twice and iOS WHEN reading the feed THEN keeps its text and both platforms`() {
        // Given
        val json = feedJson(
            releaseJson(
                version = "3.1.0",
                notes = listOf(
                    noteJson(
                        text = "Scan a receipt",
                        platforms = listOf("android", "ios", "android"),
                    ),
                ),
            ),
        )

        // When
        val notes = parser.parse(json)?.single()?.notes

        // Then
        assertEquals(
            listOf(
                PublishedNote(
                    text = "Scan a receipt",
                    platforms = setOf("android", "ios"),
                ),
            ),
            notes,
        )
    }

    @Test
    fun `GIVEN an error page in HTML WHEN reading it THEN it is not the feed`() {
        // Given
        val json = "<html><body>502 Bad gateway</body></html>"

        // When
        val releases = parser.parse(json)

        // Then
        assertNull(releases)
    }

    @Test
    fun `GIVEN an empty text WHEN reading it THEN it is not the feed`() {
        // Given
        val json = ""

        // When
        val releases = parser.parse(json)

        // Then
        assertNull(releases)
    }

    @Test
    fun `GIVEN JSON of another shape WHEN reading each THEN none is the feed`() {
        // Given
        val texts = listOf(
            "{}",
            "[]",
            "null",
            "\"releases\"",
            """{"versions":[]}""",
            """{"releases":{}}""",
            """{"releases":"none"}""",
            """{"releases":null}""",
            """{"releases":[{"version":"3.1.0","date":"2026-11-20","notes":[]}""",
        )

        // When
        val read = texts.associateWith(parser::parse)

        // Then
        assertEquals(emptyMap(), read.filterValues { it != null })
    }

    @Test
    fun `GIVEN a version missing its notes among readable ones WHEN reading the feed THEN it is not the feed`() {
        // Given
        val json = feedJson(
            releaseJson("3.1.0"),
            """{"version":"3.0.0","beta":false,"date":"2026-10-01"}""",
        )

        // When
        val releases = parser.parse(json)

        // Then
        assertNull(releases)
    }

    @Test
    fun `GIVEN a version or a note missing one of its keys WHEN reading each feed THEN none is the feed`() {
        // Given
        val texts = listOf(
            """{"releases":[{"date":"2026-11-20","notes":[]}]}""",
            """{"releases":[{"version":"3.1.0","notes":[]}]}""",
            """{"releases":[{"version":"3.1.0","date":"2026-11-20","notes":[{"text":"Faster sync"}]}]}""",
            """{"releases":[{"version":"3.1.0","date":"2026-11-20","notes":[{"platforms":["all"]}]}]}""",
        )

        // When
        val read = texts.associateWith(parser::parse)

        // Then
        assertEquals(emptyMap(), read.filterValues { it != null })
    }

    @Test
    fun `GIVEN a feed that lists no version WHEN reading it THEN gives no version rather than no feed`() {
        // Given
        val json = feedJson()

        // When
        val releases = parser.parse(json)

        // Then
        assertEquals(emptyList(), releases)
    }
}
