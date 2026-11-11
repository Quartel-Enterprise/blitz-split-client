package com.quare.blitzsplit.android.feature.legal

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.legal.presentation.model.LegalDocumentUiAction
import com.quare.blitzsplit.feature.legal.presentation.model.LegalDocumentUiEvent
import com.quare.blitzsplit.feature.legal.presentation.viewmodel.LegalDocumentViewModel
import kotlinx.coroutines.flow.filterIsInstance
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LegalDocumentScreen(
    route: Route.LegalDocument,
    // Why: one text opens over the other, or over itself at another section, each with its own ViewModel.
    viewModel: LegalDocumentViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(viewModel, uriHandler) {
        viewModel.uiAction.collect { action ->
            when (action) {
                // Why: a device with no browser or mail app throws; the link then does nothing.
                is LegalDocumentUiAction.OpenUrl -> runCatching { uriHandler.openUri(action.url) }

                // Why: the list scrolls itself, below.
                is LegalDocumentUiAction.ScrollToSection -> Unit
            }
        }
    }
    val sectionScrolls = remember(viewModel) {
        viewModel.uiAction.filterIsInstance<LegalDocumentUiAction.ScrollToSection>()
    }
    BackHandler { viewModel.onEvent(LegalDocumentUiEvent.OnBackClicked) }
    LegalDocumentContent(
        state = state,
        sectionScrolls = sectionScrolls,
        onEvent = viewModel::onEvent,
    )
}
