package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.provider.crashlytics.ReportableFailure
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException

/*
 * Why: what a sync that failed online tells Crashlytics: each failure's kind and the server's codes, cause by cause,
 * never their messages, which hold the request's URL and PostgREST's details (ids, names, amounts).
 */
internal class SyncFailure(
    failure: Throwable,
) : Exception(codesOf(failure)),
    ReportableFailure

// Why: a request that never got an answer, a timeout or a connection that dropped, which the device may still call online.
internal fun Throwable.isConnectionFailure(): Boolean = getCauses().any { it is HttpRequestException }

private fun codesOf(failure: Throwable): String = failure.getCauses().joinToString(separator = " ← ") { cause ->
    val name = cause::class.simpleName ?: "Throwable"
    when (cause) {
        is PostgrestRestException -> "$name ${cause.statusCode} ${cause.code}"
        is RestException -> "$name ${cause.statusCode}"
        else -> name
    }
}

private fun Throwable.getCauses(): Sequence<Throwable> = generateSequence(this) { it.cause }
