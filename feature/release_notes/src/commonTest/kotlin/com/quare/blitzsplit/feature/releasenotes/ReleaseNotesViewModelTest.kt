package com.quare.blitzsplit.feature.releasenotes

import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion
import com.quare.blitzsplit.core.releasenotes.domain.model.ReleaseDate
import com.quare.blitzsplit.core.releasenotes.domain.model.ReleaseNotes
import com.quare.blitzsplit.feature.releasenotes.presentation.model.ReleaseNotesUiAction
import com.quare.blitzsplit.feature.releasenotes.presentation.model.ReleaseNotesUiEvent
import com.quare.blitzsplit.feature.releasenotes.presentation.viewmodel.ReleaseNotesViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReleaseNotesViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val actions = mutableListOf<ReleaseNotesUiAction>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var device: FakeReleaseNotesDevice
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: ReleaseNotesViewModel
    private var language = LANGUAGE
    private var languageReads = 0

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN saved notes and a download still running WHEN the screen opens THEN shows the saved notes at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = listOf(stable30),
                isDownloadPending = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(stable30), state.visibleReleases)
            assertFalse(state.showsSkeleton)
            assertFalse(state.showsLoadFailed)
            assertFalse(state.showsEmpty)
            assertTrue(state.showsBetaSwitch)
        }

    @Test
    fun `GIVEN saved notes WHEN the screen opens THEN downloads once in the language the app speaks`() =
        runTest(testDispatcher) {
            // Given
            language = "pt"
            prepareScenario(saved = listOf(stable30))

            // When
            val downloads = device.refreshedLanguages

            // Then
            assertEquals(listOf("pt"), downloads)
            assertEquals(listOf("pt"), device.observedLanguages)
            assertEquals(1, languageReads)
        }

    @Test
    fun `GIVEN saved notes still being read WHEN the screen opens THEN the download has not started`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = listOf(stable30),
                readGate = CompletableDeferred(),
            )

            // When
            val downloads = device.refreshedLanguages

            // Then
            assertTrue(downloads.isEmpty())
            assertTrue(viewModel.uiState.value.showsSkeleton)
        }

    @Test
    fun `GIVEN saved notes still being read and no internet WHEN the read ends THEN load failed never showed`() =
        runTest(testDispatcher) {
            // Given
            val readGate = CompletableDeferred<Unit>()
            prepareScenario(
                saved = listOf(stable30),
                downloadError = offline,
                readGate = readGate,
            )
            val failedWhileReading = viewModel.uiState.value.showsLoadFailed

            // When
            readGate.complete(Unit)

            // Then
            assertFalse(failedWhileReading)
            assertEquals(listOf(LANGUAGE), device.refreshedLanguages)
            assertEquals(listOf(stable30), viewModel.uiState.value.visibleReleases)
            assertFalse(viewModel.uiState.value.showsLoadFailed)
        }

    @Test
    fun `GIVEN saved notes and no internet WHEN the screen opens THEN keeps the saved notes with nothing wrong`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = listOf(stable30),
                downloadError = offline,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(stable30), state.visibleReleases)
            assertFalse(state.showsLoadFailed)
            assertFalse(state.showsSkeleton)
            assertTrue(state.showsBetaSwitch)
        }

    @Test
    fun `GIVEN saved notes on screen WHEN the download brings a newer feed THEN the newer list replaces them`() =
        runTest(testDispatcher) {
            // Given
            val downloadGate = CompletableDeferred<Unit>()
            prepareScenario(
                saved = listOf(stable30),
                site = listOf(stable31, stable30),
                downloadGate = downloadGate,
            )

            // When
            downloadGate.complete(Unit)

            // Then
            assertEquals(listOf(stable31, stable30), viewModel.uiState.value.visibleReleases)
        }

    @Test
    fun `GIVEN nothing saved and a download still running WHEN the screen opens THEN shows only the skeleton`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isDownloadPending = true)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsSkeleton)
            assertFalse(state.showsLoadFailed)
            assertFalse(state.showsEmpty)
            assertFalse(state.hasOnlyBetaReleases)
            assertFalse(state.showsBetaSwitch)
            assertTrue(state.visibleReleases.isEmpty())
        }

    @Test
    fun `GIVEN nothing saved WHEN the download brings the notes THEN shows the list`() = runTest(testDispatcher) {
        // Given
        val downloadGate = CompletableDeferred<Unit>()
        prepareScenario(
            site = listOf(stable31, stable30),
            downloadGate = downloadGate,
        )

        // When
        downloadGate.complete(Unit)

        // Then
        val state = viewModel.uiState.value
        assertEquals(listOf(stable31, stable30), state.visibleReleases)
        assertFalse(state.showsSkeleton)
        assertFalse(state.showsLoadFailed)
        assertTrue(state.showsBetaSwitch)
    }

    @Test
    fun `GIVEN nothing saved WHEN the download fails THEN shows load failed and neither skeleton nor empty`() =
        runTest(testDispatcher) {
            // Given
            val downloadGate = CompletableDeferred<Unit>()
            prepareScenario(
                downloadError = offline,
                downloadGate = downloadGate,
            )

            // When
            downloadGate.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsLoadFailed)
            assertFalse(state.showsSkeleton)
            assertFalse(state.showsEmpty)
            assertFalse(state.hasOnlyBetaReleases)
            assertFalse(state.showsBetaSwitch)
        }

    @Test
    fun `GIVEN load failed WHEN try again is clicked THEN goes back to the skeleton and downloads again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(downloadError = offline)
            device.downloadGate = CompletableDeferred()

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnRetryClicked)

            // Then
            assertTrue(viewModel.uiState.value.showsSkeleton)
            assertFalse(viewModel.uiState.value.showsLoadFailed)
            assertEquals(listOf(LANGUAGE, LANGUAGE), device.refreshedLanguages)
            assertEquals(
                listOf<Pair<String, Map<String, Any>>>("release_notes_retry_clicked" to emptyMap()),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN load failed and the internet back WHEN try again is clicked THEN shows the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                site = listOf(stable31, stable30),
                downloadError = offline,
            )
            device.downloadError = null

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnRetryClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf(stable31, stable30), state.visibleReleases)
            assertFalse(state.showsLoadFailed)
            assertFalse(state.showsSkeleton)
        }

    @Test
    fun `GIVEN load failed and still no internet WHEN try again is clicked THEN shows load failed again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(downloadError = offline)

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnRetryClicked)

            // Then
            assertTrue(viewModel.uiState.value.showsLoadFailed)
            assertFalse(viewModel.uiState.value.showsSkeleton)
            assertEquals(2, device.refreshedLanguages.size)
        }

    @Test
    fun `GIVEN load failed and the app language changed since WHEN try again is clicked THEN keeps the language`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(downloadError = offline)
            language = "pt"

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnRetryClicked)

            // Then
            assertEquals(listOf(LANGUAGE, LANGUAGE), device.refreshedLanguages)
            assertEquals(listOf(LANGUAGE), device.observedLanguages)
            assertEquals(1, languageReads)
        }

    @Test
    fun `GIVEN load failed WHEN the notes are saved by another download THEN shows the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(downloadError = offline)
            val arrived = listOf(stable30)

            // When
            device.saved.value = arrived

            // Then
            assertEquals(arrived, viewModel.uiState.value.visibleReleases)
            assertFalse(viewModel.uiState.value.showsLoadFailed)
            assertFalse(viewModel.uiState.value.showsSkeleton)
        }

    @Test
    fun `GIVEN the list on screen WHEN try again is sent THEN downloads nothing and counts nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(saved = listOf(stable30))

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnRetryClicked)

            // Then
            assertEquals(listOf(LANGUAGE), device.refreshedLanguages)
            assertEquals(listOf(stable30), viewModel.uiState.value.visibleReleases)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the saved notes kept after a failed download WHEN try again is sent THEN downloads nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = listOf(stable30),
                downloadError = offline,
            )

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnRetryClicked)

            // Then
            assertEquals(listOf(LANGUAGE), device.refreshedLanguages)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the skeleton WHEN try again is sent THEN downloads nothing and counts nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isDownloadPending = true)

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnRetryClicked)

            // Then
            assertEquals(listOf(LANGUAGE), device.refreshedLanguages)
            assertTrue(viewModel.uiState.value.showsSkeleton)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN stable and beta versions and the switch off WHEN the screen opens THEN lists only the stable ones`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = mixed,
                showsBeta = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(stable31, stable30), state.visibleReleases)
            assertEquals(false, state.showsBeta)
            assertTrue(state.showsBetaSwitch)
            assertFalse(state.showsEmpty)
            assertFalse(state.hasOnlyBetaReleases)
        }

    @Test
    fun `GIVEN stable and beta versions and the switch on WHEN the screen opens THEN lists all newest first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = mixed,
                showsBeta = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(mixed, state.visibleReleases)
            assertEquals(true, state.showsBeta)
            assertTrue(state.showsBetaSwitch)
        }

    @Test
    fun `GIVEN saved notes and the choice of the switch not read yet WHEN the screen opens THEN keeps the skeleton`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = mixed,
                showsBeta = null,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsSkeleton)
            assertFalse(state.showsBetaSwitch)
            assertFalse(state.showsEmpty)
            assertFalse(state.hasOnlyBetaReleases)
            assertFalse(state.showsLoadFailed)
        }

    @Test
    fun `GIVEN the skeleton waiting for the choice of the switch WHEN it is read as on THEN lists the betas too`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = mixed,
                showsBeta = null,
            )
            val choice = true

            // When
            device.showsBeta.value = choice

            // Then
            assertFalse(viewModel.uiState.value.showsSkeleton)
            assertTrue(viewModel.uiState.value.showsBetaSwitch)
            assertEquals(mixed, viewModel.uiState.value.visibleReleases)
        }

    @Test
    fun `GIVEN the list with the switch off WHEN it is switched on THEN saves the choice and lists the betas`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = mixed,
                showsBeta = false,
            )

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnShowBetaToggled(isOn = true))

            // Then
            assertEquals(listOf(true), device.savedChoices)
            assertEquals(mixed, viewModel.uiState.value.visibleReleases)
            assertEquals(
                listOf<Pair<String, Map<String, Any>>>("release_notes_beta_toggled" to mapOf("is_on" to true)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the list with the switch on WHEN it is switched off THEN saves the choice and hides the betas`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = mixed,
                showsBeta = true,
            )

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnShowBetaToggled(isOn = false))

            // Then
            assertEquals(listOf(false), device.savedChoices)
            assertEquals(listOf(stable31, stable30), viewModel.uiState.value.visibleReleases)
            assertEquals(
                listOf<Pair<String, Map<String, Any>>>("release_notes_beta_toggled" to mapOf("is_on" to false)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the list with the switch on WHEN it is switched on again THEN saves nothing and counts nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = mixed,
                showsBeta = true,
            )

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnShowBetaToggled(isOn = true))

            // Then
            assertTrue(device.savedChoices.isEmpty())
            assertEquals(mixed, viewModel.uiState.value.visibleReleases)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the skeleton before the notes arrive WHEN the switch is sent THEN saves nothing and counts nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                showsBeta = false,
                isDownloadPending = true,
            )

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnShowBetaToggled(isOn = true))

            // Then
            assertTrue(device.savedChoices.isEmpty())
            assertEquals(false, viewModel.uiState.value.showsBeta)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN load failed WHEN the switch is sent THEN saves nothing and counts nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            showsBeta = false,
            downloadError = offline,
        )

        // When
        viewModel.onEvent(ReleaseNotesUiEvent.OnShowBetaToggled(isOn = true))

        // Then
        assertTrue(device.savedChoices.isEmpty())
        assertTrue(viewModel.uiState.value.showsLoadFailed)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the choice of the switch not read yet WHEN the switch is sent THEN saves nothing and counts nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = mixed,
                showsBeta = null,
            )

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnShowBetaToggled(isOn = true))

            // Then
            assertTrue(device.savedChoices.isEmpty())
            assertTrue(viewModel.uiState.value.showsSkeleton)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN no notes for this platform WHEN the screen opens THEN says it is empty and not for the switch`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = emptyList(),
                site = emptyList(),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsEmpty)
            assertFalse(state.hasOnlyBetaReleases)
            assertTrue(state.showsBetaSwitch)
            assertFalse(state.showsSkeleton)
            assertFalse(state.showsLoadFailed)
        }

    @Test
    fun `GIVEN no notes for this platform and the switch on WHEN the screen opens THEN says it is empty`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = emptyList(),
                site = emptyList(),
                showsBeta = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsEmpty)
            assertFalse(state.hasOnlyBetaReleases)
        }

    @Test
    fun `GIVEN only beta versions and the switch off WHEN the screen opens THEN says the switch would show some`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = listOf(beta31, beta30),
                showsBeta = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.visibleReleases.isEmpty())
            assertTrue(state.showsEmpty)
            assertTrue(state.hasOnlyBetaReleases)
            assertTrue(state.showsBetaSwitch)
        }

    @Test
    fun `GIVEN only beta versions and the empty list WHEN the switch is turned on THEN lists them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = listOf(beta31, beta30),
                showsBeta = false,
            )

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnShowBetaToggled(isOn = true))

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf(beta31, beta30), state.visibleReleases)
            assertFalse(state.showsEmpty)
            assertFalse(state.hasOnlyBetaReleases)
        }

    @Test
    fun `GIVEN only stable versions and the switch off WHEN the screen opens THEN it is not empty`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = listOf(stable31, stable30),
                showsBeta = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsEmpty)
            assertFalse(state.hasOnlyBetaReleases)
        }

    @Test
    fun `GIVEN the release notes over the profile WHEN back is clicked THEN returns to the profile uncounted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(saved = listOf(stable30))

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnBackClicked)

            // Then
            assertEquals(listOf<Route>(Route.Profile), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN load failed WHEN back is clicked THEN returns to the profile`() = runTest(testDispatcher) {
        // Given
        prepareScenario(downloadError = offline)

        // When
        viewModel.onEvent(ReleaseNotesUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf<Route>(Route.Profile), navigator.backStack.value)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the list on screen WHEN the GitHub button is clicked THEN opens every release on GitHub`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(saved = listOf(stable30))
            val stateBefore = viewModel.uiState.value

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnAllReleasesClicked)

            // Then
            assertEquals(listOf<ReleaseNotesUiAction>(ReleaseNotesUiAction.OpenUrl(ALL_RELEASES_URL)), actions)
            assertEquals(
                listOf<Pair<String, Map<String, Any>>>("release_notes_github_clicked" to emptyMap()),
                trackedEvents,
            )
            assertEquals(stateBefore, viewModel.uiState.value)
            assertEquals(listOf(Route.Profile, Route.ReleaseNotes), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the skeleton WHEN the GitHub button is clicked THEN opens every release on GitHub`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isDownloadPending = true)

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnAllReleasesClicked)

            // Then
            assertEquals(listOf<ReleaseNotesUiAction>(ReleaseNotesUiAction.OpenUrl(ALL_RELEASES_URL)), actions)
            assertEquals(
                listOf<Pair<String, Map<String, Any>>>("release_notes_github_clicked" to emptyMap()),
                trackedEvents,
            )
            assertTrue(viewModel.uiState.value.showsSkeleton)
        }

    @Test
    fun `GIVEN load failed WHEN the GitHub button is clicked THEN opens every release on GitHub`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(downloadError = offline)

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnAllReleasesClicked)

            // Then
            assertEquals(listOf<ReleaseNotesUiAction>(ReleaseNotesUiAction.OpenUrl(ALL_RELEASES_URL)), actions)
            assertEquals(
                listOf<Pair<String, Map<String, Any>>>("release_notes_github_clicked" to emptyMap()),
                trackedEvents,
            )
            assertTrue(viewModel.uiState.value.showsLoadFailed)
            assertEquals(listOf(Route.Profile, Route.ReleaseNotes), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a stable version on screen WHEN its card is clicked THEN opens its release tagged by its name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(saved = listOf(stable31, stable30))
            val stateBefore = viewModel.uiState.value

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnReleaseClicked(stable30.version))

            // Then
            assertEquals(
                listOf<ReleaseNotesUiAction>(ReleaseNotesUiAction.OpenUrl("$ALL_RELEASES_URL/tag/3.0.0")),
                actions,
            )
            assertEquals(
                listOf<Pair<String, Map<String, Any>>>(
                    "release_notes_release_clicked" to mapOf("version" to "3.0.0"),
                ),
                trackedEvents,
            )
            assertEquals(stateBefore, viewModel.uiState.value)
            assertEquals(listOf(Route.Profile, Route.ReleaseNotes), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a beta version on screen WHEN its card is clicked THEN opens its release tagged by its whole name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = listOf(stable30, beta3011),
                showsBeta = true,
            )

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnReleaseClicked(beta3011.version))

            // Then
            assertEquals(
                listOf<ReleaseNotesUiAction>(ReleaseNotesUiAction.OpenUrl("$ALL_RELEASES_URL/tag/3.0.0-beta_11")),
                actions,
            )
            assertEquals(
                listOf<Pair<String, Map<String, Any>>>(
                    "release_notes_release_clicked" to mapOf("version" to "3.0.0-beta_11"),
                ),
                trackedEvents,
            )
            assertEquals(listOf(Route.Profile, Route.ReleaseNotes), navigator.backStack.value)
        }

    @Test
    fun `GIVEN stable and beta versions on screen WHEN each card is clicked THEN each opens its own release`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = mixed,
                showsBeta = true,
            )
            val cards = viewModel.uiState.value.visibleReleases

            // When
            cards.forEach { viewModel.onEvent(ReleaseNotesUiEvent.OnReleaseClicked(it.version)) }

            // Then
            assertEquals(
                listOf<ReleaseNotesUiAction>(
                    ReleaseNotesUiAction.OpenUrl("$ALL_RELEASES_URL/tag/3.1.0"),
                    ReleaseNotesUiAction.OpenUrl("$ALL_RELEASES_URL/tag/3.1.0-beta_2"),
                    ReleaseNotesUiAction.OpenUrl("$ALL_RELEASES_URL/tag/3.0.0"),
                    ReleaseNotesUiAction.OpenUrl("$ALL_RELEASES_URL/tag/3.0.0-beta_1"),
                ),
                actions,
            )
            assertEquals(
                listOf("3.1.0", "3.1.0-beta_2", "3.0.0", "3.0.0-beta_1").map { version ->
                    "release_notes_release_clicked" to mapOf<String, Any>("version" to version)
                },
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the switch off WHEN a hidden beta card click is sent THEN opens nothing and counts nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = mixed,
                showsBeta = false,
            )

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnReleaseClicked(beta31.version))

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the list on screen WHEN a click on a version not in the feed is sent THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = listOf(stable30),
                showsBeta = true,
            )
            val unknown = checkNotNull(AppVersion.parseOrNull("9.9.9"))

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnReleaseClicked(unknown))

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the skeleton before the notes arrive WHEN a card click is sent THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isDownloadPending = true)

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnReleaseClicked(stable30.version))

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN saved notes and the choice of the switch not read yet WHEN a card click is sent THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = mixed,
                showsBeta = null,
            )

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnReleaseClicked(stable30.version))

            // Then
            assertTrue(viewModel.uiState.value.showsSkeleton)
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN load failed WHEN a card click is sent THEN opens nothing and counts nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(downloadError = offline)

            // When
            viewModel.onEvent(ReleaseNotesUiEvent.OnReleaseClicked(stable30.version))

            // Then
            assertTrue(actions.isEmpty())
            assertTrue(trackedEvents.isEmpty())
        }

    private val offline = IllegalStateException("offline")

    private val stable31 = release("3.1.0")
    private val beta31 = release("3.1.0-beta_2")
    private val stable30 = release("3.0.0")
    private val beta30 = release("3.0.0-beta_1")
    private val mixed = listOf(stable31, beta31, stable30, beta30)
    private val beta3011 = release("3.0.0-beta_11")

    private fun release(version: String): ReleaseNotes = ReleaseNotes(
        version = checkNotNull(AppVersion.parseOrNull(version)),
        date = ReleaseDate(
            year = 2026,
            month = 11,
            day = 20,
        ),
        isInstalled = false,
        notes = listOf("What $version changed"),
    )

    private fun TestScope.prepareScenario(
        saved: List<ReleaseNotes>? = null,
        site: List<ReleaseNotes> = saved ?: listOf(stable30),
        showsBeta: Boolean? = false,
        downloadError: Throwable? = null,
        isDownloadPending: Boolean = false,
        downloadGate: CompletableDeferred<Unit>? = null,
        readGate: CompletableDeferred<Unit>? = null,
    ) {
        device = FakeReleaseNotesDevice(
            site = site,
            saved = saved,
            showsBeta = showsBeta,
        ).also {
            it.downloadError = downloadError
            it.downloadGate = downloadGate ?: if (isDownloadPending) CompletableDeferred() else null
            it.readGate = readGate
        }
        navigator = BackStackNavigator(startRoute = Route.Profile).also { it.navigate(Route.ReleaseNotes) }
        viewModel = ReleaseNotesViewModel(
            navigator = navigator,
            refreshReleaseNotes = { device.refresh(it) },
            updateBetaReleaseNotesChoice = { device.saveChoice(it) },
            getReleaseNotesLanguage = {
                languageReads++
                language
            },
            observeReleaseNotes = { device.observe(it) },
            observeShowsBetaReleaseNotes = { device.showsBeta.filterNotNull() },
            trackEvent = { name, params -> trackedEvents += name to params },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val LANGUAGE = "en"
        const val ALL_RELEASES_URL = "https://github.com/Quartel-Enterprise/blitz-split-client/releases"
    }
}

// Why: what the device holds under the screen: the saved notes, the site's feed and the choice of the switch.
private class FakeReleaseNotesDevice(
    private val site: List<ReleaseNotes>,
    saved: List<ReleaseNotes>?,
    showsBeta: Boolean?,
) {
    val saved = MutableStateFlow(saved)
    val showsBeta = MutableStateFlow(showsBeta)
    val observedLanguages = mutableListOf<String>()
    val refreshedLanguages = mutableListOf<String>()
    val savedChoices = mutableListOf<Boolean>()
    var downloadError: Throwable? = null
    var downloadGate: CompletableDeferred<Unit>? = null
    var readGate: CompletableDeferred<Unit>? = null

    fun observe(language: String): Flow<List<ReleaseNotes>?> {
        observedLanguages += language
        return saved.onStart { readGate?.await() }
    }

    suspend fun refresh(language: String): Result<Unit> {
        refreshedLanguages += language
        downloadGate?.await()
        downloadError?.let { return Result.failure(it) }
        saved.value = site
        return Result.success(Unit)
    }

    fun saveChoice(isOn: Boolean) {
        savedChoices += isOn
        showsBeta.value = isOn
    }
}
