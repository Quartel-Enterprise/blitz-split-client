package com.quare.blitzsplit.android.feature.editprofile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.PhotoShortcutButton
import com.quare.blitzsplit.android.designsystem.component.PhotoViewerContent
import com.quare.blitzsplit.android.designsystem.component.PhotoViewerDestructiveContent
import com.quare.blitzsplit.android.designsystem.component.PhotoViewerIconSize
import com.quare.blitzsplit.android.designsystem.component.PhotoViewerPreparingOverlay
import com.quare.blitzsplit.android.designsystem.component.PhotoViewerShortcutsRow
import com.quare.blitzsplit.android.designsystem.component.labelRes
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.editprofile.presentation.model.ProfilePhotoUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.model.ProfilePhotoUiState
import com.quare.blitzsplit.android.designsystem.R as DesignR

// Why: the profile's avatar opened big, in the viewer the group's photo shares (PhotoViewerContent).
@Composable
internal fun ProfilePhotoContent(
    state: ProfilePhotoUiState,
    onEvent: (ProfilePhotoUiEvent) -> Unit,
    onTakePhotoClick: (() -> Unit)?,
    onChoosePhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
) {
    PhotoViewerContent(
        isLoaded = state.isLoaded,
        closeDescription = stringResource(R.string.profile_photo_close),
        onDismiss = { onEvent(ProfilePhotoUiEvent.OnDismissed) },
        modifier = modifier,
        snackbarHost = snackbarHost,
    ) { avatarSize ->
        LargeAvatarImage(
            state = state,
            size = avatarSize,
        )
        if (state.showsPhotoError) {
            Text(
                text = stringResource(R.string.edit_profile_photo_error),
                style = MaterialTheme.typography.bodyMedium,
                color = PhotoViewerDestructiveContent,
                textAlign = TextAlign.Center,
            )
        }
        PhotoViewerShortcutsRow(
            cameraLabel = stringResource(R.string.profile_photo_camera),
            cameraDescription = stringResource(R.string.edit_profile_take_photo),
            galleryLabel = stringResource(R.string.profile_photo_gallery),
            galleryDescription = stringResource(R.string.edit_profile_choose_photo),
            removeLabel = stringResource(R.string.profile_photo_remove),
            removeDescription = stringResource(R.string.edit_profile_remove_photo),
            onTakePhotoClick = onTakePhotoClick,
            onChoosePhotoClick = onChoosePhotoClick,
            onRemoveClick = { onEvent(ProfilePhotoUiEvent.OnPhotoRemoved) }.takeIf { state.hasPhoto },
            leading = { AccountPhotoShortcutButton(state, onEvent) },
        )
    }
}

@Composable
private fun LargeAvatarImage(
    state: ProfilePhotoUiState,
    size: Dp,
) {
    val description = stringResource(R.string.profile_photo_title)
    Box(contentAlignment = Alignment.Center) {
        AvatarBadge(
            initial = state.avatarInitial,
            color = state.avatarColor,
            size = size,
            photoUrl = state.photoUrl,
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                ).semantics { contentDescription = description },
        )
        if (state.isPreparingPhoto) {
            PhotoViewerPreparingOverlay(
                size = size,
                shape = CircleShape,
            )
        }
    }
}

// Why: the account's logo on white, as Bible Planner's viewer and the sign in buttons draw it.
@Composable
private fun AccountPhotoShortcutButton(
    state: ProfilePhotoUiState,
    onEvent: (ProfilePhotoUiEvent) -> Unit,
) {
    val provider = state.accountPhotoProvider ?: return
    PhotoShortcutButton(
        icon = {
            if (provider == AccountProvider.GOOGLE) {
                Image(
                    painter = painterResource(DesignR.drawable.ic_google_logo),
                    contentDescription = null,
                    modifier = Modifier.size(PhotoViewerIconSize),
                )
            } else {
                Icon(
                    painter = painterResource(DesignR.drawable.ic_apple_logo),
                    contentDescription = null,
                    modifier = Modifier.size(PhotoViewerIconSize),
                    tint = Color.Black,
                )
            }
        },
        background = Color.White,
        label = stringResource(provider.labelRes),
        accessibilityLabel = stringResource(
            R.string.edit_profile_use_account_photo,
            stringResource(provider.labelRes),
        ),
        onClick = { onEvent(ProfilePhotoUiEvent.OnAccountPhotoSelected) },
    )
}

@DevicePreviews
@Composable
private fun ProfilePhotoContentPreview() {
    BlitzSplitTheme {
        ProfilePhotoContent(
            state = ProfilePhotoUiState(
                isLoaded = true,
                displayName = "Pierre Vieira",
                avatarColor = AvatarColor.BLUE,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                photoCrop = null,
                accountProvider = AccountProvider.GOOGLE,
                accountPhotoUrl = "https://example.com/photo.jpg",
            ),
            onEvent = {},
            onTakePhotoClick = {},
            onChoosePhotoClick = {},
        )
    }
}
