package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.android.platform.PhotoPickers
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillAttachmentTile
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState

// Why: the canvas' tiles, 104×80 and 92×70 on a phone lying down, where the screen is short.
private val regularTile = TileSize(104.dp, 80.dp)
private val shortTile = TileSize(92.dp, 70.dp)
private val tileSpacing = 8.dp
private val thumbShape = RoundedCornerShape(12.dp)
private val badgeShape = RoundedCornerShape(6.dp)
private val dashWidth = 1.5.dp
private val dashOn = 6.dp
private val dashOff = 4.dp

private class TileSize(
    val width: Dp,
    val thumbHeight: Dp,
)

/*
 * Why: "Anexos" (7.3): the receipt that filled the bill first, then up to 5 photos, then "Adicionar foto" while there
 * is room; one row scrolling sideways in one column, wrapping in two columns, as the canvas draws them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BillAttachmentsSection(
    state: BillDetailUiState,
    wraps: Boolean,
    dates: DateTimeTextFormatter,
    pickers: PhotoPickers,
    onEvent: (BillDetailUiEvent) -> Unit,
) {
    val size = if (rememberFoldLayout().formFactor == FormFactor.PhoneLandscape) shortTile else regularTile
    val tiles: @Composable () -> Unit = {
        state.attachments.forEach { attachment ->
            AttachmentTile(
                attachment = attachment,
                size = size,
                dates = dates,
                onClick = { onEvent(BillDetailUiEvent.OnAttachmentClicked(attachment.id)) },
            )
        }
        if (state.showsAddPhoto) {
            AddPhotoTile(
                isEnabled = state.canAddPhoto,
                isAdding = state.isAddingPhoto,
                size = size,
                pickers = pickers,
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BillSectionTitle(
            text = stringResource(R.string.bill_detail_attachments),
            // Why: "2 de 5 fotos", whose noun follows the limit.
            trailing = pluralStringResource(
                R.plurals.bill_detail_attachments_count,
                state.photoLimit,
                state.photoCount,
                state.photoLimit,
            ),
        )
        if (wraps) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tileSpacing),
                verticalArrangement = Arrangement.spacedBy(tileSpacing),
            ) { tiles() }
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(tileSpacing),
            ) { tiles() }
        }
    }
}

@Composable
private fun AttachmentTile(
    attachment: BillAttachmentTile,
    size: TileSize,
    dates: DateTimeTextFormatter,
    onClick: () -> Unit,
) {
    TileColumn(
        size = size,
        label = attachmentTitleOf(attachment),
        subtitle = stringResource(
            R.string.bill_detail_attachment_sub,
            personNameOf(attachment.adderName, attachment.isAddedByMe),
            attachmentDayTextOf(attachment, dates),
        ),
        onClickLabel = stringResource(R.string.bill_detail_attachment_open),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .size(size.width, size.thumbHeight)
                .clip(thumbShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            // Why: under the image, so it shows while the image loads, and offline for one never loaded here.
            Icon(
                imageVector = if (attachment.isReceipt) {
                    Icons.AutoMirrored.Outlined.ReceiptLong
                } else {
                    Icons.Outlined.Image
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Why: the photo added here until it is up, else the bucket's signed URL.
            (attachment.localImage ?: attachment.imageUrl)?.let { image ->
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
            when {
                attachment.isPending -> TileBadge(
                    text = stringResource(R.string.bill_detail_attachment_pending_badge),
                    isPending = true,
                )

                attachment.isReceipt -> TileBadge(
                    text = stringResource(R.string.bill_detail_attachment_receipt_badge),
                    isPending = false,
                )
            }
        }
    }
}

// Why: "Na fila" on the inverse surface with the queue's dot, "Cupom" on the primary color, at the thumb's corner.
@Composable
private fun BoxScope.TileBadge(
    text: String,
    isPending: Boolean,
) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(6.dp)
            .height(20.dp)
            .background(
                color = if (isPending) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.primary,
                shape = badgeShape,
            ).padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPending) PendingIndicator(modifier = Modifier.size(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isPending) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
        )
    }
}

// Why: a menu on the tile with the camera and the gallery; with no camera the gallery opens straight away.
@Composable
private fun AddPhotoTile(
    isEnabled: Boolean,
    isAdding: Boolean,
    size: TileSize,
    pickers: PhotoPickers,
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val takePhoto = pickers.onTakePhotoClick
    Box {
        TileColumn(
            size = size,
            label = stringResource(R.string.bill_detail_add_photo),
            subtitle = stringResource(R.string.bill_detail_add_photo_sub),
            onClickLabel = null,
            isEnabled = isEnabled,
            onClick = { if (takePhoto != null) isMenuOpen = true else pickers.onChoosePhotoClick() },
        ) {
            val outline = MaterialTheme.colorScheme.outline
            Box(
                modifier = Modifier
                    .size(size.width, size.thumbHeight)
                    .drawBehind {
                        val stroke = dashWidth.toPx()
                        drawRoundRect(
                            color = outline,
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(this.size.width - stroke, this.size.height - stroke),
                            cornerRadius = CornerRadius(12.dp.toPx()),
                            style = Stroke(
                                width = stroke,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashOn.toPx(), dashOff.toPx())),
                            ),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AddAPhoto,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false },
        ) {
            if (takePhoto != null) {
                PickerMenuItem(Icons.Outlined.PhotoCamera, stringResource(R.string.bill_detail_take_photo)) {
                    isMenuOpen = false
                    takePhoto()
                }
            }
            PickerMenuItem(Icons.Outlined.PhotoLibrary, stringResource(R.string.bill_detail_choose_photo)) {
                isMenuOpen = false
                pickers.onChoosePhotoClick()
            }
        }
    }
}

@Composable
private fun PickerMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
    )
}

// Why: a tile is one button for TalkBack, "Foto 2, Ana · ontem", whose thumb is only decoration.
@Composable
private fun TileColumn(
    size: TileSize,
    label: String,
    subtitle: String,
    onClickLabel: String?,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    thumb: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(size.width)
            .clip(MaterialTheme.shapes.small)
            .clickable(
                enabled = isEnabled,
                onClickLabel = onClickLabel,
                role = Role.Button,
                onClick = onClick,
            ).semantics(mergeDescendants = true) {}
            .then(if (isEnabled) Modifier else Modifier.alpha(0.38f)),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(modifier = Modifier.padding(bottom = 4.dp)) { thumb() }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Why: "Foto 2", or "Cupom fiscal" for the receipt, which isn't one of the photos.
@Composable
internal fun attachmentTitleOf(attachment: BillAttachmentTile): String {
    val number = attachment.photoNumber
    return if (attachment.isReceipt || number == null) {
        stringResource(R.string.bill_detail_attachment_receipt)
    } else {
        stringResource(R.string.bill_detail_attachment_photo, number)
    }
}

// Why: "hoje", "ontem" or "27 de out.", short enough for a tile; the viewer adds the time.
@Composable
private fun attachmentDayTextOf(
    attachment: BillAttachmentTile,
    dates: DateTimeTextFormatter,
): String = when (attachment.addedDay) {
    RelativeDay.TODAY -> stringResource(R.string.bill_detail_attachment_today)
    RelativeDay.YESTERDAY -> stringResource(R.string.bill_detail_attachment_yesterday)
    RelativeDay.OTHER -> dates.toDateText(attachment.addedAt)
}
