package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.GroupAvatarShape
import com.quare.blitzsplit.android.designsystem.component.GroupBadge
import com.quare.blitzsplit.android.designsystem.component.PhotoViewerContent
import com.quare.blitzsplit.android.designsystem.component.PhotoViewerErrorText
import com.quare.blitzsplit.android.designsystem.component.PhotoViewerPreparingOverlay
import com.quare.blitzsplit.android.designsystem.component.PhotoViewerShortcutsRow
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.editgroup.presentation.model.GroupPhotoUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.GroupPhotoUiState

// Why: the group's photo opened big, in the viewer the profile's photo shares (PhotoViewerContent).
@Composable
internal fun GroupPhotoContent(
    state: GroupPhotoUiState,
    onEvent: (GroupPhotoUiEvent) -> Unit,
    onTakePhotoClick: (() -> Unit)?,
    onChoosePhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
) {
    PhotoViewerContent(
        isLoaded = state.isLoaded,
        closeDescription = stringResource(R.string.edit_group_close),
        onDismiss = { onEvent(GroupPhotoUiEvent.OnDismissed) },
        modifier = modifier,
        snackbarHost = snackbarHost,
    ) { avatarSize ->
        LargeGroupBadge(
            state = state,
            size = avatarSize,
        )
        if (state.showsPhotoError) {
            PhotoViewerErrorText(stringResource(R.string.edit_group_photo_error))
        }
        PhotoViewerShortcutsRow(
            cameraLabel = stringResource(R.string.group_photo_camera),
            cameraDescription = stringResource(R.string.edit_group_take_photo),
            galleryLabel = stringResource(R.string.group_photo_gallery),
            galleryDescription = stringResource(R.string.edit_group_choose_photo),
            removeLabel = stringResource(R.string.group_photo_remove),
            removeDescription = stringResource(R.string.group_photo_remove_photo),
            onTakePhotoClick = onTakePhotoClick,
            onChoosePhotoClick = onChoosePhotoClick,
            onRemoveClick = { onEvent(GroupPhotoUiEvent.OnPhotoRemoved) }.takeIf { state.hasPhoto },
        )
    }
}

@Composable
private fun LargeGroupBadge(
    state: GroupPhotoUiState,
    size: Dp,
) {
    val description = stringResource(R.string.group_photo_title)
    val shape = GroupAvatarShape
    Box(contentAlignment = Alignment.Center) {
        GroupBadge(
            initials = state.initials,
            color = state.color,
            photoUrl = state.photoUrl,
            size = size,
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = shape,
                ).semantics { contentDescription = description },
        )
        if (state.isPreparingPhoto) {
            PhotoViewerPreparingOverlay(
                size = size,
                shape = shape,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun GroupPhotoContentPreview() {
    BlitzSplitTheme {
        GroupPhotoContent(
            state = GroupPhotoUiState(
                isLoaded = true,
                name = "Nosso Lar",
                initials = "NL",
                color = AvatarColor.BLUE,
                photo = null,
                isPreparingPhoto = false,
                showsPhotoError = false,
                photoCrop = null,
            ),
            onEvent = {},
            onTakePhotoClick = {},
            onChoosePhotoClick = {},
        )
    }
}
