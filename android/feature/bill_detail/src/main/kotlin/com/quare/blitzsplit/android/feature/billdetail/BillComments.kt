package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.AvatarBadge
import com.quare.blitzsplit.android.designsystem.component.PendingIndicator
import com.quare.blitzsplit.android.designsystem.component.ShimmerListSkeleton
import com.quare.blitzsplit.android.designsystem.component.groupedRowShapeOf
import com.quare.blitzsplit.android.designsystem.component.opensProfile
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentThread

// Why: a reply sits under its comment's text, past the comment's avatar (16 + 32 + 12).
private val replyStartPadding = 60.dp
private const val SKELETON_COMMENTS = 2
internal val commentAvatarSize = 32.dp
private val replyAvatarSize = 28.dp
private const val HIGHLIGHT_HOLD_MILLIS = 600
private const val HIGHLIGHT_FADE_MILLIS = 1400

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

// Why: the amber dot of what waits for the server, the bill's "Na fila" and a comment's alike.
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

/*
 * Why: "Comentários" (7.3c): each comment with its replies under it, one level deep, as one card per thread; the one
 * being answered stands out while "Respondendo a Diego" is on.
 */
@Composable
internal fun BillCommentsSection(
    threads: List<BillCommentThread>,
    // Why: Conta reads them from Room after the bill; a bill's link page comes with them.
    isLoading: Boolean,
    commentCount: Int?,
    showsNoComments: Boolean,
    replyTargetId: String?,
    focusedCommentId: String?,
    dates: DateTimeTextFormatter,
    callbacks: BillCommentCallbacks,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BillSectionTitle(
            text = stringResource(R.string.bill_detail_comments),
            trailing = commentCount?.takeIf { it > 0 }?.toString(),
        )
        if (showsNoComments) {
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
        // Why: the comments shimmer while they are read, never "Nenhum comentário ainda" before it is known.
        if (isLoading) {
            ShimmerListSkeleton(
                count = SKELETON_COMMENTS,
                leadingSize = 32.dp,
                trailingWidth = null,
                containerShapeOf = { index -> groupedRowShapeOf(index, SKELETON_COMMENTS) },
                spacing = 2.dp,
            )
        }
        // Why: the comment a link opened Conta at keeps its tint while it is scrolled to, then fades it out.
        val highlight = remember { Animatable(0f) }
        var highlightedId by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(highlightedId) {
            if (highlightedId == null) return@LaunchedEffect
            highlight.snapTo(1f)
            highlight.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = HIGHLIGHT_FADE_MILLIS,
                    delayMillis = HIGHLIGHT_HOLD_MILLIS,
                ),
            )
            highlightedId = null
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            threads.forEachIndexed { threadIndex, thread ->
                val rows = listOf(thread.comment) + thread.replies
                rows.forEachIndexed { index, row ->
                    val isFocused = row.id == focusedCommentId
                    CommentRow(
                        row = row,
                        isReply = index > 0,
                        isReplyTarget = row.id == replyTargetId,
                        isFocused = isFocused,
                        highlight = when {
                            isFocused -> 1f
                            row.id == highlightedId -> highlight.value
                            else -> 0f
                        },
                        shape = groupedRowShapeOf(index, rows.size),
                        dates = dates,
                        callbacks = callbacks,
                        onFocusShown = {
                            highlightedId = row.id
                            callbacks.onFocusedCommentShown()
                        },
                        modifier = if (index == 0 && threadIndex > 0) Modifier.padding(top = 8.dp) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    row: BillCommentRow,
    isReply: Boolean,
    isReplyTarget: Boolean,
    // Why: the comment a link opened Conta at, scrolled into view once laid out, in whichever column scrolls it.
    isFocused: Boolean,
    // Why: 1 for the comment a link opened Conta at, fading to 0.
    highlight: Float,
    shape: Shape,
    dates: DateTimeTextFormatter,
    callbacks: BillCommentCallbacks,
    onFocusShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    if (isFocused) {
        val currentOnFocusShown by rememberUpdatedState(onFocusShown)
        LaunchedEffect(bringIntoView) {
            withFrameNanos {}
            bringIntoView.bringIntoView()
            currentOnFocusShown()
        }
    }
    val baseColor = if (isReplyTarget) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    Surface(
        modifier = modifier
            .bringIntoViewRequester(bringIntoView)
            .fillMaxWidth(),
        shape = shape,
        color = lerp(baseColor, MaterialTheme.colorScheme.primaryContainer, highlight),
    ) {
        if (row.isDeleted) {
            DeletedCommentText(isReply)
        } else {
            CommentContent(
                row = row,
                isReply = isReply,
                dates = dates,
                callbacks = callbacks,
            )
        }
    }
}

// Why: "Comentário apagado", a deleted comment kept over its replies, with no face, name nor actions.
@Composable
private fun DeletedCommentText(isReply: Boolean) {
    Text(
        text = stringResource(R.string.bill_detail_comment_deleted),
        modifier = Modifier.padding(
            start = if (isReply) replyStartPadding else 16.dp,
            end = 16.dp,
            top = 14.dp,
            bottom = 14.dp,
        ),
        style = MaterialTheme.typography.bodyLarge,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommentContent(
    row: BillCommentRow,
    isReply: Boolean,
    dates: DateTimeTextFormatter,
    callbacks: BillCommentCallbacks,
) {
    Row(
        modifier = Modifier.padding(
            start = if (isReply) replyStartPadding else 16.dp,
            end = 4.dp,
            top = if (isReply) 10.dp else 14.dp,
            bottom = 4.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val authorName = personNameOf(row.authorName, row.isMe)
        val openAuthor = callbacks.onPersonClick?.let { open -> row.authorMemberId?.let { id -> { open(id) } } }
        // Why: the author's face opens their profile while they are in the group, named for TalkBack.
        AvatarBadge(
            initial = row.initials,
            color = row.avatarColor,
            photoUrl = row.photoUrl,
            size = if (isReply) replyAvatarSize else commentAvatarSize,
            modifier = Modifier
                .clip(CircleShape)
                .semantics { if (openAuthor != null) contentDescription = authorName }
                .opensProfile(openAuthor),
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
                        text = authorName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (row.viaLink) ViaLinkBadge()
                    val time = commentTimeTextOf(row, dates)
                    Text(
                        text = if (row.isEdited) {
                            stringResource(
                                R.string.bill_detail_comment_time_edited,
                                time,
                            )
                        } else {
                            time
                        },
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                callbacks.onReplyClick?.let { onReplyClick ->
                    TextButton(
                        onClick = { onReplyClick(row.id) },
                        modifier = Modifier.offset(x = (-12).dp),
                    ) {
                        Text(stringResource(R.string.bill_detail_reply))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                CommentLikeButton(
                    row = row,
                    callbacks = callbacks,
                )
            }
        }
        CommentMenu(
            row = row,
            callbacks = callbacks,
        )
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
