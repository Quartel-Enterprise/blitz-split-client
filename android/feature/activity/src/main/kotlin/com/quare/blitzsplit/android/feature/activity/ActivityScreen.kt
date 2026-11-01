package com.quare.blitzsplit.android.feature.activity

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.activity.presentation.model.ActivityUiEvent
import com.quare.blitzsplit.feature.activity.presentation.viewmodel.ActivityViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// Why: the home's Atividade tab, the feed of every group and what is new in it.
@Composable
fun ActivityScreen(
    route: Route.Activity,
    viewModel: ActivityViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Why: a tapped item whose bill or group is gone says who deleted it, and the record stays.
    val deletion = state.deletedNotice
    if (deletion != null) {
        val text = deletedNoticeTextOf(deletion, rememberDateTimeTextFormatter())
        LaunchedEffect(deletion) {
            viewModel.onEvent(ActivityUiEvent.OnDeletedNoticeShown)
            scope.showReplacingSnackbar(snackbarHostState, text)
        }
    }
    ActivityContent(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
    state.filterDraft?.let { draft ->
        ActivityFilterSheet(
            draft = draft,
            resultCount = state.draftResultCount,
            groups = state.groups,
            onEvent = viewModel::onEvent,
        )
    }
}
