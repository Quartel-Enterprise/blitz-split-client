package com.quare.blitzsplit.android.platform

import android.hardware.display.DisplayManager
import android.os.SystemClock
import android.view.Surface
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.InitializationException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.awaitCancellation

// Why: the camera reads the same QR on every frame, so the same one is reported a few times a second, not 30.
private const val REPEAT_READ_INTERVAL_MILLIS = 250L
private val unavailableErrors = setOf(
    // Why: the camera can't be used while one of these lasts; a passing hiccup (ERROR_OTHER_RECOVERABLE_ERROR) isn't.
    CameraState.ERROR_CAMERA_IN_USE,
    CameraState.ERROR_MAX_CAMERAS_IN_USE,
    CameraState.ERROR_CAMERA_DISABLED,
    CameraState.ERROR_CAMERA_FATAL_ERROR,
    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED,
    CameraState.ERROR_STREAM_CONFIG,
)

/*
 * Why: the invite reader draws its own camera, so a wrong QR shows its warning while the camera keeps reading, and the
 * link can be pasted under it. Every QR read is reported, the same one again a few times a second. Needs the camera
 * permission; a device without a back camera uses the front one. A camera that opens is reported with whether it has a
 * torch; one missing, or that CameraX can't start, bind or open (another app holds it), is reported unavailable, until
 * it opens again.
 */
@Composable
fun QrCameraDisplay(
    isTorchOn: Boolean,
    onScanned: (String) -> Unit,
    onOpened: (hasTorch: Boolean) -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val rotation = rememberDisplayRotation()
    val currentRotation by rememberUpdatedState(rotation)
    val currentIsTorchOn by rememberUpdatedState(isTorchOn)
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnOpened by rememberUpdatedState(onOpened)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    var analysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    LaunchedEffect(context, lifecycleOwner) {
        // Why: CameraX fails to start on a device whose camera service doesn't answer; the paste remains.
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
        val executor = ContextCompat.getMainExecutor(context)
        var lastContent: String? = null
        var lastReadAt = 0L
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
        val previewUseCase = Preview.Builder().setTargetRotation(currentRotation).build().apply {
            setSurfaceProvider { surfaceRequest = it }
        }
        val analysisUseCase = ImageAnalysis
            .Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(currentRotation)
            .build()
            .apply {
                setAnalyzer(
                    executor,
                    MlKitAnalyzer(listOf(scanner), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, executor) { result ->
                        val content = result.getValue(scanner)?.firstNotNullOfOrNull { it.rawValue }
                        val now = SystemClock.elapsedRealtime()
                        val isRepeat = content == lastContent && now - lastReadAt < REPEAT_READ_INTERVAL_MILLIS
                        if (content != null && !isRepeat) {
                            lastContent = content
                            lastReadAt = now
                            currentOnScanned(content)
                        }
                    },
                )
            }
        try {
            // Why: CameraX refuses to bind a camera that can't run both uses; the paste remains.
            val bound = try {
                provider.bindToLifecycle(lifecycleOwner, selector, previewUseCase, analysisUseCase)
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: IllegalStateException) {
                null
            }
            if (bound == null) currentOnUnavailable()
            camera = bound
            preview = previewUseCase
            analysis = analysisUseCase
            awaitCancellation()
        } finally {
            provider.unbind(previewUseCase, analysisUseCase)
            scanner.close()
            camera = null
            preview = null
            analysis = null
            surfaceRequest = null
        }
    }
    // Why: turning the phone only turns the picture and the QR's frames; rebinding would close and reopen the camera.
    LaunchedEffect(preview, analysis, rotation) {
        preview?.targetRotation = rotation
        analysis?.targetRotation = rotation
    }
    /*
     * Why: a camera held by another app binds fine and only fails to open, so its state says when it is unavailable.
     * CameraX also turns the torch off whenever the camera closes (the app in the background), so it is set again each
     * time the camera opens.
     */
    DisposableEffect(camera, lifecycleOwner) {
        val bound = camera ?: return@DisposableEffect onDispose {}
        val cameraState = bound.cameraInfo.cameraState
        val observer = Observer<CameraState> { state ->
            when {
                state.type == CameraState.Type.OPEN -> {
                    val hasTorch = bound.cameraInfo.hasFlashUnit()
                    currentOnOpened(hasTorch)
                    if (hasTorch) bound.cameraControl.enableTorch(currentIsTorchOn)
                }

                state.error?.code in unavailableErrors -> currentOnUnavailable()
            }
        }
        cameraState.observe(lifecycleOwner, observer)
        onDispose { cameraState.removeObserver(observer) }
    }
    LaunchedEffect(camera, isTorchOn) {
        camera?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(isTorchOn)
    }
    surfaceRequest?.let { request ->
        CameraXViewfinder(
            surfaceRequest = request,
            implementationMode = ImplementationMode.EMBEDDED,
            modifier = modifier,
        )
    }
}

// Why: a camera whose details can't be read counts as missing, so the reader tries the next one or offers the paste.
private fun ProcessCameraProvider.hasCameraOrFalse(selector: CameraSelector): Boolean = try {
    hasCamera(selector)
} catch (_: CameraInfoUnavailableException) {
    false
}

// Why: a phone turned from one side to the other changes no configuration, so the rotation is followed from the display.
@Composable
private fun rememberDisplayRotation(): Int {
    val view = LocalView.current
    var rotation by remember { mutableIntStateOf(view.display?.rotation ?: Surface.ROTATION_0) }
    DisposableEffect(view) {
        val displayManager = view.context.getSystemService(DisplayManager::class.java)
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayChanged(displayId: Int) {
                view.display?.takeIf { it.displayId == displayId }?.let { rotation = it.rotation }
            }

            override fun onDisplayAdded(displayId: Int) {
                // Why: only the reader's own display turning matters.
            }

            override fun onDisplayRemoved(displayId: Int) {
                // Why: only the reader's own display turning matters.
            }
        }
        displayManager.registerDisplayListener(listener, null)
        onDispose { displayManager.unregisterDisplayListener(listener) }
    }
    return rotation
}
