package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface HoldPickedReceipt {
    // Why: Início's "Ler cupom fiscal" picked the photo, which Split a bill takes as it opens; a newer one replaces it.
    operator fun invoke(photo: ByteArray)
}
