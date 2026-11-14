package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.ConfirmDialog
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveSheet
import com.quare.blitzsplit.android.designsystem.layout.rememberDialogPlacement
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentRow

/*
 * Why: "Apagar comentário?", asked before the user's own comment leaves the bill; a book draws it in its right half.
 * Busy on a bill's link page while the deletion is on its way to the server; Conta's goes to the queue at once.
 */
@Composable
internal fun DeleteCommentDialog(
    isBusy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(R.string.bill_detail_delete_comment_title),
        message = stringResource(R.string.bill_detail_delete_comment_message),
        confirmLabel = stringResource(R.string.bill_detail_delete_comment_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isBusy = isBusy,
        placement = rememberDialogPlacement(),
    )
}

/*
 * Why: "Curtidas", who liked a comment in the order they did, "Você" for this device's user: a bottom sheet on a
 * phone, a 560dp dialog on a tablet and an upright foldable, the book's right half, as the app's other short sheets.
 */
@Composable
internal fun CommentLikesSheet(
    comment: BillCommentRow,
    onDismiss: () -> Unit,
) {
    val title = stringResource(R.string.bill_detail_likes_title)
    AdaptiveSheet(
        title = title,
        onDismissRequest = onDismiss,
        dialogShape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            comment.likers.forEach { liker ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = personNameOf(liker.name, liker.isMe),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
