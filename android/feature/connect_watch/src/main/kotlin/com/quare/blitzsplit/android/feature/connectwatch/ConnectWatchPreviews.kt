package com.quare.blitzsplit.android.feature.connectwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.feature.connectwatch.presentation.model.ConnectWatchError
import com.quare.blitzsplit.feature.connectwatch.presentation.model.ConnectWatchUiState

private fun createPreviewState(
    code: String = "K7P4QX",
    isOnline: Boolean = true,
    error: ConnectWatchError? = null,
    isConnected: Boolean = false,
): ConnectWatchUiState = ConnectWatchUiState(
    code = code,
    isOnline = isOnline,
    isConnecting = false,
    error = error,
    isConnected = isConnected,
)

@DevicePreviews
@Composable
private fun ConnectWatchPreview() {
    BlitzSplitTheme {
        ConnectWatchContent(
            state = createPreviewState(),
            isDialog = false,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ConnectWatchWrongCodePreview() {
    BlitzSplitTheme {
        ConnectWatchContent(
            state = createPreviewState(error = ConnectWatchError.INVALID_OR_EXPIRED),
            isDialog = false,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ConnectWatchOfflinePreview() {
    BlitzSplitTheme {
        ConnectWatchContent(
            state = createPreviewState(isOnline = false),
            isDialog = false,
            onEvent = {},
        )
    }
}

@Preview(
    name = "Tablet dialog",
    device = "spec:width=1280dp,height=800dp,orientation=landscape",
)
@Composable
private fun ConnectWatchDialogPreview() {
    BlitzSplitTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
            contentAlignment = Alignment.Center,
        ) {
            ConnectWatchContent(
                state = createPreviewState(isConnected = true),
                isDialog = true,
                onEvent = {},
            )
        }
    }
}
