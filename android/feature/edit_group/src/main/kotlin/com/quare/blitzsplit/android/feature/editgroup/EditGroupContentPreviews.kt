package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiState

@DevicePreviews
@Composable
private fun EditGroupPreview() {
    BlitzSplitTheme {
        EditGroupContent(
            state = buildPreviewState(
                name = "Viagem Floripa",
                isOnline = true,
            ),
            isDialog = rememberUsesEditGroupDialog(),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun EditGroupLoadingPreview() {
    BlitzSplitTheme {
        EditGroupContent(
            state = buildPreviewState(
                name = "",
                isOnline = true,
            ).copy(isLoaded = false),
            isDialog = rememberUsesEditGroupDialog(),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun EditGroupEmptyNameOfflinePreview() {
    BlitzSplitTheme {
        EditGroupContent(
            state = buildPreviewState(
                name = "",
                isOnline = false,
            ),
            isDialog = rememberUsesEditGroupDialog(),
            onEvent = {},
        )
    }
}

private fun buildPreviewState(
    name: String,
    isOnline: Boolean,
): EditGroupUiState = EditGroupUiState(
    name = name,
    initials = if (name.isEmpty()) "" else "VF",
    color = AvatarColor.PEACH,
    shape = GroupShape.COOKIE,
    photo = null,
    isPreparingPhoto = false,
    showsPhotoError = false,
    iconEditor = null,
    type = GroupType.TRIP,
    currency = GroupCurrency.BRL,
    isCurrencyLocked = true,
    defaultSplitMode = DefaultSplitMode.SHARES,
    isOnline = isOnline,
    isLoaded = true,
    isSaving = false,
    showsSaveError = false,
    hasChanges = true,
)
