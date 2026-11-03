package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptRequestDto
import com.quare.blitzsplit.core.expenses.data.dto.ReadReceiptResponseDto
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReadFailure
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReaderException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.Functions
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

class SupabaseReceiptRemoteDataSource(
    private val functions: Functions,
) : ReceiptRemoteDataSource {
    // Why: a key the server leaves out reads as null, so it never fails the whole reading.
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun readReceipt(request: ReadReceiptRequestDto): ReadReceiptResponseDto? {
        val response = try {
            functions.invoke(
                function = READ_RECEIPT_FUNCTION,
                body = request,
            )
        } catch (exception: RestException) {
            if (exception.statusCode == HTTP_UNPROCESSABLE) return null
            // Why: anything else the server answered isn't the photo's fault: its reader is down, not set up or failed.
            val failure = if (exception.statusCode == HTTP_TOO_MANY_REQUESTS) {
                ReceiptReadFailure.LIMIT_REACHED
            } else {
                ReceiptReadFailure.READER_UNAVAILABLE
            }
            throw ReceiptReaderException(failure, exception)
        } catch (exception: HttpRequestException) {
            throw ReceiptReaderException(ReceiptReadFailure.OFFLINE, exception)
        }
        return json.decodeFromString<ReadReceiptResponseDto>(response.bodyAsText())
    }

    private companion object {
        const val READ_RECEIPT_FUNCTION = "read-receipt"
        const val HTTP_UNPROCESSABLE = 422
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
