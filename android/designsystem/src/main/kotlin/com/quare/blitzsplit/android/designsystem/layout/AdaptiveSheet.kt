package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val landscapeSheetMaxWidth = 640.dp

/*
 * Why: a short sheet over the app, the same on every screen that opens one: a bottom sheet on a phone (centered at up
 * to 640dp lying down), a 560dp dialog on a tablet and an upright foldable, the book's right half with the screen
 * under the scrim. The dialog adds the top space the sheet's drag handle gives, and the title TalkBack says as it
 * opens, as Material's dialogs do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheet(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val formFactor = rememberFoldLayout().formFactor
    if (formFactor == FormFactor.Phone || formFactor == FormFactor.PhoneLandscape) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = rememberFullSheetState(),
            sheetMaxWidth = landscapeSheetMaxWidth,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            content()
        }
    } else {
        FoldDialog(onDismissRequest = onDismissRequest) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.alertDialogPane(title),
            ) {
                Box(modifier = Modifier.padding(top = 24.dp)) { content() }
            }
        }
    }
}
