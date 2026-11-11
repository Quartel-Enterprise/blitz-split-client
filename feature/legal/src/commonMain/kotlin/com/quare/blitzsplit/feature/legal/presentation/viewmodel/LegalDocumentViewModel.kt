package com.quare.blitzsplit.feature.legal.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.legal.domain.model.LegalBlock
import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.legal.domain.model.LegalLink
import com.quare.blitzsplit.core.legal.domain.usecase.ObserveLegalDocument
import com.quare.blitzsplit.core.legal.domain.usecase.RefreshLegalDocument
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.navigateBackFrom
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.legal.presentation.model.LegalDocumentUiAction
import com.quare.blitzsplit.feature.legal.presentation.model.LegalDocumentUiEvent
import com.quare.blitzsplit.feature.legal.presentation.model.LegalDocumentUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: "Privacy Policy" and "Terms of Service" drawn by the app itself, from the Markdown the site's own pages are
 * made of, so the text is written once. The text the device last saved shows at once, offline too, while the current
 * one downloads and takes its place; only a device that never saved it and can't download it shows "Couldn't load".
 */
class LegalDocumentViewModel(
    private val route: Route.LegalDocument,
    private val navigator: Navigator,
    private val refreshLegalDocument: RefreshLegalDocument,
    observeLegalDocument: ObserveLegalDocument,
    trackEvent: TrackEvent,
) : TrackedViewModel<LegalDocumentUiEvent>(trackEvent) {
    val uiState: StateFlow<LegalDocumentUiState>
        field = MutableStateFlow(
            LegalDocumentUiState(
                page = route.page,
                document = null,
                hasLoadFailed = false,
                focusedBlockIndex = null,
            ),
        )

    val uiAction: SharedFlow<LegalDocumentUiAction>
        field = MutableSharedFlow<LegalDocumentUiAction>()

    val stateWatcher: FlowWatcher<LegalDocumentUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<LegalDocumentUiAction> = FlowWatcher(uiAction)

    init {
        observeLegalDocument(route.page)
            .onEach(::showDocument)
            .launchIn(viewModelScope)
        refresh()
    }

    override fun handleEvent(event: LegalDocumentUiEvent) {
        when (event) {
            LegalDocumentUiEvent.OnBackClicked -> navigator.navigateBackFrom(route)
            LegalDocumentUiEvent.OnRetryClicked -> retry()
            is LegalDocumentUiEvent.OnLinkClicked -> openLink(event.link)
        }
    }

    // Why: "Try again" only exists on "Couldn't load"; a retry from anywhere else is neither run nor counted.
    override fun isAccepted(event: LegalDocumentUiEvent): Boolean = when (event) {
        LegalDocumentUiEvent.OnRetryClicked -> uiState.value.showsLoadFailed
        LegalDocumentUiEvent.OnBackClicked, is LegalDocumentUiEvent.OnLinkClicked -> true
    }

    private fun showDocument(document: LegalDocument?) {
        uiState.update { state ->
            state.copy(
                document = document,
                // Why: only the first text scrolls to the link's section; the current one taking its place stays put.
                focusedBlockIndex = state.focusedBlockIndex.takeUnless { state.document == null }
                    ?: document?.takeIf { state.document == null }?.findSectionIndex(route.anchor),
            )
        }
    }

    private fun retry() {
        uiState.update { it.copy(hasLoadFailed = false) }
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            // Why: a download that worked shows through the saved text; one that failed only matters without any.
            refreshLegalDocument(route.page).onFailure { uiState.update { it.copy(hasLoadFailed = true) } }
        }
    }

    private fun openLink(link: LegalLink) {
        when (link) {
            is LegalLink.External -> {
                trackEvent("legal_document_link_clicked", mapOf("target" to "external"))
                viewModelScope.launch { uiAction.emit(LegalDocumentUiAction.OpenUrl(link.url)) }
            }

            is LegalLink.Page -> {
                trackEvent("legal_document_link_clicked", mapOf("target" to link.page))
                navigator.navigate(
                    Route.LegalDocument(
                        page = link.page,
                        anchor = link.anchor,
                    ),
                )
            }
        }
    }

    private fun LegalDocument.findSectionIndex(anchor: String?): Int? = blocks
        .indexOfFirst { block -> anchor != null && block is LegalBlock.Section && block.anchor == anchor }
        .takeIf { it >= 0 }
}
