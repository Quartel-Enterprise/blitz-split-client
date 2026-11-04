package com.quare.blitzsplit.feature.inappupdate

import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateDownloadState
import com.quare.blitzsplit.feature.inappupdate.presentation.model.InAppUpdateDownloadUiAction
import com.quare.blitzsplit.feature.inappupdate.presentation.viewmodel.InAppUpdateDownloadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InAppUpdateDownloadViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var downloadStates: MutableSharedFlow<UpdateDownloadState>
    private lateinit var viewModel: InAppUpdateDownloadViewModel
    private var installs = 0
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val actions = mutableListOf<InAppUpdateDownloadUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN no download WHEN the app opens THEN shows no progress`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val percent = viewModel.uiState.value.downloadPercent

        // Then
        assertNull(percent)
    }

    @Test
    fun `GIVEN no download WHEN the update starts downloading THEN shows its percentage`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        downloadStates.emit(UpdateDownloadState.Downloading(percent = 42))

        // Then
        assertEquals(42, viewModel.uiState.value.downloadPercent)
    }

    @Test
    fun `GIVEN a download running WHEN it moves on several times THEN shows the latest percentage`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            downloadStates.emit(UpdateDownloadState.Downloading(percent = 10))
            downloadStates.emit(UpdateDownloadState.Downloading(percent = 55))

            // When
            downloadStates.emit(UpdateDownloadState.Downloading(percent = 87))

            // Then
            assertEquals(87, viewModel.uiState.value.downloadPercent)
        }

    @Test
    fun `GIVEN a download running WHEN the updater goes idle THEN hides the progress`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        downloadStates.emit(UpdateDownloadState.Downloading(percent = 42))

        // When
        downloadStates.emit(UpdateDownloadState.Idle)

        // Then
        assertNull(viewModel.uiState.value.downloadPercent)
    }

    @Test
    fun `GIVEN a download running WHEN it is still downloading THEN does not install yet`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        downloadStates.emit(UpdateDownloadState.Downloading(percent = 0))

        // When
        downloadStates.emit(UpdateDownloadState.Downloading(percent = 99))

        // Then
        assertEquals(0, installs)
    }

    @Test
    fun `GIVEN a download watched WHEN it finishes THEN hides the progress and installs the update once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            downloadStates.emit(UpdateDownloadState.Downloading(percent = 42))

            // When
            downloadStates.emit(UpdateDownloadState.Downloaded)

            // Then
            assertNull(viewModel.uiState.value.downloadPercent)
            assertEquals(1, installs)
        }

    @Test
    fun `GIVEN a download that finished before the screen was recreated WHEN it reopens THEN does not install`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(replayedState = UpdateDownloadState.Downloaded)

            // When
            val percent = viewModel.uiState.value.downloadPercent

            // Then
            assertNull(percent)
            assertEquals(0, installs)
        }

    @Test
    fun `GIVEN an installed update WHEN the updater says downloaded again THEN does not install twice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            downloadStates.emit(UpdateDownloadState.Downloading(percent = 100))
            downloadStates.emit(UpdateDownloadState.Downloaded)

            // When
            downloadStates.emit(UpdateDownloadState.Downloaded)

            // Then
            assertEquals(1, installs)
        }

    @Test
    fun `GIVEN a download running WHEN it fails THEN hides the progress and does not install`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            downloadStates.emit(UpdateDownloadState.Downloading(percent = 30))

            // When
            downloadStates.emit(UpdateDownloadState.Failed)

            // Then
            assertNull(viewModel.uiState.value.downloadPercent)
            assertEquals(0, installs)
        }

    @Test
    fun `GIVEN a download running WHEN it fails THEN tracks the failure and shows the failure notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            downloadStates.emit(UpdateDownloadState.Downloading(percent = 30))

            // When
            downloadStates.emit(UpdateDownloadState.Failed)

            // Then
            assertEquals(listOf("update_download_failed" to emptyMap<String, Any>()), trackedEvents)
            assertEquals(listOf<InAppUpdateDownloadUiAction>(InAppUpdateDownloadUiAction.ShowDownloadFailed), actions)
        }

    @Test
    fun `GIVEN a download running WHEN it finishes THEN tracks nothing and shows no failure notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            downloadStates.emit(UpdateDownloadState.Downloading(percent = 42))

            // When
            downloadStates.emit(UpdateDownloadState.Downloaded)

            // Then
            assertTrue(trackedEvents.isEmpty())
            assertTrue(actions.isEmpty())
        }

    private fun TestScope.prepareScenario(replayedState: UpdateDownloadState? = null) {
        downloadStates = MutableSharedFlow(replay = 1)
        replayedState?.let(downloadStates::tryEmit)
        viewModel = InAppUpdateDownloadViewModel(
            installDownloadedUpdate = { installs++ },
            trackEvent = { name, params -> trackedEvents += name to params },
            observeUpdateDownloadState = { downloadStates },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
