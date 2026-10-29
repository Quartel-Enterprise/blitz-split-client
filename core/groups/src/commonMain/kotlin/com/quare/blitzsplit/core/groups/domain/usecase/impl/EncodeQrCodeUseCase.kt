package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.usecase.EncodeQrCode
import com.quare.blitzsplit.core.model.QrCode
import io.github.alexzhirkevich.qrose.QroseEncoders
import io.github.alexzhirkevich.qrose.matrix.QR
import io.github.alexzhirkevich.qrose.matrix.qr.QrErrorCorrection

// Why: medium error correction, so a phone still reads the code off a scratched or glaring screen.
class EncodeQrCodeUseCase : EncodeQrCode {
    override fun invoke(text: String): QrCode {
        val matrix = QroseEncoders.QR(errorCorrection = QrErrorCorrection.M).encode(text)
        return QrCode(
            size = matrix.width,
            modules = List(matrix.width * matrix.height) { index ->
                matrix[index % matrix.width, index / matrix.width]
            },
        )
    }
}
