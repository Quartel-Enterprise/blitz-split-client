package com.quare.blitzsplit.core.expenses.domain.repository

/*
 * Why: the receipt's photo Início's "Ler cupom fiscal" picked, which Split a bill reads once it opens (beta feedback,
 * part 3, item 16). A photo is too big for a route, so it waits here, taken once.
 */
interface PickedReceiptRepository {
    fun hold(photo: ByteArray)

    // Why: null when none waits; once taken, nothing does.
    fun take(): ByteArray?
}
