package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

private val landscapeSheetMaxWidth = 640.dp
private val alertDialogMaxWidth = 560.dp
private val phoneFormFactors = setOf(FormFactor.Phone, FormFactor.PhoneLandscape)

/*
 * Why: a short sheet over the app: a bottom sheet on a phone (centered at up to 640dp lying down), a 560dp dialog on a
 * tablet and an upright foldable, the book's right half with the screen under the scrim. The dialog adds the top space
 * the sheet's drag handle gives, and the title TalkBack says as it opens, as Material's dialogs do. The defaults are
 * that look; the options only keep what the sheets drawn before it already did: a dialog on fewer devices
 * (usesDialog), a sheet that opens half way or narrower lying down, a narrower dialog, its shape and color, a dialog
 * that a tap outside leaves open (the sheet's scrim still closes it), and no pane title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheet(
    title: String?,
    onDismissRequest: () -> Unit,
    usesDialog: Boolean = rememberFoldLayout().formFactor !in phoneFormFactors,
    skipPartiallyExpanded: Boolean = true,
    sheetMaxWidth: Dp = landscapeSheetMaxWidth,
    dialogMaxWidth: Dp = alertDialogMaxWidth,
    dialogShape: Shape = RoundedCornerShape(28.dp),
    dialogColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    dialogDismissOnClickOutside: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (!usesDialog) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            // Why: ModalBottomSheet's own default, not the deprecated rememberModalBottomSheetState's legacy half way.
            sheetState = if (skipPartiallyExpanded) {
                rememberFullSheetState()
            } else {
                rememberBottomSheetState(initialValue = SheetValue.Hidden)
            },
            sheetMaxWidth = sheetMaxWidth,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            content()
        }
    } else {
        val pane = title?.let { Modifier.alertDialogPane(it) } ?: Modifier
        FoldDialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnClickOutside = dialogDismissOnClickOutside,
                usePlatformDefaultWidth = false,
            ),
        ) {
            Surface(
                shape = dialogShape,
                color = dialogColor,
                modifier = pane.widthIn(max = dialogMaxWidth),
            ) {
                Box(modifier = Modifier.padding(top = 24.dp)) { content() }
            }
        }
    }
}
