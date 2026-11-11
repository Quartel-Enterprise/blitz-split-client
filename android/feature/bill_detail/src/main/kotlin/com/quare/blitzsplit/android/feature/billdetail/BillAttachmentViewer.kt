package com.quare.blitzsplit.android.feature.billdetail

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.quare.blitzsplit.android.designsystem.component.PendingIndicator
import com.quare.blitzsplit.android.designsystem.component.ShimmerBox
import com.quare.blitzsplit.android.designsystem.component.shimmer
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.templateForActorOf
import com.quare.blitzsplit.android.designsystem.format.wordGapUi
import com.quare.blitzsplit.android.designsystem.layout.DialogPlacement
import com.quare.blitzsplit.android.designsystem.layout.FoldDialog
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.android.designsystem.theme.pendingOnBlackColor
import com.quare.blitzsplit.android.platform.GalleryPhotoSource
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillAttachmentTile
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState
import software.quare.loadable.valueOrNull

private const val SECONDARY_TEXT_ALPHA = 0.72f
private val viewerBackground = Color.Black
private val viewerContent = Color.White

/*
 * Why: the canvas' viewer (7.3): the receipt or a photo on black, full screen on every size but a book, where it takes
 * the right half clear of the hinge as the bill's dialog does. Swiping goes through the bill's attachments, each page
 * telling the ViewModel, which also moves it when the one shown is removed. It brings its own snackbar host, as its
 * window covers Conta's.
 */
@Composable
internal fun BillAttachmentViewerDialog(
    state: BillDetailUiState,
    snackbarHostState: SnackbarHostState,
    onDownloadClick: (source: GalleryPhotoSource, fileName: String) -> Unit,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    val index = state.viewedAttachmentIndex ?: return
    val isBook = rememberFoldLayout().formFactor == FormFactor.Book
    FoldDialog(
        onDismissRequest = { onEvent(BillDetailUiEvent.OnViewerDismissed) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
        placement = if (isBook) DialogPlacement.EndHalf else DialogPlacement.Centered,
    ) {
        LightSystemBarsEffect()
        CompositionLocalProvider(LocalContentColor provides viewerContent) {
            ViewerContent(
                state = state,
                index = index,
                snackbarHostState = snackbarHostState,
                onDownloadClick = onDownloadClick,
                onEvent = onEvent,
                isInHalf = isBook,
            )
        }
    }
}

// Why: the dialog's window draws behind the system bars, whose icons would otherwise stay dark on the black.
@Composable
private fun LightSystemBarsEffect() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
}

@Composable
private fun ViewerContent(
    state: BillDetailUiState,
    index: Int,
    snackbarHostState: SnackbarHostState,
    onDownloadClick: (source: GalleryPhotoSource, fileName: String) -> Unit,
    onEvent: (BillDetailUiEvent) -> Unit,
    // Why: the book's half is a card off the screen's edges; full screen keeps clear of the system bars instead.
    isInHalf: Boolean,
) {
    val insets = if (isInHalf) Modifier else Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    val attachments = state.attachments.valueOrNull().orEmpty()
    val pagerState = rememberPagerState(initialPage = index) { attachments.size }
    val currentAttachments by rememberUpdatedState(attachments)
    val currentViewedId by rememberUpdatedState(state.viewedAttachmentId)
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val id = currentAttachments.getOrNull(page)?.id
            if (id != null && id != currentViewedId) currentOnEvent(BillDetailUiEvent.OnViewerPageChanged(id))
        }
    }
    // Why: the ViewModel moves the viewer too, to the next one once the one shown is removed.
    LaunchedEffect(index) {
        if (pagerState.settledPage != index) pagerState.scrollToPage(index)
    }
    // Why: the page under the finger names the bar and the footer as soon as it is dragged in.
    val current = attachments.getOrNull(pagerState.currentPage) ?: attachments[index]
    val dates = rememberDateTimeTextFormatter()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (isInHalf) Modifier.clip(MaterialTheme.shapes.extraLarge) else Modifier)
            .background(viewerBackground),
    ) {
        Column(modifier = insets.fillMaxSize()) {
            ViewerTopBar(
                attachment = current,
                photoCount = state.photoCount ?: attachments.size,
                onDownloadClick = onDownloadClick,
                onEvent = onEvent,
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                key = { page -> attachments[page].id },
            ) { page ->
                ViewerPageBox(
                    attachment = attachments[page],
                    isOnline = state.isOnline,
                )
            }
            ViewerFooter(
                attachment = current,
                hasOthers = attachments.size > 1,
                dates = dates,
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(insets)
                .padding(bottom = 88.dp),
        )
    }
}

@Composable
private fun ViewerTopBar(
    attachment: BillAttachmentTile,
    photoCount: Int,
    onDownloadClick: (source: GalleryPhotoSource, fileName: String) -> Unit,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    val number = attachment.photoNumber
    val title = if (attachment.isReceipt || number == null) {
        stringResource(R.string.bill_detail_attachment_receipt)
    } else {
        stringResource(R.string.bill_detail_viewer_photo_title, number, photoCount)
    }
    val fileName = if (attachment.isReceipt || number == null) {
        stringResource(R.string.bill_detail_receipt_file_name)
    } else {
        stringResource(R.string.bill_detail_photo_file_name, number)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(
                start = 8.dp,
                end = 12.dp,
                top = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onEvent(BillDetailUiEvent.OnViewerDismissed) }) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.bill_detail_viewer_close),
            )
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val source = gallerySourceOf(attachment)
        // Why: a photo never loaded on this device (offline) has nothing to keep yet.
        IconButton(
            onClick = { source?.let { onDownloadClick(it, fileName) } },
            enabled = source != null,
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = stringResource(R.string.bill_detail_viewer_download),
            )
        }
        if (attachment.canRemove) {
            IconButton(onClick = { onEvent(BillDetailUiEvent.OnRemoveAttachmentClicked(attachment.id)) }) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.bill_detail_viewer_remove),
                )
            }
        }
    }
}

@Composable
private fun ViewerPageBox(
    attachment: BillAttachmentTile,
    isOnline: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 24.dp,
                vertical = 8.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Why: the photo added here until it is up, else the bucket's signed URL.
        val image = attachment.localImage ?: attachment.imageUrl
        if (image == null && isOnline) {
            // Why: online, its signed URL is on the way (or tried again each minute).
            ViewerImageSkeleton()
        } else if (image == null) {
            // Why: offline, a photo another device added and this one never loaded.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = viewerContent.copy(alpha = SECONDARY_TEXT_ALPHA),
                )
                Text(
                    text = stringResource(R.string.bill_detail_viewer_no_image),
                    color = viewerContent.copy(alpha = SECONDARY_TEXT_ALPHA),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // Why: the photo shimmers behind until Coil has it, rather than a black page while it downloads.
            var isImageLoading by remember(image) { mutableStateOf(true) }
            if (isImageLoading) ViewerImageSkeleton()
            AsyncImage(
                model = image,
                contentDescription = attachmentTitleOf(attachment),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                onState = { imageState -> isImageLoading = imageState is AsyncImagePainter.State.Loading },
            )
        }
    }
}

@Composable
private fun ViewerImageSkeleton() {
    ShimmerBox(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmer(MaterialTheme.shapes.large),
        )
    }
}

// Why: "Baixar" keeps the bytes added here as they are, and downloads anything else from its signed URL.
private fun gallerySourceOf(attachment: BillAttachmentTile): GalleryPhotoSource? =
    attachment.localImage?.let(GalleryPhotoSource::Local)
        ?: attachment.imageUrl?.let(GalleryPhotoSource::Remote)

// Why: "Ana anexou ontem às 22:40", who can remove it and how to see the next, and the queue while it isn't up.
@Composable
private fun ViewerFooter(
    attachment: BillAttachmentTile,
    hasOthers: Boolean,
    dates: DateTimeTextFormatter,
) {
    val hints = listOfNotNull(
        stringResource(R.string.bill_detail_viewer_can_remove).takeIf { attachment.canRemove },
        stringResource(R.string.bill_detail_viewer_swipe).takeIf { hasOthers },
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 16.dp,
                bottom = 28.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = viewerAddedTextOf(attachment, dates),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (hints.isNotEmpty()) {
            Text(
                text = hints.joinToString(wordGapUi()),
                color = viewerContent.copy(alpha = SECONDARY_TEXT_ALPHA),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        if (attachment.isPending) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PendingIndicator(color = pendingOnBlackColor)
                Text(
                    text = stringResource(R.string.bill_detail_comment_pending),
                    color = viewerContent.copy(alpha = SECONDARY_TEXT_ALPHA),
                    fontSize = 13.sp,
                )
            }
        }
    }
}

// Why: "Ana anexou ontem às 22:40", or "Bruno leu o cupom ontem às 22:08" for the receipt.
@Composable
private fun viewerAddedTextOf(
    attachment: BillAttachmentTile,
    dates: DateTimeTextFormatter,
): String {
    val isMe = attachment.isAddedByMe
    // Why: the user as the subject of the verb, "Tu" where a list says "Toi".
    val name = if (isMe) stringResource(R.string.bill_detail_history_you) else personNameOf(attachment.adderName, false)
    val time = dates.toTimeText(attachment.addedAt)
    val isReceipt = attachment.isReceipt
    return when (attachment.addedDay) {
        RelativeDay.TODAY -> actorTextOf(
            if (isReceipt) R.string.bill_detail_viewer_read_today else R.string.bill_detail_viewer_added_today,
            isMe,
            name,
            time,
        )

        RelativeDay.YESTERDAY -> actorTextOf(
            if (isReceipt) R.string.bill_detail_viewer_read_yesterday else R.string.bill_detail_viewer_added_yesterday,
            isMe,
            name,
            time,
        )

        RelativeDay.OTHER -> actorTextOf(
            if (isReceipt) R.string.bill_detail_viewer_read_other else R.string.bill_detail_viewer_added_other,
            isMe,
            name,
            dates.toDateText(attachment.addedAt),
            time,
        )
    }
}

// Why: "Tú la adjuntaste hoy a las 21:10": with the user as the actor, the verb after their name is said to them.
@Composable
private fun actorTextOf(
    @StringRes template: Int,
    isMe: Boolean,
    vararg args: String,
): String = templateForActorOf(stringResource(template), isMe).format(*args)
