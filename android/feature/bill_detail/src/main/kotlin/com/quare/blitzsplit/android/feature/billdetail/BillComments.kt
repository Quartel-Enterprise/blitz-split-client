package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState

// Why: a reply sits under its comment's text, past the comment's avatar (16 + 32 + 12).
private val replyStartPadding = 60.dp
private const val COMPOSER_MAX_LINES = 5
private val commentAvatarSize = 32.dp
private val replyAvatarSize = 28.dp

// Why: "Sem internet. Comentários e fotos ficam na fila e saem ao reconectar." at the top of Conta.
@Composable
internal fun BillOfflineBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 10.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.bill_detail_offline),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

// Why: a section's title over its card, "Comentários" with its count at the end.
@Composable
internal fun BillSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        trailing?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Why: the orange dot of what waits for the server, the bill's "Na fila" and a comment's alike.
@Composable
internal fun PendingLine(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PendingIndicator()
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun PendingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(8.dp)
            .background(MaterialTheme.colorScheme.tertiary, CircleShape),
    )
}

/*
 * Why: "Comentários" (7.3c): each comment with its replies under it, one level deep, as one card per thread; the one
 * being answered stands out while "Respondendo a Diego" is on.
 */
@Composable
internal fun BillCommentsSection(
    state: BillDetailUiState,
    dates: DateTimeTextFormatter,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BillSectionTitle(
            text = stringResource(R.string.bill_detail_comments),
            trailing = state.commentCount.takeIf { it > 0 }?.toString(),
        )
        if (state.showsNoComments) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = groupedRowShapeOf(
                    index = 0,
                    count = 1,
                ),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Text(
                    text = stringResource(R.string.bill_detail_no_comments),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            state.threads.forEachIndexed { threadIndex, thread ->
                val rows = listOf(thread.comment) + thread.replies
                rows.forEachIndexed { index, row ->
                    CommentRow(
                        row = row,
                        isReply = index > 0,
                        isReplyTarget = row.id == state.replyTarget?.commentId,
                        shape = groupedRowShapeOf(index, rows.size),
                        dates = dates,
                        onEvent = onEvent,
                        modifier = if (index == 0 && threadIndex > 0) Modifier.padding(top = 8.dp) else Modifier,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommentRow(
    row: BillCommentRow,
    isReply: Boolean,
    isReplyTarget: Boolean,
    shape: Shape,
    dates: DateTimeTextFormatter,
    onEvent: (BillDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = if (isReplyTarget) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
    ) {
        Row(
            modifier = Modifier.padding(
                start = if (isReply) replyStartPadding else 16.dp,
                end = 16.dp,
                top = if (isReply) 10.dp else 14.dp,
                bottom = 4.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AvatarBadge(
                initial = row.initials,
                color = row.avatarColor,
                size = if (isReply) replyAvatarSize else commentAvatarSize,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = personNameOf(row.authorName, row.isMe),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (row.viaLink) ViaLinkBadge()
                        Text(
                            text = commentTimeTextOf(row, dates),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    CommentBodyText(row)
                    if (row.isPending) {
                        PendingLine(
                            text = stringResource(R.string.bill_detail_comment_pending),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                TextButton(
                    onClick = { onEvent(BillDetailUiEvent.OnReplyClicked(row.id)) },
                    modifier = Modifier.offset(x = (-12).dp),
                ) {
                    Text(stringResource(R.string.bill_detail_reply))
                }
            }
        }
    }
}

// Why: "@Diego" before a reply to a reply, as the thread itself is only one level deep.
@Composable
private fun CommentBodyText(row: BillCommentRow) {
    val mentionColor = MaterialTheme.colorScheme.primary
    val text = buildAnnotatedString {
        row.replyToName?.let { name ->
            withStyle(
                SpanStyle(
                    color = mentionColor,
                    fontWeight = FontWeight.SemiBold,
                ),
            ) { append("@$name") }
            append(" ")
        }
        append(row.body)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun ViaLinkBadge() {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(6.dp),
            ).padding(
                horizontal = 6.dp,
                vertical = 2.dp,
            ),
    ) {
        Text(
            text = stringResource(R.string.bill_detail_via_link),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/*
 * Why: "Escreva um comentário" and its send button, with "Respondendo a Diego" over them while answering. Pinned at
 * the bottom of a phone's screen and of a one column dialog (with this device's user's face), under the comments in
 * two columns. Answering focuses the field, so the keyboard opens on what is being answered.
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
    LaunchedEffect(replyTarget?.commentId) {
        if (replyTarget != null) focusRequester.requestFocus()
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        replyTarget?.let { target ->
            ReplyingToChip(
                text = if (target.isMe) {
                    stringResource(R.string.bill_detail_replying_to_you)
                } else {
                    stringResource(
                        R.string.bill_detail_replying_to,
                        target.name ?: stringResource(R.string.bill_detail_someone),
                    )
                },
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
                    size = commentAvatarSize,
                )
            }
            TextField(
                value = state.commentDraft,
                onValueChange = { onEvent(BillDetailUiEvent.OnCommentChanged(it)) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.bill_detail_comment_placeholder)) },
                maxLines = COMPOSER_MAX_LINES,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = stringResource(R.string.bill_detail_send_comment),
                )
            }
        }
    }
}

@Composable
private fun ReplyingToChip(
    text: String,
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
                    contentDescription = stringResource(R.string.bill_detail_cancel_reply),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
