package com.quare.blitzsplit.android.platform

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

// Why: the camera reads the same QR on every frame, so the same one is reported a few times a second, not 30.
private const val REPEAT_READ_INTERVAL_MILLIS = 250L

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
    val currentOnScanned by rememberUpdatedState(onScanned)
    BoundCameraDisplay(
        isTorchOn = isTorchOn,
        onOpened = onOpened,
        onUnavailable = onUnavailable,
        modifier = modifier,
    ) { context, rotation ->
        val executor = ContextCompat.getMainExecutor(context)
        var lastContent: String? = null
        var lastReadAt = 0L
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
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
        CameraUses(
            uses = listOf(analysis),
            onRotation = { analysis.targetRotation = it },
            onUnbound = scanner::close,
        )
    }
}
