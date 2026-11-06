package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.freeTextKeyboardOptions
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState

private const val COMPOSER_MAX_LINES = 5

/*
 * Why: "Escreva um comentário" and its send button, with "Respondendo a Diego" over them while answering, or
 * "Editando comentário" and "Salvar" while editing. Pinned at the bottom of a phone's screen and of a one column
 * dialog (with this device's user's face), under the comments in two columns. Answering or editing focuses the field,
 * so the keyboard opens on it.
 */
@Composable
internal fun BillCommentComposerBox(
    state: BillDetailUiState,
    showsAvatar: Boolean,
    fieldColor: Color,
    onEvent: (BillDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val replyTarget = state.replyTarget
    LaunchedEffect(replyTarget?.commentId, state.editingCommentId) {
        if (replyTarget != null || state.isEditingComment) focusRequester.requestFocus()
    }
    /*
     * Why: a draft replaced from outside ("Editar" fills it, sending empties it) puts the caret at its end; one that is
     * only catching up with what was typed (the state comes a frame later) leaves the field, its caret included, alone.
     */
    var field by remember { mutableStateOf(TextFieldValue(state.commentDraft, TextRange(state.commentDraft.length))) }
    val typed = remember { mutableListOf<String>() }
    if (state.commentDraft == field.text) {
        typed.clear()
    } else if (state.commentDraft !in typed) {
        field = TextFieldValue(state.commentDraft, TextRange(state.commentDraft.length))
        typed.clear()
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.isEditingComment) {
            ComposerContextChip(
                text = stringResource(R.string.bill_detail_editing_comment),
                cancelLabel = stringResource(R.string.bill_detail_cancel_edit),
                onCancel = { onEvent(BillDetailUiEvent.OnCommentEditCancelled) },
            )
        }
        replyTarget?.let { target ->
            ComposerContextChip(
                text = if (target.isMe) {
                    stringResource(R.string.bill_detail_replying_to_you)
                } else {
                    stringResource(
                        R.string.bill_detail_replying_to,
                        target.name ?: stringResource(R.string.bill_detail_someone),
                    )
                },
                cancelLabel = stringResource(R.string.bill_detail_cancel_reply),
                onCancel = { onEvent(BillDetailUiEvent.OnReplyCancelled) },
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val me = state.people.firstOrNull { it.isMe }
            if (showsAvatar && me != null) {
                AvatarBadge(
                    initial = me.initials,
                    color = me.avatarColor,
                    photoUrl = me.photoUrl,
                    size = commentAvatarSize,
                )
            }
            TextField(
                value = field,
                onValueChange = {
                    field = it
                    typed += it.text
                    onEvent(BillDetailUiEvent.OnCommentChanged(it.text))
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.bill_detail_comment_placeholder)) },
                maxLines = COMPOSER_MAX_LINES,
                keyboardOptions = freeTextKeyboardOptions,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    disabledContainerColor = fieldColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
            FilledIconButton(
                onClick = { onEvent(BillDetailUiEvent.OnSendCommentClicked) },
                enabled = state.canSendComment,
            ) {
                // Why: "Salvar" (✓) while "Editando comentário", "Enviar comentário" otherwise.
                if (state.isEditingComment) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = stringResource(R.string.bill_detail_save_comment),
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = stringResource(R.string.bill_detail_send_comment),
                    )
                }
            }
        }
    }
}

// Why: "Respondendo a Diego" or "Editando comentário" over the composer, with its ✕.
@Composable
private fun ComposerContextChip(
    text: String,
    cancelLabel: String,
    onCancel: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(
                    weight = 1f,
                    fill = false,
                ),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = cancelLabel,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
