package com.quare.blitzsplit.android.platform

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

/*
 * Why: the invite reader draws its own camera, so a wrong QR shows its warning while the camera keeps reading, and the
 * link can be pasted under it. Every QR read is reported, the same one again on each frame. Needs the camera
 * permission; a device without a back camera uses the front one, and one without any draws nothing.
 */
@Composable
fun QrCameraPreview(
    isTorchOn: Boolean,
    onScanned: (String) -> Unit,
    onTorchAvailabilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val rotation = LocalView.current.display?.rotation ?: Surface.ROTATION_0
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnTorchAvailabilityChanged by rememberUpdatedState(onTorchAvailabilityChanged)
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    LaunchedEffect(context, lifecycleOwner, rotation) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        val selector = listOf(CameraSelector.DEFAULT_BACK_CAMERA, CameraSelector.DEFAULT_FRONT_CAMERA)
            .firstOrNull(provider::hasCamera) ?: return@LaunchedEffect
        val executor = ContextCompat.getMainExecutor(context)
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
                        result
                            .getValue(scanner)
                            ?.firstNotNullOfOrNull { it.rawValue }
                            ?.let(currentOnScanned)
                    },
                )
            }
        try {
            camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis).also {
                currentOnTorchAvailabilityChanged(it.cameraInfo.hasFlashUnit())
            }
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
