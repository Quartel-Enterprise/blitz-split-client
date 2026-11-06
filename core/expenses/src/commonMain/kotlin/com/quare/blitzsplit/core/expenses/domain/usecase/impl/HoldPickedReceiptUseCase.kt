package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.PickedReceiptRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.HoldPickedReceipt

class HoldPickedReceiptUseCase(
    private val repository: PickedReceiptRepository,
) : HoldPickedReceipt {
    override fun invoke(photo: ByteArray) {
        repository.hold(photo)
    }
}
