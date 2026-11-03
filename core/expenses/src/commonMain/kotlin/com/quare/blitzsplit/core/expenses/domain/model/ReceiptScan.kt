package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.provider.image.BillImage

// Why: a receipt's photo once "Ler cupom fiscal" tried to read it (7.1e, 7.1g).
sealed interface ReceiptScan {
    // Why: the photo is the image the bill attaches as its receipt once saved, as it was read.
    class Read(
        val photo: BillImage,
        val reading: ReceiptReading,
    ) : ReceiptScan

    class Failed(
        val failure: ReceiptReadFailure,
    ) : ReceiptScan
}
