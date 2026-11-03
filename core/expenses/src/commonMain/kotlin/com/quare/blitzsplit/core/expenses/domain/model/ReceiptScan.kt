package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.provider.image.BillImage

/*
 * Why: a receipt's photo once "Ler cupom fiscal" tried to read it (7.1e, 7.1g). The photo is the image the bill
 * attaches as its receipt once saved, as it was read, null when it couldn't be opened; the reading is null when nothing
 * could be read from it, offline, or when the server failed, which the form says the same way.
 */
class ReceiptScan(
    val photo: BillImage?,
    val reading: ReceiptReading?,
)
