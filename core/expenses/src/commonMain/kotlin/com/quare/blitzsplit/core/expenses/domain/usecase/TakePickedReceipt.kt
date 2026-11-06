package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface TakePickedReceipt {
    // Why: the photo Início picked, once; null when none waits (an app restored after the system closed it).
    operator fun invoke(): ByteArray?
}
