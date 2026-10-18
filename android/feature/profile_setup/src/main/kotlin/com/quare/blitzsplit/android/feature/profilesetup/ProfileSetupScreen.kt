package com.quare.blitzsplit.android.feature.profilesetup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.feature.profilesetup.presentation.model.ProfileSetupUiEvent
import com.quare.blitzsplit.feature.profilesetup.presentation.viewmodel.ProfileSetupViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileSetupScreen(viewModel: ProfileSetupViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { viewModel.onEvent(ProfileSetupUiEvent.OnBackClicked) }
    ProfileSetupContent(state = state, onEvent = viewModel::onEvent)
}
