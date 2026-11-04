package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveSheet
import com.quare.blitzsplit.android.designsystem.layout.DialogPlacement
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement

/*
 * Why: "Quem pagou", "Várias pessoas pagaram" and "Rachar em qual grupo?" rise from the bottom over the bill, but a
 * fold draws them where it draws the bill, in a book's right half (a tabletop's half above its hinge), never across
 * the hinge (7.1, flow 7's foldable rules). The column inside brings its own sides and bottom; the half adds the top
 * the sheet's handle gives.
 */
@Composable
internal fun BillSheet(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    AdaptiveSheet(
        title = title,
        onDismissRequest = onDismissRequest,
        usesDialog = rememberDialogPlacement() != DialogPlacement.Centered,
        dialogShape = MaterialTheme.shapes.extraLarge,
        dialogColor = MaterialTheme.colorScheme.surfaceContainerLow,
        content = content,
    )
}
