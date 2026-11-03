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

private const val HTTP_PAYLOAD_TOO_LARGE = 413
private const val HTTP_UNPROCESSABLE = 422
private const val HTTP_TOO_MANY_REQUESTS = 429

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
            val failure = receiptReadFailureOf(exception.statusCode)
            if (failure == ReceiptReadFailure.UNREADABLE) return null
            throw ReceiptReaderException(failure, exception)
        } catch (exception: HttpRequestException) {
            throw ReceiptReaderException(ReceiptReadFailure.OFFLINE, exception)
        }
        return json.decodeFromString<ReadReceiptResponseDto>(response.bodyAsText())
    }

    private companion object {
        const val READ_RECEIPT_FUNCTION = "read-receipt"
    }
}

/*
 * Why: what read-receipt answered besides a reading. A photo with no receipt or no total (422) or too large (413) is
 * the photo's own fault, which another photo can fix; anything else isn't: the reader is down, not set up or failed.
 */
internal fun receiptReadFailureOf(statusCode: Int): ReceiptReadFailure = when (statusCode) {
    HTTP_UNPROCESSABLE, HTTP_PAYLOAD_TOO_LARGE -> ReceiptReadFailure.UNREADABLE
    HTTP_TOO_MANY_REQUESTS -> ReceiptReadFailure.LIMIT_REACHED
    else -> ReceiptReadFailure.READER_UNAVAILABLE
}
