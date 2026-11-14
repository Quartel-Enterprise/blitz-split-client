package com.quare.blitzsplit.android.feature.categorymap

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiState

// Why: the map as a new account sees it, the app's own terms under the app's categories.
private fun createPreviewCategoryMapState(): CategoryMapUiState = CategoryMapUiState.initial.copy(isLoaded = true)

@DevicePreviews
@Composable
private fun CategoryMapContentPreview() {
    BlitzSplitTheme {
        CategoryMapContent(
            state = createPreviewCategoryMapState(),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun CategoryMapSearchPreview() {
    BlitzSplitTheme {
        CategoryMapContent(
            state = createPreviewCategoryMapState().copy(query = "ca"),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun CategoryMapLoadingPreview() {
    BlitzSplitTheme {
        CategoryMapContent(
            state = createPreviewCategoryMapState().copy(isLoaded = false),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
        )
    }
}
