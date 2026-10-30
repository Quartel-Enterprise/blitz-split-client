package com.quare.blitzsplit.android.feature.join

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.feature.join.presentation.model.CameraAccess
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiState

// Why: the camera stays dark in both themes: white marks over black, and the dark theme's error for a wrong QR.
private val cameraBackground = Color.Black
private val onCamera = Color.White
private val cameraError = Color(0xFFFFB599)
private val maxViewfinderSize = 260.dp
private val minViewfinderSize = 140.dp
private val roomForTextUnderViewfinder = 140.dp
private val maxPanelWidth = 560.dp
private val sidePanelWidth = 380.dp

/*
 * Why: the camera over the "Tem o link?" panel. Lying down, or on a wide window, the two sit side by side, so the field
 * and "Abrir convite" never hide behind the camera.
 */
@Composable
fun JoinWithLinkContent(
    state: JoinWithLinkUiState,
    field: String,
    onFieldChange: (String) -> Unit,
    hasTorch: Boolean,
    onPasteClicked: () -> Unit,
    onEvent: (JoinWithLinkUiEvent) -> Unit,
    camera: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(cameraBackground),
    ) {
        if (maxWidth > maxHeight) {
            Row(modifier = Modifier.fillMaxSize()) {
                CameraPane(
                    state = state,
                    hasTorch = hasTorch,
                    onEvent = onEvent,
                    camera = camera,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                PastePanel(
                    state = state,
                    field = field,
                    onFieldChange = onFieldChange,
                    onPasteClicked = onPasteClicked,
                    onEvent = onEvent,
                    shape = RoundedCornerShape(
                        topStart = 28.dp,
                        bottomStart = 28.dp,
                    ),
                    insets = WindowInsetsSides.End + WindowInsetsSides.Vertical,
                    modifier = Modifier
                        .width(sidePanelWidth)
                        .fillMaxHeight(),
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                CameraPane(
                    state = state,
                    hasTorch = hasTorch,
                    onEvent = onEvent,
                    camera = camera,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                PastePanel(
                    state = state,
                    field = field,
                    onFieldChange = onFieldChange,
                    onPasteClicked = onPasteClicked,
                    onEvent = onEvent,
                    shape = RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                    ),
                    insets = WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CameraPane(
    state: JoinWithLinkUiState,
    hasTorch: Boolean,
    onEvent: (JoinWithLinkUiEvent) -> Unit,
    camera: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (state.cameraAccess == CameraAccess.GRANTED) camera(Modifier.fillMaxSize())
        // Why: the design's scrim, dark toward the edges, so the white marks and text read over a bright room.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(listOf(Color.Transparent, cameraBackground.copy(alpha = 0.7f))),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Start)),
        ) {
            CameraTopBar(
                state = state,
                hasTorch = hasTorch,
                onEvent = onEvent,
            )
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // Why: a phone lying down has little height, so the viewfinder shrinks to keep the hint under it.
                val viewfinderSize = (maxHeight - roomForTextUnderViewfinder)
                    .coerceIn(minViewfinderSize, maxViewfinderSize)
                Box(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.cameraAccess == CameraAccess.DENIED) {
                        CameraDeniedMessage(onEvent = onEvent)
                    } else {
                        ViewfinderOverlay(
                            state = state,
                            size = viewfinderSize,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraTopBar(
    state: JoinWithLinkUiState,
    hasTorch: Boolean,
    onEvent: (JoinWithLinkUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = { onEvent(JoinWithLinkUiEvent.OnCloseClicked) }) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.join_close),
                tint = onCamera,
            )
        }
        Text(
            text = stringResource(R.string.join_scan_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = onCamera,
        )
        if (hasTorch && state.cameraAccess == CameraAccess.GRANTED) {
            IconButton(
                onClick = { onEvent(JoinWithLinkUiEvent.OnTorchClicked) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.4f),
                    contentColor = onCamera,
                ),
            ) {
                Icon(
                    imageVector = if (state.isTorchOn) Icons.Outlined.FlashlightOff else Icons.Outlined.FlashlightOn,
                    contentDescription = stringResource(
                        if (state.isTorchOn) R.string.join_scan_torch_off else R.string.join_scan_torch_on,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ViewfinderOverlay(
    state: JoinWithLinkUiState,
    size: Dp,
) {
    val viewfinderDescription = stringResource(R.string.join_scan_viewfinder)
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(
            modifier = Modifier
                .size(size)
                .semantics { contentDescription = viewfinderDescription }
                .viewfinderCorners(color = if (state.isQrInvalid) cameraError else onCamera),
        )
        if (state.isQrInvalid) {
            InvalidQrBanner()
        } else {
            Text(
                text = stringResource(R.string.join_scan_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = onCamera,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun InvalidQrBanner() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .padding(
                        start = 12.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 8.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.join_scan_invalid_qr),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            text = stringResource(R.string.join_scan_invalid_qr_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = onCamera,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CameraDeniedMessage(onEvent: (JoinWithLinkUiEvent) -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(onCamera.copy(alpha = 0.12f), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.NoPhotography,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = onCamera,
            )
        }
        Text(
            text = stringResource(R.string.join_scan_denied_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = onCamera,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.join_scan_denied_message),
            style = MaterialTheme.typography.bodyLarge,
            color = onCamera,
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(
            onClick = { onEvent(JoinWithLinkUiEvent.OnOpenSettingsClicked) },
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.join_scan_open_settings))
        }
    }
}

@Composable
private fun PastePanel(
    state: JoinWithLinkUiState,
    field: String,
    onFieldChange: (String) -> Unit,
    onPasteClicked: () -> Unit,
    onEvent: (JoinWithLinkUiEvent) -> Unit,
    shape: RoundedCornerShape,
    insets: WindowInsetsSides,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(insets))
                    .imePadding()
                    .widthIn(max = maxPanelWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 24.dp,
                        bottom = 32.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.join_scan_paste_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.join_scan_paste_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinkField(
                    state = state,
                    field = field,
                    onFieldChange = onFieldChange,
                    onPasteClicked = onPasteClicked,
                    onEvent = onEvent,
                )
                if (state.canOpen) {
                    Button(
                        onClick = { onEvent(JoinWithLinkUiEvent.OnOpenClicked) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.join_scan_open))
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkField(
    state: JoinWithLinkUiState,
    field: String,
    onFieldChange: (String) -> Unit,
    onPasteClicked: () -> Unit,
    onEvent: (JoinWithLinkUiEvent) -> Unit,
) {
    OutlinedTextField(
        value = field,
        onValueChange = onFieldChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        label = { Text(stringResource(R.string.join_scan_link_label)) },
        placeholder = { Text(stringResource(R.string.join_scan_link_placeholder), fontFamily = FontFamily.Monospace) },
        trailingIcon = {
            if (field.isEmpty()) {
                TextButton(onClick = onPasteClicked) {
                    Text(stringResource(R.string.join_scan_paste))
                }
            } else {
                IconButton(onClick = { onEvent(JoinWithLinkUiEvent.OnClearClicked) }) {
                    Icon(
                        imageVector = Icons.Outlined.Cancel,
                        contentDescription = stringResource(R.string.join_scan_clear),
                    )
                }
            }
        },
        supportingText = if (state.isLinkInvalid) {
            { Text(stringResource(R.string.join_scan_invalid_link)) }
        } else {
            null
        },
        isError = state.isLinkInvalid,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go,
        ),
        keyboardActions = KeyboardActions(onGo = { onEvent(JoinWithLinkUiEvent.OnOpenClicked) }),
    )
}

// Why: the four rounded corners of the design's viewfinder, not a whole frame. The square turns to draw each one.
private fun Modifier.viewfinderCorners(color: Color): Modifier = drawWithContent {
    drawContent()
    val stroke = 5.dp.toPx()
    val arm = 52.dp.toPx()
    val radius = 28.dp.toPx()
    val inset = stroke / 2
    val corner = Path().apply {
        moveTo(inset, arm)
        lineTo(inset, radius)
        arcTo(
            rect = Rect(inset, inset, inset + radius * 2, inset + radius * 2),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false,
        )
        lineTo(arm, inset)
    }
    listOf(0f, 90f, 180f, 270f).forEach { degrees ->
        rotate(degrees) {
            drawPath(
                path = corner,
                color = color,
                style = Stroke(
                    width = stroke,
                    cap = StrokeCap.Round,
                ),
            )
        }
    }
}

private fun createPreviewState(
    cameraAccess: CameraAccess,
    link: String,
    isLinkInvalid: Boolean,
    isQrInvalid: Boolean,
): JoinWithLinkUiState = JoinWithLinkUiState(
    link = link,
    isLinkInvalid = isLinkInvalid,
    isQrInvalid = isQrInvalid,
    cameraAccess = cameraAccess,
    isTorchOn = false,
)

@Composable
private fun JoinWithLinkPreviewContent(state: JoinWithLinkUiState) {
    BlitzSplitTheme {
        JoinWithLinkContent(
            state = state,
            field = state.link,
            onFieldChange = {},
            hasTorch = true,
            onPasteClicked = {},
            onEvent = {},
            camera = { Box(it.background(Color(0xFF2D3135))) },
        )
    }
}

@DevicePreviews
@Composable
private fun JoinWithLinkContentPreview() {
    JoinWithLinkPreviewContent(
        createPreviewState(
            CameraAccess.GRANTED,
            link = "",
            isLinkInvalid = false,
            isQrInvalid = false,
        ),
    )
}

@DevicePreviews
@Composable
private fun JoinWithLinkInvalidQrPreview() {
    JoinWithLinkPreviewContent(
        createPreviewState(
            CameraAccess.GRANTED,
            link = "",
            isLinkInvalid = false,
            isQrInvalid = true,
        ),
    )
}

@DevicePreviews
@Composable
private fun JoinWithLinkDeniedPreview() {
    JoinWithLinkPreviewContent(
        createPreviewState(
            CameraAccess.DENIED,
            link = "blitzsplit.com/g/7Hk2",
            isLinkInvalid = true,
            isQrInvalid = false,
        ),
    )
}
