package com.quare.blitzsplit.android.feature.groups

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiAction
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import org.koin.compose.viewmodel.koinViewModel

/*
 * Why: features never see each other, so the recovery code sheet comes in as a slot the app fills
 * with :android:feature:recovery_code.
 */
@Composable
fun GroupsScreen(
    recoveryCodeSheet: @Composable () -> Unit,
    viewModel: GroupsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val comingSoon = stringResource(R.string.groups_coming_soon)
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                GroupsUiAction.ShowComingSoon -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(comingSoon)
                }
            }
        }
    }
    GroupsContent(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
    if (state.showsRecoveryCodePrompt) recoveryCodeSheet()
}
