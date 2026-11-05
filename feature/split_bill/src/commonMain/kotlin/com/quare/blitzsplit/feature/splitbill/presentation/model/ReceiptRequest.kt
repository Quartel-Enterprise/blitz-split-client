package com.quare.blitzsplit.feature.splitbill.presentation.model

// Why: where "Ler cupom fiscal" stands until a photo comes or it closes: asking the source, or one source open.
sealed interface ReceiptRequest {
    // Why: "Tirar foto" or the gallery's choice, asked for (beta feedback, part 2, item 7).
    data object Choice : ReceiptRequest

    // Why: the camera, the gallery or the file picker, picked on that choice or open without asking.
    data class Open(
        val source: ReceiptPhotoSource,
    ) : ReceiptRequest
}
