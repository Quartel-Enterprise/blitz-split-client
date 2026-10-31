package com.quare.blitzsplit.android.feature.editprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarColorPicker
import com.quare.blitzsplit.android.designsystem.component.AvatarWithPhotoButton
import com.quare.blitzsplit.android.designsystem.component.labelRes
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileNameError
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val contentMaxWidth = 560.dp
private val avatarSize = 120.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileContent(
    state: EditProfileUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (EditProfileUiEvent) -> Unit,
    // Why: null on a device that can't take a photo, which only offers choosing one.
    onTakePhotoClick: (() -> Unit)?,
    onChoosePhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPhotoMenuOpen by rememberSaveable { mutableStateOf(false) }
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
        AdaptiveScreen(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) { layout ->
            if (layout.usesTwoColumns) {
                EditProfileTwoColumnLayout(
                    state = state,
                    onEvent = onEvent,
                    onPhotoClick = { isPhotoMenuOpen = true },
                )
            } else {
                EditProfileSingleColumn(
                    state = state,
                    onEvent = onEvent,
                    onPhotoClick = { isPhotoMenuOpen = true },
                )
            }
        }
    }
    if (isPhotoMenuOpen) {
        PhotoMenuSheet(
            hasPhoto = state.hasPhoto,
            accountPhotoProvider = state.accountPhotoProvider,
            onDismiss = { isPhotoMenuOpen = false },
            onTakePhotoClick = onTakePhotoClick,
            onChoosePhotoClick = onChoosePhotoClick,
            onUseAccountPhotoClick = { onEvent(EditProfileUiEvent.OnAccountPhotoSelected) },
            onRemoveClick = { onEvent(EditProfileUiEvent.OnPhotoRemoved) },
        )
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

@Composable
private fun AvatarSection(
    state: EditProfileUiState,
    onEvent: (EditProfileUiEvent) -> Unit,
    onPhotoClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AvatarWithPhotoButton(
            initial = state.avatarInitial,
            color = state.avatarColor,
            photoUrl = state.photoUrl,
            isPreparingPhoto = state.isPreparingPhoto,
            icon = Icons.Outlined.Edit,
            contentDescription = stringResource(R.string.edit_profile_change_photo),
            size = avatarSize,
            onClick = onPhotoClick,
        )
        if (state.showsPhotoError) {
            Text(
                text = stringResource(R.string.edit_profile_photo_error),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.showsAvatarColors) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.edit_profile_avatar_color),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = stringResource(R.string.edit_profile_avatar_color_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AvatarColorPicker(
                selected = state.avatarColor,
                colors = state.avatarColors,
                onSelected = { color -> onEvent(EditProfileUiEvent.OnAvatarColorSelected(color)) },
            )
        }
    }
}

@Composable
private fun NameSection(
    state: EditProfileUiState,
    onEvent: (EditProfileUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(EditProfileUiEvent.OnNameChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.edit_profile_name_label)) },
            isError = state.nameError != null,
            singleLine = true,
            supportingText = {
                Row {
                    val hint = when (state.nameError) {
                        EditProfileNameError.TOO_SHORT -> stringResource(
                            R.string.edit_profile_name_too_short,
                            state.nameMinLength,
                        )

                        EditProfileNameError.TOO_LONG -> stringResource(
                            R.string.edit_profile_name_too_long,
                            state.nameMaxLength,
                        )

                        null -> stringResource(
                            R.string.edit_profile_name_hint,
                            state.nameMinLength,
                            state.nameMaxLength,
                        )
                    }
                    Text(
                        text = hint,
                        modifier = Modifier.weight(1f),
                    )
                    Text(stringResource(R.string.edit_profile_name_count, state.nameLength, state.nameMaxLength))
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onEvent(EditProfileUiEvent.OnSaveClicked) }),
        )
        state.accountProvider?.let { provider -> AccountNoticeCard(provider = provider) }
        Text(
            text = stringResource(R.string.edit_profile_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountNoticeCard(provider: AccountProvider) {
    val providerName = stringResource(provider.labelRes)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
            )
            Text(
                text = stringResource(R.string.edit_profile_account_notice, providerName),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoMenuSheet(
    hasPhoto: Boolean,
    // Why: names whose picture is offered, null when there is none to offer.
    accountPhotoProvider: AccountProvider?,
    onDismiss: () -> Unit,
    onTakePhotoClick: (() -> Unit)?,
    onChoosePhotoClick: () -> Unit,
    onUseAccountPhotoClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            if (onTakePhotoClick != null) {
                PhotoMenuItem(Icons.Outlined.PhotoCamera, stringResource(R.string.edit_profile_take_photo)) {
                    onDismiss()
                    onTakePhotoClick()
                }
            }
            PhotoMenuItem(Icons.Outlined.PhotoLibrary, stringResource(R.string.edit_profile_choose_photo)) {
                onDismiss()
                onChoosePhotoClick()
            }
            if (accountPhotoProvider != null) {
                PhotoMenuItem(
                    icon = Icons.Outlined.AccountCircle,
                    label = stringResource(
                        R.string.edit_profile_use_account_photo,
                        stringResource(accountPhotoProvider.labelRes),
                    ),
                ) {
                    onDismiss()
                    onUseAccountPhotoClick()
                }
            }
            if (hasPhoto) {
                PhotoMenuItem(Icons.Outlined.Delete, stringResource(R.string.edit_profile_remove_photo)) {
                    onDismiss()
                    onRemoveClick()
                }
            }
        }
    }
}

@Composable
private fun PhotoMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        ListItem(
            headlineContent = { Text(label) },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        )
    }
}

@DevicePreviews
@Composable
private fun EditProfilePreview() {
    BlitzSplitTheme {
        EditProfileContent(
            state = EditProfileUiState(
                name = "Pierre Vieira",
                avatarColor = AvatarColor.BLUE,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                nameError = null,
                isNameValid = true,
                hasChanges = true,
                isSaving = false,
                accountProvider = AccountProvider.GOOGLE,
                accountPhotoUrl = null,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onTakePhotoClick = {},
            onChoosePhotoClick = {},
        )
    }
}
