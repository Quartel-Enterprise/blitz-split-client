package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.core.model.QrCode

/*
 * Why: an invite's QR code (Convidar, Contatos' Convidar), black on white in both themes, as scanners read it best,
 * with the quiet zone drawn around the modules.
 */
@Composable
fun QrCodeImage(
    qrCode: QrCode,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        // Why: the blank border a scanner needs around the code, in modules.
        val quietZone = 2
        val cells = qrCode.size + quietZone * 2
        val module = size.minDimension / cells
        for (row in 0 until qrCode.size) {
            for (column in 0 until qrCode.size) {
                if (!qrCode.isDark(row, column)) continue
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(
                        x = (column + quietZone) * module,
                        y = (row + quietZone) * module,
                    ),
                    // Why: a hair wider than the module, so no seam shows between two dark ones.
                    size = Size(module + 0.5f, module + 0.5f),
                )
            }
        }
    }
}
