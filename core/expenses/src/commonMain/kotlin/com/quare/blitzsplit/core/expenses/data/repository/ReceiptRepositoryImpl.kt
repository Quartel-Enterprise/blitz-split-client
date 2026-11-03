package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.data.datasource.ReceiptRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptRequestDto
import com.quare.blitzsplit.core.expenses.data.mapper.ReceiptMapper
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.core.expenses.domain.repository.ReceiptRepository
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64

class ReceiptRepositoryImpl(
    private val remote: ReceiptRemoteDataSource,
    private val mapper: ReceiptMapper,
) : ReceiptRepository {
    override suspend fun readReceipt(
        jpeg: ByteArray,
        currency: GroupCurrency,
    ): Result<ReceiptReading?> = suspendRunCatching {
        // Why: a photo of up to 1.5 MB becomes a 2 MB string, which would drop frames on the main thread.
        val image = withContext(Dispatchers.Default) { Base64.encode(jpeg) }
        val request = ReadReceiptRequestDto(
            image = image,
            currency = currency.code,
        )
        remote.readReceipt(request)?.let { mapper.toDomain(it, currency) }
    }
}
