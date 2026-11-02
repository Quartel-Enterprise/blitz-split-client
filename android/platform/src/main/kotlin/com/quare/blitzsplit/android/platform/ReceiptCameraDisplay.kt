package com.quare.blitzsplit.android.platform

import android.content.Context
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.InitializationException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.awaitCancellation
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
 * Why: the back camera's picture (the front one where there is none), for the receipt's camera. Needs the camera
 * permission. A camera that opens is reported with whether it has a torch; one missing, or that CameraX can't start,
 * bind or open, is reported unavailable, and the gallery remains.
 */
@Composable
fun ReceiptCameraDisplay(
    camera: ReceiptCamera,
    isTorchOn: Boolean,
    onOpened: (hasTorch: Boolean) -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentIsTorchOn by rememberUpdatedState(isTorchOn)
    val currentOnOpened by rememberUpdatedState(onOpened)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var bound by remember { mutableStateOf<Camera?>(null) }
    LaunchedEffect(context, lifecycleOwner) {
        val provider = try {
            ProcessCameraProvider.awaitInstance(context)
        } catch (_: InitializationException) {
            currentOnUnavailable()
            return@LaunchedEffect
        }
        val selector = listOf(CameraSelector.DEFAULT_BACK_CAMERA, CameraSelector.DEFAULT_FRONT_CAMERA)
            .firstOrNull(provider::hasCameraOrFalse)
        if (selector == null) {
            currentOnUnavailable()
            return@LaunchedEffect
        }
        val previewUseCase = Preview.Builder().build().apply {
            setSurfaceProvider { surfaceRequest = it }
        }
        // Why: a receipt's small print reads better sharp than fast.
        val captureUseCase = ImageCapture
            .Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
        try {
            val result = try {
                provider.bindToLifecycle(lifecycleOwner, selector, previewUseCase, captureUseCase)
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: IllegalStateException) {
                null
            }
            if (result == null) currentOnUnavailable()
            bound = result
            camera.capture = captureUseCase.takeIf { result != null }
            awaitCancellation()
        } finally {
            provider.unbind(previewUseCase, captureUseCase)
            bound = null
            camera.capture = null
            surfaceRequest = null
        }
    }
    // Why: CameraX turns the torch off whenever the camera closes, so it is set again each time it opens.
    DisposableEffect(bound, lifecycleOwner) {
        val opened = bound ?: return@DisposableEffect onDispose {}
        val cameraState = opened.cameraInfo.cameraState
        val observer = Observer<CameraState> { state ->
            when {
                state.type == CameraState.Type.OPEN -> {
                    val hasTorch = opened.cameraInfo.hasFlashUnit()
                    currentOnOpened(hasTorch)
                    if (hasTorch) opened.cameraControl.enableTorch(currentIsTorchOn)
                }

                state.error != null && state.type == CameraState.Type.CLOSED -> currentOnUnavailable()
            }
        }
        cameraState.observe(lifecycleOwner, observer)
        onDispose { cameraState.removeObserver(observer) }
    }
    LaunchedEffect(bound, isTorchOn) {
        bound?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(isTorchOn)
    }
    surfaceRequest?.let { request ->
        CameraXViewfinder(
            surfaceRequest = request,
            implementationMode = ImplementationMode.EMBEDDED,
            modifier = modifier,
        )
    }
}
