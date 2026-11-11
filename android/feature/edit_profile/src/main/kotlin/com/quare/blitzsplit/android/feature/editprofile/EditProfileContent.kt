package com.quare.blitzsplit.android.feature.editprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarColorPicker
import com.quare.blitzsplit.android.designsystem.component.AvatarWithPhotoButton
import com.quare.blitzsplit.android.designsystem.component.DialogCard
import com.quare.blitzsplit.android.designsystem.component.SheetActionItem
import com.quare.blitzsplit.android.designsystem.component.ShimmerAvatarSkeleton
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.ShimmerFormSkeleton
import com.quare.blitzsplit.android.designsystem.component.labelRes
import com.quare.blitzsplit.android.designsystem.component.personNameKeyboardOptions
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.FormPresentation
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileNameError
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

private val avatarSize = 120.dp

/*
 * Why: a photo, a name and a notice are too little for a screen of their own (Pierre's call, beta feedback), so Edit
 * profile opens as Registrar pagamento does: a bottom sheet on a phone, a 560dp dialog over the profile anywhere wider
 * (a book's half after the hinge), and full screen on a tabletop, as the screen was.
 */
@Composable
fun EditProfileContent(
    state: EditProfileUiState,
    presentation: FormPresentation,
    snackbarHostState: SnackbarHostState,
    onEvent: (EditProfileUiEvent) -> Unit,
    // Why: null on a device that can't take a photo, which only offers choosing one.
    onTakePhotoClick: (() -> Unit)?,
    onChoosePhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPhotoMenuOpen by rememberSaveable { mutableStateOf(false) }
    val onPhotoClick = { isPhotoMenuOpen = true }
    when (presentation) {
        FormPresentation.BottomSheet -> EditProfileSheet(
            state = state,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
            onPhotoClick = onPhotoClick,
            modifier = modifier,
        )

        FormPresentation.Dialog -> EditProfileDialog(
            state = state,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
            onPhotoClick = onPhotoClick,
            modifier = modifier,
        )

        FormPresentation.TabletopHalves -> EditProfileFullScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
            onPhotoClick = onPhotoClick,
            modifier = modifier,
        )
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

/*
 * Why: a phone's sheet, its title beside Save. The keyboard pushes the form up, so the name stays above it; lying down
 * the photo goes left and the name right, as the screen did.
 */
@Composable
private fun EditProfileSheet(
    state: EditProfileUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (EditProfileUiEvent) -> Unit,
    onPhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.edit_profile_title)
    val isLyingDown = rememberFoldLayout().formFactor == FormFactor.PhoneLandscape
    Box(modifier = modifier.semantics { paneTitle = title }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                Button(
                    onClick = { onEvent(EditProfileUiEvent.OnSaveClicked) },
                    enabled = state.canSave,
                ) { Text(stringResource(R.string.edit_profile_save)) }
            }
            if (isLyingDown) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        AvatarSection(
                            state = state,
                            onEvent = onEvent,
                            onPhotoClick = onPhotoClick,
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        NameSection(
                            state = state,
                            onEvent = onEvent,
                        )
                    }
                }
            } else {
                Column(
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// Why: a 560dp card over the profile, Cancel and Save at its foot as a dialog's buttons.
@Composable
private fun EditProfileDialog(
    state: EditProfileUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (EditProfileUiEvent) -> Unit,
    onPhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.edit_profile_title)
    DialogCard(
        isWide = false,
        spacing = 16.dp,
        snackbarHostState = snackbarHostState,
        modifier = modifier.semantics { paneTitle = title },
        cardModifier = Modifier.imePadding(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Column(
            modifier = Modifier
                .weight(
                    weight = 1f,
                    fill = false,
                ).verticalScroll(rememberScrollState()),
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
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = { onEvent(EditProfileUiEvent.OnBackClicked) },
                enabled = !state.isSaving,
            ) {
                Text(stringResource(DesignR.string.designsystem_confirm_cancel))
            }
            TextButton(
                onClick = { onEvent(EditProfileUiEvent.OnSaveClicked) },
                enabled = state.canSave,
            ) { Text(stringResource(R.string.edit_profile_save)) }
        }
    }
}

@Composable
internal fun AvatarSection(
    state: EditProfileUiState,
    onEvent: (EditProfileUiEvent) -> Unit,
    onPhotoClick: () -> Unit,
) {
    if (!state.isLoaded) {
        ShimmerBox {
            ShimmerAvatarSkeleton(size = avatarSize)
        }
        return
    }
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
internal fun NameSection(
    state: EditProfileUiState,
    onEvent: (EditProfileUiEvent) -> Unit,
) {
    // Why: never an empty field the saved name would then replace under the typing.
    if (!state.isLoaded) {
        ShimmerFormSkeleton(fieldCount = 1)
        return
    }
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
                    val error = when (state.nameError) {
                        EditProfileNameError.TOO_SHORT -> stringResource(
                            R.string.edit_profile_name_too_short,
                            state.nameMinLength,
                        )

                        EditProfileNameError.TOO_LONG -> stringResource(
                            R.string.edit_profile_name_too_long,
                            state.nameMaxLength,
                        )

                        null -> null
                    }
                    Text(
                        text = error.orEmpty(),
                        modifier = Modifier.weight(1f),
                    )
                    Text(stringResource(R.string.edit_profile_name_count, state.nameLength, state.nameMaxLength))
                }
            },
            keyboardOptions = personNameKeyboardOptions.copy(imeAction = ImeAction.Done),
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
                SheetActionItem(
                    icon = Icons.Outlined.PhotoCamera,
                    label = stringResource(R.string.edit_profile_take_photo),
                    onClick = {
                        onDismiss()
                        onTakePhotoClick()
                    },
                )
            }
            SheetActionItem(
                icon = Icons.Outlined.PhotoLibrary,
                label = stringResource(R.string.edit_profile_choose_photo),
                onClick = {
                    onDismiss()
                    onChoosePhotoClick()
                },
            )
            if (accountPhotoProvider != null) {
                SheetActionItem(
                    icon = Icons.Outlined.AccountCircle,
                    label = stringResource(
                        R.string.edit_profile_use_account_photo,
                        stringResource(accountPhotoProvider.labelRes),
                    ),
                    onClick = {
                        onDismiss()
                        onUseAccountPhotoClick()
                    },
                )
            }
            if (hasPhoto) {
                SheetActionItem(
                    icon = Icons.Outlined.Delete,
                    label = stringResource(R.string.edit_profile_remove_photo),
                    onClick = {
                        onDismiss()
                        onRemoveClick()
                    },
                )
            }
        }
    }
}
