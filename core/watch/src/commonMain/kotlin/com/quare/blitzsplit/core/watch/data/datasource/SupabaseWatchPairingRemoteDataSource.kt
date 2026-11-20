package com.quare.blitzsplit.core.watch.data.datasource

import com.quare.blitzsplit.core.watch.data.dto.EmptyRequestDto
import com.quare.blitzsplit.core.watch.data.dto.WatchPairingApproveRequestDto
import com.quare.blitzsplit.core.watch.data.dto.WatchPairingCodeDto
import com.quare.blitzsplit.core.watch.data.dto.WatchPairingPollRequestDto
import com.quare.blitzsplit.core.watch.data.dto.WatchPairingPollResponseDto
import com.quare.blitzsplit.core.watch.domain.model.ExpiredWatchPairingException
import com.quare.blitzsplit.core.watch.domain.model.InvalidWatchPairingCodeException
import com.quare.blitzsplit.core.watch.domain.model.TooManyWatchPairingAttemptsException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.Functions
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

class SupabaseWatchPairingRemoteDataSource(
    private val functions: Functions,
) : WatchPairingRemoteDataSource {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun requestCode(): WatchPairingCodeDto {
        val response = mapErrors {
            functions.invoke(
                function = REQUEST_FUNCTION,
                body = EmptyRequestDto(),
            )
        }
        return json.decodeFromString<WatchPairingCodeDto>(response.bodyAsText())
    }

    override suspend fun approve(code: String) {
        mapErrors {
            functions.invoke(
                function = APPROVE_FUNCTION,
                body = WatchPairingApproveRequestDto(code = code),
            )
        }
    }

    override suspend fun fetchStatus(secret: String): WatchPairingPollResponseDto {
        val response = mapErrors {
            functions.invoke(
                function = POLL_FUNCTION,
                body = WatchPairingPollRequestDto(secret = secret),
            )
        }
        return json.decodeFromString<WatchPairingPollResponseDto>(response.bodyAsText())
    }

    private inline fun <T> mapErrors(call: () -> T): T = try {
        call()
    } catch (exception: RestException) {
        throw when (exception.statusCode) {
            HTTP_NOT_FOUND -> InvalidWatchPairingCodeException()
            HTTP_GONE -> ExpiredWatchPairingException()
            HTTP_TOO_MANY_REQUESTS -> TooManyWatchPairingAttemptsException()
            else -> exception
        }
    }

    private companion object {
        const val REQUEST_FUNCTION = "watch-pairing-request"
        const val APPROVE_FUNCTION = "watch-pairing-approve"
        const val POLL_FUNCTION = "watch-pairing-poll"
        const val HTTP_NOT_FOUND = 404
        const val HTTP_GONE = 410
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
