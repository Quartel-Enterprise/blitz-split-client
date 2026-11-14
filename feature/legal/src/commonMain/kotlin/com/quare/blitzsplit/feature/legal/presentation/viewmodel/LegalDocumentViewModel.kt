package com.quare.blitzsplit.feature.legal.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.legal.domain.model.LegalBlock
import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.legal.domain.model.LegalLink
import com.quare.blitzsplit.core.legal.domain.usecase.GetLegalLanguage
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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: "Privacy Policy" and "Terms of Service" drawn by the app itself, from the Markdown the site's own pages are
 * made of, so the text is written once. The text the device last saved (usually at launch) shows at once, offline
 * too, while the site is asked for a newer one when a day passed since; only a device that never saved it and can't
 * download it shows "Couldn't load".
 */
class LegalDocumentViewModel(
    private val route: Route.LegalDocument,
    private val navigator: Navigator,
    private val refreshLegalDocument: RefreshLegalDocument,
    getLegalLanguage: GetLegalLanguage,
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

    /*
     * Why: worked out once for as long as the screen lives, so the text it watches is the one every download saves,
     * even when Profile › Language or the device's changes under it.
     */
    private val language = viewModelScope.async(start = CoroutineStart.LAZY) { getLegalLanguage() }

    init {
        viewModelScope.launch {
            observeLegalDocument(
                page = route.page,
                language = language.await(),
            ).collectIndexed { index, document ->
                showDocument(document)
                /*
                 * Why: the download starts once the saved text was read, so one that fails at once never shows
                 * "Couldn't load" over a device that holds the text.
                 */
                if (index == 0) refresh()
            }
        }
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
                // Why: an index into this very text's blocks, where the current one may hold the section elsewhere.
                focusedBlockIndex = document?.findSectionIndex(route.anchor),
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
            refreshLegalDocument(
                page = route.page,
                language = language.await(),
            ).onFailure { uiState.update { it.copy(hasLoadFailed = true) } }
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
                openPage(
                    Route.LegalDocument(
                        page = link.page,
                        anchor = link.anchor,
                    ),
                )
            }
        }
    }

    /*
     * Why: the two texts link to each other, and the stack never holds the same route twice (each entry is keyed by
     * its route): a link to a text already open under this one goes back to it, taking its place when the link asks
     * for another section, so the two never pile up. A section of this very text opens over it, and the one this
     * screen itself opened at, which is this same entry, scrolls back to it.
     */
    private fun openPage(target: Route.LegalDocument) {
        val stack = navigator.backStack.value
        val sameRoute = stack.indexOf(target)
        val samePage = stack.indexOfFirst { it is Route.LegalDocument && it.page == target.page }
        when {
            target == route -> scrollToOwnSection()
            sameRoute >= 0 -> navigator.popTo(sameRoute + 1)
            target.page == route.page || samePage < 0 -> navigator.navigate(target)
            else -> navigator.replaceAbove(samePage, target)
        }
    }

    private fun scrollToOwnSection() {
        val blockIndex = uiState.value.document?.findSectionIndex(route.anchor)
        viewModelScope.launch { uiAction.emit(LegalDocumentUiAction.ScrollToSection(blockIndex)) }
    }

    private fun LegalDocument.findSectionIndex(anchor: String?): Int? = blocks
        .indexOfFirst { block -> anchor != null && block is LegalBlock.Section && block.anchor == anchor }
        .takeIf { it >= 0 }
}
