package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.PickedReceiptRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.TakePickedReceipt

class TakePickedReceiptUseCase(
    private val repository: PickedReceiptRepository,
) : TakePickedReceipt {
    override fun invoke(): ByteArray? = repository.take()
}
