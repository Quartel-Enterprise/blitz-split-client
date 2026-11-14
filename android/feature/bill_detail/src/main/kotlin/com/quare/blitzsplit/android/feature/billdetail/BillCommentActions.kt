package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentRow

/*
 * Why: what "Comentários" tells its screen, Conta or a bill's link page, each with its own events. `canChange` is off
 * while the heart, Editar and Apagar can't reach the server (the link's page offline or with a change on its way);
 * no `onPersonClick` where nobody opens a profile, no `onReplyClick` where there is no composer to answer in.
 */
internal class BillCommentCallbacks(
    val canChange: Boolean,
    val onPersonClick: ((memberId: String) -> Unit)?,
    val onReplyClick: ((commentId: String) -> Unit)?,
    val onLikeClick: (commentId: String) -> Unit,
    val onLikesClick: (commentId: String) -> Unit,
    val onShareClick: (commentId: String) -> Unit,
    val onEditClick: (commentId: String) -> Unit,
    val onDeleteClick: (commentId: String) -> Unit,
    val onFocusedCommentShown: () -> Unit,
)

/*
 * Why: the heart likes the comment or undoes the like, offline too; its count, or a long press on it, lists who liked
 * it ("Curtidas").
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CommentLikeButton(
    row: BillCommentRow,
    callbacks: BillCommentCallbacks,
) {
    val likeLabel = stringResource(if (row.isLikedByMe) R.string.bill_detail_unlike else R.string.bill_detail_like)
    val countLabel = pluralStringResource(R.plurals.bill_detail_like_count, row.likeCount, row.likeCount)
    val opensLikes = { callbacks.onLikesClick(row.id) }
    val hasLikes = row.likeCount > 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(40.dp)
                .clip(CircleShape)
                .combinedClickable(
                    // Why: off, the heart still lists who liked on a long press, which changes nothing.
                    enabled = callbacks.canChange || hasLikes,
                    onClickLabel = likeLabel.takeIf { callbacks.canChange },
                    role = Role.Button,
                    onLongClickLabel = countLabel.takeIf { hasLikes },
                    onLongClick = opensLikes.takeIf { hasLikes },
                    onClick = { if (callbacks.canChange) callbacks.onLikeClick(row.id) },
                ).semantics { contentDescription = likeLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (row.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (row.isLikedByMe) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (hasLikes) {
            Text(
                text = row.likeCount.toString(),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(
                        onClickLabel = stringResource(R.string.bill_detail_likes_title),
                        role = Role.Button,
                        onClick = opensLikes,
                    ).semantics { contentDescription = countLabel }
                    .padding(
                        horizontal = 6.dp,
                        vertical = 8.dp,
                    ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Why: "Compartilhar" on every comment, "Editar" and "Apagar" on the user's own.
@Composable
internal fun CommentMenu(
    row: BillCommentRow,
    callbacks: BillCommentCallbacks,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.bill_detail_comment_options),
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.bill_detail_comment_share)) },
                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                onClick = {
                    isExpanded = false
                    callbacks.onShareClick(row.id)
                },
            )
            if (row.canChange && callbacks.canChange) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bill_detail_comment_edit)) },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    onClick = {
                        isExpanded = false
                        callbacks.onEditClick(row.id)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bill_detail_comment_delete)) },
                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                    onClick = {
                        isExpanded = false
                        callbacks.onDeleteClick(row.id)
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.error,
                        leadingIconColor = MaterialTheme.colorScheme.error,
                    ),
                )
            }
        }
    }
}
