package com.quare.blitzsplit.android.platform

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

/*
 * Why: "Tirar foto" of the receipt's camera (7.1e), which the screen draws itself, as the boards do, with its frame and
 * the gallery beside the shutter. The photo comes as the camera wrote it, a JPEG with its orientation in EXIF, which
 * the shared code turns upright as it shrinks it.
 */
@Stable
class ReceiptCamera internal constructor() {
    internal var capture: ImageCapture? by mutableStateOf(null)

    // Why: the shutter is only offered once the camera can take a photo.
    val canTakePhoto: Boolean
        get() = capture != null

    // Why: null when the camera failed to take it (closed meanwhile, or a hardware error).
    fun takePhoto(
        context: Context,
        onTaken: (ByteArray?) -> Unit,
    ) {
        val imageCapture = capture ?: return onTaken(null)
        val stream = ByteArrayOutputStream()
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(stream).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onTaken(stream.toByteArray())
                }

                override fun onError(exception: ImageCaptureException) {
                    onTaken(null)
                }
            },
        )
    }
}

@Composable
fun rememberReceiptCamera(): ReceiptCamera = remember { ReceiptCamera() }

/*
 * Why: the receipt's camera: the picture and the shutter's capture, turned with the display so the photo's EXIF says
 * which way is up. A camera missing or that can't open is reported unavailable, and the gallery remains.
 */
@Composable
fun ReceiptCameraDisplay(
    camera: ReceiptCamera,
    isTorchOn: Boolean,
    onOpened: (hasTorch: Boolean) -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoundCameraDisplay(
        isTorchOn = isTorchOn,
        onOpened = onOpened,
        onUnavailable = onUnavailable,
        modifier = modifier,
    ) { _, rotation ->
        // Why: a receipt's small print reads better sharp than fast.
        val capture = ImageCapture
            .Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(rotation)
            .build()
        camera.capture = capture
        CameraUses(
            uses = listOf(capture),
            onRotation = { capture.targetRotation = it },
            onUnbound = { camera.capture = null },
        )
    }
}
