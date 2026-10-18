package com.quare.blitzsplit.android.platform

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

// Why: Google's code scanner runs in Play services, so the app needs no camera permission.
class QrCodeScanner(
    private val context: Context,
) {
    fun scan(onScanned: (String) -> Unit) {
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        GmsBarcodeScanning
            .getClient(context, options)
            .startScan()
            .addOnSuccessListener { barcode -> barcode.rawValue?.let(onScanned) }
    }
}
