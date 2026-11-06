package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.model.ReceiptPhotoSource

// Why: where "Ler cupom fiscal" stands until a photo comes or it closes: asking the source, or one source open.
sealed interface ReceiptRequest {
    // Why: "Tirar foto" or the gallery's choice, asked for (beta feedback, part 2, item 7).
    data object Choice : ReceiptRequest

    // Why: the camera, the gallery or the file picker, picked on that choice or open without asking.
    data class Open(
        val source: ReceiptPhotoSource,
    ) : ReceiptRequest

    /*
     * Why: Início's "Ler cupom fiscal" picked the photo before the form opened (beta feedback, part 3, item 16); it is
     * read once the group is settled, in that group's currency.
     */
    data object Picked : ReceiptRequest
}
