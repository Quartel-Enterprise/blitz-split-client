package com.quare.blitzsplit.android.feature.join

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.feature.join.presentation.model.CameraAccess
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiState

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
            if (state.showsPaste) {
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
    isCameraUnavailable = false,
    hasTorch = true,
    isTorchOn = false,
)

@Composable
private fun JoinWithLinkPreviewContent(state: JoinWithLinkUiState) {
    BlitzSplitTheme {
        JoinWithLinkContent(
            state = state,
            field = state.link,
            onFieldChange = {},
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
