package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptRequestDto
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptResponseDto
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.Functions
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

class SupabaseReceiptRemoteDataSource(
    private val functions: Functions,
) : ReceiptRemoteDataSource {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun readReceipt(request: ReadReceiptRequestDto): ReadReceiptResponseDto? {
        val response = try {
            functions.invoke(
                function = READ_RECEIPT_FUNCTION,
                body = request,
            )
        } catch (exception: RestException) {
            if (exception.statusCode == HTTP_UNPROCESSABLE) return null
            throw exception
        }
        return json.decodeFromString<ReadReceiptResponseDto>(response.bodyAsText())
    }

    private companion object {
        const val READ_RECEIPT_FUNCTION = "read-receipt"
        const val HTTP_UNPROCESSABLE = 422
    }
}
