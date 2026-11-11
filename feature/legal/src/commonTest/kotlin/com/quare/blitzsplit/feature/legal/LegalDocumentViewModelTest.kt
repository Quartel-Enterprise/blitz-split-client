package com.quare.blitzsplit.feature.legal

import com.quare.blitzsplit.core.legal.domain.model.LegalBlock
import com.quare.blitzsplit.core.legal.domain.model.LegalDate
import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.legal.domain.model.LegalLink
import com.quare.blitzsplit.core.legal.domain.model.LegalSpan
import com.quare.blitzsplit.core.legal.domain.model.LegalText
import com.quare.blitzsplit.core.legal.domain.repository.LegalRepository
import com.quare.blitzsplit.core.legal.domain.usecase.impl.ObserveLegalDocumentUseCase
import com.quare.blitzsplit.core.legal.domain.usecase.impl.RefreshLegalDocumentUseCase
import com.quare.blitzsplit.core.model.LegalPage
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.legal.presentation.model.LegalDocumentUiAction
import com.quare.blitzsplit.feature.legal.presentation.model.LegalDocumentUiEvent
import com.quare.blitzsplit.feature.legal.presentation.viewmodel.LegalDocumentViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LegalDocumentViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeLegalRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: LegalDocumentViewModel
    private val actions = mutableListOf<LegalDocumentUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a saved text and a download still running WHEN the screen opens THEN shows the saved text at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = mapOf(LegalPage.PRIVACY_POLICY to savedPrivacy),
                isDownloadPending = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(LegalPage.PRIVACY_POLICY, state.page)
            assertEquals(savedPrivacy, state.document)
            assertFalse(state.showsSkeleton)
            assertFalse(state.showsLoadFailed)
        }

    @Test
    fun `GIVEN a saved text WHEN the screen opens THEN downloads the current one once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(saved = mapOf(LegalPage.PRIVACY_POLICY to savedPrivacy))

        // When
        val downloads = repository.refreshedPages

        // Then
        assertEquals(listOf(LegalPage.PRIVACY_POLICY), downloads)
    }

    @Test
    fun `GIVEN a saved text WHEN the download brings a newer one THEN shows the newer text`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = mapOf(LegalPage.PRIVACY_POLICY to savedPrivacy),
                isDownloadPending = true,
            )

            // When
            repository.downloadGate?.complete(Unit)

            // Then
            assertEquals(privacy, viewModel.uiState.value.document)
            assertFalse(viewModel.uiState.value.showsLoadFailed)
        }

    @Test
    fun `GIVEN a saved text WHEN the download fails THEN keeps the saved text without the load failed state`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                saved = mapOf(LegalPage.PRIVACY_POLICY to savedPrivacy),
                downloadError = offline,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(savedPrivacy, state.document)
            assertFalse(state.showsLoadFailed)
            assertFalse(state.showsSkeleton)
        }

    @Test
    fun `GIVEN no saved text and a download still running WHEN the screen opens THEN shows the skeleton`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isDownloadPending = true)

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.document)
            assertTrue(state.showsSkeleton)
            assertFalse(state.showsLoadFailed)
            assertEquals(listOf(LegalPage.PRIVACY_POLICY), repository.refreshedPages)
        }

    @Test
    fun `GIVEN no saved text WHEN the download fails THEN shows the load failed state`() = runTest(testDispatcher) {
        // Given
        prepareScenario(downloadError = offline)

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.document)
        assertTrue(state.showsLoadFailed)
        assertFalse(state.showsSkeleton)
    }

    @Test
    fun `GIVEN no saved text WHEN the download succeeds THEN shows the text`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isDownloadPending = true)

        // When
        repository.downloadGate?.complete(Unit)

        // Then
        assertEquals(privacy, viewModel.uiState.value.document)
        assertFalse(viewModel.uiState.value.showsSkeleton)
        assertFalse(viewModel.uiState.value.showsLoadFailed)
    }

    @Test
    fun `GIVEN the load failed state WHEN the device saves the text meanwhile THEN shows the text instead`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(downloadError = offline)

            // When
            repository.documents.update { it + (LegalPage.PRIVACY_POLICY to privacy) }

            // Then
            assertEquals(privacy, viewModel.uiState.value.document)
            assertFalse(viewModel.uiState.value.showsLoadFailed)
            assertFalse(viewModel.uiState.value.showsSkeleton)
        }

    @Test
    fun `GIVEN the load failed state WHEN trying again THEN goes back to the skeleton and downloads again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(downloadError = offline)
            repository.downloadGate = CompletableDeferred()

            // When
            viewModel.onEvent(LegalDocumentUiEvent.OnRetryClicked)

            // Then
            assertTrue(viewModel.uiState.value.showsSkeleton)
            assertFalse(viewModel.uiState.value.showsLoadFailed)
            assertEquals(listOf(LegalPage.PRIVACY_POLICY, LegalPage.PRIVACY_POLICY), repository.refreshedPages)
        }

    @Test
    fun `GIVEN the load failed state and the internet back WHEN trying again THEN shows the text`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(downloadError = offline)
            repository.downloadError = null

            // When
            viewModel.onEvent(LegalDocumentUiEvent.OnRetryClicked)

            // Then
            assertEquals(privacy, viewModel.uiState.value.document)
            assertFalse(viewModel.uiState.value.showsLoadFailed)
            assertFalse(viewModel.uiState.value.showsSkeleton)
        }

    @Test
    fun `GIVEN the load failed state and still no internet WHEN trying again THEN shows the load failed state again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(downloadError = offline)

            // When
            viewModel.onEvent(LegalDocumentUiEvent.OnRetryClicked)

            // Then
            assertNull(viewModel.uiState.value.document)
            assertTrue(viewModel.uiState.value.showsLoadFailed)
            assertFalse(viewModel.uiState.value.showsSkeleton)
            assertEquals(2, repository.refreshedPages.size)
        }

    @Test
    fun `GIVEN the load failed state WHEN trying again THEN tracks the retry`() = runTest(testDispatcher) {
        // Given
        prepareScenario(downloadError = offline)

        // When
        viewModel.onEvent(LegalDocumentUiEvent.OnRetryClicked)

        // Then
        assertEquals(
            listOf<Pair<String, Map<String, Any>>>("legal_document_retry_clicked" to emptyMap()),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the text on screen WHEN a retry arrives THEN downloads nothing again`() = runTest(testDispatcher) {
        // Given
        prepareScenario(saved = mapOf(LegalPage.PRIVACY_POLICY to savedPrivacy))

        // When
        viewModel.onEvent(LegalDocumentUiEvent.OnRetryClicked)

        // Then
        assertEquals(listOf(LegalPage.PRIVACY_POLICY), repository.refreshedPages)
        assertEquals(privacy, viewModel.uiState.value.document)
        assertFalse(viewModel.uiState.value.showsSkeleton)
    }

    @Test
    fun `GIVEN the text on screen WHEN a retry arrives THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(saved = mapOf(LegalPage.PRIVACY_POLICY to savedPrivacy))

        // When
        viewModel.onEvent(LegalDocumentUiEvent.OnRetryClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the skeleton of a running download WHEN a retry arrives THEN starts no second download`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isDownloadPending = true)

            // When
            viewModel.onEvent(LegalDocumentUiEvent.OnRetryClicked)

            // Then
            assertEquals(listOf(LegalPage.PRIVACY_POLICY), repository.refreshedPages)
            assertTrue(viewModel.uiState.value.showsSkeleton)
        }

    @Test
    fun `GIVEN the skeleton of a running download WHEN a retry arrives THEN tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isDownloadPending = true)

            // When
            viewModel.onEvent(LegalDocumentUiEvent.OnRetryClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a link to the keep section and a saved text WHEN the screen opens THEN focuses that section`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                anchor = "keep",
                saved = mapOf(LegalPage.PRIVACY_POLICY to privacy),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(KEEP_SECTION_INDEX, state.focusedBlockIndex)
            assertEquals(keepSection, state.document?.blocks?.get(KEEP_SECTION_INDEX))
        }

    @Test
    fun `GIVEN no section asked for WHEN the screen opens with a saved text THEN focuses nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                anchor = null,
                saved = mapOf(LegalPage.PRIVACY_POLICY to privacy),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.focusedBlockIndex)
        }

    @Test
    fun `GIVEN a link to a section the text does not have WHEN the screen opens THEN focuses nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                anchor = "cookies",
                saved = mapOf(LegalPage.PRIVACY_POLICY to privacy),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.focusedBlockIndex)
        }

    @Test
    fun `GIVEN a link to the keep section and no text yet WHEN the screen opens THEN focuses nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                anchor = "keep",
                isDownloadPending = true,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.focusedBlockIndex)
        }

    @Test
    fun `GIVEN a link to the keep section and no saved text WHEN the download succeeds THEN focuses that section`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                anchor = "keep",
                isDownloadPending = true,
            )

            // When
            repository.downloadGate?.complete(Unit)

            // Then
            assertEquals(KEEP_SECTION_INDEX, viewModel.uiState.value.focusedBlockIndex)
        }

    @Test
    fun `GIVEN a section focused on the saved text WHEN a newer text moves that section THEN the focus stays put`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                anchor = "keep",
                saved = mapOf(LegalPage.PRIVACY_POLICY to savedPrivacy),
                isDownloadPending = true,
            )

            // When
            repository.downloadGate?.complete(Unit)

            // Then
            assertEquals(privacy, viewModel.uiState.value.document)
            assertEquals(SAVED_KEEP_SECTION_INDEX, viewModel.uiState.value.focusedBlockIndex)
        }

    @Test
    fun `GIVEN the text on screen WHEN a link that leaves the app is clicked THEN asks to open its address`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(saved = mapOf(LegalPage.PRIVACY_POLICY to privacy))
            val link = LegalLink.External("mailto:privacy@blitzsplit.com")

            // When
            viewModel.onEvent(LegalDocumentUiEvent.OnLinkClicked(link))

            // Then
            assertEquals(
                listOf<LegalDocumentUiAction>(LegalDocumentUiAction.OpenUrl("mailto:privacy@blitzsplit.com")),
                actions,
            )
            assertEquals(listOf(Route.Profile, privacyRoute), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the text on screen WHEN a link that leaves the app is clicked THEN tracks an external link`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(saved = mapOf(LegalPage.PRIVACY_POLICY to privacy))
            val link = LegalLink.External("https://supabase.com/privacy")

            // When
            viewModel.onEvent(LegalDocumentUiEvent.OnLinkClicked(link))

            // Then
            assertEquals(
                listOf<Pair<String, Map<String, Any>>>(
                    "legal_document_link_clicked" to mapOf("target" to "external"),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the privacy policy WHEN a link to a section of the terms is clicked THEN opens the terms over it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(saved = mapOf(LegalPage.PRIVACY_POLICY to privacy))
            val link = LegalLink.Page(
                page = LegalPage.TERMS_OF_USE,
                anchor = "liability",
            )

            // When
            viewModel.onEvent(LegalDocumentUiEvent.OnLinkClicked(link))

            // Then
            assertEquals(
                listOf(
                    Route.Profile,
                    privacyRoute,
                    Route.LegalDocument(
                        page = LegalPage.TERMS_OF_USE,
                        anchor = "liability",
                    ),
                ),
                navigator.backStack.value,
            )
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the privacy policy WHEN a link to the top of the terms is clicked THEN opens the terms at the top`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(saved = mapOf(LegalPage.PRIVACY_POLICY to privacy))
            val link = LegalLink.Page(
                page = LegalPage.TERMS_OF_USE,
                anchor = null,
            )

            // When
            viewModel.onEvent(LegalDocumentUiEvent.OnLinkClicked(link))

            // Then
            assertEquals(
                Route.LegalDocument(
                    page = LegalPage.TERMS_OF_USE,
                    anchor = null,
                ),
                navigator.backStack.value.last(),
            )
        }

    @Test
    fun `GIVEN the privacy policy WHEN a link to the terms is clicked THEN tracks the terms as the target`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(saved = mapOf(LegalPage.PRIVACY_POLICY to privacy))
            val link = LegalLink.Page(
                page = LegalPage.TERMS_OF_USE,
                anchor = "liability",
            )

            // When
            viewModel.onEvent(LegalDocumentUiEvent.OnLinkClicked(link))

            // Then
            assertEquals(
                listOf<Pair<String, Map<String, Any>>>(
                    "legal_document_link_clicked" to mapOf("target" to LegalPage.TERMS_OF_USE),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the terms WHEN a link to the privacy policy is clicked THEN tracks the privacy policy as the target`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                page = LegalPage.TERMS_OF_USE,
                saved = mapOf(LegalPage.TERMS_OF_USE to terms),
            )
            val link = LegalLink.Page(
                page = LegalPage.PRIVACY_POLICY,
                anchor = "keep",
            )

            // When
            viewModel.onEvent(LegalDocumentUiEvent.OnLinkClicked(link))

            // Then
            assertEquals(
                listOf<Pair<String, Map<String, Any>>>(
                    "legal_document_link_clicked" to mapOf("target" to LegalPage.PRIVACY_POLICY),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the text opened from Profile WHEN going back THEN returns to Profile without tracking`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(saved = mapOf(LegalPage.PRIVACY_POLICY to privacy))

            // When
            viewModel.onEvent(LegalDocumentUiEvent.OnBackClicked)

            // Then
            assertEquals(listOf<Route>(Route.Profile), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the terms opened over the privacy policy WHEN the policy gets a late back THEN the terms stay`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(saved = mapOf(LegalPage.PRIVACY_POLICY to privacy))
            val termsRoute = Route.LegalDocument(
                page = LegalPage.TERMS_OF_USE,
                anchor = null,
            )
            navigator.navigate(termsRoute)

            // When
            viewModel.onEvent(LegalDocumentUiEvent.OnBackClicked)

            // Then
            assertEquals(listOf(Route.Profile, privacyRoute, termsRoute), navigator.backStack.value)
        }

    @Test
    fun `GIVEN both texts saved WHEN the terms open THEN shows and downloads the terms only`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                page = LegalPage.TERMS_OF_USE,
                saved = mapOf(
                    LegalPage.PRIVACY_POLICY to privacy,
                    LegalPage.TERMS_OF_USE to terms,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(LegalPage.TERMS_OF_USE, state.page)
            assertEquals(terms, state.document)
            assertEquals(listOf(LegalPage.TERMS_OF_USE), repository.observedPages)
            assertEquals(listOf(LegalPage.TERMS_OF_USE), repository.refreshedPages)
        }

    @Test
    fun `GIVEN only the privacy policy saved and no internet WHEN the terms open THEN shows the load failed state`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                page = LegalPage.TERMS_OF_USE,
                saved = mapOf(LegalPage.PRIVACY_POLICY to privacy),
                downloadError = offline,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.document)
            assertTrue(state.showsLoadFailed)
        }

    private val offline = IllegalStateException("offline")

    private val keepSection = section(
        number = 2,
        anchor = "keep",
    )

    private val savedPrivacy = LegalDocument(
        title = "Privacy Policy",
        updated = LegalDate(
            year = 2026,
            month = 6,
            day = 1,
        ),
        blocks = listOf(
            section(
                number = 1,
                anchor = "collect",
            ),
            keepSection,
            paragraph("We keep your data while you use the app."),
        ),
    )

    private val privacy = LegalDocument(
        title = "Privacy Policy",
        updated = LegalDate(
            year = 2026,
            month = 11,
            day = 5,
        ),
        blocks = listOf(
            paragraph("In short, the app works without an account."),
            section(
                number = 1,
                anchor = "collect",
            ),
            paragraph("We collect what you type."),
            keepSection,
            paragraph("We keep your data while you use the app."),
        ),
    )

    private val terms = LegalDocument(
        title = "Terms of Service",
        updated = LegalDate(
            year = 2026,
            month = 11,
            day = 5,
        ),
        blocks = listOf(
            section(
                number = 1,
                anchor = "liability",
            ),
            paragraph("The app is provided as is."),
        ),
    )

    private val privacyRoute = Route.LegalDocument(
        page = LegalPage.PRIVACY_POLICY,
        anchor = null,
    )

    private fun section(
        number: Int,
        anchor: String,
    ): LegalBlock.Section = LegalBlock.Section(
        number = number,
        anchor = anchor,
        title = text(anchor),
    )

    private fun paragraph(words: String): LegalBlock.Paragraph = LegalBlock.Paragraph(text(words))

    private fun text(words: String): LegalText = LegalText(
        listOf(
            LegalSpan(
                text = words,
                isBold = false,
                link = null,
            ),
        ),
    )

    private fun TestScope.prepareScenario(
        page: LegalPage = LegalPage.PRIVACY_POLICY,
        anchor: String? = null,
        saved: Map<LegalPage, LegalDocument> = emptyMap(),
        downloadError: Throwable? = null,
        isDownloadPending: Boolean = false,
    ) {
        repository = FakeLegalRepository(
            saved = saved,
            site = mapOf(
                LegalPage.PRIVACY_POLICY to privacy,
                LegalPage.TERMS_OF_USE to terms,
            ),
        ).also {
            it.downloadError = downloadError
            it.downloadGate = if (isDownloadPending) CompletableDeferred() else null
        }
        val route = Route.LegalDocument(
            page = page,
            anchor = anchor,
        )
        navigator = BackStackNavigator(startRoute = Route.Profile).also { it.navigate(route) }
        viewModel = LegalDocumentViewModel(
            route = route,
            navigator = navigator,
            refreshLegalDocument = RefreshLegalDocumentUseCase(
                repository = repository,
                getLegalLanguage = { LANGUAGE },
            ),
            observeLegalDocument = ObserveLegalDocumentUseCase(
                repository = repository,
                getLegalLanguage = { LANGUAGE },
            ),
            trackEvent = { name, params -> trackedEvents += name to params },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val LANGUAGE = "en"
        const val KEEP_SECTION_INDEX = 3
        const val SAVED_KEEP_SECTION_INDEX = 1
    }
}

private class FakeLegalRepository(
    private val site: Map<LegalPage, LegalDocument>,
    saved: Map<LegalPage, LegalDocument>,
) : LegalRepository {
    val documents = MutableStateFlow(saved)
    val observedPages = mutableListOf<LegalPage>()
    val refreshedPages = mutableListOf<LegalPage>()
    var downloadError: Throwable? = null
    var downloadGate: CompletableDeferred<Unit>? = null

    override fun observeDocument(
        page: LegalPage,
        language: String,
    ): Flow<LegalDocument?> {
        observedPages += page
        return documents.map { it[page] }
    }

    override suspend fun refreshDocument(
        page: LegalPage,
        language: String,
    ): Result<Unit> {
        refreshedPages += page
        downloadGate?.await()
        downloadError?.let { return Result.failure(it) }
        documents.update { it + (page to site.getValue(page)) }
        return Result.success(Unit)
    }
}
