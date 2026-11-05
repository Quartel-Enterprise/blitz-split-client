package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.quare.blitzsplit.android.platform.ReceiptCameraDisplay
import com.quare.blitzsplit.android.platform.rememberCameraPermissionAccess
import com.quare.blitzsplit.android.platform.rememberPhotoPickers
import com.quare.blitzsplit.android.platform.rememberReceiptCamera
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent

private val shutterSize = 76.dp
private val sideButtonSize = 48.dp
private val controlsSize = 136.dp
private const val FRAME_WIDTH_FRACTION = 0.66f
private const val FRAME_HEIGHT_FRACTION = 0.9f

// Why: what the camera's area shows: the picture, or why there is none.
private enum class CameraView { PICTURE, ASKING, DENIED, DENIED_FOR_GOOD, UNAVAILABLE }

/*
 * Why: "Ler cupom fiscal" (7.1e), the board's own camera over the bill, black in both themes: the frame to fit the
 * receipt in, the flash, the gallery and the shutter. The photo taken or picked goes to the shared code as it came; a
 * gallery file that can't be read goes too, and reads as a receipt that couldn't be read.
 */
@Composable
internal fun ReceiptCameraDialog(
    onPhoto: (ByteArray) -> Unit,
    onDismiss: () -> Unit,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        ReceiptCameraContent(
            onPhoto = onPhoto,
            onDismiss = onDismiss,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun ReceiptCameraContent(
    onPhoto: (ByteArray) -> Unit,
    onDismiss: () -> Unit,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val camera = rememberReceiptCamera()
    var view by rememberSaveable { mutableStateOf(CameraView.ASKING) }
    var hasAsked by rememberSaveable { mutableStateOf(false) }
    var hasTorch by remember { mutableStateOf(false) }
    var isTorchOn by rememberSaveable { mutableStateOf(false) }
    var isTaking by remember { mutableStateOf(false) }
    val pickers = rememberPhotoPickers(
        snackbarHostState = snackbarHostState,
        onPhotoPicked = onPhoto,
        onPhotoUnreadable = { onPhoto(ByteArray(0)) },
    )
    val permission = rememberCameraPermissionAccess { isGranted ->
        view = when {
            isGranted -> CameraView.PICTURE
            isDeniedForGood() -> CameraView.DENIED_FOR_GOOD
            else -> CameraView.DENIED
        }
    }
    // Why: asked once as the camera opens, and read again on coming back from the settings.
    LifecycleResumeEffect(permission) {
        when {
            permission.isGranted() -> if (view != CameraView.UNAVAILABLE) view = CameraView.PICTURE

            !hasAsked -> {
                hasAsked = true
                permission.request()
            }
        }
        onPauseOrDispose {}
    }
    val toggleTorch = {
        isTorchOn = !isTorchOn
        onEvent(SplitBillUiEvent.OnReceiptTorchClicked(isTorchOn))
    }
    val takePhoto = {
        if (!isTaking) {
            isTaking = true
            camera.takePhoto(context) { photo ->
                isTaking = false
                // Why: a failed capture reads as a photo that couldn't be read, which offers "Tirar outra foto".
                onPhoto(photo ?: ByteArray(0))
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val isLandscape = maxWidth > maxHeight
            val viewfinder = @Composable { modifier: Modifier ->
                ViewfinderBox(
                    view = view,
                    modifier = modifier,
                    onAllow = {
                        onEvent(SplitBillUiEvent.OnReceiptCameraAllowClicked)
                        permission.request()
                    },
                    onOpenSettings = {
                        onEvent(SplitBillUiEvent.OnReceiptCameraSettingsClicked)
                        permission.openSettings()
                    },
                ) { pictureModifier ->
                    ReceiptCameraDisplay(
                        camera = camera,
                        isTorchOn = isTorchOn,
                        onOpened = { hasTorch = it },
                        onUnavailable = { view = CameraView.UNAVAILABLE },
                        modifier = pictureModifier,
                    )
                }
            }
            val controls = @Composable { modifier: Modifier ->
                CameraControlsBar(
                    isVertical = isLandscape,
                    canTakePhoto = view == CameraView.PICTURE && camera.canTakePhoto && !isTaking,
                    onGallery = pickers.onChoosePhotoClick,
                    onShutter = takePhoto,
                    modifier = modifier,
                )
            }
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f)) {
                        CameraHeader(hasTorch, isTorchOn, toggleTorch, onDismiss)
                        viewfinder(Modifier.weight(1f).fillMaxWidth())
                        HintText(Modifier.padding(vertical = 12.dp))
                    }
                    controls(Modifier.width(controlsSize).fillMaxHeight())
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    CameraHeader(hasTorch, isTorchOn, toggleTorch, onDismiss)
                    viewfinder(Modifier.weight(1f).fillMaxWidth())
                    HintText(Modifier.padding(top = 16.dp))
                    controls(Modifier.fillMaxWidth().height(controlsSize))
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        )
    }
}

@Composable
private fun CameraHeader(
    hasTorch: Boolean,
    isTorchOn: Boolean,
    onTorch: () -> Unit,
    onDismiss: () -> Unit,
) {
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
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.split_bill_receipt_camera_close),
                tint = Color.White,
            )
        }
        Text(
            text = stringResource(R.string.split_bill_receipt_camera_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        if (hasTorch) {
            IconButton(onClick = onTorch) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Outlined.FlashOff else Icons.Outlined.FlashOn,
                    contentDescription = stringResource(
                        if (isTorchOn) {
                            R.string.split_bill_receipt_camera_flash_off
                        } else {
                            R.string.split_bill_receipt_camera_flash_on
                        },
                    ),
                    tint = Color.White,
                )
            }
        }
    }
}

// Why: the picture with the receipt's frame over it, or why there is no picture, with the gallery still below.
@Composable
private fun ViewfinderBox(
    view: CameraView,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    picture: @Composable (Modifier) -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (view == CameraView.PICTURE) {
            picture(Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxWidth(FRAME_WIDTH_FRACTION)
                    .fillMaxHeight(FRAME_HEIGHT_FRACTION)
                    .border(3.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(24.dp)),
            )
        } else {
            CameraMessage(
                view = view,
                onAllow = onAllow,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun CameraMessage(
    view: CameraView,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    if (view == CameraView.ASKING) return
    Column(
        modifier = Modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(
                if (view == CameraView.UNAVAILABLE) {
                    R.string.split_bill_receipt_camera_unavailable
                } else {
                    R.string.split_bill_receipt_camera_permission
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        when (view) {
            CameraView.DENIED -> FilledTonalButton(onClick = onAllow) {
                Text(stringResource(R.string.split_bill_receipt_camera_allow))
            }

            CameraView.DENIED_FOR_GOOD -> FilledTonalButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.split_bill_receipt_camera_settings))
            }

            else -> Unit
        }
    }
}

@Composable
private fun HintText(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.split_bill_receipt_camera_hint),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White,
        textAlign = TextAlign.Center,
    )
}

// Why: the gallery and the shutter, side by side under the picture, or one over the other beside it lying down.
@Composable
private fun CameraControlsBar(
    isVertical: Boolean,
    canTakePhoto: Boolean,
    onGallery: () -> Unit,
    onShutter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gallery = @Composable {
        Surface(
            onClick = onGallery,
            modifier = Modifier.size(sideButtonSize),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = stringResource(R.string.split_bill_receipt_camera_gallery),
                )
            }
        }
    }
    val shutterLabel = stringResource(R.string.split_bill_receipt_camera_shutter)
    val shutter = @Composable {
        Surface(
            onClick = onShutter,
            enabled = canTakePhoto,
            modifier = Modifier
                .size(shutterSize)
                .semantics { contentDescription = shutterLabel },
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(4.dp, Color.White),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White.copy(alpha = if (canTakePhoto) 1f else 0.38f), CircleShape),
                )
            }
        }
    }
    if (isVertical) {
        Column(
            modifier = modifier.padding(vertical = 40.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.size(sideButtonSize))
            shutter()
            gallery()
        }
    } else {
        Row(
            modifier = modifier.padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            gallery()
            shutter()
            Spacer(modifier = Modifier.size(sideButtonSize))
        }
    }
}
