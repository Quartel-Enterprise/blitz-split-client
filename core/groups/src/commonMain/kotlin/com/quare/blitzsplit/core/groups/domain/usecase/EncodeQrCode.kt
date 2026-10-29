package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.model.QrCode

fun interface EncodeQrCode {
    operator fun invoke(text: String): QrCode
}
