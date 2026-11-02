package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.DialogPlacement
import com.quare.blitzsplit.android.designsystem.layout.FoldDialog
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement

private val halfPaneMaxWidth = 560.dp

/*
 * Why: "Quem pagou", "Várias pessoas pagaram" and "Rachar em qual grupo?" rise from the bottom over the bill, but a
 * fold draws them where it draws the bill, in a book's right half (a tabletop's half above its hinge), never across
 * the hinge (7.1, flow 7's foldable rules). The column inside brings its own sides and bottom; the half adds the top
 * the sheet's handle gives.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BillSheet(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val placement = rememberDialogPlacement()
    if (placement == DialogPlacement.Centered) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            content = { content() },
        )
    } else {
        FoldDialog(
            onDismissRequest = onDismissRequest,
            placement = placement,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = halfPaneMaxWidth)
                    .semantics { paneTitle = title },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Box(modifier = Modifier.padding(top = 24.dp)) { content() }
            }
        }
    }
}
