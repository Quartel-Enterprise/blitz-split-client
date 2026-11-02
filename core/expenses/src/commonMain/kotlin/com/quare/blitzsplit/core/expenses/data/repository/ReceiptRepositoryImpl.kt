package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.data.datasource.ReceiptRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptRequestDto
import com.quare.blitzsplit.core.expenses.data.mapper.ReceiptMapper
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.core.expenses.domain.repository.ReceiptRepository
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlin.io.encoding.Base64

class ReceiptRepositoryImpl(
    private val remote: ReceiptRemoteDataSource,
    private val mapper: ReceiptMapper,
) : ReceiptRepository {
    override suspend fun readReceipt(
        jpeg: ByteArray,
        currency: GroupCurrency,
    ): Result<ReceiptReading?> = suspendRunCatching {
        val request = ReadReceiptRequestDto(
            image = Base64.encode(jpeg),
            currency = currency.code,
        )
        remote.readReceipt(request)?.let { mapper.toDomain(it, currency) }
    }
}
