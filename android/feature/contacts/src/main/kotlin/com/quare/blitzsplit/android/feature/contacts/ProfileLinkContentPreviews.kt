package com.quare.blitzsplit.android.feature.contacts

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.contacts.presentation.model.ProfileLinkStage
import com.quare.blitzsplit.feature.contacts.presentation.model.ProfileLinkUiState

private fun createStrangerState(canInvite: Boolean): ProfileLinkUiState = ProfileLinkUiState(
    stage = ProfileLinkStage.STRANGER,
    name = "Ana Souza",
    initials = "AS",
    avatarColor = AvatarColor.PEACH,
    photoUrl = null,
    canInvite = canInvite,
)

@Composable
private fun ProfileLinkPreviewContent(state: ProfileLinkUiState) {
    BlitzSplitTheme {
        ProfileLinkContent(
            state = state,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ProfileLinkStrangerPreview() {
    ProfileLinkPreviewContent(createStrangerState(canInvite = true))
}

@DevicePreviews
@Composable
private fun ProfileLinkStrangerWithoutProfilePreview() {
    ProfileLinkPreviewContent(createStrangerState(canInvite = false))
}

@DevicePreviews
@Composable
private fun ProfileLinkLoadingPreview() {
    ProfileLinkPreviewContent(ProfileLinkUiState.LOADING)
}

@DevicePreviews
@Composable
private fun ProfileLinkUnreachablePreview() {
    ProfileLinkPreviewContent(ProfileLinkUiState.LOADING.copy(stage = ProfileLinkStage.UNREACHABLE))
}

@DevicePreviews
@Composable
private fun ProfileLinkNotFoundPreview() {
    ProfileLinkPreviewContent(ProfileLinkUiState.LOADING.copy(stage = ProfileLinkStage.NOT_FOUND))
}
