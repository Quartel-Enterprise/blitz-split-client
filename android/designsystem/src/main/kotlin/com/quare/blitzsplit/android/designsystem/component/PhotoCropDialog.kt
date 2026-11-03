package com.quare.blitzsplit.android.designsystem.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.model.PhotoCropEditor
import com.quare.blitzsplit.core.model.PhotoCropEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Why: GroupBadge's 16 dp corners on its 48 dp side, so the frame is the badge the photo becomes, larger.
private const val ROUNDED_SQUARE_CORNER_RATIO = 1f / 3f
private const val FRAME_RATIO = 0.9f
private const val VERTICAL_MIRROR_ICON_ROTATION = 90f
private val editorBackground = Color(0xFF0B0B0F)
private val scrim = editorBackground.copy(alpha = 0.72f)
private val frameBorder = Color.White.copy(alpha = 0.9f)
private val secondaryContent = Color.White.copy(alpha = 0.7f)
private val chipBackground = Color.White.copy(alpha = 0.12f)
private val maxFrameSide = 360.dp
private val sidePanelWidth = 360.dp

// Why: a profile's photo shows in a circle, a group's in its rounded square.
enum class PhotoCropFrame {
    CIRCLE,
    ROUNDED_SQUARE,
}

/*
 * Why: "Ajustar foto" (Bible Planner's editor): a picked photo framed before it becomes the profile's or the group's,
 * dark and full screen at every size, as the system's photo editors. Lying down the controls go beside the photo, so
 * it keeps its room. Where the photo sits is the ViewModel's (PhotoCrop); only the gestures are read here.
 */
@Composable
fun PhotoCropDialog(
    editor: PhotoCropEditor,
    frame: PhotoCropFrame,
    onEvent: (PhotoCropEvent) -> Unit,
) {
    Dialog(
        onDismissRequest = { onEvent(PhotoCropEvent.OnDismissed) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(editorBackground)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            if (maxWidth > maxHeight) {
                Row(modifier = Modifier.fillMaxSize()) {
                    PhotoCropPane(
                        editor = editor,
                        frame = frame,
                        onEvent = onEvent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    Column(
                        modifier = Modifier
                            .width(sidePanelWidth)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        PhotoCropHeader(onEvent = onEvent)
                        PhotoCropPanel(
                            crop = editor.crop,
                            onEvent = onEvent,
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    PhotoCropHeader(onEvent = onEvent)
                    PhotoCropPane(
                        editor = editor,
                        frame = frame,
                        onEvent = onEvent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    PhotoCropPanel(
                        crop = editor.crop,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoCropHeader(onEvent: (PhotoCropEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onEvent(PhotoCropEvent.OnDismissed) }) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.designsystem_photo_crop_close),
                tint = Color.White,
            )
        }
        Text(
            text = stringResource(R.string.designsystem_photo_crop_title),
            modifier = Modifier.weight(1f),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Box(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun PhotoCropPane(
    editor: PhotoCropEditor,
    frame: PhotoCropFrame,
    onEvent: (PhotoCropEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberCropBitmap(editor.photo)
    val currentOnEvent by rememberUpdatedState(onEvent)
    BoxWithConstraints(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val sidePx = minOf(
            constraints.maxWidth * FRAME_RATIO,
            constraints.maxHeight * FRAME_RATIO,
            with(density) { maxFrameSide.toPx() },
        )
        val currentSidePx by rememberUpdatedState(sidePx)
        val crop = editor.crop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        currentOnEvent(PhotoCropEvent.OnDragged(pan.x / currentSidePx, pan.y / currentSidePx))
                        if (zoom != 1f) currentOnEvent(PhotoCropEvent.OnPinched(zoom))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .requiredSize(
                            width = with(density) { (crop.photoWidthInFrame * sidePx).toDp() },
                            height = with(density) { (crop.photoHeightInFrame * sidePx).toDp() },
                        ).graphicsLayer {
                            scaleX = crop.zoom * if (crop.isMirrored) -1f else 1f
                            scaleY = crop.zoom
                            rotationZ = crop.rotationDegrees.toFloat()
                            translationX = crop.offsetX * sidePx
                            translationY = crop.offsetY * sidePx
                        },
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
            ) {
                drawRect(color = scrim)
                drawFrame(frame, sidePx, Color.Black, Fill, BlendMode.Clear)
                drawFrame(frame, sidePx, frameBorder, Stroke(width = 2.dp.toPx()), BlendMode.SrcOver)
            }
        }
    }
}

private fun DrawScope.drawFrame(
    frame: PhotoCropFrame,
    side: Float,
    color: Color,
    style: DrawStyle,
    blendMode: BlendMode,
) {
    when (frame) {
        PhotoCropFrame.CIRCLE -> drawCircle(
            color = color,
            radius = side / 2,
            style = style,
            blendMode = blendMode,
        )

        PhotoCropFrame.ROUNDED_SQUARE -> drawRoundRect(
            color = color,
            topLeft = Offset((size.width - side) / 2, (size.height - side) / 2),
            size = Size(side, side),
            cornerRadius = CornerRadius(side * ROUNDED_SQUARE_CORNER_RATIO),
            style = style,
            blendMode = blendMode,
        )
    }
}

// Why: the editor's photo is at most 1600 px, decoded once off the main thread.
@Composable
private fun rememberCropBitmap(photo: CropPhoto): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photo) {
        value = withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(photo.jpeg, 0, photo.jpeg.size)?.asImageBitmap()
        }
    }
    return bitmap
}

@Composable
private fun ColumnScope.PhotoCropPanel(
    crop: PhotoCrop,
    onEvent: (PhotoCropEvent) -> Unit,
) {
    Row(
        modifier = Modifier.padding(
            start = 24.dp,
            end = 24.dp,
            top = 16.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = secondaryContent,
        )
        Slider(
            value = crop.zoom,
            onValueChange = { onEvent(PhotoCropEvent.OnZoomChanged(it)) },
            modifier = Modifier.weight(1f),
            valueRange = crop.minZoom..crop.maxZoom,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = chipBackground,
            ),
        )
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            tint = secondaryContent,
        )
    }
    Text(
        text = stringResource(R.string.designsystem_photo_crop_hint),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        color = secondaryContent,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
    Row(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(
                horizontal = 24.dp,
                vertical = 12.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OrientationChip(
            icon = Icons.Outlined.Flip,
            iconRotation = 0f,
            label = stringResource(R.string.designsystem_photo_crop_mirror_horizontally),
            onClick = { onEvent(PhotoCropEvent.OnMirrorHorizontallyClicked) },
        )
        OrientationChip(
            icon = Icons.Outlined.Flip,
            iconRotation = VERTICAL_MIRROR_ICON_ROTATION,
            label = stringResource(R.string.designsystem_photo_crop_mirror_vertically),
            onClick = { onEvent(PhotoCropEvent.OnMirrorVerticallyClicked) },
        )
        OrientationChip(
            icon = Icons.Outlined.Rotate90DegreesCw,
            iconRotation = 0f,
            label = stringResource(R.string.designsystem_photo_crop_turn),
            onClick = { onEvent(PhotoCropEvent.OnTurnClicked) },
        )
    }
    Row(
        modifier = Modifier.padding(
            start = 20.dp,
            end = 20.dp,
            top = 4.dp,
            bottom = 20.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = { onEvent(PhotoCropEvent.OnDismissed) },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        ) {
            Text(text = stringResource(R.string.designsystem_photo_crop_cancel))
        }
        Button(
            onClick = { onEvent(PhotoCropEvent.OnConfirmClicked) },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
        ) {
            Text(text = stringResource(R.string.designsystem_photo_crop_confirm))
        }
    }
}

@Composable
private fun OrientationChip(
    icon: ImageVector,
    iconRotation: Float,
    label: String,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(AssistChipDefaults.IconSize)
                    .graphicsLayer(rotationZ = iconRotation),
            )
        },
        shape = RoundedCornerShape(50),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = chipBackground,
            labelColor = Color.White,
            leadingIconContentColor = Color.White,
        ),
        border = null,
    )
}
