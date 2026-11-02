package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FormPresentation
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.billdetail.presentation.model.ShareBillUiState

private fun createPreviewShareBillState(
    link: String? = "https://blitzsplit.com/c/7Hk2pQ8x",
    isOnline: Boolean = true,
    isLoading: Boolean = false,
    hasFailed: Boolean = false,
): ShareBillUiState = ShareBillUiState(
    description = "Jantar no Ostradamus",
    totalMinor = 64_200L,
    currency = GroupCurrency.BRL,
    memberNames = listOf("Ana", "Bruno", "Carla", "Elisa", "Diego"),
    link = link,
    visibility = BillLinkVisibility.MEMBERS,
    isOnline = isOnline,
    isLoading = isLoading,
    isUpdating = false,
    hasFailed = hasFailed,
    isLinkCopied = false,
)

@DevicePreviews
@Composable
private fun ShareBillSheetPreview() {
    BlitzSplitTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            ShareBillContent(
                state = createPreviewShareBillState(),
                presentation = FormPresentation.BottomSheet,
                snackbarHostState = SnackbarHostState(),
                onEvent = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun ShareBillDialogOfflinePreview() {
    BlitzSplitTheme {
        ShareBillContent(
            state = createPreviewShareBillState(
                link = null,
                isOnline = false,
            ),
            presentation = FormPresentation.Dialog,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ShareBillLoadingPreview() {
    BlitzSplitTheme {
        ShareBillContent(
            state = createPreviewShareBillState(
                link = null,
                isLoading = true,
            ),
            presentation = FormPresentation.Dialog,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun ShareBillFailedPreview() {
    BlitzSplitTheme {
        ShareBillContent(
            state = createPreviewShareBillState(
                link = null,
                hasFailed = true,
            ),
            presentation = FormPresentation.Dialog,
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}
