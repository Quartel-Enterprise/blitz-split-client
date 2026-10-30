package com.quare.blitzsplit.android.platform

import android.hardware.display.DisplayManager
import android.os.SystemClock
import android.view.Surface
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.awaitCancellation

// Why: the camera reads the same QR on every frame, so the same one is reported a few times a second, not 30.
private const val REPEAT_READ_INTERVAL_MILLIS = 250L

/*
 * Why: the invite reader draws its own camera, so a wrong QR shows its warning while the camera keeps reading, and the
 * link can be pasted under it. Every QR read is reported, the same one again a few times a second. Needs the camera
 * permission; a device without a back camera uses the front one, and one without any, or whose camera won't open
 * (another app holds it), is reported unavailable.
 */
@Composable
fun QrCameraDisplay(
    isTorchOn: Boolean,
    onScanned: (String) -> Unit,
    onTorchAvailabilityChanged: (Boolean) -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val rotation = rememberDisplayRotation()
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnTorchAvailabilityChanged by rememberUpdatedState(onTorchAvailabilityChanged)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    LaunchedEffect(context, lifecycleOwner, rotation) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        val selector = listOf(CameraSelector.DEFAULT_BACK_CAMERA, CameraSelector.DEFAULT_FRONT_CAMERA)
            .firstOrNull(provider::hasCamera)
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
        val preview = Preview.Builder().setTargetRotation(rotation).build().apply {
            setSurfaceProvider {
                surfaceRequest =
                    it
            }
        }
        val analysis = ImageAnalysis
            .Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
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
            // Why: CameraX refuses to bind when the camera is held elsewhere or can't run both uses; the paste remains.
            val bound = runCatching { provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis) }
                .onFailure { currentOnUnavailable() }
                .getOrNull()
            camera = bound?.also { currentOnTorchAvailabilityChanged(it.cameraInfo.hasFlashUnit()) }
            awaitCancellation()
        } finally {
            provider.unbind(preview, analysis)
            scanner.close()
            camera = null
            surfaceRequest = null
        }
    }
    LaunchedEffect(camera, isTorchOn) {
        camera?.let { if (it.cameraInfo.hasFlashUnit()) it.cameraControl.enableTorch(isTorchOn) }
    }
    surfaceRequest?.let { request ->
        CameraXViewfinder(
            surfaceRequest = request,
            implementationMode = ImplementationMode.EMBEDDED,
            modifier = modifier,
        )
    }
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
