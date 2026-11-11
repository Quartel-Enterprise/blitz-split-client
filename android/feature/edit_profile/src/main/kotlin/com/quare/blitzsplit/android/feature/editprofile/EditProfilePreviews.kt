package com.quare.blitzsplit.android.feature.editprofile

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FormPresentation
import com.quare.blitzsplit.android.designsystem.layout.LocalFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberFormPresentation
import com.quare.blitzsplit.android.designsystem.layout.tabletopFoldLayoutOf
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiState

private fun createPreviewState(): EditProfileUiState = EditProfileUiState(
    isLoaded = true,
    name = "Pierre Vieira",
    avatarColor = AvatarColor.BLUE,
    photo = null,
    isPreparingPhoto = false,
    showsPhotoError = false,
    photoCrop = null,
    nameError = null,
    isNameValid = true,
    hasChanges = true,
    isSaving = false,
    accountProvider = AccountProvider.GOOGLE,
    accountPhotoUrl = null,
)

@DevicePreviews
@Composable
private fun EditProfilePreview() {
    BlitzSplitTheme {
        EditProfileContent(
            state = createPreviewState(),
            presentation = rememberFormPresentation(),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onTakePhotoClick = {},
            onChoosePhotoClick = {},
        )
    }
}

// Why: a preview's window reports no fold, so the tabletop's hinge is handed in.
@Preview(
    name = "Tabletop",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun EditProfileTabletopPreview() {
    CompositionLocalProvider(LocalFoldLayout provides tabletopFoldLayoutOf(841.dp, 673.dp)) {
        BlitzSplitTheme {
            EditProfileContent(
                state = createPreviewState(),
                presentation = FormPresentation.TabletopHalves,
                snackbarHostState = remember { SnackbarHostState() },
                onEvent = {},
                onTakePhotoClick = {},
                onChoosePhotoClick = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun EditProfileLoadingPreview() {
    BlitzSplitTheme {
        EditProfileContent(
            state = createPreviewState().copy(isLoaded = false),
            presentation = rememberFormPresentation(),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onTakePhotoClick = {},
            onChoosePhotoClick = {},
        )
    }
}

@Preview(
    name = "Tablet dialog",
    device = "spec:width=1280dp,height=800dp,orientation=landscape",
)
@Composable
private fun EditProfileDialogPreview() {
    BlitzSplitTheme {
        EditProfileContent(
            state = createPreviewState(),
            presentation = FormPresentation.Dialog,
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onTakePhotoClick = {},
            onChoosePhotoClick = {},
        )
    }
}
