package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.DialogCard
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberUsesFormColumns
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.android.platform.PhotoPickers
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState
import software.quare.loadable.isLoading
import software.quare.loadable.valueOrNull

private val previewPickers = PhotoPickers(
    onTakePhotoClick = {},
    onChoosePhotoClick = {},
)

/*
 * Why: the canvas (7.3) opens Conta full screen on a phone, in two columns lying down, with the comment field pinned
 * at the bottom upright and under the comments in two columns; on a tablet and the upright foldable it is a dialog
 * over the group, 920dp and two columns on a tablet lying down, one 560dp column upright; a book draws that column in
 * its right half and a tabletop in its half above the hinge (FoldDialog), the group in the other half.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailContent(
    state: BillDetailUiState,
    isDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    pickers: PhotoPickers,
    onEvent: (BillDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Why: a dialog is two columns wide on a tablet lying down only, one column in a book's or a tabletop's half.
    val usesTwoColumns = rememberUsesFormColumns(isDialog)
    val money = rememberMoneyFormatter(state.currency)
    val dates = rememberDateTimeTextFormatter()
    val first: @Composable ColumnScope.() -> Unit = {
        BillDetailBillSection(
            state = state,
            wrapsAttachments = usesTwoColumns,
            money = money,
            dates = dates,
            pickers = pickers,
            onEvent = onEvent,
        )
    }
    val second: @Composable ColumnScope.() -> Unit = {
        BillDetailPeopleSection(
            state = state,
            money = money,
            dates = dates,
            onEvent = onEvent,
        )
    }
    if (isDialog) {
        val fieldColor = MaterialTheme.colorScheme.surfaceContainerLowest
        DialogCard(
            isWide = usesTwoColumns,
            spacing = billSectionSpacing,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
        ) {
            DialogHeader(
                state = state,
                onEvent = onEvent,
            )
            if (!state.isOnline) BillOfflineBanner()
            BillColumnsLayout(
                isLoaded = state.isLoaded,
                usesTwoColumns = usesTwoColumns,
                fillsHeight = false,
                columnsPadding = PaddingValues(0.dp),
                scrollPadding = PaddingValues(0.dp),
                first = first,
                second = second,
                composer = {
                    BillDetailComposerBox(
                        state = state,
                        showsAvatar = false,
                        fieldColor = fieldColor,
                        onEvent = onEvent,
                    )
                },
                modifier = Modifier.weight(
                    weight = 1f,
                    fill = false,
                ),
            )
            if (!usesTwoColumns && state.isLoaded) {
                BillDetailComposerBox(
                    state = state,
                    showsAvatar = true,
                    fieldColor = fieldColor,
                    onEvent = onEvent,
                )
            }
        }
    } else {
        val fieldColor = MaterialTheme.colorScheme.surfaceContainerHighest
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.groupName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(BillDetailUiEvent.OnBackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.bill_detail_back),
                            )
                        }
                    },
                    actions = {
                        BillTopBarAction(
                            state = state,
                            onEvent = onEvent,
                        )
                    },
                )
            },
            bottomBar = {
                if (!usesTwoColumns && state.isLoaded) {
                    BillComposerBar { composerModifier ->
                        BillDetailComposerBox(
                            state = state,
                            showsAvatar = true,
                            fieldColor = fieldColor,
                            onEvent = onEvent,
                            modifier = composerModifier,
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            BillFullScreenColumn(
                padding = padding,
                usesTwoColumns = usesTwoColumns,
                isLoaded = state.isLoaded,
                banner = if (state.isOnline) null else { bannerModifier -> BillOfflineBanner(bannerModifier) },
                first = first,
                second = second,
                composer = {
                    BillDetailComposerBox(
                        state = state,
                        showsAvatar = false,
                        fieldColor = fieldColor,
                        onEvent = onEvent,
                    )
                },
            )
        }
    }
}

// Why: Conta's composer, which also waits in the queue offline, with this device's user's face where it is pinned.
@Composable
private fun BillDetailComposerBox(
    state: BillDetailUiState,
    showsAvatar: Boolean,
    fieldColor: Color,
    onEvent: (BillDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BillCommentComposerBox(
        composer = BillComposer(
            draft = state.commentDraft,
            replyTarget = state.replyTarget,
            editingCommentId = state.editingCommentId,
            canSend = state.canSendComment,
            onDraftChange = { onEvent(BillDetailUiEvent.OnCommentChanged(it)) },
            onSendClick = { onEvent(BillDetailUiEvent.OnSendCommentClicked) },
            onReplyCancel = { onEvent(BillDetailUiEvent.OnReplyCancelled) },
            onEditCancel = { onEvent(BillDetailUiEvent.OnCommentEditCancelled) },
        ),
        me = state.people.firstOrNull { it.isMe }.takeIf { showsAvatar },
        fieldColor = fieldColor,
        modifier = modifier,
    )
}

@Composable
private fun BillTopBarAction(
    state: BillDetailUiState,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    // Why: no disabled guesses while Room is read; the bill's actions show once it is.
    if (!state.isLoaded) return
    IconButton(
        onClick = { onEvent(BillDetailUiEvent.OnShareClicked) },
        enabled = state.areActionsEnabled,
    ) {
        Icon(
            imageVector = Icons.Outlined.Share,
            contentDescription = stringResource(R.string.bill_detail_share),
        )
    }
    IconButton(
        onClick = { onEvent(BillDetailUiEvent.OnEditClicked) },
        enabled = state.canEdit,
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = stringResource(R.string.bill_detail_edit),
        )
    }
    IconButton(
        onClick = { onEvent(BillDetailUiEvent.OnDeleteClicked) },
        enabled = state.areActionsEnabled,
    ) {
        Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = stringResource(R.string.bill_detail_delete),
        )
    }
}

@Composable
private fun DialogHeader(
    state: BillDetailUiState,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.groupName,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BillTopBarAction(
            state = state,
            onEvent = onEvent,
        )
        IconButton(onClick = { onEvent(BillDetailUiEvent.OnBackClicked) }) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.bill_detail_back),
            )
        }
    }
}

// Why: Conta's first column: the bill, "Sua parte" and, as the canvas puts them under the bill, "Anexos".
@Composable
private fun BillDetailBillSection(
    state: BillDetailUiState,
    wrapsAttachments: Boolean,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    pickers: PhotoPickers,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    BillSummaryColumn(
        category = state.category,
        categoryParent = state.categoryParent,
        description = state.description,
        spentAtText = spentAtTextOf(
            spentAt = state.spentAt,
            spentDay = state.spentDay,
            dates = dates,
        ),
        totalText = money.toText(state.totalMinor),
        payers = state.payers,
        onLonePayerClick = state.lonePayer?.let { payer ->
            { onEvent(BillDetailUiEvent.OnPersonClicked(payer.memberId)) }
        },
    ) {
        if (state.isPending) PendingLine(stringResource(R.string.bill_detail_pending))
        if (state.showsUnknownCurrencyNote) {
            Text(
                text = stringResource(R.string.bill_detail_unknown_currency),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (state.showsMyPart) {
        MyPartCard(
            state = state,
            money = money,
        )
    }
    BillAttachmentsSection(
        attachments = state.attachments.valueOrNull(),
        // Why: "2 de 5 fotos", whose noun follows the limit.
        countText = state.photoCount?.let { count ->
            pluralStringResource(
                R.plurals.bill_detail_attachments_count,
                state.photoLimit,
                count,
                state.photoLimit,
            )
        },
        wraps = wrapsAttachments,
        dates = dates,
        addPhoto = BillAddPhoto(
            isEnabled = state.canAddPhoto,
            isAdding = state.isAddingPhoto,
            pickers = pickers,
        ).takeIf { state.showsAddPhoto },
        onAttachmentClick = { onEvent(BillDetailUiEvent.OnAttachmentClicked(it)) },
    )
}

// Why: Conta's second column: who is in the split, the comments and the history.
@Composable
private fun BillDetailPeopleSection(
    state: BillDetailUiState,
    money: MoneyFormatter,
    dates: DateTimeTextFormatter,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    // Why: Conta's likes, edits and deletions wait in the queue offline, so they are never off.
    val comments = remember(onEvent) {
        BillCommentCallbacks(
            canChange = true,
            onPersonClick = { onEvent(BillDetailUiEvent.OnPersonClicked(it)) },
            onReplyClick = { onEvent(BillDetailUiEvent.OnReplyClicked(it)) },
            onLikeClick = { onEvent(BillDetailUiEvent.OnCommentLikeClicked(it)) },
            onLikesClick = { onEvent(BillDetailUiEvent.OnCommentLikesClicked(it)) },
            onShareClick = { onEvent(BillDetailUiEvent.OnCommentShareClicked(it)) },
            onEditClick = { onEvent(BillDetailUiEvent.OnCommentEditClicked(it)) },
            onDeleteClick = { onEvent(BillDetailUiEvent.OnCommentDeleteClicked(it)) },
            onFocusedCommentShown = { onEvent(BillDetailUiEvent.OnFocusedCommentShown) },
        )
    }
    BillSplitSection(
        splitMode = state.splitMode,
        participantCount = state.participantCount,
        people = state.people,
        money = money,
        onPersonClick = comments.onPersonClick,
    )
    BillCommentsSection(
        threads = state.threads.valueOrNull().orEmpty(),
        isLoading = state.threads.isLoading(),
        commentCount = state.commentCount,
        showsNoComments = state.showsNoComments,
        replyTargetId = state.replyTarget?.commentId,
        focusedCommentId = state.focusedCommentId,
        dates = dates,
        callbacks = comments,
    )
    BillHistorySection(
        state = state,
        money = money,
        dates = dates,
        onEvent = onEvent,
    )
}

@DevicePreviews
@Composable
private fun BillDetailContentPreview() {
    PreviewBillDetailContent(createPreviewBillDetailState())
}

@DevicePreviews
@Composable
private fun BillDetailContentLoadingPreview() {
    PreviewBillDetailContent(createPreviewBillDetailState().copy(isLoaded = false))
}

@DevicePreviews
@Composable
private fun BillDetailContentPayerPreview() {
    PreviewBillDetailContent(createPreviewBillDetailState(isMePayer = true))
}

@DevicePreviews
@Composable
private fun BillDetailContentPendingPreview() {
    PreviewBillDetailContent(createPreviewBillDetailState(isPending = true))
}

@DevicePreviews
@Composable
private fun BillDetailContentReplyOfflinePreview() {
    PreviewBillDetailContent(createPreviewBillDetailState(isReplyingOffline = true))
}

@DevicePreviews
@Composable
private fun BillDetailContentEditingCommentPreview() {
    PreviewBillDetailContent(createPreviewBillDetailState(isEditingComment = true))
}

// Why: a preview draws no dialog window, so the book's right half, where Conta opens, is drawn by hand around it.
@Preview(
    name = "Book",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun BillDetailContentBookPreview() {
    BlitzSplitTheme {
        Surface(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(modifier = Modifier.width(376.dp)) {
                    BillDetailContent(
                        state = createPreviewBillDetailState(),
                        isDialog = true,
                        snackbarHostState = SnackbarHostState(),
                        pickers = previewPickers,
                        onEvent = {},
                    )
                }
            }
        }
    }
}

// Why: as AppNavDisplay opens it: a dialog anywhere wider than a phone, full screen on one.
@Composable
private fun PreviewBillDetailContent(state: BillDetailUiState) {
    BlitzSplitTheme {
        val formFactor = rememberFoldLayout().formFactor
        BillDetailContent(
            state = state,
            isDialog = formFactor != FormFactor.Phone && formFactor != FormFactor.PhoneLandscape,
            snackbarHostState = SnackbarHostState(),
            pickers = previewPickers,
            onEvent = {},
        )
    }
}
