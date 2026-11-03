package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.ReceiptCategoryHints
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReadFailure
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReaderException
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
        val photo = suspendRunCatching { encodeImage(source) }.getOrNull()
            ?: return ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE)
        return repository.readReceipt(photo.jpeg, currency, hints).fold(
            onSuccess = { reading ->
                reading?.let { ReceiptScan.Read(photo, it) } ?: ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE)
            },
            // Why: a failure the data source didn't name (a response it couldn't decode) is the server's, not the photo's.
            onFailure = { error ->
                ReceiptScan.Failed((error as? ReceiptReaderException)?.failure ?: ReceiptReadFailure.READER_UNAVAILABLE)
            },
        )
    }
}
