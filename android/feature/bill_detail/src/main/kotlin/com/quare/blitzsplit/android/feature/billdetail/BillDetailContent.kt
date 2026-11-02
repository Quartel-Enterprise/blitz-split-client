package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.DialogCard
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

private val singleColumnMaxWidth = 592.dp
private val sectionSpacing = 16.dp
private val twoColumnsStartPadding = 56.dp
private val twoColumnsEndPadding = 24.dp
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
    if (isDialog) {
        DialogCard(
            isWide = usesTwoColumns,
            spacing = sectionSpacing,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
        ) {
            DialogHeader(
                state = state,
                onEvent = onEvent,
            )
            if (!state.isOnline) BillOfflineBanner()
            BillColumnsLayout(
                state = state,
                usesTwoColumns = usesTwoColumns,
                fillsHeight = false,
                columnsPadding = PaddingValues(0.dp),
                scrollPadding = PaddingValues(0.dp),
                pickers = pickers,
                onEvent = onEvent,
                modifier = Modifier.weight(
                    weight = 1f,
                    fill = false,
                ),
            )
            if (!usesTwoColumns && state.isLoaded) {
                BillCommentComposerBox(
                    state = state,
                    showsAvatar = true,
                    fieldColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    onEvent = onEvent,
                )
            }
        }
    } else {
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
                    ComposerBar(
                        state = state,
                        onEvent = onEvent,
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    // Why: upright the composer bar rises with the keyboard; lying down the column it sits under does.
                    .then(if (usesTwoColumns) Modifier.imePadding() else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!state.isOnline) {
                    BillOfflineBanner(
                        modifier = Modifier
                            .widthIn(max = if (usesTwoColumns) Dp.Unspecified else singleColumnMaxWidth)
                            .padding(
                                start = if (usesTwoColumns) twoColumnsStartPadding else 16.dp,
                                end = if (usesTwoColumns) twoColumnsEndPadding else 16.dp,
                                bottom = 8.dp,
                            ),
                    )
                }
                BillColumnsLayout(
                    state = state,
                    usesTwoColumns = usesTwoColumns,
                    fillsHeight = true,
                    columnsPadding = if (usesTwoColumns) {
                        PaddingValues(
                            start = twoColumnsStartPadding,
                            end = twoColumnsEndPadding,
                        )
                    } else {
                        PaddingValues(horizontal = 16.dp)
                    },
                    scrollPadding = if (usesTwoColumns) {
                        PaddingValues(
                            top = 4.dp,
                            bottom = 16.dp,
                        )
                    } else {
                        PaddingValues(
                            top = 8.dp,
                            bottom = 24.dp,
                        )
                    },
                    pickers = pickers,
                    onEvent = onEvent,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (usesTwoColumns) Modifier else Modifier.widthIn(max = singleColumnMaxWidth)),
                )
            }
        }
    }
}

// Why: the phone's composer, on the sheet color across the bottom, above the navigation bar and then the keyboard.
@Composable
private fun ComposerBar(
    state: BillDetailUiState,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            BillCommentComposerBox(
                state = state,
                showsAvatar = true,
                fieldColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                onEvent = onEvent,
                modifier = Modifier
                    .widthIn(max = singleColumnMaxWidth)
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 10.dp,
                        bottom = 12.dp,
                    ),
            )
        }
    }
}

@Composable
private fun BillTopBarAction(
    state: BillDetailUiState,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    IconButton(
        onClick = { onEvent(BillDetailUiEvent.OnEditClicked) },
        enabled = state.areActionsEnabled,
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

/*
 * Why: the bill, your part and its attachments first; who is in the split, the comments and the history next to them when lying down,
 * with the comment field under them, and all under the bill otherwise.
 */
@Composable
private fun BillColumnsLayout(
    state: BillDetailUiState,
    usesTwoColumns: Boolean,
    // Why: full screen the columns take the screen's height, the composer at its bottom; a dialog wraps them.
    fillsHeight: Boolean,
    columnsPadding: PaddingValues,
    scrollPadding: PaddingValues,
    pickers: PhotoPickers,
    onEvent: (BillDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoaded) return
    val money = rememberMoneyFormatter(state.currency)
    val dates = rememberDateTimeTextFormatter()
    val first: @Composable ColumnScope.() -> Unit = {
        BillSummaryColumn(
            state = state,
            money = money,
            dates = dates,
        )
        if (state.showsMyPart) {
            MyPartCard(
                state = state,
                money = money,
            )
        }
        // Why: the canvas puts "Anexos" under the bill, so at the foot of the first column in two columns.
        BillAttachmentsSection(
            state = state,
            wraps = usesTwoColumns,
            dates = dates,
            pickers = pickers,
            onEvent = onEvent,
        )
    }
    val second: @Composable ColumnScope.() -> Unit = {
        BillSplitSection(
            state = state,
            money = money,
        )
        BillCommentsSection(
            state = state,
            dates = dates,
            onEvent = onEvent,
        )
        BillHistorySection(
            state = state,
            money = money,
            dates = dates,
            onEvent = onEvent,
        )
    }
    if (usesTwoColumns) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .then(if (fillsHeight) Modifier.fillMaxHeight() else Modifier)
                .padding(columnsPadding),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(scrollPadding),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                content = first,
            )
            Column(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .weight(
                            weight = 1f,
                            fill = fillsHeight,
                        ).verticalScroll(rememberScrollState())
                        .padding(scrollPadding),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                    content = second,
                )
                BillCommentComposerBox(
                    state = state,
                    showsAvatar = false,
                    fieldColor = if (fillsHeight) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    },
                    onEvent = onEvent,
                    modifier = Modifier.padding(
                        top = 10.dp,
                        bottom = if (fillsHeight) 12.dp else 0.dp,
                    ),
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(columnsPadding)
                .padding(scrollPadding),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing),
        ) {
            first()
            second()
        }
    }
}

@DevicePreviews
@Composable
private fun BillDetailContentPreview() {
    PreviewBillDetailContent(createPreviewBillDetailState())
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
