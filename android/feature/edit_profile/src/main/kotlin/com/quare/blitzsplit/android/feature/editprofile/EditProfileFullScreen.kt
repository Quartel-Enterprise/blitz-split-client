package com.quare.blitzsplit.android.feature.editprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.TwoHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

// Why: the gap of flow 4's book, centered on the hinge: its 24dp band and 24dp on each side.
private val bookGap = 72.dp
private val contentMaxWidth = 560.dp

// Why: a tabletop keeps the screen, split on the hinge lying down as a book's was.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditProfileFullScreen(
    state: EditProfileUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (EditProfileUiEvent) -> Unit,
    onPhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_profile_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(EditProfileUiEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(DesignR.string.designsystem_back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onEvent(EditProfileUiEvent.OnSaveClicked) },
                        enabled = state.canSave,
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text(stringResource(R.string.edit_profile_save)) }
                },
            )
        },
    ) { padding ->
        val isBook = rememberFoldLayout().bookSplit != null
        AdaptiveScreen(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) { layout ->
            if (isBook) {
                EditProfileBookLayout(
                    state = state,
                    onEvent = onEvent,
                    onPhotoClick = onPhotoClick,
                )
            } else if (layout.usesTwoColumns) {
                EditProfileTwoColumnLayout(
                    state = state,
                    onEvent = onEvent,
                    onPhotoClick = onPhotoClick,
                )
            } else {
                EditProfileSingleColumn(
                    state = state,
                    onEvent = onEvent,
                    onPhotoClick = onPhotoClick,
                )
            }
        }
    }
}

@Composable
private fun EditProfileSingleColumn(
    state: EditProfileUiState,
    onEvent: (EditProfileUiEvent) -> Unit,
    onPhotoClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = contentMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 24.dp,
                    vertical = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarSection(
                state = state,
                onEvent = onEvent,
                onPhotoClick = onPhotoClick,
            )
            NameSection(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

// Why: lying down there is little height, so the avatar goes left and the name right.
@Composable
private fun EditProfileTwoColumnLayout(
    state: EditProfileUiState,
    onEvent: (EditProfileUiEvent) -> Unit,
    onPhotoClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            AvatarSection(
                state = state,
                onEvent = onEvent,
                onPhotoClick = onPhotoClick,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
        ) {
            NameSection(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

// Why: a book (and a tabletop lying down) splits on the hinge: the avatar in the half before it, the name after it.
@Composable
private fun EditProfileBookLayout(
    state: EditProfileUiState,
    onEvent: (EditProfileUiEvent) -> Unit,
    onPhotoClick: () -> Unit,
) {
    TwoHalvesLayout(
        start = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 32.dp,
                        top = 16.dp,
                        bottom = 24.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AvatarSection(
                    state = state,
                    onEvent = onEvent,
                    onPhotoClick = onPhotoClick,
                )
            }
        },
        end = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        end = 32.dp,
                        top = 16.dp,
                        bottom = 24.dp,
                    ),
                verticalArrangement = Arrangement.Center,
            ) {
                NameSection(
                    state = state,
                    onEvent = onEvent,
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
        gap = bookGap,
    )
}
