package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.ReceiptCategoryHints
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptScan
import com.quare.blitzsplit.core.expenses.domain.repository.ReceiptRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ReadReceipt
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.provider.image.BillImageEncoder
import com.quare.blitzsplit.core.utils.suspendRunCatching

/*
 * Why: the photo goes up as the bill will keep it (upright, 1600 px, a JPEG under 1.5 MB), so what is read is what
 * everyone sees under "Anexos", and a phone's 12 MP photo never travels whole.
 */
class ReadReceiptUseCase(
    private val repository: ReceiptRepository,
    private val encodeImage: BillImageEncoder,
) : ReadReceipt {
    override suspend fun invoke(
        source: ByteArray,
        currency: GroupCurrency,
        hints: ReceiptCategoryHints,
    ): ReceiptScan {
        val photo = suspendRunCatching { encodeImage(source) }.getOrNull() ?: return ReceiptScan(null, null)
        val reading = repository.readReceipt(photo.jpeg, currency, hints).getOrNull()
        return ReceiptScan(photo, reading)
    }
}
