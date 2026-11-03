package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.data.datasource.ReceiptRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptCategoryDto
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptRequestDto
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptTermDto
import com.quare.blitzsplit.core.expenses.data.mapper.ReceiptMapper
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptCategoryHints
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
        hints: ReceiptCategoryHints,
    ): Result<ReceiptReading?> = suspendRunCatching {
        // Why: a photo of up to 1.5 MB becomes a 2 MB string, which would drop frames on the main thread.
        val image = withContext(Dispatchers.Default) { Base64.encode(jpeg) }
        val request = ReadReceiptRequestDto(
            image = image,
            currency = currency.code,
            categories = hints.groupCategories.mapNotNull { category ->
                ReadReceiptCategoryDto(
                    id = category.id,
                    name = category.name ?: return@mapNotNull null,
                    parentId = category.parentId,
                )
            },
            terms = hints.terms.map { term ->
                ReadReceiptTermDto(
                    term = term.term,
                    categoryId = term.categoryId,
                )
            },
        )
        // Why: only a category the request listed may come back as the bill's.
        val groupCategoryIds = request.categories.mapTo(mutableSetOf()) { it.id }
        remote.readReceipt(request)?.let { mapper.toDomain(it, currency, groupCategoryIds) }
    }
}
