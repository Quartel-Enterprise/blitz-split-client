package com.quare.blitzsplit.core.releasenotes.domain.usecase

import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedRelease
import com.quare.blitzsplit.core.releasenotes.domain.model.ReleaseDate
import com.quare.blitzsplit.core.releasenotes.domain.model.ReleaseNotes
import com.quare.blitzsplit.core.releasenotes.domain.usecase.impl.ObserveReleaseNotesUseCase
import com.quare.blitzsplit.core.releasenotes.note
import com.quare.blitzsplit.core.releasenotes.published
import com.quare.blitzsplit.core.releasenotes.version
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveReleaseNotesUseCaseTest {
    private lateinit var repository: FakeReleaseNotesRepository
    private lateinit var observeReleaseNotes: ObserveReleaseNotesUseCase

    private val onePerPlatform = published(
        version = "3.1.0",
        notes = listOf(
            note("For Android", "android"),
            note("For the iPhone", "ios"),
            note("For the Mac", "mac"),
            note("For the web", "web"),
        ),
    )
    private val history = listOf(
        published("3.1.0"),
        published("3.1.0-beta_3"),
        published("3.1.0-beta_2"),
        published("3.1.0-beta_1"),
        published("3.0.1"),
        published("3.0.1-beta_1"),
        published("3.0.0"),
        published("3.0.0-beta_2"),
        published("3.0.0-beta_1"),
        published("2.9.0"),
    )

    @Test
    fun `GIVEN a note for all and one only for Android and iOS WHEN the web observes THEN shows only the first`() =
        runTest {
            // Given
            prepareScenario(
                platform = ClientPlatform.WEB,
                saved = listOf(
                    published(
                        version = "3.1.0",
                        notes = listOf(note("Faster sync", "all"), note("Scan a receipt", "android", "ios")),
                    ),
                ),
            )

            // When
            val shown = observeReleaseNotes("pt").first()

            // Then
            assertEquals(listOf(listOf("Faster sync")), shown?.map(ReleaseNotes::notes))
        }

    @Test
    fun `GIVEN a version with one note per platform WHEN Android observes THEN shows only the note for Android`() =
        runTest {
            // Given
            prepareScenario(
                platform = ClientPlatform.ANDROID,
                saved = listOf(onePerPlatform),
            )

            // When
            val shown = observeReleaseNotes("pt").first()

            // Then
            assertEquals(listOf(listOf("For Android")), shown?.map(ReleaseNotes::notes))
        }

    @Test
    fun `GIVEN a version with one note per platform WHEN the iPhone observes THEN shows only the note for iOS`() =
        runTest {
            // Given
            prepareScenario(
                platform = ClientPlatform.IOS,
                saved = listOf(onePerPlatform),
            )

            // When
            val shown = observeReleaseNotes("pt").first()

            // Then
            assertEquals(listOf(listOf("For the iPhone")), shown?.map(ReleaseNotes::notes))
        }

    @Test
    fun `GIVEN a version with one note per platform WHEN the Mac observes THEN shows only the note for the Mac`() =
        runTest {
            // Given
            prepareScenario(
                platform = ClientPlatform.MACOS,
                saved = listOf(onePerPlatform),
            )

            // When
            val shown = observeReleaseNotes("pt").first()

            // Then
            assertEquals(listOf(listOf("For the Mac")), shown?.map(ReleaseNotes::notes))
        }

    @Test
    fun `GIVEN a version with one note per platform WHEN the web observes THEN shows only the note for the web`() =
        runTest {
            // Given
            prepareScenario(
                platform = ClientPlatform.WEB,
                saved = listOf(onePerPlatform),
            )

            // When
            val shown = observeReleaseNotes("pt").first()

            // Then
            assertEquals(listOf(listOf("For the web")), shown?.map(ReleaseNotes::notes))
        }

    @Test
    fun `GIVEN notes for all platforms among others WHEN the Mac observes THEN shows its notes in the order written`() =
        runTest {
            // Given
            prepareScenario(
                platform = ClientPlatform.MACOS,
                saved = listOf(
                    published(
                        version = "3.1.0",
                        notes = listOf(
                            note("A wider sidebar", "mac", "web"),
                            note("Widgets", "android"),
                            note("Faster sync", "all"),
                            note("Menu bar shortcuts", "mac"),
                        ),
                    ),
                ),
            )

            // When
            val shown = observeReleaseNotes("pt").first()

            // Then
            assertEquals(
                listOf(listOf("A wider sidebar", "Faster sync", "Menu bar shortcuts")),
                shown?.map(ReleaseNotes::notes),
            )
        }

    @Test
    fun `GIVEN a version that changed only Android and iOS WHEN the web observes THEN the version is not listed`() =
        runTest {
            // Given
            prepareScenario(
                platform = ClientPlatform.WEB,
                saved = listOf(
                    published("3.1.0", notes = listOf(note("Scan a receipt", "android", "ios"))),
                    published("3.0.0", notes = listOf(note("Faster sync", "all"))),
                ),
            )

            // When
            val shown = observeReleaseNotes("pt").first()

            // Then
            assertEquals(listOf("3.0.0"), shown?.map { it.version.fullName })
        }

    @Test
    fun `GIVEN a version without notes WHEN observing THEN the version is not listed`() = runTest {
        // Given
        prepareScenario(saved = listOf(published("3.1.0", notes = emptyList()), published("3.0.0")))

        // When
        val shown = observeReleaseNotes("pt").first()

        // Then
        assertEquals(listOf("3.0.0"), shown?.map { it.version.fullName })
    }

    @Test
    fun `GIVEN a note for a platform the app does not know WHEN observing THEN no app shows it`() = runTest {
        // Given
        prepareScenario(
            platform = ClientPlatform.IOS,
            saved = listOf(published("3.1.0", notes = listOf(note("A complication", "watch", "IOS", "ipad")))),
        )

        // When
        val shown = observeReleaseNotes("pt").first()

        // Then
        assertEquals(emptyList(), shown)
    }

    @Test
    fun `GIVEN the stable 3_0_0 installed WHEN observing THEN hides what came after it and shows it and its betas`() =
        runTest {
            // Given
            prepareScenario(
                installed = "3.0.0",
                saved = history,
            )

            // When
            val shown = observeReleaseNotes("pt").first()

            // Then
            assertEquals(
                listOf("3.0.0", "3.0.0-beta_2", "3.0.0-beta_1", "2.9.0"),
                shown?.map { it.version.fullName },
            )
        }

    @Test
    fun `GIVEN the beta 2 of 3_1_0 installed WHEN observing THEN hides beta 3 and the stable and shows the rest`() =
        runTest {
            // Given
            prepareScenario(
                installed = "3.1.0-beta_2",
                saved = history,
            )

            // When
            val shown = observeReleaseNotes("pt").first()

            // Then
            assertEquals(
                listOf(
                    "3.1.0-beta_2",
                    "3.1.0-beta_1",
                    "3.0.1",
                    "3.0.1-beta_1",
                    "3.0.0",
                    "3.0.0-beta_2",
                    "3.0.0-beta_1",
                    "2.9.0",
                ),
                shown?.map { it.version.fullName },
            )
        }

    @Test
    fun `GIVEN an installed version the site did not publish WHEN observing THEN shows the older and marks none`() =
        runTest {
            // Given
            prepareScenario(
                installed = "3.0.2",
                saved = history,
            )

            // When
            val shown = observeReleaseNotes("pt").first()

            // Then
            assertEquals("3.0.1", shown?.first()?.version?.fullName)
            assertEquals(emptyList(), shown?.filter(ReleaseNotes::isInstalled))
        }

    @Test
    fun `GIVEN the stable 3_0_0 installed WHEN observing THEN marks it as installed and no other`() = runTest {
        // Given
        prepareScenario(
            installed = "3.0.0",
            saved = history,
        )

        // When
        val shown = observeReleaseNotes("pt").first()

        // Then
        assertEquals(listOf("3.0.0"), shown?.filter(ReleaseNotes::isInstalled)?.map { it.version.fullName })
    }

    @Test
    fun `GIVEN the beta 2 of 3_1_0 installed WHEN observing THEN marks that beta as installed and no other`() =
        runTest {
            // Given
            prepareScenario(
                installed = "3.1.0-beta_2",
                saved = history,
            )

            // When
            val shown = observeReleaseNotes("pt").first()

            // Then
            assertEquals(listOf("3.1.0-beta_2"), shown?.filter(ReleaseNotes::isInstalled)?.map { it.version.fullName })
        }

    @Test
    fun `GIVEN a local build whose version can not be read WHEN observing THEN hides none by version and marks none`() =
        runTest {
            // Given
            prepareScenario(
                installed = null,
                saved = history,
            )

            // When
            val shown = observeReleaseNotes("pt").first()

            // Then
            assertEquals(history.map { it.version }, shown?.map(ReleaseNotes::version))
            assertEquals(emptyList(), shown?.filter(ReleaseNotes::isInstalled))
        }

    @Test
    fun `GIVEN a saved version WHEN observing THEN gives its version its date and its notes`() = runTest {
        // Given
        val date = ReleaseDate(
            year = 2026,
            month = 3,
            day = 9,
        )
        prepareScenario(
            platform = ClientPlatform.ANDROID,
            installed = "3.1.0",
            saved = listOf(
                published(
                    version = "3.1.0-beta_1",
                    notes = listOf(note("Widgets", "android"), note("Faster sync", "all")),
                    date = date,
                ),
            ),
        )

        // When
        val shown = observeReleaseNotes("pt").first()

        // Then
        assertEquals(
            listOf(
                ReleaseNotes(
                    version = version("3.1.0-beta_1"),
                    date = date,
                    isInstalled = false,
                    notes = listOf("Widgets", "Faster sync"),
                ),
            ),
            shown,
        )
    }

    @Test
    fun `GIVEN nothing saved yet WHEN observing THEN there is no list rather than an empty one`() = runTest {
        // Given
        prepareScenario(saved = null)

        // When
        val shown = observeReleaseNotes("pt").first()

        // Then
        assertNull(shown)
    }

    @Test
    fun `GIVEN a saved feed without versions WHEN observing THEN the list is empty rather than missing`() = runTest {
        // Given
        prepareScenario(saved = emptyList())

        // When
        val shown = observeReleaseNotes("pt").first()

        // Then
        assertEquals(emptyList(), shown)
    }

    @Test
    fun `GIVEN every saved version is newer than the installed one WHEN observing THEN the list is empty`() = runTest {
        // Given
        prepareScenario(
            installed = "2.0.0",
            saved = history,
        )

        // When
        val shown = observeReleaseNotes("pt").first()

        // Then
        assertEquals(emptyList(), shown)
    }

    @Test
    fun `GIVEN feeds saved in Portuguese and in English WHEN observing in Portuguese THEN shows the Portuguese one`() =
        runTest {
            // Given
            prepareScenario(saved = listOf(published("3.0.0", notes = listOf(note("Sincroniza mais rápido")))))
            repository.releases.value += "en" to listOf(published("3.0.0", notes = listOf(note("Faster sync"))))

            // When
            val shown = observeReleaseNotes("pt").first()

            // Then
            assertEquals(listOf(listOf("Sincroniza mais rápido")), shown?.map(ReleaseNotes::notes))
            assertEquals(listOf("pt"), repository.observed)
        }

    @Test
    fun `GIVEN the screen is open with nothing saved WHEN the feed is saved THEN the screen follows it`() = runTest {
        // Given
        prepareScenario(saved = null)
        val seen = mutableListOf<List<String>?>()
        backgroundScope.launch {
            observeReleaseNotes("pt").collect { shown -> seen += shown?.map { it.version.fullName } }
        }
        runCurrent()

        // When
        repository.releases.value = mapOf("pt" to listOf(published("3.1.0"), published("3.0.0")))
        runCurrent()

        // Then
        assertEquals(listOf(null, listOf("3.1.0", "3.0.0")), seen)
    }

    private fun prepareScenario(
        platform: ClientPlatform = ClientPlatform.ANDROID,
        installed: String? = null,
        saved: List<PublishedRelease>? = emptyList(),
    ) {
        repository = FakeReleaseNotesRepository().also { fake ->
            saved?.let { fake.releases.value = mapOf("pt" to it) }
        }
        observeReleaseNotes = ObserveReleaseNotesUseCase(
            repository = repository,
            platform = platform,
            getInstalledVersion = { installed?.let(::version) },
        )
    }
}
