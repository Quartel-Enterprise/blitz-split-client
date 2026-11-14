package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalResources
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.android.platform.GalleryPhotoSource
import com.quare.blitzsplit.android.platform.rememberGalleryPhotoSaver

// Why: the viewer's "Baixar" keeps the photo in the gallery and says how it went, on Conta and a bill's link page alike.
@Composable
internal fun rememberBillPhotoSaver(
    snackbarHostState: SnackbarHostState,
): (source: GalleryPhotoSource, fileName: String) -> Unit {
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    return rememberGalleryPhotoSaver { isSaved ->
        scope.showReplacingSnackbar(
            snackbarHostState = snackbarHostState,
            message = resources.getString(
                if (isSaved) R.string.bill_detail_photo_saved else R.string.bill_detail_photo_save_error,
            ),
        )
    }
}
